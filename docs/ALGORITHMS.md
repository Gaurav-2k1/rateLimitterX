# Rate Limiting Algorithms - Technical Documentation

This document provides a comprehensive, code-level explanation of the three rate limiting algorithms implemented in RateLimitX.

---

## Table of Contents

1. [Overview](#overview)
2. [Token Bucket Algorithm](#token-bucket-algorithm)
3. [Sliding Window Algorithm](#sliding-window-algorithm)
4. [Fixed Window Algorithm](#fixed-window-algorithm)
5. [Algorithm Comparison](#algorithm-comparison)
6. [Implementation Details](#implementation-details)

---

## Overview

RateLimitX implements three industry-standard rate limiting algorithms, each with different characteristics suitable for various use cases. All algorithms use Redis for distributed state management, ensuring consistency across multiple application instances.

### Algorithm Selection

- **Token Bucket**: Best for smooth rate limiting with burst capacity
- **Sliding Window**: Best for precise rate limiting over time windows
- **Fixed Window**: Best for simple, low-overhead rate limiting

---

## Token Bucket Algorithm

### Concept

The Token Bucket algorithm maintains a bucket of tokens that are refilled at a constant rate. Each request consumes one token. If tokens are available, the request is allowed; otherwise, it's denied.

### Algorithm Flow

```
1. Calculate tokens to add based on elapsed time
2. Refill bucket (up to maximum capacity)
3. Check if tokens >= 1
4. If allowed: consume 1 token
5. Return response with remaining tokens
```

### Code Implementation

**Location**: `backend/src/main/java/com/ratelimitx/service/algorithm/TokenBucketAlgorithm.java`

```java
public RateLimitCheckResponse check(String key, int maxTokens, int windowSeconds) {
    long now = System.currentTimeMillis();
    
    // Get current state from Redis
    String tokensStr = redis.hget(key, "tokens");
    String lastRefillStr = redis.hget(key, "last_refill");
    
    // Initialize or retrieve current token count
    double tokens = tokensStr != null ? Double.parseDouble(tokensStr) : maxTokens;
    long lastRefill = lastRefillStr != null ? Long.parseLong(lastRefillStr) : now;
    
    // Calculate refill rate (tokens per second)
    double refillRate = (double) maxTokens / windowSeconds;
    
    // Calculate tokens to add based on elapsed time
    long elapsedMs = now - lastRefill;
    double tokensToAdd = (elapsedMs / 1000.0) * refillRate;
    
    // Refill bucket (capped at maxTokens)
    tokens = Math.min(maxTokens, tokens + tokensToAdd);
    
    // Check if request is allowed
    boolean allowed = tokens >= 1.0;
    
    if (allowed) {
        tokens -= 1.0;
        // Update state atomically in Redis
        redis.hmset(key, 
            "tokens", String.valueOf(tokens),
            "last_refill", String.valueOf(now));
        redis.expire(key, windowSeconds);
    }
    
    // Calculate reset time and retry after
    long resetAt = now + (long) ((maxTokens - tokens) / refillRate * 1000);
    int retryAfter = allowed ? 0 : (int) Math.ceil((1.0 - tokens) / refillRate);
    
    return RateLimitCheckResponse.builder()
        .allowed(allowed)
        .remaining((int) Math.max(0, Math.floor(tokens)))
        .resetAt(resetAt)
        .retryAfter(retryAfter)
        .build();
}
```

### Step-by-Step Explanation

1. **State Retrieval**: Get current token count and last refill timestamp from Redis hash
2. **Refill Calculation**: 
   - Calculate refill rate: `maxTokens / windowSeconds` (tokens per second)
   - Calculate elapsed time: `now - lastRefill`
   - Calculate tokens to add: `elapsedTime * refillRate`
3. **Bucket Refill**: Add tokens up to maximum capacity
4. **Request Check**: If `tokens >= 1`, allow request
5. **Token Consumption**: If allowed, decrement tokens by 1
6. **State Update**: Atomically update Redis with new state
7. **Response Calculation**: Calculate remaining tokens, reset time, and retry after

### Redis Data Structure

```
Key: rl:{tenantId}:{resource}:{identifier}
Type: Hash
Fields:
  - tokens: "95.5" (double as string)
  - last_refill: "1704067200000" (timestamp)
TTL: windowSeconds
```

### Characteristics

- **Burst Capacity**: Allows bursts up to `maxTokens`
- **Smooth Refill**: Continuous token refill over time
- **Memory**: O(1) per identifier
- **Precision**: High (uses double for fractional tokens)

### Example

**Configuration**: 100 tokens, 60-second window

```
Time 0s:  100 tokens → Request 1 → 99 tokens (allowed)
Time 1s:  99.67 tokens → Request 2 → 98.67 tokens (allowed)
Time 30s: 50 tokens → Request 3 → 49 tokens (allowed)
Time 60s: 100 tokens (refilled) → Request 4 → 99 tokens (allowed)
```

---

## Sliding Window Algorithm

### Concept

The Sliding Window algorithm tracks individual request timestamps within a time window. It maintains a sorted set of timestamps and removes old entries outside the window.

### Algorithm Flow

```
1. Remove timestamps outside the current window
2. Count remaining timestamps
3. If count < maxRequests: allow and add current timestamp
4. Return response with remaining capacity
```

### Code Implementation

**Location**: `backend/src/main/java/com/ratelimitx/service/algorithm/SlidingWindowAlgorithm.java`

```java
public RateLimitCheckResponse check(String key, int maxRequests, int windowSeconds) {
    long now = System.currentTimeMillis();
    long windowStart = now - (windowSeconds * 1000L);
    
    // Remove old entries outside the window
    redis.zremrangebyscore(key, 0, windowStart);
    
    // Count current requests in the window
    long count = redis.zcard(key);
    
    boolean allowed = count < maxRequests;
    
    if (allowed) {
        // Add current request timestamp as member
        redis.zadd(key, now, String.valueOf(now));
        redis.expire(key, windowSeconds);
        count++;
    }
    
    long resetAt = now + (windowSeconds * 1000L);
    
    return RateLimitCheckResponse.builder()
        .allowed(allowed)
        .remaining((int) Math.max(0, maxRequests - count))
        .resetAt(resetAt)
        .retryAfter(allowed ? 0 : windowSeconds)
        .build();
}
```

### Step-by-Step Explanation

1. **Window Calculation**: Calculate window start time: `now - windowSeconds`
2. **Cleanup**: Remove all timestamps older than window start using `ZREMRANGEBYSCORE`
3. **Count Requests**: Count remaining timestamps in the sorted set using `ZCARD`
4. **Check Limit**: If `count < maxRequests`, allow the request
5. **Record Request**: If allowed, add current timestamp to sorted set
6. **Set TTL**: Set expiration on the key to auto-cleanup
7. **Response**: Calculate remaining capacity and reset time

### Redis Data Structure

```
Key: rl:{tenantId}:{resource}:{identifier}
Type: Sorted Set (ZSET)
Members: Timestamp strings (e.g., "1704067200000")
Scores: Timestamp values (for sorting)
TTL: windowSeconds
```

### Characteristics

- **Precision**: Very high - tracks exact request times
- **No Burst at Boundaries**: Smooth transitions between windows
- **Memory**: O(n) where n = number of requests in window
- **Accuracy**: Most accurate algorithm

### Example

**Configuration**: 100 requests, 60-second window

```
Time 0s:  [] → Request 1 → [0s] (allowed, 99 remaining)
Time 1s:  [0s] → Request 2 → [0s, 1s] (allowed, 98 remaining)
Time 30s: [0s, 1s, ...] → Request 50 → [0s, 1s, ..., 30s] (allowed, 50 remaining)
Time 61s: [1s, 2s, ...] (0s removed) → Request 51 → [1s, ..., 61s] (allowed, 49 remaining)
```

---

## Fixed Window Algorithm

### Concept

The Fixed Window algorithm divides time into fixed-size windows and maintains a counter for each window. Requests increment the counter, and the window resets at fixed intervals.

### Algorithm Flow

```
1. Calculate current window start time (aligned to window boundary)
2. Get or create counter for this window
3. Increment counter
4. If count <= maxRequests: allow
5. Return response with remaining capacity
```

### Code Implementation

**Location**: `backend/src/main/java/com/ratelimitx/service/algorithm/FixedWindowAlgorithm.java`

```java
public RateLimitCheckResponse check(String key, int maxRequests, int windowSeconds) {
    long now = System.currentTimeMillis();
    
    // Calculate window start (aligned to window boundary)
    long windowStart = (now / (windowSeconds * 1000L)) * (windowSeconds * 1000L);
    
    // Create window-specific key
    String windowKey = key + ":" + windowStart;
    
    // Increment counter for this window
    long count = redis.incr(windowKey);
    
    // Set expiration on first request in window
    if (count == 1) {
        redis.expire(windowKey, windowSeconds);
    }
    
    boolean allowed = count <= maxRequests;
    long resetAt = windowStart + (windowSeconds * 1000L);
    int retryAfter = allowed ? 0 : (int) ((resetAt - now) / 1000);
    
    return RateLimitCheckResponse.builder()
        .allowed(allowed)
        .remaining((int) Math.max(0, maxRequests - count))
        .resetAt(resetAt)
        .retryAfter(retryAfter)
        .build();
}
```

### Step-by-Step Explanation

1. **Window Alignment**: Calculate window start by aligning current time to window boundary
   - Formula: `(now / windowSize) * windowSize`
2. **Key Generation**: Create window-specific key: `{baseKey}:{windowStart}`
3. **Counter Increment**: Atomically increment counter using `INCR`
4. **TTL Setup**: Set expiration on first request (only once per window)
5. **Limit Check**: If `count <= maxRequests`, allow request
6. **Response**: Calculate remaining capacity and time until reset

### Redis Data Structure

```
Key: rl:{tenantId}:{resource}:{identifier}:{windowStart}
Type: String (counter)
Value: "45" (request count)
TTL: windowSeconds
```

### Characteristics

- **Simplicity**: Simplest algorithm to implement
- **Low Memory**: O(1) per identifier per window
- **Burst at Reset**: All tokens available at window start
- **Efficiency**: Single Redis operation (INCR)

### Example

**Configuration**: 100 requests, 60-second window

```
Window 1 (0-60s):
  Time 0s:  0 → Request 1 → 1 (allowed, 99 remaining)
  Time 30s: 50 → Request 51 → 51 (allowed, 49 remaining)
  Time 59s: 99 → Request 100 → 100 (allowed, 0 remaining)
  Time 60s: 100 → Request 101 → 101 (denied, 0 remaining)

Window 2 (60-120s):
  Time 60s: 0 (new window) → Request 1 → 1 (allowed, 99 remaining)
```

**Note**: At window boundary (60s), counter resets to 0, allowing a burst of up to 100 requests.

---

## Algorithm Comparison

| Feature | Token Bucket | Sliding Window | Fixed Window |
|---------|--------------|----------------|--------------|
| **Accuracy** | High | Very High | Medium |
| **Burst Capacity** | Yes (configurable) | No | Yes (at reset) |
| **Memory Usage** | O(1) | O(n) | O(1) |
| **Complexity** | Medium | High | Low |
| **Boundary Behavior** | Smooth | Smooth | Burst |
| **Redis Operations** | 2-3 (HGET, HMSET, EXPIRE) | 3 (ZREMRANGEBYSCORE, ZCARD, ZADD) | 1-2 (INCR, EXPIRE) |
| **Best For** | Smooth traffic, bursts | Precise limits | Simple use cases |

### When to Use Each Algorithm

**Token Bucket:**
- ✅ Need burst capacity
- ✅ Want smooth traffic distribution
- ✅ Can tolerate slight inaccuracy

**Sliding Window:**
- ✅ Need precise rate limiting
- ✅ Want to avoid bursts at boundaries
- ✅ Can handle higher memory usage

**Fixed Window:**
- ✅ Simple use case
- ✅ Low memory is critical
- ✅ Burst at reset is acceptable

---

## Implementation Details

### Redis Key Patterns

All algorithms use the same base key pattern:
```
rl:{tenantId}:{resource}:{identifier}
```

**Example:**
```
rl:550e8400-e29b-41d4-a716-446655440000:api.payment.create:user123
```

### Thread Safety

All algorithms use Redis atomic operations:
- **Token Bucket**: `HMSET` (atomic hash update)
- **Sliding Window**: `ZADD`, `ZCARD` (atomic sorted set operations)
- **Fixed Window**: `INCR` (atomic increment)

### Error Handling

All algorithms implement fail-open strategy:
- If Redis is unavailable, requests are allowed
- Errors are logged but don't block requests
- This ensures service availability over strict rate limiting

### Performance Considerations

1. **Token Bucket**: 
   - 2-3 Redis operations per request
   - O(1) memory per identifier
   - Suitable for high-throughput scenarios

2. **Sliding Window**:
   - 3 Redis operations per request
   - O(n) memory where n = requests in window
   - Cleanup operation may be expensive with many requests

3. **Fixed Window**:
   - 1-2 Redis operations per request
   - O(1) memory per identifier per window
   - Most efficient for high-throughput scenarios

### Distributed System Considerations

All algorithms are designed for distributed systems:
- State stored in Redis (shared across instances)
- Atomic operations ensure consistency
- TTL prevents memory leaks
- No single point of failure (if Redis is clustered)

---

## Advanced Topics

### Custom Algorithms

RateLimitX supports custom algorithm implementations. To add a new algorithm:

1. Create a new class implementing the algorithm logic
2. Add it as a Spring `@Component`
3. Update `RateLimitService` to use the new algorithm
4. Add algorithm enum to `RateLimitRule.Algorithm`

### Algorithm Selection Strategy

The system selects algorithms based on:
1. Rule configuration (user-specified)
2. Tier limitations (ENTERPRISE allows custom algorithms)
3. Resource requirements

### Monitoring and Metrics

Each algorithm exposes metrics:
- Check count (total, allowed, denied)
- Latency (P50, P95, P99)
- Algorithm-specific metrics (token refill rate, window utilization)

---

## References

- [Token Bucket Algorithm - Wikipedia](https://en.wikipedia.org/wiki/Token_bucket)
- [Sliding Window Log - Rate Limiting Patterns](https://konghq.com/blog/how-to-design-a-scalable-rate-limiting-algorithm)
- [Fixed Window Counter - Rate Limiting Patterns](https://stripe.com/blog/rate-limiters)

---

**Last Updated**: 2024
**Version**: 1.0.0

