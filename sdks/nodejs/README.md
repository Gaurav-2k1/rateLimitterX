# RateLimitX Node.js SDK

Official TypeScript/JavaScript SDK for RateLimitX - Production-Grade API Rate Limiting Platform.

## Installation

```bash
npm install @ratelimitx/sdk
```

## Quick Start

```typescript
import RateLimitX from '@ratelimitx/sdk';

// Initialize with API key
const client = new RateLimitX({
  baseUrl: 'https://your-api-url.com',
  apiKey: 'rlx_your-api-key'
});

// Check rate limit
const result = await client.check({
  identifier: 'user123',
  resource: 'api.payment.create',
  tokens: 1
});

if (result.allowed) {
  console.log(`Allowed! ${result.remaining} requests remaining`);
} else {
  console.log(`Rate limited! Retry after ${result.retryAfter} seconds`);
}
```

## Authentication

### Using API Key

```typescript
const client = new RateLimitX({
  apiKey: 'rlx_your-api-key'
});
```

### Using Access Token (for management operations)

```typescript
// Register
const auth = await client.register({
  email: 'user@example.com',
  password: 'password123'
});

// Login
const auth = await client.login({
  email: 'user@example.com',
  password: 'password123'
});

// Use access token
client.setAccessToken(auth.accessToken);
```

## API Reference

### Rate Limiting

#### `check(request: RateLimitCheckRequest): Promise<RateLimitCheckResponse>`

Check if a request should be rate limited.

```typescript
const result = await client.check({
  identifier: 'user123',
  resource: 'api.payment.create',
  tokens: 1
});
```

### Rules Management

#### `getRules(): Promise<RateLimitRule[]>`

Get all rate limit rules.

#### `createRule(request: CreateRuleRequest): Promise<RateLimitRule>`

Create a new rate limit rule.

```typescript
const rule = await client.createRule({
  resource: 'api.payment.create',
  algorithm: 'TOKEN_BUCKET',
  maxRequests: 100,
  windowSeconds: 60,
  burstCapacity: 10,
  identifierType: 'USER_ID'
});
```

#### `updateRule(id: string, request: UpdateRuleRequest): Promise<RateLimitRule>`

Update an existing rule.

#### `deleteRule(id: string): Promise<void>`

Delete a rule.

### API Key Management

#### `getApiKeys(): Promise<ApiKey[]>`

Get all API keys.

#### `createApiKey(request: CreateApiKeyRequest): Promise<{ id, apiKey, name, environment }>`

Create a new API key.

```typescript
const key = await client.createApiKey({
  name: 'Production Key',
  environment: 'production'
});
console.log(key.apiKey); // Save this!
```

#### `deleteApiKey(id: string): Promise<void>`

Delete an API key.

#### `rotateApiKey(id: string): Promise<{ apiKey }>`

Rotate an API key.

### Analytics

#### `getRealtimeAnalytics(): Promise<AnalyticsMetrics>`

Get real-time analytics metrics.

#### `getTopIdentifiers(limit?: number): Promise<TopIdentifier[]>`

Get top rate-limited identifiers.

#### `getTrends(start?: string, end?: string): Promise<any>`

Get analytics trends.

### Alerts

#### `getAlerts(): Promise<AlertConfiguration[]>`

Get all alert configurations.

#### `createAlert(request: CreateAlertRequest): Promise<AlertConfiguration>`

Create an alert configuration.

```typescript
await client.createAlert({
  alertType: 'TIER_LIMIT_APPROACHING',
  destination: 'https://your-webhook.com',
  destinationType: 'WEBHOOK',
  thresholdPercent: 80
});
```

#### `deleteAlert(id: string): Promise<void>`

Delete an alert configuration.

### Bulk Operations

#### `exportRules(format?: 'json' | 'yaml'): Promise<string>`

Export all rules as JSON or YAML.

#### `importRules(content: string, format?: 'json' | 'yaml'): Promise<{ created, skipped, errors }>`

Import rules from JSON or YAML.

## Error Handling

```typescript
import { RateLimitXError } from '@ratelimitx/sdk';

try {
  await client.check({ identifier: 'user123', resource: 'api.test' });
} catch (error) {
  if (error instanceof RateLimitXError) {
    console.error(`Error ${error.statusCode}: ${error.message}`);
  }
}
```

## TypeScript Support

Full TypeScript definitions are included. All types are exported:

```typescript
import {
  RateLimitX,
  RateLimitCheckRequest,
  RateLimitCheckResponse,
  RateLimitRule,
  // ... other types
} from '@ratelimitx/sdk';
```

## License

MIT

