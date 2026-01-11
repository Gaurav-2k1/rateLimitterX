package com.ratelimitx.service.algorithm;

import com.ratelimitx.common.dto.RateLimitCheckResponse;
import com.ratelimitx.infrastructure.redis.UpstashRedisClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FixedWindowAlgorithm {

    private final UpstashRedisClient redis;

    public RateLimitCheckResponse check(String key, int maxRequests, int windowSeconds) {
        long now = System.currentTimeMillis();
        // Calculate window start (aligned to window boundary)
        long windowStart = (now / (windowSeconds * 1000L)) * (windowSeconds * 1000L);
        String windowKey = key + ":" + windowStart;

        // Lua script for atomic fixed window operations
        String luaScript = """
            local key = KEYS[1]
            local max_requests = tonumber(ARGV[1])
            local window_seconds = tonumber(ARGV[2])
            local window_start = tonumber(ARGV[3])
            local now = tonumber(ARGV[4])
            
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
                
                -- Set expiration only on first increment
                if count == 1 then
                    redis.call('EXPIRE', key, window_seconds)
                end
            else
                count = count + 1  -- For accurate remaining calculation
            end
            
            local remaining = math.max(0, max_requests - count)
            local reset_at = window_start + (window_seconds * 1000)
            local retry_after = 0
            
            if allowed == 0 then
                retry_after = math.ceil((reset_at - now) / 1000)
            end
            
            return {allowed, remaining, reset_at, retry_after}
            """;

        try {
            String result = redis.eval(
                    luaScript,
                    new String[]{windowKey},
                    String.valueOf(maxRequests),
                    String.valueOf(windowSeconds),
                    String.valueOf(windowStart),
                    String.valueOf(now)
            );

            // Parse result: [allowed, remaining, resetAt, retryAfter]
            String[] parts = result.replaceAll("[\\[\\]]", "").split(",");
            boolean allowed = Integer.parseInt(parts[0].trim()) == 1;
            int remaining = Integer.parseInt(parts[1].trim());
            long resetAt = Long.parseLong(parts[2].trim());
            int retryAfter = Integer.parseInt(parts[3].trim());

            return RateLimitCheckResponse.builder()
                    .allowed(allowed)
                    .remaining(remaining)
                    .resetAt(resetAt)
                    .retryAfter(retryAfter)
                    .build();

        } catch (Exception e) {
            log.error("Fixed window algorithm failed for key: {}", key, e);
            throw e;
        }
    }
}