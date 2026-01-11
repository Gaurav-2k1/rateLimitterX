# RateLimitX Deployment Guide

## Free Tier Deployment Strategy

This guide walks you through deploying RateLimitX completely free using free-tier services.

## Services Used

### 1. Backend Hosting: Render.com (FREE)
- **Free Tier**: 750 hours/month
- **Limitations**: Auto-sleeps after 15 min inactivity
- **RAM**: 512 MB
- **Perfect for**: Development, demos, low-traffic production

### 2. Frontend Hosting: Vercel (FREE)
- **Free Tier**: Unlimited
- **Features**: Global CDN, automatic HTTPS, custom domains
- **Perfect for**: Any production use

### 3. Database: Neon.tech (FREE)
- **Free Tier**: 0.5 GB storage
- **Features**: Serverless PostgreSQL, auto-suspend
- **Perfect for**: Development and small production

### 4. Redis: Upstash (FREE)
- **Free Tier**: 10,000 commands/day
- **Features**: Serverless Redis, REST API
- **Perfect for**: Rate limiting (low to medium traffic)

## Step-by-Step Deployment

### Step 1: Setup Database (Neon.tech)

1. Go to https://neon.tech
2. Sign up for free account
3. Create a new project
4. Copy the connection string (format: `postgresql://user:password@host/dbname`)
5. Note down:
   - Host
   - Database name
   - Username
   - Password

### Step 2: Setup Redis (Upstash)

1. Go to https://upstash.com
2. Sign up for free account
3. Create a new Redis database
4. Go to "REST API" tab
5. Copy:
   - REST API URL
   - REST API Token

### Step 3: Deploy Backend (Render.com)

1. Push your code to GitHub
2. Go to https://render.com
3. Sign up for free account
4. Click "New" → "Web Service"
5. Connect your GitHub repository
6. Configure:
   - **Name**: `ratelimitx-backend`
   - **Region**: Choose closest to you
   - **Branch**: `main`
   - **Root Directory**: `backend`
   - **Environment**: `Java`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/ratelimitx-1.0.0.jar`
7. Add Environment Variables:
   ```
   DB_HOST=your-neon-host
   DB_NAME=ratelimitx
   DB_USER=your-neon-user
   DB_PASSWORD=your-neon-password
   DB_SSLMODE=require
   UPSTASH_REDIS_URL=https://your-redis.upstash.io
   UPSTASH_REDIS_TOKEN=your-redis-token
   JWT_SECRET=generate-a-random-32-char-secret
   PORT=8080
   ```
8. Click "Create Web Service"
9. Wait for deployment (5-10 minutes)
10. Copy your service URL (e.g., `https://ratelimitx-backend.onrender.com`)

### Step 4: Deploy Frontend (Vercel)

1. Go to https://vercel.com
2. Sign up for free account
3. Click "Add New" → "Project"
4. Import your GitHub repository
5. Configure:
   - **Framework Preset**: Vite
   - **Root Directory**: `frontend/dashboard`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
6. Add Environment Variable:
   ```
   VITE_API_URL=https://ratelimitx-backend.onrender.com
   ```
7. Click "Deploy"
8. Wait for deployment (2-3 minutes)
9. Your frontend is live!

### Step 5: Verify Deployment

1. Visit your Vercel URL
2. Register a new account
3. Create a rate limit rule
4. Test the API endpoint

## Testing the API

### Register a User

```bash
curl -X POST https://your-backend.onrender.com/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

Save the `apiKey` from the response.

### Create a Rate Limit Rule

First, login to get JWT token:

```bash
curl -X POST https://your-backend.onrender.com/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

Then create a rule:

```bash
curl -X POST https://your-backend.onrender.com/rules \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "resource": "api.test",
    "algorithm": "TOKEN_BUCKET",
    "maxRequests": 100,
    "windowSeconds": 60,
    "identifierType": "USER_ID"
  }'
```

### Test Rate Limiting

```bash
curl -X POST https://your-backend.onrender.com/api/v1/check \
  -H "Content-Type: application/json" \
  -H "X-API-Key: YOUR_API_KEY" \
  -d '{
    "identifier": "user123",
    "resource": "api.test",
    "tokens": 1
  }'
```

## Monitoring

### Render.com
- View logs in Render dashboard
- Check service health
- Monitor resource usage

### Vercel
- View deployment logs
- Check analytics
- Monitor performance

### Upstash
- View command usage in dashboard
- Monitor Redis operations
- Check rate limits

## Troubleshooting

### Backend won't start
- Check environment variables are set correctly
- Verify database connection string
- Check Redis credentials
- View logs in Render dashboard

### Frontend can't connect to backend
- Verify `VITE_API_URL` is correct
- Check CORS settings
- Ensure backend is running (not sleeping)

### Rate limiting not working
- Check Upstash Redis quota (10K commands/day)
- Verify API key is correct
- Check rule is active
- View backend logs

## Cost Optimization

1. **Use Render's free tier efficiently**:
   - Service sleeps after 15 min inactivity
   - First request after sleep takes ~30 seconds
   - Consider using Railway.app ($5 credit/month) for no sleep

2. **Monitor Upstash usage**:
   - 10K commands/day = ~7 commands/second average
   - For higher traffic, consider Redis Cloud free tier (30MB)

3. **Database optimization**:
   - Use connection pooling (already configured)
   - Clean up old metrics periodically
   - Use indexes (already created)

## Scaling Beyond Free Tier

When you outgrow free tier:

1. **Backend**: Upgrade to Render paid ($7/month) or Railway ($5/month)
2. **Database**: Upgrade Neon to paid tier ($19/month)
3. **Redis**: Upgrade Upstash to paid tier ($0.20 per 100K commands)
4. **Frontend**: Vercel Pro ($20/month) for team features

## Security Checklist

- [ ] Change JWT secret to strong random value
- [ ] Use strong database passwords
- [ ] Enable SSL for database (already done with Neon)
- [ ] Configure CORS properly for production
- [ ] Set up monitoring and alerts
- [ ] Regular backups (Neon has automatic backups)

## Support

For issues:
1. Check logs in Render/Vercel dashboards
2. Verify environment variables
3. Test API endpoints directly
4. Open GitHub issue

---

Happy deploying! 🚀

