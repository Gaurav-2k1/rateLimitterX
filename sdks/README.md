# RateLimitX SDKs

Official SDKs for RateLimitX - Production-Grade API Rate Limiting Platform.

## Available SDKs

- **[Node.js/TypeScript](./nodejs/)** - Full TypeScript support with type definitions
- **[Python](./python/)** - Python 3.8+ support with type hints
- **[Java](./java/)** - Java 11+ support with Maven
- **[Go](./go/)** - Go 1.21+ support

## Quick Start

### Node.js/TypeScript

```bash
npm install @ratelimitx/sdk
```

```typescript
import RateLimitX from '@ratelimitx/sdk';

const client = new RateLimitX({
  baseUrl: 'https://your-api-url.com',
  apiKey: 'rlx_your-api-key'
});

const result = await client.check({
  identifier: 'user123',
  resource: 'api.payment.create',
  tokens: 1
});
```

### Python

```bash
pip install ratelimitx
```

```python
from ratelimitx import RateLimitX, RateLimitCheckRequest

client = RateLimitX(
    base_url="https://your-api-url.com",
    api_key="rlx_your-api-key"
)

result = client.check(RateLimitCheckRequest(
    identifier="user123",
    resource="api.payment.create",
    tokens=1
))
```

### Java

```xml
<dependency>
    <groupId>com.ratelimitx</groupId>
    <artifactId>ratelimitx-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
import com.ratelimitx.sdk.RateLimitX;
import static com.ratelimitx.sdk.Models.*;

RateLimitX client = new RateLimitX(
    "https://your-api-url.com",
    "rlx_your-api-key"
);

RateLimitCheckRequest request = new RateLimitCheckRequest(
    "user123",
    "api.payment.create",
    1
);

RateLimitCheckResponse result = client.check(request);
```

### Go

```bash
go get github.com/ratelimitx/ratelimitx-go
```

```go
import "github.com/ratelimitx/ratelimitx-go"

client := ratelimitx.New("https://your-api-url.com", "rlx_your-api-key")

req := ratelimitx.RateLimitCheckRequest{
    Identifier: "user123",
    Resource:   "api.payment.create",
    Tokens:     1,
}

result, err := client.Check(req)
```

## Features

All SDKs support:

- ✅ Rate limit checking
- ✅ Authentication (register, login, refresh)
- ✅ Rule management (CRUD operations)
- ✅ API key management
- ✅ Analytics (real-time, trends, top identifiers)
- ✅ Alert configuration
- ✅ Bulk import/export

## Documentation

- [Node.js SDK Documentation](./nodejs/README.md)
- [Python SDK Documentation](./python/README.md)
- [Java SDK Documentation](./java/README.md)
- [Go SDK Documentation](./go/README.md)

## Examples

### Rate Limiting

All SDKs provide a simple `check` method:

```typescript
// Node.js
const result = await client.check({
  identifier: 'user123',
  resource: 'api.payment.create',
  tokens: 1
});
```

```python
# Python
result = client.check(RateLimitCheckRequest(
    identifier="user123",
    resource="api.payment.create",
    tokens=1
))
```

### Rule Management

```typescript
// Node.js
const rule = await client.createRule({
  resource: 'api.payment.create',
  algorithm: 'TOKEN_BUCKET',
  maxRequests: 100,
  windowSeconds: 60,
  burstCapacity: 10,
  identifierType: 'USER_ID'
});
```

### Analytics

```typescript
// Node.js
const analytics = await client.getRealtimeAnalytics();
console.log(`Total checks: ${analytics.totalChecks}`);
console.log(`Rate limit hits: ${analytics.rateLimitHits}`);
console.log(`Remaining checks: ${analytics.remainingChecksThisMonth}`);
```

## Error Handling

All SDKs provide consistent error handling:

```typescript
// Node.js
try {
  await client.check(request);
} catch (error) {
  if (error instanceof RateLimitXError) {
    console.error(`Error ${error.statusCode}: ${error.message}`);
  }
}
```

```python
# Python
try:
    client.check(request)
except RateLimitXError as e:
    print(f"Error {e.status_code}: {e.message}")
```

## Contributing

Contributions are welcome! Please see the individual SDK READMEs for contribution guidelines.

## License

MIT

