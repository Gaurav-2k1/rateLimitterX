package com.ratelimitx.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rate_limit_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitRule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(nullable = false, length = 255)
    private String resource;
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Algorithm algorithm;
    
    @Column(name = "max_requests", nullable = false)
    private Integer maxRequests;
    
    @Column(name = "window_seconds", nullable = false)
    private Integer windowSeconds;
    
    @Column(name = "burst_capacity")
    private Integer burstCapacity;
    
    @Column(name = "identifier_type", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private IdentifierType identifierType = IdentifierType.USER_ID;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(name = "limit_scope", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LimitScope limitScope = LimitScope.RESOURCE;
    
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0; // Higher priority = evaluated first
    
    @Column(name = "condition_json", columnDefinition = "TEXT")
    private String conditionJson; // JSON for time-based, geo-based, header-based conditions
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum Algorithm {
        TOKEN_BUCKET, SLIDING_WINDOW, FIXED_WINDOW
    }
    
    public enum IdentifierType {
        USER_ID, IP_ADDRESS, API_KEY, CUSTOM
    }
    
    public enum LimitScope {
        GLOBAL,      // Applies to entire tenant
        RESOURCE,    // Applies to specific resource/endpoint
        IDENTIFIER   // Applies to specific identifier (user/IP)
    }
}

