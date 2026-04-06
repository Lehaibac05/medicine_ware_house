package com.pharmacy.warehouse.repository;

import com.pharmacy.warehouse.model.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, Long> {

    Optional<SystemConfiguration> findByParameterName(String parameterName);
}
