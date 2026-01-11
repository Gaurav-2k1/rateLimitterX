package com.ratelimitx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.ratelimitx.common.dto.ApiResponse;
import com.ratelimitx.common.entity.RateLimitRule;
import com.ratelimitx.repository.RateLimitRuleRepository;
import com.ratelimitx.service.TierService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bulk")
@RequiredArgsConstructor
public class BulkController {
    
    private final RateLimitRuleRepository ruleRepository;
    private final TierService tierService;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importRules(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "json") String format,
            Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        
        try {
            String content = new String(file.getBytes());
            List<RuleImport> rules;
            
            if ("yaml".equalsIgnoreCase(format) || "yml".equalsIgnoreCase(format)) {
                rules = Arrays.asList(yamlMapper.readValue(content, RuleImport[].class));
            } else {
                rules = Arrays.asList(jsonMapper.readValue(content, RuleImport[].class));
            }
            
            int created = 0;
            int skipped = 0;
            List<String> errors = new ArrayList<>();
            
            for (RuleImport ruleImport : rules) {
                try {
                    // Validate tier limits
                    tierService.validateRuleCreation(tenantId);
                    
                    RateLimitRule rule = RateLimitRule.builder()
                        .tenantId(tenantId)
                        .resource(ruleImport.getResource())
                        .algorithm(RateLimitRule.Algorithm.valueOf(ruleImport.getAlgorithm()))
                        .maxRequests(ruleImport.getMaxRequests())
                        .windowSeconds(ruleImport.getWindowSeconds())
                        .burstCapacity(ruleImport.getBurstCapacity())
                        .identifierType(ruleImport.getIdentifierType() != null ?
                            RateLimitRule.IdentifierType.valueOf(ruleImport.getIdentifierType()) :
                            RateLimitRule.IdentifierType.USER_ID)
                        .limitScope(ruleImport.getLimitScope() != null ?
                            RateLimitRule.LimitScope.valueOf(ruleImport.getLimitScope()) :
                            RateLimitRule.LimitScope.RESOURCE)
                        .priority(ruleImport.getPriority() != null ? ruleImport.getPriority() : 0)
                        .active(true)
                        .build();
                    
                    ruleRepository.save(rule);
                    created++;
                } catch (Exception e) {
                    skipped++;
                    errors.add(ruleImport.getResource() + ": " + e.getMessage());
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("created", created);
            result.put("skipped", skipped);
            result.put("errors", errors);
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to parse file: " + e.getMessage()));
        }
    }
    
    @GetMapping("/export")
    public ResponseEntity<String> exportRules(
            @RequestParam(defaultValue = "json") String format,
            Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        
        List<RateLimitRule> rules = ruleRepository.findByTenantId(tenantId);
        List<RuleExport> exports = rules.stream()
            .map(rule -> new RuleExport(
                rule.getResource(),
                rule.getAlgorithm().name(),
                rule.getMaxRequests(),
                rule.getWindowSeconds(),
                rule.getBurstCapacity(),
                rule.getIdentifierType().name(),
                rule.getLimitScope().name(),
                rule.getPriority(),
                rule.getActive()
            ))
            .collect(Collectors.toList());
        
        try {
            String content;
            MediaType mediaType;
            
            if ("yaml".equalsIgnoreCase(format) || "yml".equalsIgnoreCase(format)) {
                content = yamlMapper.writeValueAsString(exports);
                mediaType = MediaType.parseMediaType("application/x-yaml");
            } else {
                content = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exports);
                mediaType = MediaType.APPLICATION_JSON;
            }
            
            return ResponseEntity.ok()
                .contentType(mediaType)
                .header("Content-Disposition", "attachment; filename=rules." + format)
                .body(content);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    private UUID getTenantId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
    
    @Data
    static class RuleImport {
        private String resource;
        private String algorithm;
        private Integer maxRequests;
        private Integer windowSeconds;
        private Integer burstCapacity;
        private String identifierType;
        private String limitScope;
        private Integer priority;
    }
    
    @Data
    @lombok.AllArgsConstructor
    static class RuleExport {
        private String resource;
        private String algorithm;
        private Integer maxRequests;
        private Integer windowSeconds;
        private Integer burstCapacity;
        private String identifierType;
        private String limitScope;
        private Integer priority;
        private Boolean active;
    }
}

