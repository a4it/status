package org.automatize.status.controllers.api;

import org.automatize.status.api.request.HealthCheckSettingsRequest;
import org.automatize.status.api.response.HealthCheckSettingsResponse;
import org.automatize.status.api.response.HealthCheckStatusResponse;
import org.automatize.status.api.response.HealthCheckTriggerResponse;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.automatize.status.services.HealthCheckScheduler;
import org.automatize.status.services.HealthCheckSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private final StatusAppRepository statusAppRepository;
    private final StatusComponentRepository statusComponentRepository;

    public HealthCheckController(HealthCheckSettingsService settingsService,
                                  HealthCheckScheduler healthCheckScheduler,
                                  StatusAppRepository statusAppRepository,
                                  StatusComponentRepository statusComponentRepository) {
        this.settingsService = settingsService;
        this.healthCheckScheduler = healthCheckScheduler;
        this.statusAppRepository = statusAppRepository;
        this.statusComponentRepository = statusComponentRepository;
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

        if (request.getEnabled() != null) {
            updates.put(HealthCheckSettingsService.KEY_ENABLED, String.valueOf(request.getEnabled()));
        }
        if (request.getSchedulerIntervalMs() != null) {
            updates.put(HealthCheckSettingsService.KEY_SCHEDULER_INTERVAL_MS, String.valueOf(request.getSchedulerIntervalMs()));
        }
        if (request.getThreadPoolSize() != null) {
            updates.put(HealthCheckSettingsService.KEY_THREAD_POOL_SIZE, String.valueOf(request.getThreadPoolSize()));
        }
        if (request.getDefaultIntervalSeconds() != null) {
            updates.put(HealthCheckSettingsService.KEY_DEFAULT_INTERVAL_SECONDS, String.valueOf(request.getDefaultIntervalSeconds()));
        }
        if (request.getDefaultTimeoutSeconds() != null) {
            updates.put(HealthCheckSettingsService.KEY_DEFAULT_TIMEOUT_SECONDS, String.valueOf(request.getDefaultTimeoutSeconds()));
        }

        settingsService.updateSettings(updates);

        return getSettings();
    }

    /**
     * Get health check status for all entities (apps and components).
     */
    @GetMapping("/status")
    public ResponseEntity<List<HealthCheckStatusResponse>> getHealthCheckStatus(
            @RequestParam(required = false) UUID platformId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean checkEnabled) {

        List<HealthCheckStatusResponse> responses = new ArrayList<>();

        // Get all apps
        List<StatusApp> apps = statusAppRepository.findAll();
        for (StatusApp app : apps) {
            // Apply filters
            if (platformId != null && (app.getPlatform() == null || !platformId.equals(app.getPlatform().getId()))) {
                continue;
            }
            if (status != null && !status.equals(app.getStatus())) {
                continue;
            }
            if (checkEnabled != null && !checkEnabled.equals(app.getCheckEnabled())) {
                continue;
            }

            HealthCheckStatusResponse response = new HealthCheckStatusResponse();
            response.setEntityId(app.getId());
            response.setEntityType("APP");
            response.setName(app.getName());
            if (app.getPlatform() != null) {
                response.setPlatformId(app.getPlatform().getId().toString());
                response.setPlatformName(app.getPlatform().getName());
            }
            response.setCheckEnabled(app.getCheckEnabled());
            response.setCheckType(app.getCheckType());
            response.setCheckUrl(app.getCheckUrl());
            response.setCheckIntervalSeconds(app.getCheckIntervalSeconds());
            response.setCheckTimeoutSeconds(app.getCheckTimeoutSeconds());
            response.setCheckExpectedStatus(app.getCheckExpectedStatus());
            response.setCheckFailureThreshold(app.getCheckFailureThreshold());
            response.setLastCheckAt(app.getLastCheckAt());
            response.setLastCheckSuccess(app.getLastCheckSuccess());
            response.setLastCheckMessage(app.getLastCheckMessage());
            response.setConsecutiveFailures(app.getConsecutiveFailures());
            response.setStatus(app.getStatus());
            responses.add(response);
        }

        // Get all components (excluding those that inherit from app)
        List<StatusComponent> components = statusComponentRepository.findAll();
        for (StatusComponent component : components) {
            // Skip components that inherit check from app
            if (Boolean.TRUE.equals(component.getCheckInheritFromApp())) {
                continue;
            }

            // Apply filters
            if (platformId != null) {
                StatusApp componentApp = component.getApp();
                if (componentApp == null || componentApp.getPlatform() == null ||
                    !platformId.equals(componentApp.getPlatform().getId())) {
                    continue;
                }
            }
            if (status != null && !status.equals(component.getStatus())) {
                continue;
            }
            if (checkEnabled != null && !checkEnabled.equals(component.getCheckEnabled())) {
                continue;
            }

            HealthCheckStatusResponse response = new HealthCheckStatusResponse();
            response.setEntityId(component.getId());
            response.setEntityType("COMPONENT");
            response.setName(component.getName());
            if (component.getApp() != null && component.getApp().getPlatform() != null) {
                response.setPlatformId(component.getApp().getPlatform().getId().toString());
                response.setPlatformName(component.getApp().getPlatform().getName());
            }
            response.setCheckEnabled(component.getCheckEnabled());
            response.setCheckType(component.getCheckType());
            response.setCheckUrl(component.getCheckUrl());
            response.setCheckIntervalSeconds(component.getCheckIntervalSeconds());
            response.setCheckTimeoutSeconds(component.getCheckTimeoutSeconds());
            response.setCheckExpectedStatus(component.getCheckExpectedStatus());
            response.setCheckFailureThreshold(component.getCheckFailureThreshold());
            response.setLastCheckAt(component.getLastCheckAt());
            response.setLastCheckSuccess(component.getLastCheckSuccess());
            response.setLastCheckMessage(component.getLastCheckMessage());
            response.setConsecutiveFailures(component.getConsecutiveFailures());
            response.setStatus(component.getStatus());
            responses.add(response);
        }

        return ResponseEntity.ok(responses);
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
        if (result.getDurationMs() == null) {
            result.setDurationMs(System.currentTimeMillis() - startTime);
        }
        return ResponseEntity.ok(result);
    }
}
