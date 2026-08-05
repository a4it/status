package org.automatize.status.controllers.api;

import org.automatize.status.api.request.HealthCheckSettingsRequest;
import org.automatize.status.api.response.HealthCheckSettingsResponse;
import org.automatize.status.api.response.HealthCheckStatusResponse;
import org.automatize.status.api.response.HealthCheckTriggerResponse;
import org.automatize.status.services.HealthCheckScheduler;
import org.automatize.status.services.HealthCheckSettingsService;
import org.automatize.status.services.HealthCheckStatusService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for health check management.
 * <p>
 * Provides endpoints for managing global health check settings,
 * viewing health check status of all entities, and triggering
 * manual health checks.
 * </p>
 */
@RestController
@RequestMapping("/api/health-checks")
@PreAuthorize("isAuthenticated()")
public class HealthCheckController {

    private final HealthCheckSettingsService settingsService;
    private final HealthCheckScheduler healthCheckScheduler;
    private final HealthCheckStatusService healthCheckStatusService;

    /**
     * Constructs the controller with its required collaborators.
     *
     * @param settingsService service managing global health check settings
     * @param healthCheckScheduler scheduler used to trigger health checks
     * @param healthCheckStatusService service assembling the health check status overview
     */
    public HealthCheckController(HealthCheckSettingsService settingsService,
                                  HealthCheckScheduler healthCheckScheduler,
                                  HealthCheckStatusService healthCheckStatusService) {
        this.settingsService = settingsService;
        this.healthCheckScheduler = healthCheckScheduler;
        this.healthCheckStatusService = healthCheckStatusService;
    }

    /**
     * Get global health check settings.
     */
    @GetMapping("/settings")
    public ResponseEntity<HealthCheckSettingsResponse> getSettings() {
        HealthCheckSettingsResponse response = new HealthCheckSettingsResponse();
        response.setEnabled(settingsService.isEnabled());
        response.setSchedulerIntervalMs(settingsService.getSchedulerIntervalMs());
        response.setThreadPoolSize(settingsService.getThreadPoolSize());
        response.setDefaultIntervalSeconds(settingsService.getDefaultIntervalSeconds());
        response.setDefaultTimeoutSeconds(settingsService.getDefaultTimeoutSeconds());
        return ResponseEntity.ok(response);
    }

    /**
     * Update global health check settings.
     */
    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<HealthCheckSettingsResponse> updateSettings(@RequestBody HealthCheckSettingsRequest request) {
        Map<String, String> updates = new HashMap<>();

        // Include the enabled flag only when supplied in the request
        if (request.getEnabled() != null) {
            updates.put(HealthCheckSettingsService.KEY_ENABLED, String.valueOf(request.getEnabled()));
        }
        // Include the scheduler interval only when supplied in the request
        if (request.getSchedulerIntervalMs() != null) {
            updates.put(HealthCheckSettingsService.KEY_SCHEDULER_INTERVAL_MS, String.valueOf(request.getSchedulerIntervalMs()));
        }
        // Include the thread pool size only when supplied in the request
        if (request.getThreadPoolSize() != null) {
            updates.put(HealthCheckSettingsService.KEY_THREAD_POOL_SIZE, String.valueOf(request.getThreadPoolSize()));
        }
        // Include the default interval only when supplied in the request
        if (request.getDefaultIntervalSeconds() != null) {
            updates.put(HealthCheckSettingsService.KEY_DEFAULT_INTERVAL_SECONDS, String.valueOf(request.getDefaultIntervalSeconds()));
        }
        // Include the default timeout only when supplied in the request
        if (request.getDefaultTimeoutSeconds() != null) {
            updates.put(HealthCheckSettingsService.KEY_DEFAULT_TIMEOUT_SECONDS, String.valueOf(request.getDefaultTimeoutSeconds()));
        }

        settingsService.updateSettings(updates);

        return getSettings();
    }

    /**
     * Get a page of health check status rows for all entities (apps and components).
     * All filtering and pagination is performed by the database.
     */
    @GetMapping("/status")
    public ResponseEntity<Page<HealthCheckStatusResponse>> getHealthCheckStatus(
            @RequestParam(required = false) UUID platformId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean checkEnabled,
            Pageable pageable) {

        return ResponseEntity.ok(
                healthCheckStatusService.getHealthCheckStatus(platformId, status, checkEnabled, pageable));
    }

    /**
     * Trigger all health checks immediately.
     */
    @PostMapping("/trigger/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<HealthCheckTriggerResponse> triggerAllChecks() {
        long startTime = System.currentTimeMillis();
        int count = healthCheckScheduler.triggerAllChecks();
        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok(new HealthCheckTriggerResponse(
                true,
                "Triggered health checks for " + count + " entities",
                duration
        ));
    }

    /**
     * Trigger health check for a specific app.
     */
    @PostMapping("/trigger/app/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<HealthCheckTriggerResponse> triggerAppCheck(@PathVariable UUID id) {
        long startTime = System.currentTimeMillis();
        HealthCheckTriggerResponse result = healthCheckScheduler.triggerAppCheck(id);
        // Fill in the elapsed duration when the scheduler did not record one
        if (result.getDurationMs() == null) {
            result.setDurationMs(System.currentTimeMillis() - startTime);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Trigger health check for a specific component.
     */
    @PostMapping("/trigger/component/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<HealthCheckTriggerResponse> triggerComponentCheck(@PathVariable UUID id) {
        long startTime = System.currentTimeMillis();
        HealthCheckTriggerResponse result = healthCheckScheduler.triggerComponentCheck(id);
        // Fill in the elapsed duration when the scheduler did not record one
        if (result.getDurationMs() == null) {
            result.setDurationMs(System.currentTimeMillis() - startTime);
        }
        return ResponseEntity.ok(result);
    }
}
