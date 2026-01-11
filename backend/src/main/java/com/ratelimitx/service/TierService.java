package com.ratelimitx.service;

import com.ratelimitx.common.entity.Tenant;
import com.ratelimitx.repository.RateLimitRuleRepository;
import com.ratelimitx.repository.TenantRepository;
import com.ratelimitx.repository.UsageMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TierService {
    
    private final TenantRepository tenantRepository;
    private final RateLimitRuleRepository ruleRepository;
    private final UsageMetricRepository usageMetricRepository;
    
    public enum TierLimits {
        FREE(1, 10_000, false),           // 1 rule, 10k checks/month, no custom algorithms
        PRO(Integer.MAX_VALUE, 1_000_000, false),  // unlimited rules, 1M checks/month, no custom algorithms
        ENTERPRISE(Integer.MAX_VALUE, Integer.MAX_VALUE, true); // unlimited everything, custom algorithms
        
        public final int maxRules;
        public final int maxChecksPerMonth;
        public final boolean allowCustomAlgorithms;
        
        TierLimits(int maxRules, int maxChecksPerMonth, boolean allowCustomAlgorithms) {
            this.maxRules = maxRules;
            this.maxChecksPerMonth = maxChecksPerMonth;
            this.allowCustomAlgorithms = allowCustomAlgorithms;
        }
    }
    
    public TierLimits getTierLimits(Tenant.Tier tier) {
        return switch (tier) {
            case FREE -> TierLimits.FREE;
            case PRO -> TierLimits.PRO;
            case ENTERPRISE -> TierLimits.ENTERPRISE;
        };
    }
    
    public void validateRuleCreation(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant not found"));
        
        TierLimits limits = getTierLimits(tenant.getTier());
        long currentRuleCount = ruleRepository.findByTenantId(tenantId).size();
        
        if (currentRuleCount >= limits.maxRules) {
            throw new RuntimeException(
                String.format("Tier limit exceeded: %s tier allows maximum %d rules. Current: %d",
                    tenant.getTier(), limits.maxRules, currentRuleCount));
        }
    }
    
    public void validateCheckRequest(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant not found"));
        
        TierLimits limits = getTierLimits(tenant.getTier());
        
        if (limits.maxChecksPerMonth == Integer.MAX_VALUE) {
            return; // Unlimited
        }
        
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long checksThisMonth = usageMetricRepository.countByTenantIdAndTimestampAfter(tenantId, monthStart);
        
        if (checksThisMonth >= limits.maxChecksPerMonth) {
            throw new RuntimeException(
                String.format("Monthly check limit exceeded: %s tier allows %d checks/month. Current: %d",
                    tenant.getTier(), limits.maxChecksPerMonth, checksThisMonth));
        }
    }
    
    public boolean canUseCustomAlgorithm(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant not found"));
        
        TierLimits limits = getTierLimits(tenant.getTier());
        return limits.allowCustomAlgorithms;
    }
    
    public int getRemainingChecksThisMonth(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant not found"));
        
        TierLimits limits = getTierLimits(tenant.getTier());
        
        if (limits.maxChecksPerMonth == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long checksThisMonth = usageMetricRepository.countByTenantIdAndTimestampAfter(tenantId, monthStart);
        
        return (int) Math.max(0, limits.maxChecksPerMonth - checksThisMonth);
    }
}

