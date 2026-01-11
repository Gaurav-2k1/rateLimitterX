package com.ratelimitx.repository;

import com.ratelimitx.common.entity.RateLimitRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RateLimitRuleRepository extends JpaRepository<RateLimitRule, UUID> {
    Optional<RateLimitRule> findByTenantIdAndResourceAndActive(UUID tenantId, String resource, Boolean active);
    List<RateLimitRule> findByTenantId(UUID tenantId);
    List<RateLimitRule> findByTenantIdAndActive(UUID tenantId, Boolean active);
    List<RateLimitRule> findByTenantIdAndLimitScope(UUID tenantId, RateLimitRule.LimitScope limitScope);
}

