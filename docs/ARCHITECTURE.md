# RateLimitX - Architecture Documentation

This document contains UML diagrams, system design flows, and architectural documentation for RateLimitX.

---

## Table of Contents

1. [System Architecture Overview](#system-architecture-overview)
2. [Class Diagram](#class-diagram)
3. [Sequence Diagrams](#sequence-diagrams)
4. [System Design Flow](#system-design-flow)
5. [Component Diagram](#component-diagram)

---

## System Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │ Node.js  │  │  Python  │  │   Java   │  │    Go    │      │
│  │   SDK    │  │   SDK    │  │   SDK    │  │   SDK    │      │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘      │
│       │             │             │             │             │
│       └─────────────┴─────────────┴─────────────┘             │
│                          │                                      │
│                          │ HTTPS                                │
└──────────────────────────┼──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    API Gateway Layer                             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         Spring Boot REST API (Port 8080)                 │  │
│  │  • Rate Limit Check Endpoint                            │  │
│  │  • Management Endpoints (Rules, API Keys, Analytics)   │  │
│  │  • Authentication & Authorization                       │  │
│  └──────────────────────────────────────────────────────────┘  │
└───────────────────────────┬────────────────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Service    │    │   Service    │    │   Service    │
│    Layer     │    │    Layer     │    │    Layer     │
│              │    │              │    │              │
│ • RateLimit  │    │ • Auth       │    │ • Analytics  │
│ • Algorithm  │    │ • API Key    │    │ • Metrics    │
│ • Tier       │    │ • Rule       │    │ • Alert      │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Data Layer                                  │
│  ┌──────────────────────┐      ┌──────────────────────┐       │
│  │    PostgreSQL        │      │    Redis (Upstash)    │       │
│  │                      │      │                      │       │
│  │ • Tenants            │      │ • Rate Limit State   │       │
│  │ • Rules              │      │ • Counters           │       │
│  │ • API Keys           │      │ • Token Buckets      │       │
│  │ • Metrics            │      │ • Sliding Windows    │       │
│  │ • Alerts             │      │ • Fixed Windows      │       │
│  └──────────────────────┘      └──────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Class Diagram

### Backend Class Structure

```mermaid
classDiagram
    class RateLimitXApplication {
        +main(String[] args)
    }
    
    class RateLimitController {
        -RateLimitService rateLimitService
        -ApiKeyService apiKeyService
        -RateLimitApiService rateLimitApiService
        +check(String apiKey, RateLimitCheckRequest request) RateLimitCheckResponse
    }
    
    class RateLimitService {
        -RateLimitRuleRepository ruleRepository
        -TokenBucketAlgorithm tokenBucket
        -SlidingWindowAlgorithm slidingWindow
        -FixedWindowAlgorithm fixedWindow
        -MetricsService metricsService
        -TierService tierService
        -AlertService alertService
        +check(UUID tenantId, RateLimitCheckRequest request) RateLimitCheckResponse
    }
    
    class TokenBucketAlgorithm {
        -UpstashRedisClient redis
        +check(String key, int maxTokens, int windowSeconds) RateLimitCheckResponse
    }
    
    class SlidingWindowAlgorithm {
        -UpstashRedisClient redis
        +check(String key, int maxRequests, int windowSeconds) RateLimitCheckResponse
    }
    
    class FixedWindowAlgorithm {
        -UpstashRedisClient redis
        +check(String key, int maxRequests, int windowSeconds) RateLimitCheckResponse
    }
    
    class AuthService {
        -TenantRepository tenantRepository
        -ApiKeyService apiKeyService
        -PasswordEncoder passwordEncoder
        -JwtTokenProvider jwtTokenProvider
        +register(String email, String password) Map~String,Object~
        +login(String email, String password) Map~String,Object~
        +validateToken(String token) Claims
    }
    
    class ApiKeyService {
        -ApiKeyRepository apiKeyRepository
        +validateAndGetTenant(String apiKey) UUID
        +createApiKey(UUID tenantId, String name, String environment) ApiKey
        +rotateApiKey(UUID keyId, UUID tenantId) String
        +hashApiKey(String apiKey) String
    }
    
    class TierService {
        -TenantRepository tenantRepository
        -RateLimitRuleRepository ruleRepository
        -UsageMetricRepository usageMetricRepository
        +validateRuleCreation(UUID tenantId) void
        +validateCheckRequest(UUID tenantId) void
        +getRemainingChecksThisMonth(UUID tenantId) int
    }
    
    class MetricsService {
        -UsageMetricRepository metricsRepository
        +recordCheck(UUID tenantId, String identifier, String resource, boolean allowed, int latencyMs) void
    }
    
    class AlertService {
        -AlertConfigurationRepository alertRepository
        +checkAndSendAlerts(UUID tenantId, int usagePercent) void
        +sendWebhookAlert(String url, Map data) void
    }
    
    class RateLimitRule {
        -UUID id
        -UUID tenantId
        -String resource
        -Algorithm algorithm
        -Integer maxRequests
        -Integer windowSeconds
        -Integer burstCapacity
        -IdentifierType identifierType
        -LimitScope limitScope
        -Integer priority
        -Boolean active
    }
    
    class Tenant {
        -UUID id
        -String email
        -String passwordHash
        -Tier tier
        -LocalDateTime createdAt
    }
    
    class ApiKey {
        -UUID id
        -UUID tenantId
        -String keyHash
        -String name
        -String environment
        -Boolean active
    }
    
    class UpstashRedisClient {
        -String redisUrl
        -String redisToken
        +hget(String key, String field) String
        +hmset(String key, String... fields) void
        +zadd(String key, double score, String member) void
        +zcard(String key) long
        +incr(String key) long
        +expire(String key, int seconds) void
    }
    
    RateLimitController --> RateLimitService
    RateLimitController --> ApiKeyService
    RateLimitService --> TokenBucketAlgorithm
    RateLimitService --> SlidingWindowAlgorithm
    RateLimitService --> FixedWindowAlgorithm
    RateLimitService --> MetricsService
    RateLimitService --> TierService
    RateLimitService --> AlertService
    TokenBucketAlgorithm --> UpstashRedisClient
    SlidingWindowAlgorithm --> UpstashRedisClient
    FixedWindowAlgorithm --> UpstashRedisClient
    AuthService --> ApiKeyService
    RateLimitService --> RateLimitRule
    ApiKeyService --> ApiKey
    AuthService --> Tenant
    TierService --> Tenant
```

---

## Sequence Diagrams

### Rate Limit Check Flow

```mermaid
sequenceDiagram
    participant Client
    participant RateLimitController
    participant RateLimitApiService
    participant ApiKeyService
    participant RateLimitService
    participant TierService
    participant Algorithm
    participant Redis
    participant MetricsService
    participant AlertService
    
    Client->>RateLimitController: POST /api/v1/check<br/>(X-API-Key, Request)
    
    RateLimitController->>RateLimitApiService: isAllowed(apiKey)
    RateLimitApiService->>Redis: Check API rate limit
    Redis-->>RateLimitApiService: Allowed/Denied
    alt Rate Limited
        RateLimitApiService-->>RateLimitController: Denied
        RateLimitController-->>Client: 429 Too Many Requests
    end
    
    RateLimitController->>ApiKeyService: validateAndGetTenant(apiKey)
    ApiKeyService->>Redis: Validate API key hash
    Redis-->>ApiKeyService: Tenant ID
    ApiKeyService-->>RateLimitController: tenantId
    
    RateLimitController->>RateLimitService: check(tenantId, request)
    
    RateLimitService->>TierService: validateCheckRequest(tenantId)
    TierService->>Redis: Check monthly usage
    Redis-->>TierService: Usage count
    alt Tier Limit Exceeded
        TierService-->>RateLimitService: Exception
        RateLimitService-->>RateLimitController: Error
    end
    
    RateLimitService->>RateLimitService: Find applicable rules
    RateLimitService->>RateLimitService: Select most restrictive rule
    
    RateLimitService->>Algorithm: check(key, maxRequests, windowSeconds)
    Algorithm->>Redis: Get/Update state
    Redis-->>Algorithm: State/Result
    Algorithm-->>RateLimitService: RateLimitCheckResponse
    
    RateLimitService->>MetricsService: recordCheck(tenantId, identifier, resource, allowed, latency)
    MetricsService->>Redis: Store metric (async)
    
    RateLimitService->>AlertService: checkAndSendAlerts(tenantId, usagePercent)
    AlertService->>Redis: Check alert thresholds
    alt Alert Threshold Reached
        AlertService->>AlertService: sendWebhookAlert() (async)
    end
    
    RateLimitService-->>RateLimitController: RateLimitCheckResponse
    RateLimitController-->>Client: 200 OK / 429 Too Many Requests
```

### User Registration Flow

```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant AuthService
    participant TenantRepository
    participant ApiKeyService
    participant RuleRepository
    participant Database
    
    Client->>AuthController: POST /auth/register<br/>(email, password)
    
    AuthController->>AuthService: register(email, password)
    
    AuthService->>TenantRepository: findByEmail(email)
    TenantRepository->>Database: SELECT * FROM tenants
    Database-->>TenantRepository: Result
    alt Email Exists
        TenantRepository-->>AuthService: Tenant found
        AuthService-->>AuthController: Exception: Email exists
        AuthController-->>Client: 400 Bad Request
    end
    
    AuthService->>AuthService: hashPassword(password)
    AuthService->>AuthService: createTenant(email, passwordHash)
    
    AuthService->>TenantRepository: save(tenant)
    TenantRepository->>Database: INSERT INTO tenants
    Database-->>TenantRepository: Saved tenant
    
    AuthService->>ApiKeyService: createApiKey(tenantId, "default", "production")
    ApiKeyService->>ApiKeyService: generateApiKey()
    ApiKeyService->>ApiKeyService: hashApiKey(apiKey)
    ApiKeyService->>Database: INSERT INTO api_keys
    Database-->>ApiKeyService: Saved API key
    
    AuthService->>RuleRepository: createDefaultRule(tenantId)
    RuleRepository->>Database: INSERT INTO rate_limit_rules
    Database-->>RuleRepository: Saved rule
    
    AuthService->>AuthService: generateAccessToken(tenantId, email)
    AuthService->>AuthService: generateRefreshToken(tenantId, email)
    
    AuthService-->>AuthController: {accessToken, refreshToken, apiKey, tenantId}
    AuthController-->>Client: 200 OK with auth data
```

### Rule Creation Flow

```mermaid
sequenceDiagram
    participant Client
    participant RuleController
    participant TierService
    participant RuleRepository
    participant Database
    
    Client->>RuleController: POST /rules<br/>(Authorization: Bearer token, Rule data)
    
    RuleController->>RuleController: Extract tenantId from JWT
    
    RuleController->>TierService: validateRuleCreation(tenantId)
    TierService->>Database: SELECT COUNT(*) FROM rate_limit_rules WHERE tenant_id = ?
    Database-->>TierService: Rule count
    TierService->>TierService: Check tier limits
    alt Tier Limit Exceeded
        TierService-->>RuleController: Exception: Tier limit exceeded
        RuleController-->>Client: 403 Forbidden
    end
    
    RuleController->>RuleController: Build RateLimitRule entity
    
    RuleController->>RuleRepository: save(rule)
    RuleRepository->>Database: INSERT INTO rate_limit_rules
    Database-->>RuleRepository: Saved rule
    
    RuleRepository-->>RuleController: Saved rule entity
    RuleController-->>Client: 200 OK with rule data
```

---

## System Design Flow

### Complete Request Flow

```mermaid
flowchart TD
    Start([Client Request]) --> CheckAuth{Has API Key?}
    CheckAuth -->|No| AuthError[Return 401 Unauthorized]
    CheckAuth -->|Yes| ValidateKey[Validate API Key]
    
    ValidateKey --> KeyValid{Valid?}
    KeyValid -->|No| AuthError
    KeyValid -->|Yes| RateLimitAPI[Rate Limit API Itself]
    
    RateLimitAPI --> APILimited{Rate Limited?}
    APILimited -->|Yes| API429[Return 429 Too Many Requests]
    APILimited -->|No| GetTenant[Get Tenant ID]
    
    GetTenant --> CheckTier[Check Tier Limits]
    CheckTier --> TierOK{Within Limits?}
    TierOK -->|No| TierError[Return 403 Forbidden]
    TierOK -->|Yes| FindRules[Find Applicable Rules]
    
    FindRules --> RulesFound{Rules Found?}
    RulesFound -->|No| AllowDefault[Allow Request - No Rules]
    RulesFound -->|Yes| SelectRule[Select Most Restrictive Rule]
    
    SelectRule --> GetAlgorithm[Get Algorithm Type]
    GetAlgorithm --> AlgoType{Algorithm?}
    
    AlgoType -->|TOKEN_BUCKET| TokenBucket[Token Bucket Algorithm]
    AlgoType -->|SLIDING_WINDOW| SlidingWindow[Sliding Window Algorithm]
    AlgoType -->|FIXED_WINDOW| FixedWindow[Fixed Window Algorithm]
    
    TokenBucket --> CheckRedis1[Check Redis State]
    SlidingWindow --> CheckRedis2[Check Redis State]
    FixedWindow --> CheckRedis3[Check Redis State]
    
    CheckRedis1 --> UpdateRedis1[Update Redis]
    CheckRedis2 --> UpdateRedis2[Update Redis]
    CheckRedis3 --> UpdateRedis3[Update Redis]
    
    UpdateRedis1 --> CheckAllowed1{Allowed?}
    UpdateRedis2 --> CheckAllowed2{Allowed?}
    UpdateRedis3 --> CheckAllowed3{Allowed?}
    
    CheckAllowed1 -->|Yes| RecordMetric1[Record Metric]
    CheckAllowed2 -->|Yes| RecordMetric2[Record Metric]
    CheckAllowed3 -->|Yes| RecordMetric3[Record Metric]
    
    CheckAllowed1 -->|No| RecordMetric1
    CheckAllowed2 -->|No| RecordMetric2
    CheckAllowed3 -->|No| RecordMetric3
    
    RecordMetric1 --> CheckAlerts[Check Alert Thresholds]
    RecordMetric2 --> CheckAlerts
    RecordMetric3 --> CheckAlerts
    AllowDefault --> CheckAlerts
    
    CheckAlerts --> AlertNeeded{Alert Needed?}
    AlertNeeded -->|Yes| SendAlert[Send Alert Async]
    AlertNeeded -->|No| BuildResponse
    SendAlert --> BuildResponse[Build Response]
    
    BuildResponse --> ReturnResponse{Allowed?}
    ReturnResponse -->|Yes| Return200[Return 200 OK]
    ReturnResponse -->|No| Return429[Return 429 Too Many Requests]
    
    Return200 --> End([End])
    Return429 --> End
    AuthError --> End
    API429 --> End
    TierError --> End
```

### Multi-Tenant Isolation Flow

```mermaid
flowchart LR
    Client1[Client 1<br/>Tenant A] --> API[RateLimitX API]
    Client2[Client 2<br/>Tenant B] --> API
    Client3[Client 3<br/>Tenant A] --> API
    
    API --> Auth[Authentication Layer]
    Auth --> TenantA[Tenant A Context]
    Auth --> TenantB[Tenant B Context]
    
    TenantA --> RulesA[Rules A]
    TenantB --> RulesB[Rules B]
    
    RulesA --> RedisA[Redis Keys:<br/>rl:tenantA:*]
    RulesB --> RedisB[Redis Keys:<br/>rl:tenantB:*]
    
    RulesA --> DBQueryA[(Database:<br/>WHERE tenant_id = A)]
    RulesB --> DBQueryB[(Database:<br/>WHERE tenant_id = B)]
    
    RedisA --> ResponseA[Response A]
    RedisB --> ResponseB[Response B]
    
    ResponseA --> Client1
    ResponseA --> Client3
    ResponseB --> Client2
```

---

## Component Diagram

### System Components

```mermaid
graph TB
    subgraph "Client Layer"
        SDK1[Node.js SDK]
        SDK2[Python SDK]
        SDK3[Java SDK]
        SDK4[Go SDK]
        HTTP[Direct HTTP]
    end
    
    subgraph "API Gateway"
        Gateway[Spring Boot REST API]
    end
    
    subgraph "Application Layer"
        Controllers[Controllers]
        Services[Services]
        Algorithms[Algorithms]
    end
    
    subgraph "Infrastructure Layer"
        Redis[Redis/Upstash]
        Postgres[PostgreSQL]
        Metrics[Prometheus]
    end
    
    subgraph "Frontend"
        Dashboard[React Dashboard]
    end
    
    SDK1 --> Gateway
    SDK2 --> Gateway
    SDK3 --> Gateway
    SDK4 --> Gateway
    HTTP --> Gateway
    Dashboard --> Gateway
    
    Gateway --> Controllers
    Controllers --> Services
    Services --> Algorithms
    Services --> Redis
    Services --> Postgres
    Services --> Metrics
```

---

## Data Flow Architecture

### Request Processing Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│                    Request Pipeline                          │
└─────────────────────────────────────────────────────────────┘

1. Client Request
   │
   ├─> API Gateway (Spring Boot)
   │   ├─> Security Filter (JWT/API Key validation)
   │   ├─> Rate Limiting Filter (API self-rate-limiting)
   │   └─> Request Routing
   │
2. Controller Layer
   │   ├─> Input Validation
   │   ├─> Authentication Check
   │   └─> Service Invocation
   │
3. Service Layer
   │   ├─> Business Logic
   │   ├─> Rule Resolution
   │   ├─> Algorithm Selection
   │   └─> State Management
   │
4. Algorithm Layer
   │   ├─> Token Bucket
   │   ├─> Sliding Window
   │   └─> Fixed Window
   │
5. Data Layer
   │   ├─> Redis (State)
   │   ├─> PostgreSQL (Metadata)
   │   └─> Metrics (Observability)
   │
6. Response Pipeline
   │   ├─> Metric Recording (Async)
   │   ├─> Alert Checking (Async)
   │   └─> Response Building
   │
7. Client Response
```

---

## Deployment Architecture

### Production Deployment

```mermaid
graph TB
    subgraph "CDN/Edge"
        CDN[Cloudflare/Vercel Edge]
    end
    
    subgraph "Frontend"
        Vercel[Vercel<br/>React App]
    end
    
    subgraph "Backend"
        Render1[Render Instance 1]
        Render2[Render Instance 2]
        Render3[Render Instance N]
    end
    
    subgraph "Load Balancer"
        LB[Load Balancer]
    end
    
    subgraph "Database Layer"
        Neon[(Neon PostgreSQL<br/>Primary)]
        Replica[(Read Replica)]
    end
    
    subgraph "Cache Layer"
        Upstash[Upstash Redis<br/>Global]
    end
    
    subgraph "Monitoring"
        Prometheus[Prometheus]
        Grafana[Grafana]
    end
    
    CDN --> Vercel
    CDN --> LB
    LB --> Render1
    LB --> Render2
    LB --> Render3
    
    Render1 --> Neon
    Render2 --> Neon
    Render3 --> Neon
    
    Render1 --> Upstash
    Render2 --> Upstash
    Render3 --> Upstash
    
    Render1 --> Prometheus
    Render2 --> Prometheus
    Render3 --> Prometheus
    
    Prometheus --> Grafana
```

---

## Security Architecture

### Authentication & Authorization Flow

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant AuthController
    participant AuthService
    participant JWT
    participant Database
    
    User->>Frontend: Login (email, password)
    Frontend->>AuthController: POST /auth/login
    AuthController->>AuthService: login(email, password)
    AuthService->>Database: Verify credentials
    Database-->>AuthService: User data
    AuthService->>JWT: Generate tokens
    JWT-->>AuthService: accessToken, refreshToken
    AuthService-->>AuthController: Tokens
    AuthController-->>Frontend: Tokens
    Frontend->>Frontend: Store tokens
    
    User->>Frontend: API Request
    Frontend->>AuthController: Request + Bearer token
    AuthController->>JWT: Validate token
    JWT-->>AuthController: Claims (tenantId, email)
    AuthController->>AuthController: Extract tenantId
    AuthController->>AuthController: Process request
```

---

**Last Updated**: 2024
**Version**: 1.0.0

