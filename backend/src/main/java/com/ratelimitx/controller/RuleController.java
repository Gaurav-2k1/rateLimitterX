package com.ratelimitx.controller;

import com.ratelimitx.common.dto.ApiResponse;
import com.ratelimitx.common.entity.RateLimitRule;
import com.ratelimitx.repository.RateLimitRuleRepository;
import com.ratelimitx.service.TierService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/rules")
@RequiredArgsConstructor
public class RuleController {
    
    private final RateLimitRuleRepository ruleRepository;
    private final TierService tierService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<RateLimitRule>>> getRules(Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        List<RateLimitRule> rules = ruleRepository.findByTenantId(tenantId);
        return ResponseEntity.ok(ApiResponse.success(rules));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<RateLimitRule>> createRule(
            @RequestBody CreateRuleRequest request,
            Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        
        // Validate tier limits
        tierService.validateRuleCreation(tenantId);
        
        RateLimitRule rule = RateLimitRule.builder()
            .tenantId(tenantId)
            .resource(request.getResource())
            .algorithm(RateLimitRule.Algorithm.valueOf(request.getAlgorithm()))
            .maxRequests(request.getMaxRequests())
            .windowSeconds(request.getWindowSeconds())
            .burstCapacity(request.getBurstCapacity())
            .identifierType(RateLimitRule.IdentifierType.valueOf(request.getIdentifierType()))
            .active(true)
            .build();
        
        rule = ruleRepository.save(rule);
        return ResponseEntity.ok(ApiResponse.success(rule));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RateLimitRule>> updateRule(
            @PathVariable UUID id,
            @RequestBody UpdateRuleRequest request,
            Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        
        RateLimitRule rule = ruleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Rule not found"));
        
        if (!rule.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        if (request.getResource() != null) rule.setResource(request.getResource());
        if (request.getAlgorithm() != null) rule.setAlgorithm(RateLimitRule.Algorithm.valueOf(request.getAlgorithm()));
        if (request.getMaxRequests() != null) rule.setMaxRequests(request.getMaxRequests());
        if (request.getWindowSeconds() != null) rule.setWindowSeconds(request.getWindowSeconds());
        if (request.getBurstCapacity() != null) rule.setBurstCapacity(request.getBurstCapacity());
        if (request.getActive() != null) rule.setActive(request.getActive());
        
        rule = ruleRepository.save(rule);
        return ResponseEntity.ok(ApiResponse.success(rule));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        
        RateLimitRule rule = ruleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Rule not found"));
        
        if (!rule.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        ruleRepository.delete(rule);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    private UUID getTenantId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
    
    @Data
    static class CreateRuleRequest {
        private String resource;
        private String algorithm;
        private Integer maxRequests;
        private Integer windowSeconds;
        private Integer burstCapacity;
        private String identifierType;
    }
    
    @Data
    static class UpdateRuleRequest {
        private String resource;
        private String algorithm;
        private Integer maxRequests;
        private Integer windowSeconds;
        private Integer burstCapacity;
        private Boolean active;
        private String identifierType;
    }
}

