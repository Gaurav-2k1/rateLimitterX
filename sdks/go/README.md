# RateLimitX Go SDK

Official Go SDK for RateLimitX - Production-Grade API Rate Limiting Platform.

## Installation

```bash
go get github.com/ratelimitx/ratelimitx-go
```

## Quick Start

```go
package main

import (
    "fmt"
    "github.com/ratelimitx/ratelimitx-go"
)

func main() {
    // Initialize with API key
    client := ratelimitx.New("https://your-api-url.com", "rlx_your-api-key")

    // Check rate limit
    req := ratelimitx.RateLimitCheckRequest{
        Identifier: "user123",
        Resource:   "api.payment.create",
        Tokens:     1,
    }

    result, err := client.Check(req)
    if err != nil {
        panic(err)
    }

    if result.Allowed {
        fmt.Printf("Allowed! %d requests remaining\n", result.Remaining)
    } else {
        fmt.Printf("Rate limited! Retry after %d seconds\n", result.RetryAfter)
    }
}
```

## Authentication

### Using API Key

```go
client := ratelimitx.New("https://api.example.com", "rlx_your-api-key")
```

### Using Access Token (for management operations)

```go
// Register
auth, err := client.Register("user@example.com", "password123")

// Login
auth, err := client.Login("user@example.com", "password123")

// Use access token
client.SetAccessToken(auth.AccessToken)
```

## API Reference

### Rate Limiting

#### `Check(req RateLimitCheckRequest) (*RateLimitCheckResponse, error)`

Check if a request should be rate limited.

```go
req := ratelimitx.RateLimitCheckRequest{
    Identifier: "user123",
    Resource:   "api.payment.create",
    Tokens:     1,
}
result, err := client.Check(req)
```

### Rules Management

#### `GetRules() ([]RateLimitRule, error)`

Get all rate limit rules.

#### `CreateRule(req CreateRuleRequest) (*RateLimitRule, error)`

Create a new rate limit rule.

```go
req := ratelimitx.CreateRuleRequest{
    Resource:      "api.payment.create",
    Algorithm:     "TOKEN_BUCKET",
    MaxRequests:   100,
    WindowSeconds: 60,
    BurstCapacity: intPtr(10),
    IdentifierType: "USER_ID",
}
rule, err := client.CreateRule(req)
```

#### `UpdateRule(ruleID string, req UpdateRuleRequest) (*RateLimitRule, error)`

Update an existing rule.

#### `DeleteRule(ruleID string) error`

Delete a rule.

### API Key Management

#### `GetAPIKeys() ([]APIKey, error)`

Get all API keys.

#### `CreateAPIKey(req CreateAPIKeyRequest) (*CreateAPIKeyResponse, error)`

Create a new API key.

```go
req := ratelimitx.CreateAPIKeyRequest{
    Name:        "Production Key",
    Environment: "production",
}
key, err := client.CreateAPIKey(req)
fmt.Println(key.APIKey) // Save this!
```

#### `DeleteAPIKey(keyID string) error`

Delete an API key.

#### `RotateAPIKey(keyID string) (*RotateAPIKeyResponse, error)`

Rotate an API key.

### Analytics

#### `GetRealtimeAnalytics() (*AnalyticsMetrics, error)`

Get real-time analytics metrics.

#### `GetTopIdentifiers(limit int) ([]TopIdentifier, error)`

Get top rate-limited identifiers.

#### `GetTrends(start, end string) (map[string]interface{}, error)`

Get analytics trends.

### Alerts

#### `GetAlerts() ([]AlertConfiguration, error)`

Get all alert configurations.

#### `CreateAlert(req CreateAlertRequest) (*AlertConfiguration, error)`

Create an alert configuration.

```go
req := ratelimitx.CreateAlertRequest{
    AlertType:        "TIER_LIMIT_APPROACHING",
    Destination:      "https://your-webhook.com",
    DestinationType:  "WEBHOOK",
    ThresholdPercent: 80,
}
alert, err := client.CreateAlert(req)
```

#### `DeleteAlert(alertID string) error`

Delete an alert configuration.

### Bulk Operations

#### `ExportRules(format string) (string, error)`

Export all rules as JSON or YAML.

#### `ImportRules(content []byte, format string) (*ImportRulesResponse, error)`

Import rules from JSON or YAML.

## Error Handling

```go
result, err := client.Check(req)
if err != nil {
    if rateLimitErr, ok := err.(*ratelimitx.RateLimitXError); ok {
        fmt.Printf("Error %d: %s\n", rateLimitErr.StatusCode, rateLimitErr.Message)
    } else {
        fmt.Printf("Error: %v\n", err)
    }
}
```

## License

MIT

