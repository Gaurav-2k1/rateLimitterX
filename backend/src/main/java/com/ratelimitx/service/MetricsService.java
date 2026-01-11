package com.ratelimitx.service;

import com.ratelimitx.common.entity.UsageMetric;
import com.ratelimitx.repository.UsageMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {
    
    private final UsageMetricRepository metricsRepository;
    
    @Async
    public void recordCheckAsync(UUID tenantId, String resource, 
            String identifier, boolean allowed, long latencyMs) {
        try {
            UsageMetric metric = UsageMetric.builder()
                .tenantId(tenantId)
                .resource(resource)
                .identifier(identifier)
                .checksPerformed(1)
                .checksDenied(allowed ? 0 : 1)
                .latencyMs((int) latencyMs)
                .timestamp(LocalDateTime.now())
                .build();
                
            metricsRepository.save(metric);
        } catch (Exception e) {
            log.error("Error recording metric", e);
            // Don't throw - metrics are non-critical
        }
    }
}

