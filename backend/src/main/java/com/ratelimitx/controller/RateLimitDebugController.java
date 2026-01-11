package com.ratelimitx.controller;

import com.ratelimitx.common.dto.ApiResponse;
import com.ratelimitx.common.dto.RateLimitCheckResponse;
import com.ratelimitx.service.RateLimitDebugger;
import com.ratelimitx.service.algorithm.FixedWindowAlgorithm;
import com.ratelimitx.service.algorithm.SlidingWindowAlgorithm;
import com.ratelimitx.service.algorithm.TokenBucketAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Debug controller - ONLY ENABLE IN DEV/TEST ENVIRONMENTS
 * Add @Profile("dev") or @Profile("test") to restrict to non-production
 */
@RestController
@RequestMapping("/api/v1/debug")
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "test"}) // Only available in dev/test profiles
public class RateLimitDebugController {

    private final FixedWindowAlgorithm fixedWindow;
    private final SlidingWindowAlgorithm slidingWindowCounter;
    private final TokenBucketAlgorithm tokenBucket;
    private final RateLimitDebugger debugger;

    /**
     * Debug a specific Redis key
     * GET /api/v1/debug/key?key=rl:tenant:resource
     */
    @GetMapping("/key")
    public ApiResponse<RateLimitDebugger.DebugInfo> debugKey(@RequestParam String key) {
        log.info("Debugging key: {}", key);
        RateLimitDebugger.DebugInfo info = debugger.debugKey(key);
        return ApiResponse.success(info);
    }

    /**
     * Test sequential requests to see count progression
     * POST /api/v1/debug/sequential
     */
    @PostMapping("/sequential")
    public ApiResponse<SequentialTestResult> testSequential(
            @RequestParam(defaultValue = "test:sequential") String key,
            @RequestParam(defaultValue = "10") int requests,
            @RequestParam(defaultValue = "5") int maxRequests,
            @RequestParam(defaultValue = "60") int windowSeconds,
            @RequestParam(defaultValue = "FIXED_WINDOW") AlgorithmType algorithm) {

        log.info("Running sequential test: {} requests, limit={}, window={}s, algo={}",
            requests, maxRequests, windowSeconds, algorithm);

        SequentialTestResult result = new SequentialTestResult();
        result.totalRequests = requests;
        result.maxRequests = maxRequests;
        result.windowSeconds = windowSeconds;
        result.algorithm = algorithm.name();

        for (int i = 0; i < requests; i++) {
            RateLimitCheckResponse response = executeAlgorithm(algorithm, key, maxRequests, windowSeconds);

            RequestResult reqResult = new RequestResult();
            reqResult.requestNumber = i + 1;
            reqResult.allowed = response.getAllowed();
            reqResult.remaining = response.getRemaining();
            reqResult.resetAt = response.getResetAt();
            reqResult.retryAfter = response.getRetryAfter();

            result.requests.add(reqResult);

            if (response.getAllowed()) {
                result.allowedCount++;
            } else {
                result.deniedCount++;
            }

            log.info("Request #{}: Allowed={}, Remaining={}, RetryAfter={}",
                i + 1, response.getAllowed(), response.getRemaining(), response.getRetryAfter());
        }

        result.success = (result.allowedCount == maxRequests);
        result.message = String.format("Allowed: %d/%d, Denied: %d/%d",
            result.allowedCount, maxRequests, result.deniedCount, requests - maxRequests);

        return ApiResponse.success(result);
    }

    /**
     * Test concurrent requests to detect race conditions
     * POST /api/v1/debug/concurrent
     */
    @PostMapping("/concurrent")
    public ApiResponse<ConcurrentTestResult> testConcurrent(
            @RequestParam(defaultValue = "test:concurrent") String key,
            @RequestParam(defaultValue = "50") int requests,
            @RequestParam(defaultValue = "10") int maxRequests,
            @RequestParam(defaultValue = "60") int windowSeconds,
            @RequestParam(defaultValue = "FIXED_WINDOW") AlgorithmType algorithm,
            @RequestParam(defaultValue = "10") int threads) throws Exception {

        log.info("Running concurrent test: {} requests on {} threads, limit={}, window={}s, algo={}",
            requests, threads, maxRequests, windowSeconds, algorithm);

        ConcurrentTestResult result = new ConcurrentTestResult();
        result.totalRequests = requests;
        result.maxRequests = maxRequests;
        result.windowSeconds = windowSeconds;
        result.algorithm = algorithm.name();
        result.threadCount = threads;

        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger deniedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(requests);

        List<Future<RateLimitCheckResponse>> futures = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        // Submit all requests
        for (int i = 0; i < requests; i++) {
            final int requestNum = i + 1;
            Future<RateLimitCheckResponse> future = executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for signal to start
                    RateLimitCheckResponse response = executeAlgorithm(algorithm, key, maxRequests, windowSeconds);

                    if (response.getAllowed()) {
                        allowedCount.incrementAndGet();
                    } else {
                        deniedCount.incrementAndGet();
                    }

                    return response;
                } finally {
                    doneLatch.countDown();
                }
            });
            futures.add(future);
        }

        // Start all requests at once
        startLatch.countDown();

        // Wait for completion (with timeout)
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        executor.shutdown();

        result.allowedCount = allowedCount.get();
        result.deniedCount = deniedCount.get();
        result.durationMs = endTime - startTime;
        result.completed = completed;

        // Check for race condition
        int expectedAllowed = maxRequests;
        int margin = 2; // Allow small margin for edge cases
        result.hasRaceCondition = Math.abs(result.allowedCount - expectedAllowed) > margin;

        result.success = !result.hasRaceCondition && completed;
        result.message = String.format(
            "Allowed: %d (expected ~%d), Denied: %d, Duration: %dms, Race Condition: %s",
            result.allowedCount, expectedAllowed, result.deniedCount,
            result.durationMs, result.hasRaceCondition ? "YES ⚠️" : "NO ✓"
        );

        log.info("Concurrent test result: {}", result.message);

        return ApiResponse.success(result);
    }

    /**
     * Simulate multiple sequential requests and track count changes
     * POST /api/v1/debug/simulate
     */
    @PostMapping("/simulate")
    public ApiResponse<RateLimitDebugger.SimulationResult> simulateRequests(
            @RequestParam String key,
            @RequestParam(defaultValue = "10") int requests) {

        log.info("Simulating {} requests to key: {}", requests, key);
        RateLimitDebugger.SimulationResult result = debugger.simulateRequests(key, requests);
        return ApiResponse.success(result);
    }

    /**
     * Monitor a key's value over time
     * POST /api/v1/debug/monitor
     */
    @PostMapping("/monitor")
    public ApiResponse<String> monitorKey(
            @RequestParam String key,
            @RequestParam(defaultValue = "10") int durationSeconds,
            @RequestParam(defaultValue = "1000") int intervalMs) {

        // Run in separate thread to not block response
        CompletableFuture.runAsync(() -> {
            debugger.monitorKey(key, durationSeconds, intervalMs);
        });

        return ApiResponse.success(String.format(
            "Monitoring started for key '%s' (%d seconds, %dms interval). Check logs for output.",
            key, durationSeconds, intervalMs
        ));
    }

    /**
     * Compare all algorithms side by side
     * POST /api/v1/debug/compare
     */
    @PostMapping("/compare")
    public ApiResponse<Map<String, SequentialTestResult>> compareAlgorithms(
            @RequestParam(defaultValue = "10") int requests,
            @RequestParam(defaultValue = "5") int maxRequests,
            @RequestParam(defaultValue = "60") int windowSeconds) {

        log.info("Comparing algorithms with {} requests, limit={}", requests, maxRequests);

        Map<String, SequentialTestResult> results = new HashMap<>();

        for (AlgorithmType algo : AlgorithmType.values()) {
            String key = "test:compare:" + algo.name() + ":" + System.currentTimeMillis();

            SequentialTestResult result = new SequentialTestResult();
            result.totalRequests = requests;
            result.maxRequests = maxRequests;
            result.windowSeconds = windowSeconds;
            result.algorithm = algo.name();

            for (int i = 0; i < requests; i++) {
                RateLimitCheckResponse response = executeAlgorithm(algo, key, maxRequests, windowSeconds);

                RequestResult reqResult = new RequestResult();
                reqResult.requestNumber = i + 1;
                reqResult.allowed = response.getAllowed();
                reqResult.remaining = response.getRemaining();

                result.requests.add(reqResult);

                if (response.getAllowed()) {
                    result.allowedCount++;
                } else {
                    result.deniedCount++;
                }
            }

            result.success = (result.allowedCount == maxRequests);
            result.message = String.format("Allowed: %d/%d", result.allowedCount, maxRequests);

            results.put(algo.name(), result);
        }

        return ApiResponse.success(results);
    }

    private RateLimitCheckResponse executeAlgorithm(AlgorithmType type, String key,
                                                     int maxRequests, int windowSeconds) {
        return switch (type) {
            case FIXED_WINDOW -> fixedWindow.check(key, maxRequests, windowSeconds);
            case SLIDING_WINDOW -> slidingWindowCounter.check(key, maxRequests, windowSeconds);
            case TOKEN_BUCKET -> tokenBucket.check(key, maxRequests, windowSeconds);
        };
    }

    // Enums and DTOs
    public enum AlgorithmType {
        FIXED_WINDOW,
        SLIDING_WINDOW,
        TOKEN_BUCKET
    }

    public static class SequentialTestResult {
        public int totalRequests;
        public int maxRequests;
        public int windowSeconds;
        public String algorithm;
        public int allowedCount = 0;
        public int deniedCount = 0;
        public boolean success;
        public String message;
        public List<RequestResult> requests = new ArrayList<>();
    }

    public static class ConcurrentTestResult {
        public int totalRequests;
        public int maxRequests;
        public int windowSeconds;
        public String algorithm;
        public int threadCount;
        public int allowedCount;
        public int deniedCount;
        public long durationMs;
        public boolean completed;
        public boolean hasRaceCondition;
        public boolean success;
        public String message;
    }

    public static class RequestResult {
        public int requestNumber;
        public boolean allowed;
        public int remaining;
        public Long resetAt;
        public Integer retryAfter;
    }
}