package com.ratelimitx.service;

import com.ratelimitx.common.entity.ApiKey;
import com.ratelimitx.exception.InvalidApiKeyException;
import com.ratelimitx.exception.ResourceNotFoundException;
import com.ratelimitx.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    /**
     * Validate API key and return the associated tenant ID
     * @param apiKey The API key to validate
     * @return UUID of the tenant
     * @throws InvalidApiKeyException if the API key is invalid or inactive
     */
    public UUID validateAndGetTenant(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new InvalidApiKeyException("API key is required");
        }

        String keyHash = hashApiKey(apiKey);
        ApiKey key = apiKeyRepository.findByKeyHash(keyHash)
                .orElseThrow(() -> {
                    log.warn("Invalid API key attempted: {}", maskApiKey(apiKey));
                    return new InvalidApiKeyException("Invalid API key");
                });

        if (!key.getActive()) {
            log.warn("Inactive API key used: {} (ID: {})", maskApiKey(apiKey), key.getId());
            throw new InvalidApiKeyException("API key is inactive");
        }

        // Check if key has expired (if expiresAt is set)
//        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(LocalDateTime.now())) {
//            log.warn("Expired API key used: {} (ID: {})", maskApiKey(apiKey), key.getId());
//            throw new InvalidApiKeyException("API key has expired");
//        }

        // Update last used timestamp asynchronously to avoid blocking
        updateLastUsedAsync(key);

        return key.getTenantId();
    }

    /**
     * Update last used timestamp asynchronously
     */
    private void updateLastUsedAsync(ApiKey key) {
        try {
            // In production, consider using @Async or a background job
            key.setLastUsedAt(LocalDateTime.now());
            apiKeyRepository.save(key);
        } catch (Exception e) {
            // Don't fail the request if updating last used fails
            log.error("Failed to update last used timestamp for API key ID: {}", key.getId(), e);
        }
    }

    /**
     * Generate a new API key with prefix
     */
    public String generateApiKey() {
        return "rlx_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Hash an API key using SHA-256
     */
    public String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Error hashing API key", e);
        }
    }

    /**
     * Mask API key for logging (show first 8 characters only)
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 8) + "***";
    }

    /**
     * Create a new API key for a tenant
     * @return ApiKey with the plain key temporarily stored in keyHash field (only shown once)
     */
    public ApiKey createApiKey(UUID tenantId, String name, String environment) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("API key name is required");
        }

        if (environment == null || environment.trim().isEmpty()) {
            throw new IllegalArgumentException("Environment is required");
        }

        String apiKey = generateApiKey();
        String keyHash = hashApiKey(apiKey);

        ApiKey key = ApiKey.builder()
                .tenantId(tenantId)
                .keyHash(keyHash)
                .name(name)
                .environment(environment)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        apiKeyRepository.save(key);

        log.info("Created new API key for tenant: {} (Name: {}, Environment: {})",
                tenantId, name, environment);

        // Return the plain key (only time it's shown)
        key.setKeyHash(apiKey); // Temporarily store plain key for response
        return key;
    }

    /**
     * Get all API keys for a tenant
     */
    public List<ApiKey> getApiKeysByTenant(UUID tenantId) {
        return apiKeyRepository.findByTenantId(tenantId);
    }

    /**
     * Delete an API key
     * @throws ResourceNotFoundException if key not found
     * @throws InvalidApiKeyException if unauthorized
     */
    @Transactional
    public void deleteApiKey(UUID keyId, UUID tenantId) {
        ApiKey key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API Key", keyId.toString()));

        if (!key.getTenantId().equals(tenantId)) {
            log.warn("Unauthorized attempt to delete API key: {} by tenant: {}", keyId, tenantId);
            throw new InvalidApiKeyException("Unauthorized to delete this API key");
        }

        apiKeyRepository.delete(key);
        log.info("Deleted API key: {} for tenant: {}", keyId, tenantId);
    }

    /**
     * Rotate an API key (deactivate old, create new)
     * @return The new plain API key (only shown once)
     * @throws ResourceNotFoundException if key not found
     * @throws InvalidApiKeyException if unauthorized
     */
    @Transactional
    public String rotateApiKey(UUID keyId, UUID tenantId) {
        ApiKey oldKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API Key", keyId.toString()));

        if (!oldKey.getTenantId().equals(tenantId)) {
            log.warn("Unauthorized attempt to rotate API key: {} by tenant: {}", keyId, tenantId);
            throw new InvalidApiKeyException("Unauthorized to rotate this API key");
        }

        // Deactivate old key
        oldKey.setActive(false);
        apiKeyRepository.save(oldKey);

        // Create new key
        String newApiKey = generateApiKey();
        String keyHash = hashApiKey(newApiKey);

        ApiKey newKey = ApiKey.builder()
                .tenantId(tenantId)
                .keyHash(keyHash)
                .name(oldKey.getName() + " (rotated)")
                .environment(oldKey.getEnvironment())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        apiKeyRepository.save(newKey);

        log.info("Rotated API key: {} for tenant: {} (New ID: {})", keyId, tenantId, newKey.getId());

        return newApiKey;
    }

    /**
     * Toggle API key active status
     */
    @Transactional
    public void toggleApiKeyStatus(UUID keyId, UUID tenantId, boolean active) {
        ApiKey key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API Key", keyId.toString()));

        if (!key.getTenantId().equals(tenantId)) {
            log.warn("Unauthorized attempt to toggle API key status: {} by tenant: {}", keyId, tenantId);
            throw new InvalidApiKeyException("Unauthorized to modify this API key");
        }

        key.setActive(active);
        apiKeyRepository.save(key);

        log.info("API key {} status changed to: {} for tenant: {}", keyId, active ? "active" : "inactive", tenantId);
    }

    /**
     * Update API key metadata (name, environment)
     */
    @Transactional
    public ApiKey updateApiKey(UUID keyId, UUID tenantId, String name, String environment) {
        ApiKey key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API Key", keyId.toString()));

        if (!key.getTenantId().equals(tenantId)) {
            log.warn("Unauthorized attempt to update API key: {} by tenant: {}", keyId, tenantId);
            throw new InvalidApiKeyException("Unauthorized to modify this API key");
        }

        if (name != null && !name.trim().isEmpty()) {
            key.setName(name);
        }

        if (environment != null && !environment.trim().isEmpty()) {
            key.setEnvironment(environment);
        }

        apiKeyRepository.save(key);

        log.info("Updated API key: {} for tenant: {}", keyId, tenantId);

        return key;
    }
}