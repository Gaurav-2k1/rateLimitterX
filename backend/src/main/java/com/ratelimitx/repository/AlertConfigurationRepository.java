package com.ratelimitx.repository;

import com.ratelimitx.common.entity.AlertConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertConfigurationRepository extends JpaRepository<AlertConfiguration, UUID> {
    List<AlertConfiguration> findByTenantIdAndEnabled(UUID tenantId, Boolean enabled);
    List<AlertConfiguration> findByTenantIdAndAlertTypeAndEnabled(UUID tenantId, AlertConfiguration.AlertType alertType, Boolean enabled);
}

