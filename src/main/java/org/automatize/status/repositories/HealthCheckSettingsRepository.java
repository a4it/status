package org.automatize.status.repositories;

import org.automatize.status.models.HealthCheckSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for health check settings data access.
 */
@Repository
public interface HealthCheckSettingsRepository extends JpaRepository<HealthCheckSettings, UUID> {

    /**
     * Find a setting by its key.
     *
     * @param settingKey the setting key to search for
     * @return the setting if found
     */
    Optional<HealthCheckSettings> findBySettingKey(String settingKey);
}
