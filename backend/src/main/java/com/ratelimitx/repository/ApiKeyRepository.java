package com.ratelimitx.repository;

import com.ratelimitx.common.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByKeyHash(String keyHash);
    List<ApiKey> findByTenantId(UUID tenantId);
    List<ApiKey> findByTenantIdAndActive(UUID tenantId, Boolean active);
}

