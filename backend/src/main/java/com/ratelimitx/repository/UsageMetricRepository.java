package com.ratelimitx.repository;

import com.ratelimitx.common.entity.UsageMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UsageMetricRepository extends JpaRepository<UsageMetric, Long> {
    @Query("SELECT m FROM UsageMetric m WHERE m.tenantId = :tenantId AND m.timestamp >= :startTime ORDER BY m.timestamp DESC")
    List<UsageMetric> findByTenantIdAndTimestampAfter(@Param("tenantId") UUID tenantId, @Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT COUNT(m) FROM UsageMetric m WHERE m.tenantId = :tenantId AND m.timestamp >= :startTime")
    Long countByTenantIdAndTimestampAfter(@Param("tenantId") UUID tenantId, @Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT COUNT(m) FROM UsageMetric m WHERE m.tenantId = :tenantId AND m.checksDenied > 0 AND m.timestamp >= :startTime")
    Long countDeniedByTenantIdAndTimestampAfter(@Param("tenantId") UUID tenantId, @Param("startTime") LocalDateTime startTime);
}

