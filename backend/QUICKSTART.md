# RateLimitX Quick Start Guide

Get up and running with RateLimitX in 5 minutes!

## Prerequisites

- Java 21+
- Node.js 18+
- Docker (optional, for PostgreSQL)

## Local Setup

### 1. Start PostgreSQL

```bash
docker-compose up -d postgres
```

Or use a cloud database (Neon.tech free tier).

### 2. Get Upstash Redis Credentials

1. Sign up at https://upstash.com (free)
2. Create Redis database
3. Copy REST API URL and token

### 3. Configure Backend

Create `backend/.env`:

```env
DB_HOST=localhost:5432
DB_NAME=ratelimitx
DB_USER=ratelimitx
DB_PASSWORD=dev_password
DB_SSLMODE=disable
JWT_SECRET=your-secret-key-min-32-chars-long
UPSTASH_REDIS_URL=https://your-redis.upstash.io
UPSTASH_REDIS_TOKEN=your-redis-token
PORT=8080
```

### 4. Run Backend

```bash
cd backend
mvn spring-boot:run
```

Backend runs on http://localhost:8080

### 5. Configure Frontend

Create `frontend/dashboard/.env`:

```env
VITE_API_URL=http://localhost:8080
```

### 6. Run Frontend

```bash
cd frontend/dashboard
npm install
npm run dev
```

Frontend runs on http://localhost:5173

## First Steps

1. **Register**: Go to http://localhost:5173/register
2. **Save API Key**: Copy the API key shown after registration
3. **Create Rule**: Go to Rules page and create a rate limit rule
4. **Test API**: Use the API key to make rate limit checks

## Example API Usage

```bash
# Register
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'

# Check rate limit
curl -X POST http://localhost:8080/api/v1/check \
  -H "Content-Type: application/json" \
  -H "X-API-Key: rlx_your-api-key" \
  -d '{
    "identifier": "user123",
    "resource": "api.test",
    "tokens": 1
  }'
```

## Next Steps

- Read [DEPLOYMENT.md](./DEPLOYMENT.md) for free-tier deployment
- Check [README.md](../README.md) for full documentation
- Explore the dashboard features

---

That's it! You're ready to use RateLimitX. 🎉

