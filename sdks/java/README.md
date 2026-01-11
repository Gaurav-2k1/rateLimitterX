# RateLimitX Java SDK

Official Java SDK for RateLimitX - Production-Grade API Rate Limiting Platform.

## Installation

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.ratelimitx</groupId>
    <artifactId>ratelimitx-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

Or build from source:

```bash
cd sdks/java
mvn clean install
```

## Quick Start

```java
import com.ratelimitx.sdk.RateLimitX;
import com.ratelimitx.sdk.RateLimitXException;
import static com.ratelimitx.sdk.Models.*;

// Initialize with API key
RateLimitX client = new RateLimitX(
    "https://your-api-url.com",
    "rlx_your-api-key"
);

// Check rate limit
RateLimitCheckRequest request = new RateLimitCheckRequest(
    "user123",
    "api.payment.create",
    1
);

try {
    RateLimitCheckResponse result = client.check(request);
    if (result.getAllowed()) {
        System.out.println("Allowed! " + result.getRemaining() + " requests remaining");
    } else {
        System.out.println("Rate limited! Retry after " + result.getRetryAfter() + " seconds");
    }
} catch (RateLimitXException e) {
    System.err.println("Error: " + e.getMessage());
}
```

## Authentication

### Using API Key

```java
RateLimitX client = new RateLimitX("https://api.example.com", "rlx_your-api-key");
```

### Using Access Token (for management operations)

```java
// Register
AuthResponse auth = client.register("user@example.com", "password123");

// Login
AuthResponse auth = client.login("user@example.com", "password123");

// Use access token
client.setAccessToken(auth.getAccessToken());
```

## API Reference

### Rate Limiting

#### `check(RateLimitCheckRequest) -> RateLimitCheckResponse`

Check if a request should be rate limited.

```java
RateLimitCheckRequest request = new RateLimitCheckRequest(
    "user123",
    "api.payment.create",
    1
);
RateLimitCheckResponse result = client.check(request);
```

### Rules Management

#### `getRules() -> List<RateLimitRule>`

Get all rate limit rules.

#### `createRule(CreateRuleRequest) -> RateLimitRule`

Create a new rate limit rule.

```java
CreateRuleRequest ruleRequest = new CreateRuleRequest();
ruleRequest.setResource("api.payment.create");
ruleRequest.setAlgorithm("TOKEN_BUCKET");
ruleRequest.setMaxRequests(100);
ruleRequest.setWindowSeconds(60);
ruleRequest.setBurstCapacity(10);
ruleRequest.setIdentifierType("USER_ID");

RateLimitRule rule = client.createRule(ruleRequest);
```

#### `updateRule(String ruleId, UpdateRuleRequest) -> RateLimitRule`

Update an existing rule.

#### `deleteRule(String ruleId) -> void`

Delete a rule.

### API Key Management

#### `getApiKeys() -> List<ApiKey>`

Get all API keys.

#### `createApiKey(String name, String environment) -> CreateApiKeyResponse`

Create a new API key.

```java
CreateApiKeyResponse key = client.createApiKey("Production Key", "production");
System.out.println(key.getApiKey()); // Save this!
```

#### `deleteApiKey(String keyId) -> void`

Delete an API key.

#### `rotateApiKey(String keyId) -> RotateApiKeyResponse`

Rotate an API key.

### Analytics

#### `getRealtimeAnalytics() -> AnalyticsMetrics`

Get real-time analytics metrics.

#### `getTopIdentifiers(int limit) -> List<TopIdentifier>`

Get top rate-limited identifiers.

#### `getTrends(String start, String end) -> Map<String, Object>`

Get analytics trends.

### Alerts

#### `getAlerts() -> List<AlertConfiguration>`

Get all alert configurations.

#### `createAlert(CreateAlertRequest) -> AlertConfiguration`

Create an alert configuration.

```java
CreateAlertRequest alertRequest = new CreateAlertRequest();
alertRequest.setAlertType("TIER_LIMIT_APPROACHING");
alertRequest.setDestination("https://your-webhook.com");
alertRequest.setDestinationType("WEBHOOK");
alertRequest.setThresholdPercent(80);

AlertConfiguration alert = client.createAlert(alertRequest);
```

#### `deleteAlert(String alertId) -> void`

Delete an alert configuration.

### Bulk Operations

#### `exportRules(String format) -> String`

Export all rules as JSON or YAML.

## Error Handling

```java
try {
    client.check(request);
} catch (RateLimitXException e) {
    System.err.println("Error " + e.getStatusCode() + ": " + e.getMessage());
}
```

## License

MIT

