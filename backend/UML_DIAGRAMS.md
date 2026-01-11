# UML Diagrams - RateLimitX

This document contains detailed UML diagrams for the RateLimitX system.

---

## Class Diagram - Complete System

```mermaid
classDiagram
    %% Controllers
    class RateLimitController {
        -RateLimitService rateLimitService
        -ApiKeyService apiKeyService
        -RateLimitApiService rateLimitApiService
        +check(String apiKey, RateLimitCheckRequest) RateLimitCheckResponse
    }
    
    class AuthController {
        -AuthService authService
        +register(RegisterRequest) ApiResponse
        +login(LoginRequest) ApiResponse
        +refresh(RefreshRequest) ApiResponse
    }
    
    class RuleController {
        -RateLimitRuleRepository ruleRepository
        -TierService tierService
        +getRules() ApiResponse~List~RateLimitRule~~
        +createRule(CreateRuleRequest) ApiResponse~RateLimitRule~
        +updateRule(UUID, UpdateRuleRequest) ApiResponse~RateLimitRule~
        +deleteRule(UUID) ApiResponse~Void~
    }
    
    class ApiKeyController {
        -ApiKeyService apiKeyService
        +getApiKeys() ApiResponse~List~ApiKey~~
        +createApiKey(CreateApiKeyRequest) ApiResponse~Map~
        +deleteApiKey(UUID) ApiResponse~Void~
        +rotateApiKey(UUID) ApiResponse~Map~
    }
    
    class AnalyticsController {
        -UsageMetricRepository metricsRepository
        -TierService tierService
        +getRealtime() ApiResponse~Map~
        +getTopIdentifiers(int) ApiResponse~List~
        +getTrends(String, String) ApiResponse~Map~
    }
    
    class AlertController {
        -AlertConfigurationRepository alertRepository
        +getAlerts() ApiResponse~List~AlertConfiguration~~
        +createAlert(CreateAlertRequest) ApiResponse~AlertConfiguration~
        +deleteAlert(UUID) ApiResponse~Void~
    }
    
    class BulkController {
        -RateLimitRuleRepository ruleRepository
        -TierService tierService
        +importRules(MultipartFile, String) ApiResponse~Map~
        +exportRules(String) ResponseEntity~String~
    }
    
    %% Services
    class RateLimitService {
        -RateLimitRuleRepository ruleRepository
        -TokenBucketAlgorithm tokenBucket
        -SlidingWindowAlgorithm slidingWindow
        -FixedWindowAlgorithm fixedWindow
        -MetricsService metricsService
        -TierService tierService
        -AlertService alertService
        +check(UUID, RateLimitCheckRequest) RateLimitCheckResponse
    }
    
    class AuthService {
        -TenantRepository tenantRepository
        -ApiKeyService apiKeyService
        -PasswordEncoder passwordEncoder
        -JwtTokenProvider jwtTokenProvider
        +register(String, String) Map~String,Object~
        +login(String, String) Map~String,Object~
        +validateToken(String) Claims
        +generateAccessToken(UUID, String) String
    }
    
    class ApiKeyService {
        -ApiKeyRepository apiKeyRepository
        +validateAndGetTenant(String) UUID
        +createApiKey(UUID, String, String) ApiKey
        +rotateApiKey(UUID, UUID) String
        +deleteApiKey(UUID, UUID) void
        +hashApiKey(String) String
        +generateApiKey() String
    }
    
    class TierService {
        -TenantRepository tenantRepository
        -RateLimitRuleRepository ruleRepository
        -UsageMetricRepository usageMetricRepository
        +validateRuleCreation(UUID) void
        +validateCheckRequest(UUID) void
        +getRemainingChecksThisMonth(UUID) int
        +getTierLimits(Tier) TierLimits
    }
    
    class MetricsService {
        -UsageMetricRepository metricsRepository
        +recordCheck(UUID, String, String, boolean, int) void
    }
    
    class AlertService {
        -AlertConfigurationRepository alertRepository
        +checkAndSendAlerts(UUID, int) void
        +sendWebhookAlert(String, Map) void
        +sendSlackAlert(String, Map) void
        +sendDiscordAlert(String, Map) void
    }
    
    class RoleService {
        -UserRoleRepository roleRepository
        -TenantRepository tenantRepository
        +getUserRole(UUID, String) Role
        +hasPermission(UUID, String, Role) boolean
    }
    
    class RateLimitApiService {
        -UpstashRedisClient redis
        +isAllowed(String) boolean
    }
    
    %% Algorithms
    class TokenBucketAlgorithm {
        -UpstashRedisClient redis
        +check(String, int, int) RateLimitCheckResponse
    }
    
    class SlidingWindowAlgorithm {
        -UpstashRedisClient redis
        +check(String, int, int) RateLimitCheckResponse
    }
    
    class FixedWindowAlgorithm {
        -UpstashRedisClient redis
        +check(String, int, int) RateLimitCheckResponse
    }
    
    %% Entities
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
        -String conditionJson
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
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
        -LocalDateTime createdAt
        -LocalDateTime lastUsedAt
    }
    
    class UsageMetric {
        -UUID id
        -UUID tenantId
        -String identifier
        -String resource
        -Integer checksAllowed
        -Integer checksDenied
        -Integer latencyMs
        -LocalDateTime timestamp
    }
    
    class AlertConfiguration {
        -UUID id
        -UUID tenantId
        -AlertType alertType
        -String destination
        -DestinationType destinationType
        -Integer thresholdPercent
        -Boolean enabled
        -LocalDateTime createdAt
    }
    
    class UserRole {
        -UUID id
        -UUID tenantId
        -String userEmail
        -Role role
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
    }
    
    %% Infrastructure
    class UpstashRedisClient {
        -String redisUrl
        -String redisToken
        +hget(String, String) String
        +hmset(String, String...) void
        +zadd(String, double, String) void
        +zremrangebyscore(String, long, long) void
        +zcard(String) long
        +incr(String) long
        +expire(String, int) void
    }
    
    %% Repositories
    class RateLimitRuleRepository {
        +findByTenantId(UUID) List~RateLimitRule~
        +findByTenantIdAndResource(UUID, String) List~RateLimitRule~
        +findByTenantIdAndActive(UUID, boolean) List~RateLimitRule~
    }
    
    class TenantRepository {
        +findByEmail(String) Optional~Tenant~
        +findById(UUID) Optional~Tenant~
    }
    
    class ApiKeyRepository {
        +findByKeyHash(String) Optional~ApiKey~
        +findByTenantId(UUID) List~ApiKey~
    }
    
    class UsageMetricRepository {
        +countByTenantIdAndTimestampAfter(UUID, LocalDateTime) Long
        +countDeniedByTenantIdAndTimestampAfter(UUID, LocalDateTime) Long
        +findByTenantIdAndTimestampAfter(UUID, LocalDateTime) List~UsageMetric~
    }
    
    class AlertConfigurationRepository {
        +findByTenantIdAndEnabled(UUID, boolean) List~AlertConfiguration~
    }
    
    class UserRoleRepository {
        +findByTenantIdAndUserEmail(UUID, String) Optional~UserRole~
    }
    
    %% DTOs
    class RateLimitCheckRequest {
        -String identifier
        -String resource
        -Integer tokens
    }
    
    class RateLimitCheckResponse {
        -Boolean allowed
        -Integer remaining
        -Long resetAt
        -Integer retryAfter
    }
    
    class ApiResponse~T~ {
        -Boolean success
        -T data
        -String error
        -LocalDateTime timestamp
    }
    
    %% Relationships
    RateLimitController --> RateLimitService
    RateLimitController --> ApiKeyService
    RateLimitController --> RateLimitApiService
    AuthController --> AuthService
    RuleController --> RateLimitRuleRepository
    RuleController --> TierService
    ApiKeyController --> ApiKeyService
    AnalyticsController --> UsageMetricRepository
    AnalyticsController --> TierService
    AlertController --> AlertConfigurationRepository
    BulkController --> RateLimitRuleRepository
    BulkController --> TierService
    
    RateLimitService --> RateLimitRuleRepository
    RateLimitService --> TokenBucketAlgorithm
    RateLimitService --> SlidingWindowAlgorithm
    RateLimitService --> FixedWindowAlgorithm
    RateLimitService --> MetricsService
    RateLimitService --> TierService
    RateLimitService --> AlertService
    
    AuthService --> TenantRepository
    AuthService --> ApiKeyService
    ApiKeyService --> ApiKeyRepository
    TierService --> TenantRepository
    TierService --> RateLimitRuleRepository
    TierService --> UsageMetricRepository
    MetricsService --> UsageMetricRepository
    AlertService --> AlertConfigurationRepository
    
    TokenBucketAlgorithm --> UpstashRedisClient
    SlidingWindowAlgorithm --> UpstashRedisClient
    FixedWindowAlgorithm --> UpstashRedisClient
    RateLimitApiService --> UpstashRedisClient
    
    RateLimitRuleRepository --> RateLimitRule
    TenantRepository --> Tenant
    ApiKeyRepository --> ApiKey
    UsageMetricRepository --> UsageMetric
    AlertConfigurationRepository --> AlertConfiguration
    UserRoleRepository --> UserRole
    
    RateLimitController --> RateLimitCheckRequest
    RateLimitController --> RateLimitCheckResponse
    RateLimitService --> RateLimitCheckRequest
    RateLimitService --> RateLimitCheckResponse
```

---

## Sequence Diagram - Complete Rate Limit Check

```mermaid
sequenceDiagram
    participant Client
    participant Controller as RateLimitController
    participant APIRateLimit as RateLimitApiService
    participant ApiKeyService
    participant RateLimitService
    participant TierService
    participant RuleRepo as RateLimitRuleRepository
    participant Algorithm
    participant Redis
    participant MetricsService
    participant AlertService
    participant Database
    
    Client->>Controller: POST /api/v1/check<br/>{identifier, resource, tokens}
    
    Note over Controller: Extract X-API-Key header
    
    Controller->>APIRateLimit: isAllowed(apiKey)
    APIRateLimit->>Redis: Check API rate limit<br/>(1000 req/min per key)
    Redis-->>APIRateLimit: Allowed/Denied
    
    alt API Rate Limited
        APIRateLimit-->>Controller: Denied
        Controller-->>Client: 429 Too Many Requests
    else API Allowed
        APIRateLimit-->>Controller: Allowed
        
        Controller->>ApiKeyService: validateAndGetTenant(apiKey)
        ApiKeyService->>Database: SELECT * FROM api_keys<br/>WHERE key_hash = ?
        Database-->>ApiKeyService: ApiKey record
        ApiKeyService->>ApiKeyService: Check if active
        ApiKeyService->>Database: UPDATE api_keys<br/>SET last_used_at = NOW()
        ApiKeyService-->>Controller: tenantId
        
        Controller->>RateLimitService: check(tenantId, request)
        
        RateLimitService->>TierService: validateCheckRequest(tenantId)
        TierService->>Database: SELECT COUNT(*) FROM usage_metrics<br/>WHERE tenant_id = ? AND month = ?
        Database-->>TierService: Usage count
        TierService->>TierService: Check tier limits
        alt Tier Limit Exceeded
            TierService-->>RateLimitService: Exception
            RateLimitService-->>Controller: Error
            Controller-->>Client: 403 Forbidden
        else Tier OK
            TierService-->>RateLimitService: OK
            
            RateLimitService->>RuleRepo: findByTenantIdAndActive(tenantId, true)
            RuleRepo->>Database: SELECT * FROM rate_limit_rules<br/>WHERE tenant_id = ? AND active = true
            Database-->>RuleRepo: List of rules
            RuleRepo-->>RateLimitService: Rules
            
            RateLimitService->>RateLimitService: Filter rules by resource/identifier
            RateLimitService->>RateLimitService: Select most restrictive rule<br/>(highest priority, lowest limit)
            
            RateLimitService->>Algorithm: check(key, maxRequests, windowSeconds)
            
            alt Token Bucket
                Algorithm->>Redis: HGET key "tokens"
                Algorithm->>Redis: HGET key "last_refill"
                Redis-->>Algorithm: Current state
                Algorithm->>Algorithm: Calculate refill
                Algorithm->>Algorithm: Check if tokens >= 1
                alt Allowed
                    Algorithm->>Redis: HMSET key tokens, last_refill
                    Algorithm->>Redis: EXPIRE key windowSeconds
                end
            else Sliding Window
                Algorithm->>Redis: ZREMRANGEBYSCORE key 0 windowStart
                Algorithm->>Redis: ZCARD key
                Redis-->>Algorithm: Count
                alt Allowed
                    Algorithm->>Redis: ZADD key now timestamp
                    Algorithm->>Redis: EXPIRE key windowSeconds
                end
            else Fixed Window
                Algorithm->>Redis: INCR windowKey
                Redis-->>Algorithm: Count
                alt First in window
                    Algorithm->>Redis: EXPIRE windowKey windowSeconds
                end
            end
            
            Algorithm-->>RateLimitService: RateLimitCheckResponse
            
            RateLimitService->>MetricsService: recordCheck(tenantId, identifier, resource, allowed, latency)
            MetricsService->>Database: INSERT INTO usage_metrics<br/>(async)
            
            RateLimitService->>AlertService: checkAndSendAlerts(tenantId, usagePercent)
            AlertService->>Database: SELECT * FROM alert_configurations<br/>WHERE tenant_id = ? AND enabled = true
            Database-->>AlertService: Alerts
            alt Alert Threshold Reached
                AlertService->>AlertService: sendWebhookAlert() (async)
            end
            
            RateLimitService-->>Controller: RateLimitCheckResponse
            Controller-->>Client: 200 OK / 429 Too Many Requests
        end
    end
```

---

## State Diagram - Rate Limit Rule Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Draft: Create Rule
    
    Draft --> Active: Activate
    Draft --> Deleted: Delete
    
    Active --> Inactive: Deactivate
    Active --> Updated: Update
    Active --> Deleted: Delete
    
    Inactive --> Active: Reactivate
    Inactive --> Deleted: Delete
    
    Updated --> Active: Save Changes
    
    Deleted --> [*]
    
    note right of Active
        Rule is evaluated
        during rate limit checks
    end note
    
    note right of Inactive
        Rule exists but
        is not evaluated
    end note
```

---

## Activity Diagram - Rule Evaluation Process

```mermaid
flowchart TD
    Start([Rate Limit Check Request]) --> GetRules[Get All Active Rules<br/>for Tenant]
    GetRules --> FilterResource[Filter Rules by Resource]
    FilterResource --> FilterIdentifier[Filter Rules by Identifier]
    FilterIdentifier --> FilterScope[Filter Rules by Scope]
    
    FilterScope --> CheckGlobal{Global Rules<br/>Found?}
    CheckGlobal -->|Yes| GlobalRules[Global Rules]
    CheckGlobal -->|No| CheckResource{Resource Rules<br/>Found?}
    
    CheckResource -->|Yes| ResourceRules[Resource Rules]
    CheckResource -->|No| CheckIdentifier{Identifier Rules<br/>Found?}
    
    CheckIdentifier -->|Yes| IdentifierRules[Identifier Rules]
    CheckIdentifier -->|No| NoRules[No Rules Found]
    
    GlobalRules --> SortByPriority[Sort All Rules by Priority]
    ResourceRules --> SortByPriority
    IdentifierRules --> SortByPriority
    
    SortByPriority --> SelectMostRestrictive[Select Most Restrictive Rule<br/>Lowest maxRequests]
    
    SelectMostRestrictive --> GetAlgorithm[Get Algorithm Type]
    
    GetAlgorithm --> AlgoCheck{Algorithm?}
    
    AlgoCheck -->|TOKEN_BUCKET| TokenBucket[Execute Token Bucket]
    AlgoCheck -->|SLIDING_WINDOW| SlidingWindow[Execute Sliding Window]
    AlgoCheck -->|FIXED_WINDOW| FixedWindow[Execute Fixed Window]
    
    TokenBucket --> Result{Allowed?}
    SlidingWindow --> Result
    FixedWindow --> Result
    NoRules --> AllowDefault[Allow Request]
    
    Result -->|Yes| RecordMetric[Record Metric: Allowed]
    Result -->|No| RecordMetricDenied[Record Metric: Denied]
    AllowDefault --> RecordMetric
    
    RecordMetric --> BuildResponse[Build Response]
    RecordMetricDenied --> BuildResponse
    
    BuildResponse --> End([Return Response])
```

---

## Component Diagram - System Components

```mermaid
graph TB
    subgraph "Presentation Layer"
        REST[REST API Controllers]
        Dashboard[React Dashboard]
    end
    
    subgraph "Application Layer"
        Auth[Authentication Service]
        RateLimit[Rate Limit Service]
        Rule[Rule Management Service]
        Analytics[Analytics Service]
        Alert[Alert Service]
        Tier[Tier Service]
    end
    
    subgraph "Algorithm Layer"
        TB[Token Bucket]
        SW[Sliding Window]
        FW[Fixed Window]
    end
    
    subgraph "Infrastructure Layer"
        Redis[Redis Client]
        DB[(PostgreSQL)]
        Metrics[Prometheus]
    end
    
    REST --> Auth
    REST --> RateLimit
    REST --> Rule
    REST --> Analytics
    REST --> Alert
    
    Dashboard --> REST
    
    RateLimit --> TB
    RateLimit --> SW
    RateLimit --> FW
    RateLimit --> Tier
    RateLimit --> Analytics
    RateLimit --> Alert
    
    TB --> Redis
    SW --> Redis
    FW --> Redis
    
    Auth --> DB
    Rule --> DB
    Analytics --> DB
    Alert --> DB
    Tier --> DB
    
    RateLimit --> Metrics
    Analytics --> Metrics
```

---

**Last Updated**: 2024
**Version**: 1.0.0

