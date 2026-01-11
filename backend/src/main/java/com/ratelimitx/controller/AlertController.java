package com.ratelimitx.controller;

import com.ratelimitx.common.dto.ApiResponse;
import com.ratelimitx.common.entity.AlertConfiguration;
import com.ratelimitx.repository.AlertConfigurationRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertController {
    
    private final AlertConfigurationRepository alertRepository;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertConfiguration>>> getAlerts(Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        List<AlertConfiguration> alerts = alertRepository.findByTenantIdAndEnabled(tenantId, true);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<AlertConfiguration>> createAlert(
            @RequestBody CreateAlertRequest request,
            Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        
        AlertConfiguration alert = AlertConfiguration.builder()
            .tenantId(tenantId)
            .alertType(AlertConfiguration.AlertType.valueOf(request.getAlertType()))
            .destination(request.getDestination())
            .destinationType(AlertConfiguration.DestinationType.valueOf(request.getDestinationType()))
            .thresholdPercent(request.getThresholdPercent())
            .enabled(true)
            .build();
        
        alert = alertRepository.save(alert);
        return ResponseEntity.ok(ApiResponse.success(alert));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAlert(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID tenantId = getTenantId(authentication);
        
        AlertConfiguration alert = alertRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alert not found"));
        
        if (!alert.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        alertRepository.delete(alert);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    private UUID getTenantId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
    
    @Data
    static class CreateAlertRequest {
        private String alertType;
        private String destination;
        private String destinationType;
        private Integer thresholdPercent;
    }
}

