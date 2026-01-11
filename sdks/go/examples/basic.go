package main

import (
	"fmt"
	"log"

	"github.com/ratelimitx/ratelimitx-go"
)

func main() {
	// Initialize client
	client := ratelimitx.New("http://localhost:8080", "rlx_your-api-key")

	// Check rate limit
	req := ratelimitx.RateLimitCheckRequest{
		Identifier: "user123",
		Resource:   "api.payment.create",
		Tokens:     1,
	}

	result, err := client.Check(req)
	if err != nil {
		log.Fatal(err)
	}

	if result.Allowed {
		fmt.Printf("✅ Allowed! %d requests remaining\n", result.Remaining)
	} else {
		fmt.Printf("❌ Rate limited! Retry after %d seconds\n", result.RetryAfter)
	}

	// Get analytics
	analytics, err := client.GetRealtimeAnalytics()
	if err != nil {
		log.Fatal(err)
	}

	fmt.Println("\n📊 Analytics:")
	fmt.Printf("Total checks: %d\n", analytics.TotalChecks)
	fmt.Printf("Rate limit hits: %d\n", analytics.RateLimitHits)
	fmt.Printf("Hit rate: %.2f%%\n", analytics.HitRate)
	if analytics.RemainingChecksThisMonth != nil {
		fmt.Printf("Remaining checks this month: %d\n", *analytics.RemainingChecksThisMonth)
	}
}

