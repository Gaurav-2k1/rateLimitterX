package com.ratelimitx.service;

import com.ratelimitx.common.dto.RateLimitCheckRequest;
import com.ratelimitx.common.dto.RateLimitCheckResponse;
import com.ratelimitx.common.entity.RateLimitRule;
import com.ratelimitx.repository.RateLimitRuleRepository;
import com.ratelimitx.service.algorithm.FixedWindowAlgorithm;
import com.ratelimitx.service.algorithm.SlidingWindowAlgorithm;
import com.ratelimitx.service.algorithm.TokenBucketAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RateLimitRuleRepository ruleRepository;
    private final TokenBucketAlgorithm tokenBucket;
    private final SlidingWindowAlgorithm slidingWindow;
    private final FixedWindowAlgorithm fixedWindow;
    private final MetricsService metricsService;
    private final TierService tierService;
    private final AlertService alertService;

    @Value("${ratelimit.fail-open:true}")
    private boolean failOpen;

    @Value("${ratelimit.redis.failure-threshold:5}")
    private int redisFailureThreshold;

    // Circuit breaker state
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    public RateLimitCheckResponse check(UUID tenantId, RateLimitCheckRequest request) {
        long startTime = System.currentTimeMillis();

        // Validate input to prevent Redis key injection
        validateRequest(request);

        try {
            // Validate tier limits (monthly check count)
            try {
                tierService.validateCheckRequest(tenantId);
            } catch (RuntimeException e) {
                alertService.sendTierLimitExceededAlert(tenantId);
                throw e;
            }

            // Check tier limit alerts (80%, 90%)
            alertService.checkAndSendTierLimitAlerts(tenantId);

            // Find applicable rules
            List<RateLimitRule> applicableRules = findApplicableRules(tenantId, request);

            if (applicableRules.isEmpty()) {
                log.warn("No active rule found for tenant: {}, resource: {}", tenantId, request.getResource());
                return createDefaultAllowedResponse();
            }

            // Check all applicable rules (most restrictive wins)
            RateLimitCheckResponse finalResponse = null;

            for (RateLimitRule rule : applicableRules) {
                String key = buildRedisKey(tenantId, rule, request);

                try {
                    RateLimitCheckResponse response = applyAlgorithm(rule, key);

                    // Reset failure counter on success
                    consecutiveFailures.set(0);

                    if (!response.getAllowed()) {
                        // Request denied - return immediately
                        recordMetrics(tenantId, request, false, System.currentTimeMillis() - startTime);
                        return response;
                    }

                    // Track most restrictive remaining count
                    if (finalResponse == null || response.getRemaining() < finalResponse.getRemaining()) {
                        finalResponse = response;
                    }

                } catch (Exception e) {
                    log.error("Error applying rate limit algorithm for rule: {}", rule.getId(), e);

                    int failures = consecutiveFailures.incrementAndGet();

                    // Circuit breaker logic
                    if (failures >= redisFailureThreshold) {
                        log.error("Redis failure threshold reached ({}), circuit breaker activated", failures);
                        alertService.sendTierLimitExceededAlert(tenantId);
                    }

                    // Handle based on fail-open/fail-closed strategy
                    if (!failOpen) {
                        // Fail closed - deny request
                        return RateLimitCheckResponse.builder()
                                .allowed(false)
                                .remaining(0)
                                .resetAt(System.currentTimeMillis() + 60000)
                                .retryAfter(60)
                                .build();
                    }
                    // Fail open - continue to next rule or allow
                }
            }

            // All rules passed or failed open
            if (finalResponse == null) {
                finalResponse = createDefaultAllowedResponse();
            }

            recordMetrics(tenantId, request, finalResponse.getAllowed(),
                    System.currentTimeMillis() - startTime);

            return finalResponse;

        } catch (Exception e) {
            log.error("Error checking rate limit for tenant: {}", tenantId, e);
            recordMetrics(tenantId, request, failOpen, System.currentTimeMillis() - startTime);

            if (failOpen) {
                return createDefaultAllowedResponse();
            } else {
                return RateLimitCheckResponse.builder()
                        .allowed(false)
                        .remaining(0)
                        .resetAt(System.currentTimeMillis() + 60000)
                        .retryAfter(60)
                        .build();
            }
        }
    }

    private void validateRequest(RateLimitCheckRequest request) {
        if (request.getResource() == null || request.getResource().trim().isEmpty()) {
            throw new IllegalArgumentException("Resource cannot be empty");
        }

        // Prevent Redis key injection by validating characters
        String resource = request.getResource();
        String identifier = request.getIdentifier();

        if (resource.contains(":") || resource.contains(" ") || resource.contains("\n")) {
            throw new IllegalArgumentException("Invalid characters in resource");
        }

        if (identifier != null && (identifier.contains(":") || identifier.contains(" ") || identifier.contains("\n"))) {
            throw new IllegalArgumentException("Invalid characters in identifier");
        }
    }

    private List<RateLimitRule> findApplicableRules(UUID tenantId, RateLimitCheckRequest request) {
        List<RateLimitRule> allRules = ruleRepository.findByTenantIdAndActive(tenantId, true);

        return allRules.stream()
                .filter(rule -> {
                    return switch (rule.getLimitScope()) {
                        case GLOBAL -> true;
                        case RESOURCE -> rule.getResource().equals(request.getResource()) ||
                                rule.getResource().equals("*");
                        case IDENTIFIER -> {
                            // Ensure identifier is present for IDENTIFIER scope
                            if (request.getIdentifier() == null || request.getIdentifier().isEmpty()) {
                                yield false;
                            }
                            yield (rule.getResource().equals(request.getResource()) ||
                                    rule.getResource().equals("*"));
                        }
                    };
                })
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
                .collect(java.util.stream.Collectors.toList());
    }

    private String buildRedisKey(UUID tenantId, RateLimitRule rule, RateLimitCheckRequest request) {
        // Sanitize inputs (already validated, but extra safety)
        String sanitizedResource = request.getResource().replaceAll("[^a-zA-Z0-9_-]", "_");
        String sanitizedIdentifier = request.getIdentifier() != null ?
                request.getIdentifier().replaceAll("[^a-zA-Z0-9_-]", "_") : "";

        return switch (rule.getLimitScope()) {
            case GLOBAL -> String.format("rl:%s:global:%s", tenantId, rule.getId());
            case RESOURCE -> String.format("rl:%s:res:%s:%s", tenantId, sanitizedResource, rule.getId());
            case IDENTIFIER -> String.format("rl:%s:id:%s:%s:%s",
                    tenantId, sanitizedResource, sanitizedIdentifier, rule.getId());
        };
    }

    private RateLimitCheckResponse applyAlgorithm(RateLimitRule rule, String key) {
        return switch (rule.getAlgorithm()) {
            case TOKEN_BUCKET -> tokenBucket.check(key, rule.getMaxRequests(), rule.getWindowSeconds());
            case SLIDING_WINDOW -> slidingWindow.check(key, rule.getMaxRequests(), rule.getWindowSeconds());
            case FIXED_WINDOW -> fixedWindow.check(key, rule.getMaxRequests(), rule.getWindowSeconds());
        };
    }

    private RateLimitCheckResponse createDefaultAllowedResponse() {
        return RateLimitCheckResponse.builder()
                .allowed(true)
                .remaining(999)
                .resetAt(System.currentTimeMillis() + 3600000)
                .retryAfter(0)
                .build();
    }

    private void recordMetrics(UUID tenantId, RateLimitCheckRequest request,
                               boolean allowed, long latency) {
        try {
            metricsService.recordCheckAsync(tenantId, request.getResource(),
                    request.getIdentifier(), allowed, latency);
        } catch (Exception e) {
            log.error("Failed to record metrics", e);
            // Don't fail the request due to metrics failure
        }
    }
}