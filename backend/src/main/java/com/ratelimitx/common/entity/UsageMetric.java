package com.ratelimitx.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usage_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(length = 255)
    private String resource;
    
    @Column(length = 255)
    private String identifier;
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @Column(name = "checks_performed", nullable = false)
    @Builder.Default
    private Integer checksPerformed = 1;
    
    @Column(name = "checks_denied", nullable = false)
    @Builder.Default
    private Integer checksDenied = 0;
    
    @Column(name = "latency_ms")
    private Integer latencyMs;
}

