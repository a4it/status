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

    /**
     * Constructs the controller with its required collaborators.
     *
     * @param settingsService service managing global health check settings
     * @param healthCheckScheduler scheduler used to trigger health checks
     * @param statusAppRepository repository providing access to status apps
     * @param statusComponentRepository repository providing access to status components
     */
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
     * Get health check status for all entities (apps and components).
     */
    @GetMapping("/status")
    public ResponseEntity<List<HealthCheckStatusResponse>> getHealthCheckStatus(
            @RequestParam(required = false) UUID platformId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean checkEnabled) {

        List<HealthCheckStatusResponse> responses = new ArrayList<>();

        for (StatusApp app : statusAppRepository.findAll()) {
            // Include the app only when it passes all supplied filters
            if (appMatchesFilters(app, platformId, status, checkEnabled)) {
                responses.add(buildAppResponse(app));
            }
        }

        for (StatusComponent component : statusComponentRepository.findAll()) {
            // Include the component only when it passes all supplied filters
            if (componentMatchesFilters(component, platformId, status, checkEnabled)) {
                responses.add(buildComponentResponse(component));
            }
        }

        return ResponseEntity.ok(responses);
    }

    /**
     * Evaluates whether an app passes the supplied status filters.
     */
    private boolean appMatchesFilters(StatusApp app, UUID platformId, String status, Boolean checkEnabled) {
        // Exclude apps that do not belong to the requested platform
        if (platformId != null && (app.getPlatform() == null || !platformId.equals(app.getPlatform().getId()))) {
            return false;
        }
        // Exclude apps whose status does not match the requested status
        if (status != null && !status.equals(app.getStatus())) {
            return false;
        }
        return checkEnabled == null || checkEnabled.equals(app.getCheckEnabled());
    }

    /**
     * Evaluates whether a component passes the supplied status filters.
     * Components inheriting their check from the parent app are always excluded.
     */
    private boolean componentMatchesFilters(StatusComponent component, UUID platformId, String status, Boolean checkEnabled) {
        // Exclude components that inherit their health check from the parent app
        if (Boolean.TRUE.equals(component.getCheckInheritFromApp())) {
            return false;
        }
        // Apply the platform filter when a platform id was supplied
        if (platformId != null) {
            StatusApp componentApp = component.getApp();
            // Exclude components whose parent app is not on the requested platform
            if (componentApp == null || componentApp.getPlatform() == null
                    || !platformId.equals(componentApp.getPlatform().getId())) {
                return false;
            }
        }
        // Exclude components whose status does not match the requested status
        if (status != null && !status.equals(component.getStatus())) {
            return false;
        }
        return checkEnabled == null || checkEnabled.equals(component.getCheckEnabled());
    }

    /**
     * Maps an app to its health check status response.
     */
    private HealthCheckStatusResponse buildAppResponse(StatusApp app) {
        HealthCheckStatusResponse response = new HealthCheckStatusResponse();
        response.setEntityId(app.getId());
        response.setEntityType("APP");
        response.setName(app.getName());
        // Populate platform details only when the app is associated with a platform
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
        return response;
    }

    /**
     * Maps a component to its health check status response.
     */
    private HealthCheckStatusResponse buildComponentResponse(StatusComponent component) {
        HealthCheckStatusResponse response = new HealthCheckStatusResponse();
        response.setEntityId(component.getId());
        response.setEntityType("COMPONENT");
        response.setName(component.getName());
        // Populate platform details only when the component's parent app has a platform
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
        return response;
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
