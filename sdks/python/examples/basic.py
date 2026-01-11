#!/usr/bin/env python3
"""Basic example of using RateLimitX Python SDK"""

from ratelimitx import RateLimitX, RateLimitCheckRequest

def main():
    # Initialize client
    client = RateLimitX(
        base_url="http://localhost:8080",
        api_key="rlx_your-api-key"
    )

    try:
        # Check rate limit
        request = RateLimitCheckRequest(
            identifier="user123",
            resource="api.payment.create",
            tokens=1
        )
        result = client.check(request)

        if result.allowed:
            print(f"✅ Allowed! {result.remaining} requests remaining")
        else:
            print(f"❌ Rate limited! Retry after {result.retryAfter} seconds")

        # Get analytics
        analytics = client.get_realtime_analytics()
        print("\n📊 Analytics:")
        print(f"Total checks: {analytics.totalChecks}")
        print(f"Rate limit hits: {analytics.rateLimitHits}")
        print(f"Hit rate: {analytics.hitRate:.2f}%")
        print(f"Remaining checks this month: {analytics.remainingChecksThisMonth}")

    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    main()

