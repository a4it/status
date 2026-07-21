package org.automatize.status.services;

import org.automatize.status.models.HealthCheckSettings;
import org.automatize.status.repositories.HealthCheckSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HealthCheckSettingsService}.
 *
 * <p>Testing approach: the {@link HealthCheckSettingsRepository} is a Mockito mock and the
 * service under test is created via {@link InjectMocks}. Tests stub repository lookups to
 * return persisted {@link HealthCheckSettings} rows (or empties) and assert on the mapped
 * results, the create-vs-update save behaviour (captured with {@link ArgumentCaptor}), and
 * the typed getters' parsing and default-fallback logic for malformed values.</p>
 */
@ExtendWith(MockitoExtension.class)
class HealthCheckSettingsServiceTest {

    @Mock
    private HealthCheckSettingsRepository repository;

    @InjectMocks
    private HealthCheckSettingsService service;

    /**
     * Builds a {@link HealthCheckSettings} row with the given key and value for stubbing
     * repository responses.
     *
     * @param key   the setting key
     * @param value the setting value
     * @return a populated {@link HealthCheckSettings} test fixture
     */
    private HealthCheckSettings setting(String key, String value) {
        HealthCheckSettings s = new HealthCheckSettings();
        s.setSettingKey(key);
        s.setSettingValue(value);
        return s;
    }

    /**
     * Verifies that all settings rows are collapsed into a key-to-value map preserving each
     * entry.
     */
    @Test
    void getAllSettings_mapsEntitiesToKeyValueMap() {
        when(repository.findAll()).thenReturn(List.of(
                setting("enabled", "true"),
                setting("thread_pool_size", "5")));

        Map<String, String> result = service.getAllSettings();

        assertThat(result)
                .containsEntry("enabled", "true")
                .containsEntry("thread_pool_size", "5")
                .hasSize(2);
    }

    /**
     * Verifies that a present setting returns its stored value rather than the supplied
     * default.
     */
    @Test
    void getSetting_whenFound_returnsValue() {
        when(repository.findBySettingKey("enabled")).thenReturn(Optional.of(setting("enabled", "false")));

        assertThat(service.getSetting("enabled", "true")).isEqualTo("false");
    }

    /**
     * Verifies that a missing setting returns the supplied default value.
     */
    @Test
    void getSetting_whenMissing_returnsDefault() {
        when(repository.findBySettingKey("missing")).thenReturn(Optional.empty());

        assertThat(service.getSetting("missing", "fallback")).isEqualTo("fallback");
    }

    /**
     * Verifies that updating an existing setting mutates the stored row's value in place and
     * saves it.
     */
    @Test
    void updateSetting_whenExisting_updatesValueAndSaves() {
        HealthCheckSettings existing = setting("enabled", "true");
        when(repository.findBySettingKey("enabled")).thenReturn(Optional.of(existing));

        service.updateSetting("enabled", "false");

        assertThat(existing.getSettingValue()).isEqualTo("false");
        verify(repository).save(existing);
    }

    /**
     * Verifies that updating a missing setting creates a new row with the given key/value
     * and saves it (asserted via a captured argument).
     */
    @Test
    void updateSetting_whenMissing_createsNewAndSaves() {
        when(repository.findBySettingKey("enabled")).thenReturn(Optional.empty());

        service.updateSetting("enabled", "true");

        ArgumentCaptor<HealthCheckSettings> captor = ArgumentCaptor.forClass(HealthCheckSettings.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSettingKey()).isEqualTo("enabled");
        assertThat(captor.getValue().getSettingValue()).isEqualTo("true");
    }

    @Test
    void updateSettings_updatesEachEntry() {
        when(repository.findBySettingKey(any())).thenReturn(Optional.empty());

        service.updateSettings(Map.of("enabled", "true", "thread_pool_size", "8"));

        verify(repository, times(2)).save(any(HealthCheckSettings.class));
    }

    @Test
    void isEnabled_whenValueTrue_returnsTrue() {
        when(repository.findBySettingKey("enabled")).thenReturn(Optional.of(setting("enabled", "true")));

        assertThat(service.isEnabled()).isTrue();
    }

    @Test
    void isEnabled_whenValueFalse_returnsFalse() {
        when(repository.findBySettingKey("enabled")).thenReturn(Optional.of(setting("enabled", "false")));

        assertThat(service.isEnabled()).isFalse();
    }

    @Test
    void isEnabled_whenMissing_defaultsToTrue() {
        when(repository.findBySettingKey("enabled")).thenReturn(Optional.empty());

        assertThat(service.isEnabled()).isTrue();
    }

    @Test
    void getSchedulerIntervalMs_validValue_isParsed() {
        when(repository.findBySettingKey("scheduler_interval_ms")).thenReturn(Optional.of(setting("scheduler_interval_ms", "25000")));

        assertThat(service.getSchedulerIntervalMs()).isEqualTo(25000L);
    }

    @Test
    void getSchedulerIntervalMs_invalidValue_returnsDefault() {
        when(repository.findBySettingKey("scheduler_interval_ms")).thenReturn(Optional.of(setting("scheduler_interval_ms", "notanumber")));

        assertThat(service.getSchedulerIntervalMs()).isEqualTo(10000L);
    }

    @Test
    void getThreadPoolSize_validValue_isParsed() {
        when(repository.findBySettingKey("thread_pool_size")).thenReturn(Optional.of(setting("thread_pool_size", "20")));

        assertThat(service.getThreadPoolSize()).isEqualTo(20);
    }

    @Test
    void getThreadPoolSize_invalidValue_returnsDefault() {
        when(repository.findBySettingKey("thread_pool_size")).thenReturn(Optional.of(setting("thread_pool_size", "x")));

        assertThat(service.getThreadPoolSize()).isEqualTo(10);
    }

    @Test
    void getDefaultIntervalSeconds_invalidValue_returnsDefault() {
        when(repository.findBySettingKey("default_interval_seconds")).thenReturn(Optional.of(setting("default_interval_seconds", "bad")));

        assertThat(service.getDefaultIntervalSeconds()).isEqualTo(60);
    }

    @Test
    void getDefaultTimeoutSeconds_validValue_isParsed() {
        when(repository.findBySettingKey("default_timeout_seconds")).thenReturn(Optional.of(setting("default_timeout_seconds", "30")));

        assertThat(service.getDefaultTimeoutSeconds()).isEqualTo(30);
    }
}
