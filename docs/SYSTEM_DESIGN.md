# RateLimitX - System Design Document

This document provides a comprehensive system design overview of RateLimitX, including architecture decisions, scalability considerations, and design patterns.

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture Patterns](#architecture-patterns)
3. [Data Flow](#data-flow)
4. [Scalability Design](#scalability-design)
5. [Reliability & Fault Tolerance](#reliability--fault-tolerance)
6. [Security Design](#security-design)

---

## System Overview

### Problem Statement

RateLimitX solves the problem of implementing distributed rate limiting across multiple services and applications. It provides:

- **Centralized Rate Limiting**: Single source of truth for rate limits
- **Multi-Tenant Support**: Isolated rate limits per tenant
- **Multiple Algorithms**: Choose the best algorithm for your use case
- **Easy Integration**: REST API and SDKs for multiple languages

### System Requirements

**Functional Requirements:**
- Rate limit checking via REST API
- Rule management (CRUD operations)
- Multi-tenant isolation
- Tier-based feature access
- Real-time analytics
- Alerting system

**Non-Functional Requirements:**
- **Performance**: <50ms P95 latency for rate limit checks
- **Scalability**: Support 10,000+ RPS per instance
- **Availability**: 99.95% uptime SLA
- **Consistency**: Strong consistency for rate limit state
- **Security**: API key authentication, JWT for management

---

## Architecture Patterns

### 1. Multi-Tenant Architecture

**Pattern**: Database-per-tenant isolation with shared infrastructure

```
┌─────────────────────────────────────────────────────────┐
│              Shared Infrastructure                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │   Tenant A   │  │   Tenant B   │  │   Tenant C   │ │
│  │              │  │              │  │              │ │
│  │ Rules: 5     │  │ Rules: 10    │  │ Rules: 20    │ │
│  │ Tier: FREE   │  │ Tier: PRO    │  │ Tier: ENTER  │ │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘ │
│         │                 │                 │          │
│         └─────────────────┼─────────────────┘          │
│                           │                            │
│                           ▼                            │
│              ┌──────────────────────┐                 │
│              │   Shared Services    │                 │
│              │  • PostgreSQL        │                 │
│              │  • Redis             │                 │
│              │  • Application       │                 │
│              └──────────────────────┘                 │
└─────────────────────────────────────────────────────────┘
```

**Isolation Strategy:**
- **Data Isolation**: All queries filtered by `tenant_id`
- **Key Isolation**: Redis keys prefixed with `rl:{tenantId}:`
- **Resource Isolation**: Separate API keys per tenant

### 2. Strategy Pattern for Algorithms

```java
// Algorithm interface (conceptual)
interface RateLimitAlgorithm {
    RateLimitCheckResponse check(String key, int maxRequests, int windowSeconds);
}

// Concrete implementations
class TokenBucketAlgorithm implements RateLimitAlgorithm { ... }
class SlidingWindowAlgorithm implements RateLimitAlgorithm { ... }
class FixedWindowAlgorithm implements RateLimitAlgorithm { ... }

// Context
class RateLimitService {
    private Map<Algorithm, RateLimitAlgorithm> algorithms;
    
    public RateLimitCheckResponse check(...) {
        RateLimitAlgorithm algorithm = algorithms.get(rule.getAlgorithm());
        return algorithm.check(key, maxRequests, windowSeconds);
    }
}
```

**Benefits:**
- Easy to add new algorithms
- Algorithm selection at runtime
- Testable in isolation

### 3. Repository Pattern

```java
interface RateLimitRuleRepository extends JpaRepository<RateLimitRule, UUID> {
    List<RateLimitRule> findByTenantId(UUID tenantId);
    List<RateLimitRule> findByTenantIdAndActive(UUID tenantId, boolean active);
}
```

**Benefits:**
- Abstraction over data access
- Easy to mock for testing
- Consistent data access patterns

### 4. Service Layer Pattern

```
Controller → Service → Repository → Database
           ↓
        Algorithm → Redis
```

**Benefits:**
- Separation of concerns
- Reusable business logic
- Transaction management

---

## Data Flow

### Complete Request Flow

```
┌──────────────────────────────────────────────────────────────┐
│                    Client Request                            │
│  POST /api/v1/check                                           │
│  Headers: X-API-Key: rlx_xxx                                 │
│  Body: {identifier, resource, tokens}                       │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│             1. API Gateway Layer                             │
│  • Extract API key from header                              │
│  • Validate request format                                   │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│        2. Rate Limit API Protection                          │
│  • Check if API itself is rate limited                       │
│  • Limit: 1000 requests/minute per API key                  │
│  • Redis Key: rl:api:{apiKeyHash}                            │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│        3. Authentication & Authorization                     │
│  • Validate API key hash                                     │
│  • Get tenant ID                                             │
│  • Check API key is active                                   │
│  • Update last used timestamp                                │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│        4. Tier Validation                                    │
│  • Check monthly usage count                                 │
│  • Validate against tier limits                              │
│  • FREE: 10k/month, PRO: 1M/month, ENTERPRISE: unlimited   │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│        5. Rule Resolution                                    │
│  • Query active rules for tenant                             │
│  • Filter by resource and identifier                         │
│  • Apply scope (GLOBAL, RESOURCE, IDENTIFIER)                │
│  • Select most restrictive rule (priority + limit)          │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│        6. Algorithm Execution                                │
│  • Get algorithm type from rule                              │
│  • Execute algorithm-specific logic                          │
│  • Update Redis state                                        │
│  • Return allowed/denied decision                           │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│        7. Metrics & Observability                            │
│  • Record metric (async)                                     │
│  • Update usage counter                                      │
│  • Calculate latency                                          │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│        8. Alert Checking                                    │
│  • Check if usage threshold reached                          │
│  • Send alerts (webhook, Slack, Discord) if needed           │
│  • Async processing                                           │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│        9. Response Building                                  │
│  • Build RateLimitCheckResponse                              │
│  • Set HTTP headers (X-RateLimit-*)                          │
│  • Return 200 OK or 429 Too Many Requests                    │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
                    [Response]
```

### Data Flow - Rule Creation

```
User → Dashboard → POST /rules → RuleController
                                      │
                                      ▼
                              TierService.validateRuleCreation()
                                      │
                                      ▼
                              Check tier limits (FREE: 1 rule)
                                      │
                                      ▼
                              RateLimitRuleRepository.save()
                                      │
                                      ▼
                              PostgreSQL INSERT
                                      │
                                      ▼
                              Return created rule
```

### Data Flow - Analytics Query

```
User → Dashboard → GET /analytics/realtime → AnalyticsController
                                                    │
                                                    ▼
                                          UsageMetricRepository
                                                    │
                                                    ▼
                                          PostgreSQL Query
                                          SELECT COUNT(*), 
                                                 SUM(checks_denied),
                                                 AVG(latency_ms)
                                          FROM usage_metrics
                                          WHERE tenant_id = ? 
                                            AND timestamp > NOW() - INTERVAL '1 hour'
                                                    │
                                                    ▼
                                          Calculate percentiles
                                                    │
                                                    ▼
                                          Return analytics data
```

---

## Scalability Design

### Horizontal Scaling

```
                    ┌─────────────┐
                    │ Load Balancer│
                    └──────┬───────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
   ┌─────────┐       ┌─────────┐       ┌─────────┐
   │Instance │       │Instance │       │Instance │
   │    1    │       │    2    │       │    N    │
   └────┬────┘       └────┬────┘       └────┬────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │   Redis     │
                    │  (Shared)   │
                    └─────────────┘
```

**Scaling Strategy:**
- **Stateless Application**: All instances share Redis state
- **Database Connection Pooling**: Each instance has its own connection pool
- **Session Affinity**: Not required (stateless)

### Redis Scaling

**Current**: Upstash REST API (managed)
**Production**: Redis Cluster

```
┌─────────────────────────────────────────┐
│         Redis Cluster                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐│
│  │  Node 1  │  │  Node 2  │  │  Node 3  ││
│  │ (Master) │  │ (Master) │  │ (Master) ││
│  └────┬─────┘  └────┬─────┘  └────┬─────┘│
│       │            │            │        │
│       └────────────┼────────────┘        │
│                    │                     │
│              ┌─────▼─────┐               │
│              │ Replicas  │               │
│              └───────────┘               │
└─────────────────────────────────────────┘
```

**Key Distribution:**
- Keys distributed across cluster using consistent hashing
- Each key maps to a specific node
- Automatic failover if node fails

### Database Scaling

**Read Replicas:**
```
Primary (Write) ──┐
                  │
                  ├──> Replica 1 (Read)
                  │
                  └──> Replica 2 (Read)
```

**Sharding Strategy (Future):**
- Shard by `tenant_id` hash
- Each shard handles subset of tenants
- Application routes queries to correct shard

---

## Reliability & Fault Tolerance

### Fail-Open Strategy

```java
try {
    RateLimitCheckResponse response = rateLimitService.check(tenantId, request);
    return response;
} catch (Exception e) {
    log.error("Rate limit check failed", e);
    // Fail open - allow request
    return RateLimitCheckResponse.builder()
        .allowed(true)
        .remaining(999)
        .resetAt(System.currentTimeMillis() + 3600000)
        .retryAfter(0)
        .build();
}
```

**Rationale:**
- Better to allow requests than block legitimate traffic
- Prevents cascading failures
- Can be configured per tenant (future)

### Circuit Breaker Pattern

**Future Implementation:**
```java
@CircuitBreaker(name = "redis", fallbackMethod = "fallbackCheck")
public RateLimitCheckResponse check(...) {
    // Redis operation
}

public RateLimitCheckResponse fallbackCheck(...) {
    // Return allowed when circuit is open
}
```

### Retry Strategy

**For Redis Operations:**
- Exponential backoff: 100ms, 200ms, 400ms
- Max 3 retries
- Only retry on transient errors

### Health Checks

```java
@GetMapping("/actuator/health")
public Health health() {
    // Check Redis connectivity
    // Check Database connectivity
    // Return UP/DOWN status
}
```

---

## Security Design

### Authentication Flow

```
┌─────────────────────────────────────────────────┐
│            Authentication Layers                │
└─────────────────────────────────────────────────┘

1. API Key Authentication (Rate Limit API)
   │
   ├─> Extract X-API-Key header
   ├─> Hash API key (SHA-256)
   ├─> Lookup in database
   └─> Validate active status

2. JWT Authentication (Management API)
   │
   ├─> Extract Authorization: Bearer token
   ├─> Validate JWT signature
   ├─> Check expiration
   └─> Extract tenant ID and email
```

### API Key Security

**Storage:**
- Never store plain API keys
- Hash with SHA-256 before storage
- Only return plain key once (on creation)

**Validation:**
```java
String keyHash = hashApiKey(apiKey);
ApiKey stored = apiKeyRepository.findByKeyHash(keyHash);
if (stored == null || !stored.getActive()) {
    throw new RuntimeException("Invalid API key");
}
```

### JWT Security

**Token Structure:**
```json
{
  "tenantId": "uuid",
  "email": "user@example.com",
  "exp": 1234567890,
  "iat": 1234567890
}
```

**Security Measures:**
- HS256 signing with secret key (min 32 chars)
- Short expiration (1 hour for access token)
- Refresh token for long-lived sessions
- Token rotation on refresh

### Rate Limiting Security

**API Self-Protection:**
- Rate limit the rate limit API itself
- Prevents abuse and DoS attacks
- 1000 requests/minute per API key

**Input Validation:**
- Jakarta Validation on all DTOs
- Sanitize all inputs
- Reject malformed requests

---

## Performance Optimization

### Caching Strategy

**Rule Caching:**
```java
@Cacheable(value = "rules", key = "#tenantId")
public List<RateLimitRule> getRules(UUID tenantId) {
    return ruleRepository.findByTenantId(tenantId);
}
```

**Cache Invalidation:**
- On rule create/update/delete
- TTL: 5 minutes

### Database Optimization

**Indexes:**
```sql
CREATE INDEX idx_rules_tenant_active ON rate_limit_rules(tenant_id, active);
CREATE INDEX idx_api_keys_hash ON api_keys(key_hash);
CREATE INDEX idx_metrics_tenant_time ON usage_metrics(tenant_id, timestamp);
```

**Query Optimization:**
- Use `EXPLAIN` to analyze queries
- Avoid N+1 queries
- Use batch operations where possible

### Redis Optimization

**Connection Pooling:**
- Reuse connections
- Pool size: 10-20 connections per instance

**Pipeline Operations:**
- Batch multiple Redis operations
- Reduce round trips

**Key Expiration:**
- Set TTL on all keys
- Automatic cleanup of expired keys

---

## Monitoring & Observability

### Metrics

**Application Metrics:**
- `ratelimitx_checks_total`: Total checks
- `ratelimitx_checks_allowed_total`: Allowed checks
- `ratelimitx_checks_denied_total`: Denied checks
- `ratelimitx_check_latency_ms`: Check latency histogram

**Business Metrics:**
- Active tenants
- Rules per tenant
- API keys per tenant
- Usage by tier

### Logging

**Structured Logging:**
```json
{
  "timestamp": "2024-01-01T00:00:00Z",
  "level": "INFO",
  "service": "ratelimitx",
  "tenantId": "uuid",
  "message": "Rate limit check",
  "allowed": true,
  "resource": "api.payment.create"
}
```

### Tracing

**Future: OpenTelemetry Integration**
- Distributed tracing across services
- Trace ID propagation
- Performance analysis

---

## Future Enhancements

### Planned Features

1. **Multi-Region Deployment**
   - Redis replication across regions
   - Geographic routing
   - Regional data residency

2. **Advanced Analytics**
   - Time-series database (TimescaleDB)
   - Real-time dashboards
   - Predictive analytics

3. **Custom Algorithms**
   - User-defined algorithms (ENTERPRISE)
   - Plugin system
   - Algorithm marketplace

4. **Rate Limit Testing**
   - Load testing tools
   - Simulation mode
   - Performance benchmarking

---

**Last Updated**: 2024
**Version**: 1.0.0

