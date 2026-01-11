package com.ratelimitx.controller;

import com.ratelimitx.common.dto.ApiResponse;
import com.ratelimitx.common.dto.RateLimitCheckRequest;
import com.ratelimitx.common.dto.RateLimitCheckResponse;
import com.ratelimitx.exception.InvalidApiKeyException;
import com.ratelimitx.service.ApiKeyService;
import com.ratelimitx.service.RateLimitApiService;
import com.ratelimitx.service.RateLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class RateLimitController {

    private final RateLimitService rateLimitService;
    private final ApiKeyService apiKeyService;
    private final RateLimitApiService rateLimitApiService;

    /**
     * Check if a request should be rate limited
     */
    @PostMapping("/check")
    public ResponseEntity<RateLimitCheckResponse> check(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestBody @Valid RateLimitCheckRequest request) {

        // Rate limit the API itself
        if (!rateLimitApiService.isAllowed(apiKey)) {
            RateLimitCheckResponse response = RateLimitCheckResponse.builder()
                    .allowed(false)
                    .remaining(0)
                    .resetAt(System.currentTimeMillis() + 60000)
                    .retryAfter(60)
                    .build();

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-RateLimit-Remaining", "0")
                    .header("X-RateLimit-Reset", String.valueOf(response.getResetAt()))
                    .header("Retry-After", "60")
                    .body(response);
        }

        // Validate API key and get tenant (throws InvalidApiKeyException if invalid)
        UUID tenantId = apiKeyService.validateAndGetTenant(apiKey);

        // Perform rate limit check
        RateLimitCheckResponse response = rateLimitService.check(tenantId, request);

        HttpStatus status = response.getAllowed() ? HttpStatus.OK : HttpStatus.TOO_MANY_REQUESTS;

        return ResponseEntity
                .status(status)
                .header("X-RateLimit-Remaining", String.valueOf(response.getRemaining()))
                .header("X-RateLimit-Reset", String.valueOf(response.getResetAt()))
                .header("Retry-After", String.valueOf(response.getRetryAfter()))
                .body(response);
    }

    /**
     * Get current API usage for the authenticated API key
     */
    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<RateLimitApiService.ApiUsageInfo>> getApiUsage(
            @RequestHeader("X-API-Key") String apiKey) {

        // Validate API key
        apiKeyService.validateAndGetTenant(apiKey);

        // Get usage info
        RateLimitApiService.ApiUsageInfo usageInfo = rateLimitApiService.getUsageInfo(apiKey);

        return ResponseEntity.ok(ApiResponse.success(usageInfo));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("OK"));
    }
}