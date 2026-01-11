package com.ratelimitx.service;

import com.ratelimitx.common.entity.AlertConfiguration;
import com.ratelimitx.common.entity.Tenant;
import com.ratelimitx.repository.AlertConfigurationRepository;
import com.ratelimitx.repository.TenantRepository;
import com.ratelimitx.repository.UsageMetricRepository;
import com.ratelimitx.service.TierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {
    
    private final AlertConfigurationRepository alertRepository;
    private final TierService tierService;
    private final UsageMetricRepository usageMetricRepository;
    private final TenantRepository tenantRepository;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    
    @Async
    public void checkAndSendTierLimitAlerts(UUID tenantId) {
        try {
            List<AlertConfiguration> alerts = alertRepository
                .findByTenantIdAndAlertTypeAndEnabled(tenantId, 
                    AlertConfiguration.AlertType.TIER_LIMIT_APPROACHING, true);
            
            if (alerts.isEmpty()) {
                return;
            }
            
            int remaining = tierService.getRemainingChecksThisMonth(tenantId);
            int maxChecks = getMaxChecksForTenant(tenantId);
            int used = maxChecks - remaining;
            double usagePercent = (double) used / maxChecks * 100;
            
            for (AlertConfiguration alert : alerts) {
                if (alert.getThresholdPercent() != null && 
                    usagePercent >= alert.getThresholdPercent() &&
                    usagePercent < alert.getThresholdPercent() + 5) { // Avoid duplicate alerts
                    sendAlert(alert, String.format(
                        "Tier limit alert: %d%% of monthly checks used (%d/%d remaining)",
                        alert.getThresholdPercent(), remaining, maxChecks));
                }
            }
        } catch (Exception e) {
            log.error("Error checking tier limit alerts", e);
        }
    }
    
    @Async
    public void sendTierLimitExceededAlert(UUID tenantId) {
        List<AlertConfiguration> alerts = alertRepository
            .findByTenantIdAndAlertTypeAndEnabled(tenantId, 
                AlertConfiguration.AlertType.TIER_LIMIT_EXCEEDED, true);
        
        for (AlertConfiguration alert : alerts) {
            sendAlert(alert, "Tier limit exceeded: Monthly check limit has been reached");
        }
    }
    
    private void sendAlert(AlertConfiguration alert, String message) {
        try {
            switch (alert.getDestinationType()) {
                case EMAIL:
                    sendEmailAlert(alert.getDestination(), message);
                    break;
                case WEBHOOK:
                    sendWebhookAlert(alert.getDestination(), message);
                    break;
                case SLACK:
                case DISCORD:
                    sendWebhookAlert(alert.getDestination(), formatSlackDiscordMessage(message));
                    break;
            }
        } catch (Exception e) {
            log.error("Error sending alert", e);
        }
    }
    
    private void sendEmailAlert(String email, String message) {
        // In production, use a proper email service (SendGrid, SES, etc.)
        log.info("Email alert to {}: {}", email, message);
        // TODO: Integrate with email service
    }
    
    private void sendWebhookAlert(String url, String message) {
        try {
            String json = String.format("{\"message\":\"%s\",\"timestamp\":\"%s\"}", 
                message.replace("\"", "\\\""), LocalDateTime.now());
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(5))
                .build();
            
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.error("Error sending webhook alert", e);
        }
    }
    
    private String formatSlackDiscordMessage(String message) {
        return String.format("{\"text\":\"%s\"}", message.replace("\"", "\\\""));
    }
    
    private int getMaxChecksForTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant not found"));
        TierService.TierLimits limits = tierService.getTierLimits(tenant.getTier());
        return limits.maxChecksPerMonth;
    }
}

