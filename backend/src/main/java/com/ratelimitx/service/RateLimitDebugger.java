package com.ratelimitx.service;

import com.ratelimitx.infrastructure.redis.UpstashRedisClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility to debug rate limiting issues
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitDebugger {
    
    private final UpstashRedisClient redis;
    
    /**
     * Debug a specific rate limit key
     */
    public DebugInfo debugKey(String key) {
        DebugInfo info = new DebugInfo();
        info.key = key;
        info.timestamp = System.currentTimeMillis();
        
        try {
            // Try to get the value
            String value = redis.get(key);
            info.value = value;
            info.exists = value != null && !value.equals("null");
            
            if (info.exists) {
                try {
                    info.numericValue = Long.parseLong(value);
                } catch (NumberFormatException e) {
                    info.numericValue = null;
                }
            }
            
            // Try to get hash fields if it's a hash
            try {
                String tokens = redis.hget(key, "tokens");
                String lastRefill = redis.hget(key, "last_refill");
                
                if (tokens != null && !tokens.equals("null")) {
                    info.hashFields.add("tokens=" + tokens);
                }
                if (lastRefill != null && !lastRefill.equals("null")) {
                    info.hashFields.add("last_refill=" + lastRefill);
                }
            } catch (Exception e) {
                // Not a hash, ignore
            }
            
            // Try to get sorted set cardinality
            try {
                long cardinality = redis.zcard(key);
                if (cardinality > 0) {
                    info.sortedSetCardinality = cardinality;
                }
            } catch (Exception e) {
                // Not a sorted set, ignore
            }
            
        } catch (Exception e) {
            info.error = e.getMessage();
            log.error("Error debugging key: {}", key, e);
        }
        
        return info;
    }
    
    /**
     * Simulate multiple requests and track the count
     */
    public SimulationResult simulateRequests(String key, int numRequests) {
        SimulationResult result = new SimulationResult();
        result.key = key;
        result.requestCount = numRequests;
        
        log.info("=== Starting Simulation: {} requests to key {} ===", numRequests, key);
        
        for (int i = 0; i < numRequests; i++) {
            try {
                // Get current value before increment
                String beforeValue = redis.get(key);
                long beforeCount = beforeValue != null && !beforeValue.equals("null") 
                    ? Long.parseLong(beforeValue) : 0;
                
                // Increment
                long afterCount = redis.incr(key);
                
                RequestTrace trace = new RequestTrace();
                trace.requestNum = i + 1;
                trace.beforeCount = beforeCount;
                trace.afterCount = afterCount;
                trace.expected = beforeCount + 1;
                trace.correct = (afterCount == (beforeCount + 1));
                
                result.traces.add(trace);
                
                log.info("Request #{}: Before={}, After={}, Expected={}, Correct={}", 
                    trace.requestNum, trace.beforeCount, trace.afterCount, 
                    trace.expected, trace.correct);
                
                if (!trace.correct) {
                    result.anomalyCount++;
                    log.warn("ANOMALY DETECTED at request #{}", trace.requestNum);
                }
                
                // Small delay to simulate real-world timing
                Thread.sleep(10);
                
            } catch (Exception e) {
                log.error("Error during simulation at request #{}", i + 1, e);
                result.errorCount++;
            }
        }
        
        log.info("=== Simulation Complete: {} anomalies, {} errors ===", 
            result.anomalyCount, result.errorCount);
        
        return result;
    }
    
    /**
     * Monitor a key over time
     */
    public void monitorKey(String key, int durationSeconds, int intervalMs) {
        log.info("=== Monitoring key {} for {} seconds ===", key, durationSeconds);
        
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        int iteration = 0;
        
        while (System.currentTimeMillis() < endTime) {
            try {
                iteration++;
                DebugInfo info = debugKey(key);
                
                log.info("Monitor [{}] - Key: {}, Value: {}, Exists: {}", 
                    iteration, info.key, info.value, info.exists);
                
                if (!info.hashFields.isEmpty()) {
                    log.info("  Hash Fields: {}", String.join(", ", info.hashFields));
                }
                
                if (info.sortedSetCardinality != null) {
                    log.info("  Sorted Set Size: {}", info.sortedSetCardinality);
                }
                
                Thread.sleep(intervalMs);
                
            } catch (Exception e) {
                log.error("Error monitoring key", e);
            }
        }
        
        log.info("=== Monitoring Complete ===");
    }
    
    // DTOs
    public static class DebugInfo {
        public String key;
        public long timestamp;
        public String value;
        public boolean exists;
        public Long numericValue;
        public List<String> hashFields = new ArrayList<>();
        public Long sortedSetCardinality;
        public String error;
        
        @Override
        public String toString() {
            return String.format("DebugInfo[key=%s, exists=%s, value=%s, numeric=%s, hash=%s, zset=%s, error=%s]",
                key, exists, value, numericValue, hashFields, sortedSetCardinality, error);
        }
    }
    
    public static class SimulationResult {
        public String key;
        public int requestCount;
        public int anomalyCount = 0;
        public int errorCount = 0;
        public List<RequestTrace> traces = new ArrayList<>();
    }
    
    public static class RequestTrace {
        public int requestNum;
        public long beforeCount;
        public long afterCount;
        public long expected;
        public boolean correct;
    }
}