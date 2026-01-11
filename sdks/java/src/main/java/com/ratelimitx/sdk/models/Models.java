package com.ratelimitx.sdk;

import java.util.Map;

/**
 * Data models for RateLimitX SDK
 */
public class Models {
    public static class RateLimitCheckRequest {
        private String identifier;
        private String resource;
        private Integer tokens = 1;

        public RateLimitCheckRequest(String identifier, String resource) {
            this.identifier = identifier;
            this.resource = resource;
        }

        public RateLimitCheckRequest(String identifier, String resource, Integer tokens) {
            this.identifier = identifier;
            this.resource = resource;
            this.tokens = tokens;
        }

        public String getIdentifier() { return identifier; }
        public void setIdentifier(String identifier) { this.identifier = identifier; }
        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }
        public Integer getTokens() { return tokens; }
        public void setTokens(Integer tokens) { this.tokens = tokens; }
    }

    public static class RateLimitCheckResponse {
        private Boolean allowed;
        private Integer remaining;
        private Long resetAt;
        private Integer retryAfter;

        public Boolean getAllowed() { return allowed; }
        public void setAllowed(Boolean allowed) { this.allowed = allowed; }
        public Integer getRemaining() { return remaining; }
        public void setRemaining(Integer remaining) { this.remaining = remaining; }
        public Long getResetAt() { return resetAt; }
        public void setResetAt(Long resetAt) { this.resetAt = resetAt; }
        public Integer getRetryAfter() { return retryAfter; }
        public void setRetryAfter(Integer retryAfter) { this.retryAfter = retryAfter; }
    }

    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String apiKey;
        private String tenantId;

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    }

    public static class RateLimitRule {
        private String id;
        private String resource;
        private String algorithm;
        private Integer maxRequests;
        private Integer windowSeconds;
        private Integer burstCapacity;
        private String identifierType;
        private String limitScope;
        private Integer priority;
        private Boolean active;
        private String createdAt;
        private String updatedAt;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public Integer getMaxRequests() { return maxRequests; }
        public void setMaxRequests(Integer maxRequests) { this.maxRequests = maxRequests; }
        public Integer getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(Integer windowSeconds) { this.windowSeconds = windowSeconds; }
        public Integer getBurstCapacity() { return burstCapacity; }
        public void setBurstCapacity(Integer burstCapacity) { this.burstCapacity = burstCapacity; }
        public String getIdentifierType() { return identifierType; }
        public void setIdentifierType(String identifierType) { this.identifierType = identifierType; }
        public String getLimitScope() { return limitScope; }
        public void setLimitScope(String limitScope) { this.limitScope = limitScope; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class CreateRuleRequest {
        private String resource;
        private String algorithm;
        private Integer maxRequests;
        private Integer windowSeconds;
        private Integer burstCapacity;
        private String identifierType;

        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public Integer getMaxRequests() { return maxRequests; }
        public void setMaxRequests(Integer maxRequests) { this.maxRequests = maxRequests; }
        public Integer getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(Integer windowSeconds) { this.windowSeconds = windowSeconds; }
        public Integer getBurstCapacity() { return burstCapacity; }
        public void setBurstCapacity(Integer burstCapacity) { this.burstCapacity = burstCapacity; }
        public String getIdentifierType() { return identifierType; }
        public void setIdentifierType(String identifierType) { this.identifierType = identifierType; }
    }

    public static class UpdateRuleRequest {
        private String resource;
        private String algorithm;
        private Integer maxRequests;
        private Integer windowSeconds;
        private Integer burstCapacity;
        private Boolean active;
        private String identifierType;

        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public Integer getMaxRequests() { return maxRequests; }
        public void setMaxRequests(Integer maxRequests) { this.maxRequests = maxRequests; }
        public Integer getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(Integer windowSeconds) { this.windowSeconds = windowSeconds; }
        public Integer getBurstCapacity() { return burstCapacity; }
        public void setBurstCapacity(Integer burstCapacity) { this.burstCapacity = burstCapacity; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        public String getIdentifierType() { return identifierType; }
        public void setIdentifierType(String identifierType) { this.identifierType = identifierType; }
    }

    public static class ApiKey {
        private String id;
        private String name;
        private String environment;
        private String keyHash;
        private String createdAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        public String getKeyHash() { return keyHash; }
        public void setKeyHash(String keyHash) { this.keyHash = keyHash; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    public static class CreateApiKeyResponse {
        private String id;
        private String apiKey;
        private String name;
        private String environment;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
    }

    public static class RotateApiKeyResponse {
        private String apiKey;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public static class AnalyticsMetrics {
        private Long totalChecks;
        private Long rateLimitHits;
        private Double hitRate;
        private Integer latencyP50;
        private Integer latencyP95;
        private Integer latencyP99;
        private Integer remainingChecksThisMonth;
        private String timestamp;

        public Long getTotalChecks() { return totalChecks; }
        public void setTotalChecks(Long totalChecks) { this.totalChecks = totalChecks; }
        public Long getRateLimitHits() { return rateLimitHits; }
        public void setRateLimitHits(Long rateLimitHits) { this.rateLimitHits = rateLimitHits; }
        public Double getHitRate() { return hitRate; }
        public void setHitRate(Double hitRate) { this.hitRate = hitRate; }
        public Integer getLatencyP50() { return latencyP50; }
        public void setLatencyP50(Integer latencyP50) { this.latencyP50 = latencyP50; }
        public Integer getLatencyP95() { return latencyP95; }
        public void setLatencyP95(Integer latencyP95) { this.latencyP95 = latencyP95; }
        public Integer getLatencyP99() { return latencyP99; }
        public void setLatencyP99(Integer latencyP99) { this.latencyP99 = latencyP99; }
        public Integer getRemainingChecksThisMonth() { return remainingChecksThisMonth; }
        public void setRemainingChecksThisMonth(Integer remainingChecksThisMonth) { this.remainingChecksThisMonth = remainingChecksThisMonth; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    public static class TopIdentifier {
        private String identifier;
        private Long deniedCount;

        public String getIdentifier() { return identifier; }
        public void setIdentifier(String identifier) { this.identifier = identifier; }
        public Long getDeniedCount() { return deniedCount; }
        public void setDeniedCount(Long deniedCount) { this.deniedCount = deniedCount; }
    }

    public static class AlertConfiguration {
        private String id;
        private String alertType;
        private String destination;
        private String destinationType;
        private Integer thresholdPercent;
        private Boolean enabled;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public String getDestinationType() { return destinationType; }
        public void setDestinationType(String destinationType) { this.destinationType = destinationType; }
        public Integer getThresholdPercent() { return thresholdPercent; }
        public void setThresholdPercent(Integer thresholdPercent) { this.thresholdPercent = thresholdPercent; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }

    public static class CreateAlertRequest {
        private String alertType;
        private String destination;
        private String destinationType;
        private Integer thresholdPercent;

        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public String getDestinationType() { return destinationType; }
        public void setDestinationType(String destinationType) { this.destinationType = destinationType; }
        public Integer getThresholdPercent() { return thresholdPercent; }
        public void setThresholdPercent(Integer thresholdPercent) { this.thresholdPercent = thresholdPercent; }
    }

    public static class ApiResponse<T> {
        private Boolean success;
        private T data;
        private String error;
        private String timestamp;

        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
}

