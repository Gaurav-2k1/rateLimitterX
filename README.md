# 🚀 RateLimitX - Production-Grade API Rate Limiting Platform

<div align="center">

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen.svg)
![React](https://img.shields.io/badge/React-18-blue.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)

**A multi-tenant SaaS platform for API rate limiting with enterprise-grade features**

[Features](#-features) • [Quick Start](#-quick-start) • [Documentation](#-documentation) • [SDKs](#-sdks) • [Architecture](#-architecture)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Architecture](#-architecture)
- [Quick Start](#-quick-start)
- [API Documentation](#-api-documentation)
- [SDKs](#-sdks)
- [Deployment](#-deployment)
- [Algorithm Details](#-algorithm-details)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 Overview

RateLimitX is a production-ready, multi-tenant SaaS platform that provides API rate limiting as a service. It supports multiple rate limiting algorithms, offers a comprehensive dashboard for management, and includes SDKs for easy integration.

### Key Highlights

- ✅ **Three Rate Limiting Algorithms**: Token Bucket, Sliding Window, Fixed Window
- ✅ **Multi-tenant Architecture**: Complete tenant isolation with tier-based features
- ✅ **Full-Stack Application**: Spring Boot backend + React TypeScript frontend
- ✅ **Multi-Language SDKs**: Node.js, Python, Java, Go
- ✅ **Production Ready**: Deployable on free-tier services (Render, Vercel, Neon, Upstash)
- ✅ **Enterprise Features**: RBAC, Analytics, Alerting, Bulk Operations

---

## ✨ Features

### Core Features

- **Multiple Algorithms**: Choose from Token Bucket, Sliding Window, or Fixed Window algorithms
- **Hierarchical Limits**: Global, resource-level, and identifier-level rate limits
- **Priority-Based Rules**: Most restrictive rule wins with priority evaluation
- **Real-time Analytics**: Track usage, latency percentiles (P50, P95, P99), and top identifiers
- **Tier-Based Monetization**: FREE, PRO, and ENTERPRISE tiers with usage limits

### Management Features

- **Modern Dashboard**: React-based UI with shadcn/ui components
- **API Key Management**: Create, rotate, and revoke API keys with environment support
- **Rule Management**: CRUD operations with bulk import/export (JSON/YAML)
- **Role-Based Access Control**: Owner, Admin, and Viewer roles
- **Alerting System**: Webhook, Slack, Discord, and email notifications

### Developer Experience

- **RESTful API**: Simple HTTP-based integration
- **Official SDKs**: TypeScript, Python, Java, Go
- **Comprehensive Documentation**: API docs, examples, and guides
- **Free Deployment**: Complete setup guide for free-tier services

---

## 🏗️ Architecture

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Applications                     │
│  (Node.js, Python, Java, Go, or direct HTTP)                │
└────────────────────┬────────────────────────────────────────┘
                      │
                      │ HTTPS
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                    RateLimitX API Gateway                    │
│              (Spring Boot REST API - Port 8080)              │
└───────┬───────────────────────────────────────────┬─────────┘
        │                                           │
        │                                           │
        ▼                                           ▼
┌──────────────────┐                      ┌──────────────────┐
│  Rate Limit      │                      │  Management      │
│  Service         │                      │  Dashboard       │
│                  │                      │  (React + Vite)  │
│  • Token Bucket  │                      │                  │
│  • Sliding Win   │                      │  • Rules UI      │
│  • Fixed Window  │                      │  • Analytics     │
└───────┬──────────┘                      │  • API Keys      │
        │                                 └──────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│                    Data Layer                                │
│                                                              │
│  ┌──────────────┐              ┌──────────────┐            │
│  │  PostgreSQL  │              │  Redis       │            │
│  │              │              │  (Upstash)   │            │
│  │  • Tenants   │              │              │            │
│  │  • Rules     │              │  • Rate      │            │
│  │  • API Keys  │              │    Limits    │            │
│  │  • Metrics   │              │  • Counters  │            │
│  └──────────────┘              └──────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

### Technology Stack

**Backend:**
- Java 21
- Spring Boot 3.2.1
- PostgreSQL 16
- Redis (Upstash REST API)
- JWT Authentication
- Spring Security

**Frontend:**
- React 18
- TypeScript
- Vite
- shadcn/ui
- TanStack Query

**Infrastructure:**
- Docker & Docker Compose
- Prometheus Metrics
- Spring Actuator

---

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Node.js 18+
- PostgreSQL (or Neon.tech free tier)
- Upstash Redis account (free tier)
- Maven 3.8+

### Local Development

#### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/ratelimitx.git
cd ratelimitx
```

#### 2. Start PostgreSQL

```bash
docker-compose up -d postgres
```

Or use [Neon.tech](https://neon.tech) free PostgreSQL:
- Sign up and create a new project
- Copy the connection string

#### 3. Setup Upstash Redis

1. Sign up at [Upstash](https://upstash.com)
2. Create a Redis database
3. Copy the REST API URL and token

#### 4. Configure Backend

```bash
cd backend
cp .env.example .env
```

Edit `.env` with your credentials:
```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ratelimitx
DB_USER=postgres
DB_PASSWORD=yourpassword
UPSTASH_REDIS_URL=https://your-redis.upstash.io
UPSTASH_REDIS_TOKEN=your-token
JWT_SECRET=your-secret-key-min-32-characters
```

#### 5. Run Backend

```bash
mvn spring-boot:run
```

Backend will be available at `http://localhost:8080`

#### 6. Setup Frontend

```bash
cd frontend/dashboard
npm install
cp .env.example .env
```

Edit `.env`:
```env
VITE_API_URL=http://localhost:8080
```

```bash
npm run dev
```

Frontend will be available at `http://localhost:5173`

### Quick Test

```bash
# Register a new user
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Save the API key from response, then check rate limit
curl -X POST http://localhost:8080/api/v1/check \
  -H "Content-Type: application/json" \
  -H "X-API-Key: rlx_your-api-key" \
  -d '{
    "identifier": "user123",
    "resource": "api.test",
    "tokens": 1
  }'
```

---

## 📚 API Documentation

### Authentication

#### Register
```http
POST /auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "jwt-token",
    "apiKey": "rlx_...",
    "tenantId": "uuid"
  }
}
```

#### Login
```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

### Rate Limiting

#### Check Rate Limit
```http
POST /api/v1/check
X-API-Key: rlx_your-api-key
Content-Type: application/json

{
  "identifier": "user123",
  "resource": "api.payment.create",
  "tokens": 1
}
```

**Response:**
```json
{
  "allowed": true,
  "remaining": 99,
  "resetAt": 1704067200000,
  "retryAfter": 0
}
```

**HTTP Headers:**
- `X-RateLimit-Remaining`: Remaining requests
- `X-RateLimit-Reset`: Reset timestamp
- `Retry-After`: Seconds to wait (if rate limited)

### Rules Management

#### Create Rule
```http
POST /rules
Authorization: Bearer jwt-token
Content-Type: application/json

{
  "resource": "api.payment.create",
  "algorithm": "TOKEN_BUCKET",
  "maxRequests": 100,
  "windowSeconds": 60,
  "burstCapacity": 10,
  "identifierType": "USER_ID"
}
```

#### Get Rules
```http
GET /rules
Authorization: Bearer jwt-token
```

#### Update Rule
```http
PUT /rules/{ruleId}
Authorization: Bearer jwt-token
Content-Type: application/json

{
  "maxRequests": 200,
  "active": true
}
```

#### Delete Rule
```http
DELETE /rules/{ruleId}
Authorization: Bearer jwt-token
```

### API Key Management

#### Create API Key
```http
POST /api-keys
Authorization: Bearer jwt-token
Content-Type: application/json

{
  "name": "Production Key",
  "environment": "production"
}
```

#### Get API Keys
```http
GET /api-keys
Authorization: Bearer jwt-token
```

#### Rotate API Key
```http
POST /api-keys/{keyId}/rotate
Authorization: Bearer jwt-token
```

#### Delete API Key
```http
DELETE /api-keys/{keyId}
Authorization: Bearer jwt-token
```

### Analytics

#### Real-time Analytics
```http
GET /analytics/realtime
Authorization: Bearer jwt-token
```

**Response:**
```json
{
  "totalChecks": 1500,
  "rateLimitHits": 45,
  "hitRate": 3.0,
  "latencyP50": 12,
  "latencyP95": 45,
  "latencyP99": 89,
  "remainingChecksThisMonth": 8500
}
```

#### Top Identifiers
```http
GET /analytics/top-identifiers?limit=10
Authorization: Bearer jwt-token
```

### Bulk Operations

#### Export Rules
```http
GET /bulk/export?format=json
Authorization: Bearer jwt-token
```

#### Import Rules
```http
POST /bulk/import
Authorization: Bearer jwt-token
Content-Type: multipart/form-data

file: rules.json
format: json
```

---

## 📦 SDKs

Official SDKs are available for multiple languages:

| Language | Package | Documentation |
|----------|---------|---------------|
| **Node.js/TypeScript** | `@ratelimitx/sdk` | [Node.js SDK](./sdks/nodejs/README.md) |
| **Python** | `ratelimitx` | [Python SDK](./sdks/python/README.md) |
| **Java** | `com.ratelimitx:ratelimitx-sdk` | [Java SDK](./sdks/java/README.md) |
| **Go** | `github.com/ratelimitx/ratelimitx-go` | [Go SDK](./sdks/go/README.md) |

### Quick SDK Examples

**Node.js:**
```typescript
import RateLimitX from '@ratelimitx/sdk';

const client = new RateLimitX({
  baseUrl: 'https://api.ratelimitx.com',
  apiKey: 'rlx_your-api-key'
});

const result = await client.check({
  identifier: 'user123',
  resource: 'api.payment.create',
  tokens: 1
});
```

**Python:**
```python
from ratelimitx import RateLimitX, RateLimitCheckRequest

client = RateLimitX(
    base_url="https://api.ratelimitx.com",
    api_key="rlx_your-api-key"
)

result = client.check(RateLimitCheckRequest(
    identifier="user123",
    resource="api.payment.create",
    tokens=1
))
```

See [SDKs Documentation](./sdks/README.md) for complete examples.

---

## 🚢 Deployment

### Free Tier Deployment Guide

RateLimitX can be deployed completely free using free-tier services. See the [Deployment Guide](./docs/DEPLOYMENT.md) for detailed instructions.

**Quick Deploy:**

1. **Backend** → [Render.com](https://render.com) (Free tier)
2. **Frontend** → [Vercel](https://vercel.com) (Free tier)
3. **Database** → [Neon.tech](https://neon.tech) (Free tier)
4. **Redis** → [Upstash](https://upstash.com) (Free tier)

### Docker Deployment

```bash
docker-compose up -d
```

### Production Considerations

- Use managed PostgreSQL (AWS RDS, Google Cloud SQL, etc.)
- Use Redis Cluster for high availability
- Enable HTTPS with SSL certificates
- Set up monitoring (Prometheus + Grafana)
- Configure backup strategies
- Use environment variables for secrets

---

## 🔬 Algorithm Details

RateLimitX supports three rate limiting algorithms. See [Algorithm Documentation](./docs/ALGORITHMS.md) for detailed implementation.

### Token Bucket
- **Use Case**: Smooth rate limiting with burst capacity
- **Pros**: Allows bursts, smooth traffic
- **Cons**: More complex, requires token tracking

### Sliding Window
- **Use Case**: Precise rate limiting over time window
- **Pros**: Accurate, no burst at window boundaries
- **Cons**: Higher memory usage

### Fixed Window
- **Use Case**: Simple rate limiting per time period
- **Pros**: Simple, low memory
- **Cons**: Burst at window reset

---

## 📊 Monitoring

### Health Checks

```bash
curl http://localhost:8080/actuator/health
```

### Metrics

```bash
curl http://localhost:8080/actuator/prometheus
```

### Available Metrics

- `ratelimitx_checks_total`: Total rate limit checks
- `ratelimitx_checks_allowed_total`: Allowed checks
- `ratelimitx_checks_denied_total`: Denied checks
- `ratelimitx_check_latency_ms`: Check latency

---

## 🔒 Security

- **API Keys**: SHA-256 hashed storage
- **Passwords**: BCrypt hashing (10 rounds)
- **JWT Tokens**: HS256 signing with configurable secret
- **CORS**: Configurable for frontend domain
- **Rate Limiting**: API itself is rate-limited
- **Input Validation**: Jakarta Validation on all endpoints

---

## 🧪 Testing

### Backend Tests

```bash
cd backend
mvn test
```

### Frontend Tests

```bash
cd frontend/dashboard
npm test
```

### Integration Tests

```bash
cd backend
mvn verify
```

---

## 📖 Documentation

- [Quick Start Guide](./docs/QUICKSTART.md)
- [Deployment Guide](./docs/DEPLOYMENT.md)
- [Algorithm Documentation](./docs/ALGORITHMS.md)
- [API Reference](./docs/API.md)
- [Architecture Diagrams](./docs/ARCHITECTURE.md)

---

## 🤝 Contributing

Contributions are welcome! Please see our [Contributing Guide](./CONTRIBUTING.md) for details.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- Built with [Spring Boot](https://spring.io/projects/spring-boot)
- Frontend powered by [React](https://react.dev) and [Vite](https://vitejs.dev)
- UI components from [shadcn/ui](https://ui.shadcn.com)
- Redis via [Upstash](https://upstash.com)

---

## 📧 Support

- 📖 [Documentation](./docs/)
- 🐛 [Issue Tracker](https://github.com/yourusername/ratelimitx/issues)
- 💬 [Discussions](https://github.com/yourusername/ratelimitx/discussions)

---

<div align="center">

**Made with ❤️ by the RateLimitX Team**

⭐ Star us on GitHub if you find this project useful!

</div>
"# rateLimitterX" 
