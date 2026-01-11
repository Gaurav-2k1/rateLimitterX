package com.ratelimitx.repository;

import com.ratelimitx.common.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    Optional<UserRole> findByTenantIdAndUserEmail(UUID tenantId, String userEmail);
    List<UserRole> findByTenantId(UUID tenantId);
}

