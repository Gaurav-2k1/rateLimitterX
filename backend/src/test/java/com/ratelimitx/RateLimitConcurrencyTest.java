package com.ratelimitx;

import com.ratelimitx.common.dto.RateLimitCheckResponse;
import com.ratelimitx.service.algorithm.FixedWindowAlgorithm;
import com.ratelimitx.service.algorithm.SlidingWindowAlgorithm;
import com.ratelimitx.service.algorithm.TokenBucketAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test concurrent requests to verify atomicity of rate limiting algorithms
 */
@SpringBootTest
@Slf4j
public class RateLimitConcurrencyTest {
    
    @Autowired
    private FixedWindowAlgorithm fixedWindow;
    
    @Autowired
    private SlidingWindowAlgorithm slidingWindowCounter;
    
    @Autowired
    private TokenBucketAlgorithm tokenBucket;
    
    @Test
    public void testFixedWindowConcurrency() throws Exception {
        String testKey = "test:fixed:" + System.currentTimeMillis();
        int maxRequests = 10;
        int windowSeconds = 60;
        int concurrentRequests = 50;
        
        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger deniedCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(concurrentRequests);
        
        List<Future<RateLimitCheckResponse>> futures = new ArrayList<>();
        
        // Submit concurrent requests
        for (int i = 0; i < concurrentRequests; i++) {
            Future<RateLimitCheckResponse> future = executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await(); // Wait for all threads to be ready
                    return fixedWindow.check(testKey, maxRequests, windowSeconds);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }
        
        // Collect results
        for (Future<RateLimitCheckResponse> future : futures) {
            RateLimitCheckResponse response = future.get();
            if (response.getAllowed()) {
                allowedCount.incrementAndGet();
            } else {
                deniedCount.incrementAndGet();
            }
        }
        
        executor.shutdown();
        
        log.info("Fixed Window Results: Allowed={}, Denied={}", allowedCount.get(), deniedCount.get());
        
        // Verify: exactly maxRequests should be allowed
        // Allow small margin for edge cases (±2)
        assertTrue(Math.abs(allowedCount.get() - maxRequests) <= 2, 
            String.format("Expected ~%d allowed, got %d", maxRequests, allowedCount.get()));
        assertEquals(concurrentRequests, allowedCount.get() + deniedCount.get());
    }
    
    @Test
    public void testSlidingWindowCounterConcurrency() throws Exception {
        String testKey = "test:sliding:" + System.currentTimeMillis();
        int maxRequests = 10;
        int windowSeconds = 60;
        int concurrentRequests = 50;
        
        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger deniedCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(concurrentRequests);
        
        List<Future<RateLimitCheckResponse>> futures = new ArrayList<>();
        
        for (int i = 0; i < concurrentRequests; i++) {
            Future<RateLimitCheckResponse> future = executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();
                    return slidingWindowCounter.check(testKey, maxRequests, windowSeconds);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }
        
        for (Future<RateLimitCheckResponse> future : futures) {
            RateLimitCheckResponse response = future.get();
            if (response.getAllowed()) {
                allowedCount.incrementAndGet();
            } else {
                deniedCount.incrementAndGet();
            }
        }
        
        executor.shutdown();
        
        log.info("Sliding Window Counter Results: Allowed={}, Denied={}", 
            allowedCount.get(), deniedCount.get());
        
        // Sliding window counter has more margin due to weighted calculation
        assertTrue(Math.abs(allowedCount.get() - maxRequests) <= 3, 
            String.format("Expected ~%d allowed, got %d", maxRequests, allowedCount.get()));
        assertEquals(concurrentRequests, allowedCount.get() + deniedCount.get());
    }
    
    @Test
    public void testSequentialRequests() throws Exception {
        String testKey = "test:sequential:" + System.currentTimeMillis();
        int maxRequests = 5;
        int windowSeconds = 60;
        
        int allowedCount = 0;
        int deniedCount = 0;
        
        // Make sequential requests
        for (int i = 0; i < 10; i++) {
            RateLimitCheckResponse response = fixedWindow.check(testKey, maxRequests, windowSeconds);
            
            if (response.getAllowed()) {
                allowedCount++;
                log.info("Request {} - ALLOWED (Remaining: {})", i + 1, response.getRemaining());
            } else {
                deniedCount++;
                log.info("Request {} - DENIED (Retry After: {}s)", i + 1, response.getRetryAfter());
            }
        }
        
        log.info("Sequential Test Results: Allowed={}, Denied={}", allowedCount, deniedCount);
        
        assertEquals(maxRequests, allowedCount, "Exactly maxRequests should be allowed");
        assertEquals(5, deniedCount, "Remaining requests should be denied");
    }
    
    @Test
    public void testRemainingCountAccuracy() throws Exception {
        String testKey = "test:remaining:" + System.currentTimeMillis();
        int maxRequests = 5;
        int windowSeconds = 60;
        
        for (int i = 0; i < maxRequests; i++) {
            RateLimitCheckResponse response = fixedWindow.check(testKey, maxRequests, windowSeconds);
            
            assertTrue(response.getAllowed(), "Request " + (i + 1) + " should be allowed");
            int expectedRemaining = maxRequests - (i + 1);
            assertEquals(expectedRemaining, response.getRemaining(), 
                String.format("After request %d, remaining should be %d but got %d", 
                    i + 1, expectedRemaining, response.getRemaining()));
            
            log.info("Request {}: Allowed={}, Remaining={}", 
                i + 1, response.getAllowed(), response.getRemaining());
        }
        
        // Next request should be denied
        RateLimitCheckResponse deniedResponse = fixedWindow.check(testKey, maxRequests, windowSeconds);
        assertFalse(deniedResponse.getAllowed(), "Request after limit should be denied");
        assertEquals(0, deniedResponse.getRemaining(), "Remaining should be 0 when denied");
    }
}