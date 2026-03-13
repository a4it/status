package org.automatize.status.services;

import org.automatize.status.api.response.HealthCheckTriggerResponse;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Scheduled service responsible for orchestrating health checks on status apps and components.
 * <p>
 * This service runs on a configurable interval and identifies apps and components that are due
 * for health checks based on their configured check intervals. Health checks are executed
 * asynchronously using a thread pool to avoid blocking and to handle multiple checks concurrently.
 * </p>
 * <p>
 * Configuration can be set via database (HealthCheckSettingsService) or application.properties.
 * Database settings take precedence when available.
 * </p>
 *
 * @author Tim De Smedt
 * @see HealthCheckService
 * @see HealthCheckSettingsService
 */
@Service
public class HealthCheckScheduler {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckScheduler.class);

    private final StatusAppRepository statusAppRepository;
    private final StatusComponentRepository statusComponentRepository;
    private final HealthCheckService healthCheckService;
    private final HealthCheckSettingsService settingsService;
    private final ExecutorService executorService;

    @Value("${health-check.enabled:true}")
    private boolean healthCheckEnabledDefault;

    /**
     * Constructs a new HealthCheckScheduler with the required dependencies.
     *
     * @param statusAppRepository repository for status app data access
     * @param statusComponentRepository repository for status component data access
     * @param healthCheckService service for performing health checks
     * @param settingsService service for accessing health check settings from database
     * @param threadPoolSize the size of the thread pool for concurrent health checks
     */
    public HealthCheckScheduler(StatusAppRepository statusAppRepository,
                                StatusComponentRepository statusComponentRepository,
                                HealthCheckService healthCheckService,
                                HealthCheckSettingsService settingsService,
                                @Value("${health-check.thread-pool-size:10}") int threadPoolSize) {
        this.statusAppRepository = statusAppRepository;
        this.statusComponentRepository = statusComponentRepository;
        this.healthCheckService = healthCheckService;
        this.settingsService = settingsService;
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    /**
     * Check if health checks are enabled (from database or default).
     *
     * @return true if health checks are enabled
     */
    private boolean isHealthCheckEnabled() {
        try {
            return settingsService.isEnabled();
        } catch (Exception e) {
            logger.debug("Could not read health check enabled setting from database, using default: {}", healthCheckEnabledDefault);
            return healthCheckEnabledDefault;
        }
    }

    /**
     * Scheduled method that runs health checks on apps and components that are due.
     * <p>
     * This method is triggered at a fixed rate defined by the configuration property
     * {@code health-check.scheduler-interval-ms}. It identifies all apps and components
     * that need to be checked based on their last check time and configured interval,
     * then submits each check to the thread pool for asynchronous execution.
     * </p>
     */
    @Scheduled(fixedRateString = "${health-check.scheduler-interval-ms:10000}")
    public void runHealthChecks() {
        if (!isHealthCheckEnabled()) {
            return;
        }

        try {
            List<StatusApp> appsDueForCheck = getAppsDueForCheck();
            List<StatusComponent> componentsDueForCheck = getComponentsDueForCheck();

            logger.debug("Found {} apps and {} components due for health check",
                    appsDueForCheck.size(), componentsDueForCheck.size());

            for (StatusApp app : appsDueForCheck) {
                executorService.submit(() -> checkApp(app));
            }

            for (StatusComponent component : componentsDueForCheck) {
                executorService.submit(() -> checkComponent(component));
            }
        } catch (Exception e) {
            logger.error("Error running health checks", e);
        }
    }

    /**
     * Trigger all health checks immediately, regardless of their scheduled intervals.
     *
     * @return the number of entities triggered for health check
     */
    public int triggerAllChecks() {
        logger.info("Manual trigger: Running all health checks");

        List<StatusApp> apps = statusAppRepository.findAll().stream()
                .filter(app -> Boolean.TRUE.equals(app.getCheckEnabled()))
                .filter(app -> app.getCheckType() != null && !"NONE".equals(app.getCheckType()))
                .filter(app -> app.getCheckUrl() != null && !app.getCheckUrl().isBlank())
                .toList();

        List<StatusComponent> components = statusComponentRepository.findAll().stream()
                .filter(component -> !Boolean.TRUE.equals(component.getCheckInheritFromApp()))
                .filter(component -> Boolean.TRUE.equals(component.getCheckEnabled()))
                .filter(component -> component.getCheckType() != null && !"NONE".equals(component.getCheckType()))
                .filter(component -> component.getCheckUrl() != null && !component.getCheckUrl().isBlank())
                .toList();

        for (StatusApp app : apps) {
            executorService.submit(() -> checkApp(app));
        }

        for (StatusComponent component : components) {
            executorService.submit(() -> checkComponent(component));
        }

        int count = apps.size() + components.size();
        logger.info("Manual trigger: Submitted {} health checks", count);
        return count;
    }

    /**
     * Trigger health check for a specific app immediately.
     *
     * @param appId the UUID of the app to check
     * @return the result of the health check trigger
     */
    public HealthCheckTriggerResponse triggerAppCheck(UUID appId) {
        logger.info("Manual trigger: Running health check for app {}", appId);

        Optional<StatusApp> appOpt = statusAppRepository.findById(appId);
        if (appOpt.isEmpty()) {
            return new HealthCheckTriggerResponse(false, "App not found: " + appId);
        }

        StatusApp app = appOpt.get();

        if (!Boolean.TRUE.equals(app.getCheckEnabled())) {
            return new HealthCheckTriggerResponse(false, "Health checking is not enabled for this app");
        }

        if (app.getCheckType() == null || "NONE".equals(app.getCheckType())) {
            return new HealthCheckTriggerResponse(false, "No check type configured for this app");
        }

        if (app.getCheckUrl() == null || app.getCheckUrl().isBlank()) {
            return new HealthCheckTriggerResponse(false, "No check URL configured for this app");
        }

        long startTime = System.currentTimeMillis();

        try {
            HealthCheckService.HealthCheckResult result = healthCheckService.performCheck(
                    app.getCheckType(),
                    app.getCheckUrl(),
                    app.getCheckTimeoutSeconds() != null ? app.getCheckTimeoutSeconds() : 10,
                    app.getCheckExpectedStatus()
            );

            healthCheckService.updateAppCheckResult(app, result);

            long duration = System.currentTimeMillis() - startTime;
            return new HealthCheckTriggerResponse(result.success(), result.message(), duration);
        } catch (Exception e) {
            logger.error("Error during manual app check for {}: {}", appId, e.getMessage());
            long duration = System.currentTimeMillis() - startTime;
            return new HealthCheckTriggerResponse(false, "Check error: " + e.getMessage(), duration);
        }
    }

    /**
     * Trigger health check for a specific component immediately.
     *
     * @param componentId the UUID of the component to check
     * @return the result of the health check trigger
     */
    public HealthCheckTriggerResponse triggerComponentCheck(UUID componentId) {
        logger.info("Manual trigger: Running health check for component {}", componentId);

        Optional<StatusComponent> componentOpt = statusComponentRepository.findById(componentId);
        if (componentOpt.isEmpty()) {
            return new HealthCheckTriggerResponse(false, "Component not found: " + componentId);
        }

        StatusComponent component = componentOpt.get();

        if (Boolean.TRUE.equals(component.getCheckInheritFromApp())) {
            return new HealthCheckTriggerResponse(false, "Component inherits check from app, trigger the app check instead");
        }

        if (!Boolean.TRUE.equals(component.getCheckEnabled())) {
            return new HealthCheckTriggerResponse(false, "Health checking is not enabled for this component");
        }

        if (component.getCheckType() == null || "NONE".equals(component.getCheckType())) {
            return new HealthCheckTriggerResponse(false, "No check type configured for this component");
        }

        if (component.getCheckUrl() == null || component.getCheckUrl().isBlank()) {
            return new HealthCheckTriggerResponse(false, "No check URL configured for this component");
        }

        long startTime = System.currentTimeMillis();

        try {
            HealthCheckService.HealthCheckResult result = healthCheckService.performCheck(
                    component.getCheckType(),
                    component.getCheckUrl(),
                    component.getCheckTimeoutSeconds() != null ? component.getCheckTimeoutSeconds() : 10,
                    component.getCheckExpectedStatus()
            );

            healthCheckService.updateComponentCheckResult(component, result);

            long duration = System.currentTimeMillis() - startTime;
            return new HealthCheckTriggerResponse(result.success(), result.message(), duration);
        } catch (Exception e) {
            logger.error("Error during manual component check for {}: {}", componentId, e.getMessage());
            long duration = System.currentTimeMillis() - startTime;
            return new HealthCheckTriggerResponse(false, "Check error: " + e.getMessage(), duration);
        }
    }

    /**
     * Retrieves all status apps that are due for a health check.
     */
    private List<StatusApp> getAppsDueForCheck() {
        List<StatusApp> allApps = statusAppRepository.findAll();
        ZonedDateTime now = ZonedDateTime.now();

        return allApps.stream()
                .filter(app -> Boolean.TRUE.equals(app.getCheckEnabled()))
                .filter(app -> app.getCheckType() != null && !"NONE".equals(app.getCheckType()))
                .filter(app -> app.getCheckUrl() != null && !app.getCheckUrl().isBlank())
                .filter(app -> isDueForCheck(app.getLastCheckAt(), app.getCheckIntervalSeconds(), now))
                .toList();
    }

    /**
     * Retrieves all status components that are due for a health check.
     */
    private List<StatusComponent> getComponentsDueForCheck() {
        List<StatusComponent> allComponents = statusComponentRepository.findAll();
        ZonedDateTime now = ZonedDateTime.now();

        return allComponents.stream()
                .filter(component -> {
                    if (Boolean.TRUE.equals(component.getCheckInheritFromApp())) {
                        return false;
                    }
                    return Boolean.TRUE.equals(component.getCheckEnabled());
                })
                .filter(component -> component.getCheckType() != null && !"NONE".equals(component.getCheckType()))
                .filter(component -> component.getCheckUrl() != null && !component.getCheckUrl().isBlank())
                .filter(component -> isDueForCheck(component.getLastCheckAt(), component.getCheckIntervalSeconds(), now))
                .toList();
    }

    /**
     * Determines whether an entity is due for a health check based on its last check time and interval.
     */
    private boolean isDueForCheck(ZonedDateTime lastCheckAt, Integer intervalSeconds, ZonedDateTime now) {
        if (lastCheckAt == null) {
            return true;
        }
        int interval = intervalSeconds != null ? intervalSeconds : 60;
        return lastCheckAt.plusSeconds(interval).isBefore(now);
    }

    /**
     * Performs a health check on a single status app.
     */
    private void checkApp(StatusApp app) {
        try {
            logger.debug("Checking app: {} ({})", app.getName(), app.getCheckType());

            HealthCheckService.HealthCheckResult result = healthCheckService.performCheck(
                    app.getCheckType(),
                    app.getCheckUrl(),
                    app.getCheckTimeoutSeconds() != null ? app.getCheckTimeoutSeconds() : 10,
                    app.getCheckExpectedStatus()
            );

            healthCheckService.updateAppCheckResult(app, result);

            if (result.success()) {
                logger.debug("App {} check successful: {}", app.getName(), result.message());
            } else {
                logger.warn("App {} check failed: {}", app.getName(), result.message());
            }
        } catch (Exception e) {
            logger.error("Error checking app {}: {}", app.getName(), e.getMessage());
            healthCheckService.updateAppCheckResult(app,
                    new HealthCheckService.HealthCheckResult(false, "Check error: " + e.getMessage()));
        }
    }

    /**
     * Performs a health check on a single status component.
     */
    private void checkComponent(StatusComponent component) {
        try {
            logger.debug("Checking component: {} ({})", component.getName(), component.getCheckType());

            HealthCheckService.HealthCheckResult result = healthCheckService.performCheck(
                    component.getCheckType(),
                    component.getCheckUrl(),
                    component.getCheckTimeoutSeconds() != null ? component.getCheckTimeoutSeconds() : 10,
                    component.getCheckExpectedStatus()
            );

            healthCheckService.updateComponentCheckResult(component, result);

            if (result.success()) {
                logger.debug("Component {} check successful: {}", component.getName(), result.message());
            } else {
                logger.warn("Component {} check failed: {}", component.getName(), result.message());
            }
        } catch (Exception e) {
            logger.error("Error checking component {}: {}", component.getName(), e.getMessage());
            healthCheckService.updateComponentCheckResult(component,
                    new HealthCheckService.HealthCheckResult(false, "Check error: " + e.getMessage()));
        }
    }
}
