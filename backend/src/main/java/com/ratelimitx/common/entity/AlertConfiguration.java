package com.ratelimitx.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alert_configurations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AlertType alertType;
    
    @Column(nullable = false)
    private String destination; // Email address, webhook URL, or Slack/Discord webhook
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DestinationType destinationType = DestinationType.EMAIL;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;
    
    @Column(name = "threshold_percent")
    private Integer thresholdPercent; // 80, 90, 100
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public enum AlertType {
        TIER_LIMIT_APPROACHING,  // 80%, 90% of monthly checks
        TIER_LIMIT_EXCEEDED,     // 100% of monthly checks
        RATE_LIMIT_SPIKE,        // Unusual spike in rate limit hits
        API_ERROR_RATE           // High error rate
    }
    
    public enum DestinationType {
        EMAIL, WEBHOOK, SLACK, DISCORD
    }
}

