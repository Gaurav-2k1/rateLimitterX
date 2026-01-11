package com.ratelimitx.sdk.examples;

import com.ratelimitx.sdk.RateLimitX;
import com.ratelimitx.sdk.RateLimitXException;
import static com.ratelimitx.sdk.Models.*;

public class BasicExample {
    public static void main(String[] args) {
        // Initialize client
        RateLimitX client = new RateLimitX(
            "http://localhost:8080",
            "rlx_your-api-key"
        );

        try {
            // Check rate limit
            RateLimitCheckRequest request = new RateLimitCheckRequest(
                "user123",
                "api.payment.create",
                1
            );
            RateLimitCheckResponse result = client.check(request);

            if (result.getAllowed()) {
                System.out.println("✅ Allowed! " + result.getRemaining() + " requests remaining");
            } else {
                System.out.println("❌ Rate limited! Retry after " + result.getRetryAfter() + " seconds");
            }

            // Get analytics
            AnalyticsMetrics analytics = client.getRealtimeAnalytics();
            System.out.println("\n📊 Analytics:");
            System.out.println("Total checks: " + analytics.getTotalChecks());
            System.out.println("Rate limit hits: " + analytics.getRateLimitHits());
            System.out.println("Hit rate: " + String.format("%.2f%%", analytics.getHitRate()));
            System.out.println("Remaining checks this month: " + analytics.getRemainingChecksThisMonth());

        } catch (RateLimitXException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}

