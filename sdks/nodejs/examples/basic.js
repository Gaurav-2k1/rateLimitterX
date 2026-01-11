const RateLimitX = require('../dist/index').default;

async function main() {
  // Initialize client
  const client = new RateLimitX({
    baseUrl: 'http://localhost:8080',
    apiKey: 'rlx_your-api-key'
  });

  try {
    // Check rate limit
    const result = await client.check({
      identifier: 'user123',
      resource: 'api.payment.create',
      tokens: 1
    });

    if (result.allowed) {
      console.log(`✅ Allowed! ${result.remaining} requests remaining`);
    } else {
      console.log(`❌ Rate limited! Retry after ${result.retryAfter} seconds`);
    }

    // Get analytics
    const analytics = await client.getRealtimeAnalytics();
    console.log('\n📊 Analytics:');
    console.log(`Total checks: ${analytics.totalChecks}`);
    console.log(`Rate limit hits: ${analytics.rateLimitHits}`);
    console.log(`Hit rate: ${analytics.hitRate.toFixed(2)}%`);
    console.log(`Remaining checks this month: ${analytics.remainingChecksThisMonth}`);

  } catch (error) {
    console.error('Error:', error.message);
  }
}

main();

