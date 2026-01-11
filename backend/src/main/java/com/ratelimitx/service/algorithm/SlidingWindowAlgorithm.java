package com.ratelimitx.service.algorithm;

import com.ratelimitx.common.dto.RateLimitCheckResponse;
import com.ratelimitx.infrastructure.redis.UpstashRedisClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sliding Window Counter Algorithm
 * More accurate than fixed window, more performant than pure sliding window
 * Uses weighted average of current and previous windows
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlidingWindowAlgorithm {

    private final UpstashRedisClient redis;

    public RateLimitCheckResponse check(String key, int maxRequests, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowSizeMs = windowSeconds * 1000L;

        // Calculate current and previous window boundaries
        long currentWindowStart = (now / windowSizeMs) * windowSizeMs;
        long previousWindowStart = currentWindowStart - windowSizeMs;

        String currentWindowKey = key + ":" + currentWindowStart;
        String previousWindowKey = key + ":" + previousWindowStart;

        try {
            // Get counts from both windows
            String currentCountStr = redis.get(currentWindowKey);
            String previousCountStr = redis.get(previousWindowKey);

            int currentCount = currentCountStr != null && !currentCountStr.equals("null")
                    ? Integer.parseInt(currentCountStr) : 0;
            int previousCount = previousCountStr != null && !previousCountStr.equals("null")
                    ? Integer.parseInt(previousCountStr) : 0;

            // Calculate how far into the current window we are (0.0 to 1.0)
            double currentWindowProgress = (double)(now - currentWindowStart) / windowSizeMs;

            // Weighted count: more weight to previous window at start, more to current at end
            double estimatedCount = (previousCount * (1.0 - currentWindowProgress)) + currentCount;

            boolean allowed = estimatedCount < maxRequests;

            if (allowed) {
                // Increment current window
                long newCount = redis.incr(currentWindowKey);

                // Set expiration on first request in window
                if (newCount == 1) {
                    // Keep for 2 windows to support sliding calculation
                    redis.expire(currentWindowKey, windowSeconds * 2);
                }

                currentCount = (int) newCount;

                // Recalculate after increment
                estimatedCount = (previousCount * (1.0 - currentWindowProgress)) + currentCount;
            }

            int remaining = Math.max(0, (int) Math.floor(maxRequests - estimatedCount));
            long resetAt = currentWindowStart + windowSizeMs;
            int retryAfter = allowed ? 0 : (int) Math.ceil((resetAt - now) / 1000.0);

            return RateLimitCheckResponse.builder()
                    .allowed(allowed)
                    .remaining(remaining)
                    .resetAt(resetAt)
                    .retryAfter(retryAfter)
                    .build();

        } catch (Exception e) {
            log.error("Sliding window counter algorithm failed for key: {}", key, e);
            throw e;
        }
    }
}