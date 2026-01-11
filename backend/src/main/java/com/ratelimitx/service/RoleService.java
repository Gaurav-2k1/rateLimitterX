package com.ratelimitx.service;

import com.ratelimitx.common.entity.Tenant;
import com.ratelimitx.common.entity.UserRole;
import com.ratelimitx.repository.TenantRepository;
import com.ratelimitx.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {
    
    private final UserRoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    
    public UserRole.Role getUserRole(UUID tenantId, String userEmail) {
        // Owner is the tenant's email
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant not found"));
        
        if (tenant.getEmail().equals(userEmail)) {
            return UserRole.Role.OWNER;
        }
        
        return roleRepository.findByTenantIdAndUserEmail(tenantId, userEmail)
            .map(UserRole::getRole)
            .orElse(UserRole.Role.VIEWER); // Default to viewer
    }
    
    public boolean hasPermission(UUID tenantId, String userEmail, UserRole.Role requiredRole) {
        UserRole.Role userRole = getUserRole(tenantId, userEmail);
        return hasPermission(userRole, requiredRole);
    }
    
    public boolean hasPermission(UserRole.Role userRole, UserRole.Role requiredRole) {
        return switch (requiredRole) {
            case VIEWER -> true; // Everyone can view
            case ADMIN -> userRole == UserRole.Role.ADMIN || userRole == UserRole.Role.OWNER;
            case OWNER -> userRole == UserRole.Role.OWNER;
        };
    }
    
    @Transactional
    public UserRole assignRole(UUID tenantId, String userEmail, UserRole.Role role) {
        UserRole existing = roleRepository.findByTenantIdAndUserEmail(tenantId, userEmail)
            .orElse(null);
        
        if (existing != null) {
            existing.setRole(role);
            return roleRepository.save(existing);
        }
        
        UserRole newRole = UserRole.builder()
            .tenantId(tenantId)
            .userEmail(userEmail)
            .role(role)
            .build();
        
        return roleRepository.save(newRole);
    }
}

