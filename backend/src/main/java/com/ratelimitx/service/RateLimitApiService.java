package com.ratelimitx.service;

import com.ratelimitx.infrastructure.redis.UpstashRedisClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service to rate limit the rate limit API itself to prevent abuse
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitApiService {

    private final UpstashRedisClient redis;

    @Value("${ratelimit.api.max-requests:1000}")
    private int apiRateLimit;

    @Value("${ratelimit.api.window-seconds:60}")
    private int apiWindowSeconds;

    @Value("${ratelimit.api.fail-open:true}")
    private boolean failOpen;

    // Local cache to reduce Redis load for repeat offenders
    private final ConcurrentHashMap<String, Long> deniedUntilCache = new ConcurrentHashMap<>();

    // Circuit breaker for Redis failures
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private static final int FAILURE_THRESHOLD = 3;

    public boolean isAllowed(String apiKey) {
        // Input validation
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Empty API key provided to rate limiter");
            return false;
        }

        // Sanitize API key to prevent Redis key injection
        String sanitizedApiKey = sanitizeApiKey(apiKey);

        // Check local cache first - if recently denied, skip Redis call
        Long deniedUntil = deniedUntilCache.get(sanitizedApiKey);
        if (deniedUntil != null && System.currentTimeMillis() < deniedUntil) {
            return false;
        }

        try {
            // Use Lua script for atomic operation
            String luaScript = """
                local key = KEYS[1]
                local max_requests = tonumber(ARGV[1])
                local window_seconds = tonumber(ARGV[2])
                local now = tonumber(ARGV[3])
                local window_start = tonumber(ARGV[4])
                
                -- Get current count
                local count = redis.call('GET', key)
                
                if not count then
                    count = 0
                else
                    count = tonumber(count)
                end
                
                local allowed = 0
                if count < max_requests then
                    allowed = 1
                    count = redis.call('INCR', key)
                    
                    -- Set expiration on first request
                    if count == 1 then
                        redis.call('EXPIRE', key, window_seconds)
                    end
                end
                
                local remaining = math.max(0, max_requests - count)
                local reset_at = window_start + (window_seconds * 1000)
                
                return {allowed, remaining, reset_at, count}
                """;

            long now = System.currentTimeMillis();
            long windowStart = (now / (apiWindowSeconds * 1000L)) * (apiWindowSeconds * 1000L);
            String windowKey = buildRedisKey(sanitizedApiKey, windowStart);

            String result = redis.eval(
                    luaScript,
                    new String[]{windowKey},
                    String.valueOf(apiRateLimit),
                    String.valueOf(apiWindowSeconds),
                    String.valueOf(now),
                    String.valueOf(windowStart)
            );

            // Parse result: [allowed, remaining, resetAt, count]
            String[] parts = result.replaceAll("[\\[\\]]", "").split(",");
            boolean allowed = Integer.parseInt(parts[0].trim()) == 1;
            int remaining = Integer.parseInt(parts[1].trim());
            long resetAt = Long.parseLong(parts[2].trim());
            int currentCount = Integer.parseInt(parts[3].trim());

            // Reset failure counter on success
            consecutiveFailures.set(0);

            // If denied, cache the denial locally to reduce Redis load
            if (!allowed) {
                deniedUntilCache.put(sanitizedApiKey, resetAt);
                log.warn("API rate limit exceeded for key: {} (count: {}/{})",
                        maskApiKey(apiKey), currentCount, apiRateLimit);
            }

            // Clean up cache periodically (only on allowed requests to avoid overhead)
            if (allowed && deniedUntilCache.size() > 1000) {
                cleanupDeniedCache();
            }

            return allowed;

        } catch (Exception e) {
            int failures = consecutiveFailures.incrementAndGet();

            if (failures >= FAILURE_THRESHOLD) {
                log.error("API rate limiter Redis failure threshold reached ({})", failures, e);
            } else {
                log.error("Error checking API rate limit (failure {}/{})", failures, FAILURE_THRESHOLD, e);
            }

            // Fail based on configured strategy
            if (failOpen) {
                log.warn("Failing open - allowing request despite rate limit check failure");
                return true;
            } else {
                log.warn("Failing closed - denying request due to rate limit check failure");
                return false;
            }
        }
    }

    /**
     * Sanitize API key to prevent Redis key injection
     */
    private String sanitizeApiKey(String apiKey) {
        // Remove any characters that could be used for injection
        return apiKey.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    /**
     * Build Redis key with proper namespacing
     */
    private String buildRedisKey(String sanitizedApiKey, long windowStart) {
        return String.format("api_rl:%s:%d", sanitizedApiKey, windowStart);
    }

    /**
     * Mask API key for logging (show first 8 chars only)
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 8) + "***";
    }

    /**
     * Clean up expired entries from the denied cache
     */
    private void cleanupDeniedCache() {
        try {
            long now = System.currentTimeMillis();
            deniedUntilCache.entrySet().removeIf(entry -> entry.getValue() < now);
            log.debug("Cleaned up denied cache, current size: {}", deniedUntilCache.size());
        } catch (Exception e) {
            log.error("Error cleaning up denied cache", e);
        }
    }

    /**
     * Get current usage for an API key (for monitoring/debugging)
     */
    public ApiUsageInfo getUsageInfo(String apiKey) {
        try {
            String sanitizedApiKey = sanitizeApiKey(apiKey);
            long now = System.currentTimeMillis();
            long windowStart = (now / (apiWindowSeconds * 1000L)) * (apiWindowSeconds * 1000L);
            String windowKey = buildRedisKey(sanitizedApiKey, windowStart);

            String countStr = redis.get(windowKey);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;

            return new ApiUsageInfo(
                    currentCount,
                    apiRateLimit,
                    Math.max(0, apiRateLimit - currentCount),
                    windowStart + (apiWindowSeconds * 1000L)
            );
        } catch (Exception e) {
            log.error("Error getting usage info", e);
            return null;
        }
    }

    /**
     * DTO for API usage information
     */
    public record ApiUsageInfo(
            int currentCount,
            int limit,
            int remaining,
            long resetAt
    ) {}
}