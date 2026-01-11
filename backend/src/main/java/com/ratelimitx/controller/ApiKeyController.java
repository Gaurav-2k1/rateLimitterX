package com.ratelimitx.controller;

import com.ratelimitx.common.dto.ApiResponse;
import com.ratelimitx.common.entity.ApiKey;
import com.ratelimitx.service.ApiKeyService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {
    
    private final ApiKeyService apiKeyService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiKey>>> getApiKeys(Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        List<ApiKey> keys = apiKeyService.getApiKeysByTenant(tenantId);
        // Mask keys for security
        keys.forEach(key -> key.setKeyHash("****" + key.getKeyHash().substring(Math.max(0, key.getKeyHash().length() - 4))));
        return ResponseEntity.ok(ApiResponse.success(keys));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createApiKey(
            @RequestBody CreateApiKeyRequest request,
            Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        ApiKey key = apiKeyService.createApiKey(tenantId, request.getName(), request.getEnvironment());
        String plainKey = key.getKeyHash(); // Temporarily stored plain key
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "id", key.getId().toString(),
            "apiKey", plainKey,
            "name", key.getName(),
            "environment", key.getEnvironment()
        )));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteApiKey(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        apiKeyService.deleteApiKey(id, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    @PostMapping("/{id}/rotate")
    public ResponseEntity<ApiResponse<Map<String, String>>> rotateApiKey(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        String newKey = apiKeyService.rotateApiKey(id, tenantId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "apiKey", newKey
        )));
    }
    
    private UUID getTenantId(Authentication authentication) {
        // Extract tenant ID from JWT claims
        return UUID.fromString(authentication.getName());
    }
    
    @Data
    static class CreateApiKeyRequest {
        private String name;
        private String environment;
    }
}

