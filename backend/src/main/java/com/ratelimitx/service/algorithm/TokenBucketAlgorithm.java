package com.ratelimitx.service.algorithm;

import com.ratelimitx.common.dto.RateLimitCheckResponse;
import com.ratelimitx.infrastructure.redis.UpstashRedisClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Token Bucket Algorithm using atomic Redis commands
 * Note: This has a small race condition window but is acceptable for most use cases
 * For critical applications, use Fixed Window or Sliding Window Counter instead
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenBucketAlgorithm {

    private final UpstashRedisClient redis;

    public RateLimitCheckResponse check(String key, int maxTokens, int windowSeconds) {
        long now = System.currentTimeMillis();

        try {
            // Get current state
            String tokensStr = redis.hget(key, "tokens");
            String lastRefillStr = redis.hget(key, "last_refill");

            double tokens = tokensStr != null && !tokensStr.equals("null")
                    ? Double.parseDouble(tokensStr) : maxTokens;
            long lastRefill = lastRefillStr != null && !lastRefillStr.equals("null")
                    ? Long.parseLong(lastRefillStr) : now;

            // Calculate refill rate (tokens per second)
            double refillRate = (double) maxTokens / windowSeconds;

            // Calculate tokens to add based on elapsed time
            long elapsedMs = now - lastRefill;
            double tokensToAdd = (elapsedMs / 1000.0) * refillRate;
            tokens = Math.min(maxTokens, tokens + tokensToAdd);

            // Check if request is allowed
            boolean allowed = tokens >= 1.0;

            if (allowed) {
                tokens -= 1.0;
                // Update state with new values
                redis.hset(key, "tokens", String.format("%.6f", tokens));
                redis.hset(key, "last_refill", String.valueOf(now));
                redis.expire(key, windowSeconds * 2);
            }

            int remaining = (int) Math.max(0, Math.floor(tokens));
            long tokensNeeded = (long) Math.ceil(maxTokens - tokens);
            long resetAt = now + (long) ((tokensNeeded / refillRate) * 1000);
            int retryAfter = allowed ? 0 : (int) Math.ceil((1.0 - tokens) / refillRate);

            return RateLimitCheckResponse.builder()
                    .allowed(allowed)
                    .remaining(remaining)
                    .resetAt(resetAt)
                    .retryAfter(retryAfter)
                    .build();

        } catch (Exception e) {
            log.error("Token bucket algorithm failed for key: {}", key, e);
            throw e;
        }
    }
}