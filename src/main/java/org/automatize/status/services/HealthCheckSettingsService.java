package org.automatize.status.services;

import org.automatize.status.models.HealthCheckSettings;
import org.automatize.status.repositories.HealthCheckSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing health check global settings.
 * <p>
 * Provides access to health check configuration stored in the database,
 * allowing runtime changes without application restart.
 * </p>
 */
@Service
public class HealthCheckSettingsService {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckSettingsService.class);

    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_SCHEDULER_INTERVAL_MS = "scheduler_interval_ms";
    public static final String KEY_THREAD_POOL_SIZE = "thread_pool_size";
    public static final String KEY_DEFAULT_INTERVAL_SECONDS = "default_interval_seconds";
    public static final String KEY_DEFAULT_TIMEOUT_SECONDS = "default_timeout_seconds";

    private final HealthCheckSettingsRepository repository;

    public HealthCheckSettingsService(HealthCheckSettingsRepository repository) {
        this.repository = repository;
    }

    /**
     * Get all settings as a map.
     *
     * @return map of setting key to value
     */
    @Transactional(readOnly = true)
    public Map<String, String> getAllSettings() {
        List<HealthCheckSettings> settings = repository.findAll();
        Map<String, String> result = new HashMap<>();
        for (HealthCheckSettings setting : settings) {
            result.put(setting.getSettingKey(), setting.getSettingValue());
        }
        return result;
    }

    /**
     * Get a single setting value.
     *
     * @param key the setting key
     * @param defaultValue default value if not found
     * @return the setting value or default
     */
    @Transactional(readOnly = true)
    public String getSetting(String key, String defaultValue) {
        return repository.findBySettingKey(key)
                .map(HealthCheckSettings::getSettingValue)
                .orElse(defaultValue);
    }

    /**
     * Update a single setting.
     *
     * @param key the setting key
     * @param value the new value
     */
    @Transactional
    public void updateSetting(String key, String value) {
        HealthCheckSettings setting = repository.findBySettingKey(key)
                .orElseGet(() -> {
                    HealthCheckSettings newSetting = new HealthCheckSettings();
                    newSetting.setSettingKey(key);
                    return newSetting;
                });
        setting.setSettingValue(value);
        repository.save(setting);
        logger.info("Updated health check setting: {} = {}", key, value);
    }

    /**
     * Update multiple settings at once.
     *
     * @param settings map of key-value pairs to update
     */
    @Transactional
    public void updateSettings(Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            updateSetting(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Check if health checks are globally enabled.
     *
     * @return true if enabled
     */
    @Transactional(readOnly = true)
    public boolean isEnabled() {
        return Boolean.parseBoolean(getSetting(KEY_ENABLED, "true"));
    }

    /**
     * Get the scheduler polling interval in milliseconds.
     *
     * @return interval in milliseconds
     */
    @Transactional(readOnly = true)
    public long getSchedulerIntervalMs() {
        try {
            return Long.parseLong(getSetting(KEY_SCHEDULER_INTERVAL_MS, "10000"));
        } catch (NumberFormatException e) {
            logger.warn("Invalid scheduler interval setting, using default 10000ms");
            return 10000;
        }
    }

    /**
     * Get the thread pool size for health checks.
     *
     * @return thread pool size
     */
    @Transactional(readOnly = true)
    public int getThreadPoolSize() {
        try {
            return Integer.parseInt(getSetting(KEY_THREAD_POOL_SIZE, "10"));
        } catch (NumberFormatException e) {
            logger.warn("Invalid thread pool size setting, using default 10");
            return 10;
        }
    }

    /**
     * Get the default check interval for new entities.
     *
     * @return interval in seconds
     */
    @Transactional(readOnly = true)
    public int getDefaultIntervalSeconds() {
        try {
            return Integer.parseInt(getSetting(KEY_DEFAULT_INTERVAL_SECONDS, "60"));
        } catch (NumberFormatException e) {
            logger.warn("Invalid default interval setting, using default 60s");
            return 60;
        }
    }

    /**
     * Get the default timeout for health checks.
     *
     * @return timeout in seconds
     */
    @Transactional(readOnly = true)
    public int getDefaultTimeoutSeconds() {
        try {
            return Integer.parseInt(getSetting(KEY_DEFAULT_TIMEOUT_SECONDS, "10"));
        } catch (NumberFormatException e) {
            logger.warn("Invalid default timeout setting, using default 10s");
            return 10;
        }
    }
}
