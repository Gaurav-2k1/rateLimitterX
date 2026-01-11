# RateLimitX Python SDK

Official Python SDK for RateLimitX - Production-Grade API Rate Limiting Platform.

## Installation

```bash
pip install ratelimitx
```

Or from source:

```bash
pip install -e .
```

## Quick Start

```python
from ratelimitx import RateLimitX, RateLimitCheckRequest

# Initialize with API key
client = RateLimitX(
    base_url="https://your-api-url.com",
    api_key="rlx_your-api-key"
)

# Check rate limit
request = RateLimitCheckRequest(
    identifier="user123",
    resource="api.payment.create",
    tokens=1
)

result = client.check(request)

if result.allowed:
    print(f"Allowed! {result.remaining} requests remaining")
else:
    print(f"Rate limited! Retry after {result.retryAfter} seconds")
```

## Authentication

### Using API Key

```python
client = RateLimitX(api_key="rlx_your-api-key")
```

### Using Access Token (for management operations)

```python
# Register
auth = client.register(email="user@example.com", password="password123")

# Login
auth = client.login(email="user@example.com", password="password123")

# Use access token
client.set_access_token(auth.accessToken)
```

## API Reference

### Rate Limiting

#### `check(request: RateLimitCheckRequest) -> RateLimitCheckResponse`

Check if a request should be rate limited.

```python
request = RateLimitCheckRequest(
    identifier="user123",
    resource="api.payment.create",
    tokens=1
)
result = client.check(request)
```

### Rules Management

#### `get_rules() -> List[RateLimitRule]`

Get all rate limit rules.

#### `create_rule(...) -> RateLimitRule`

Create a new rate limit rule.

```python
rule = client.create_rule(
    resource="api.payment.create",
    algorithm="TOKEN_BUCKET",
    max_requests=100,
    window_seconds=60,
    burst_capacity=10,
    identifier_type="USER_ID"
)
```

#### `update_rule(rule_id, ...) -> RateLimitRule`

Update an existing rule.

#### `delete_rule(rule_id) -> None`

Delete a rule.

### API Key Management

#### `get_api_keys() -> List[ApiKey]`

Get all API keys.

#### `create_api_key(name, environment) -> Dict`

Create a new API key.

```python
key = client.create_api_key(
    name="Production Key",
    environment="production"
)
print(key["apiKey"])  # Save this!
```

#### `delete_api_key(key_id) -> None`

Delete an API key.

#### `rotate_api_key(key_id) -> Dict`

Rotate an API key.

### Analytics

#### `get_realtime_analytics() -> AnalyticsMetrics`

Get real-time analytics metrics.

#### `get_top_identifiers(limit=10) -> List[TopIdentifier]`

Get top rate-limited identifiers.

#### `get_trends(start=None, end=None) -> Dict`

Get analytics trends.

### Alerts

#### `get_alerts() -> List[AlertConfiguration]`

Get all alert configurations.

#### `create_alert(...) -> AlertConfiguration`

Create an alert configuration.

```python
alert = client.create_alert(
    alert_type="TIER_LIMIT_APPROACHING",
    destination="https://your-webhook.com",
    destination_type="WEBHOOK",
    threshold_percent=80
)
```

#### `delete_alert(alert_id) -> None`

Delete an alert configuration.

### Bulk Operations

#### `export_rules(format="json") -> str`

Export all rules as JSON or YAML.

#### `import_rules(content, format="json") -> Dict`

Import rules from JSON or YAML.

## Error Handling

```python
from ratelimitx import RateLimitXError

try:
    client.check(request)
except RateLimitXError as e:
    print(f"Error {e.status_code}: {e.message}")
```

## License

MIT

