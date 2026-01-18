package org.automatize.status.services;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * Scheduled service responsible for orchestrating health checks on status apps and components.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Identify apps and components due for health checks</li>
 *   <li>Execute health checks asynchronously using a thread pool</li>
 *   <li>Coordinate with HealthCheckService for actual check execution</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see HealthCheckService
 */
@Service
public class HealthCheckScheduler {

    /**
     * Logger instance for health check scheduling operations.
     */
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckScheduler.class);

    /**
     * Repository for accessing status app data.
     */
    private final StatusAppRepository statusAppRepository;

    /**
     * Repository for accessing status component data.
     */
    private final StatusComponentRepository statusComponentRepository;

    /**
     * Service that performs the actual health checks.
     */
    private final HealthCheckService healthCheckService;

    /**
     * Thread pool executor for running health checks asynchronously.
     */
    private final ExecutorService executorService;

    /**
     * Flag indicating whether health checks are enabled.
     */
    @Value("${health-check.enabled:true}")
    private boolean healthCheckEnabled;

    /**
     * Constructs a new HealthCheckScheduler with the required dependencies.
     *
     * @param statusAppRepository repository for status app data access
     * @param statusComponentRepository repository for status component data access
     * @param healthCheckService service for performing health checks
     * @param threadPoolSize the size of the thread pool for concurrent health checks
     */
    public HealthCheckScheduler(StatusAppRepository statusAppRepository,
                                StatusComponentRepository statusComponentRepository,
                                HealthCheckService healthCheckService,
                                @Value("${health-check.thread-pool-size:10}") int threadPoolSize) {
        this.statusAppRepository = statusAppRepository;
        this.statusComponentRepository = statusComponentRepository;
        this.healthCheckService = healthCheckService;
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    /**
     * Scheduled method that runs health checks on apps and components that are due.
     * <p>
     * This method is triggered at a fixed rate defined by the configuration property
     * {@code health-check.scheduler-interval-ms}. It identifies all apps and components
     * that need to be checked based on their last check time and configured interval,
     * then submits each check to the thread pool for asynchronous execution.
     * </p>
     * <p>
     * Health checks are skipped entirely if {@code health-check.enabled} is set to false.
     * </p>
     */
    @Scheduled(fixedRateString = "${health-check.scheduler-interval-ms:10000}")
    public void runHealthChecks() {
        if (!healthCheckEnabled) {
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
     * Retrieves all status apps that are due for a health check.
     * <p>
     * An app is considered due for check if:
     * <ul>
     *   <li>Health checking is enabled for the app</li>
     *   <li>The check type is configured and not "NONE"</li>
     *   <li>A check URL is configured</li>
     *   <li>The time since the last check exceeds the configured interval</li>
     * </ul>
     * </p>
     *
     * @return a list of StatusApp entities that need to be checked
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
     * <p>
     * A component is considered due for check if:
     * <ul>
     *   <li>It does not inherit check settings from its parent app</li>
     *   <li>Health checking is enabled for the component</li>
     *   <li>The check type is configured and not "NONE"</li>
     *   <li>A check URL is configured</li>
     *   <li>The time since the last check exceeds the configured interval</li>
     * </ul>
     * </p>
     *
     * @return a list of StatusComponent entities that need to be checked
     */
    private List<StatusComponent> getComponentsDueForCheck() {
        List<StatusComponent> allComponents = statusComponentRepository.findAll();
        ZonedDateTime now = ZonedDateTime.now();

        return allComponents.stream()
                .filter(component -> {
                    // If inheriting from app, skip - the app check will cover it
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
     *
     * @param lastCheckAt the timestamp of the last health check, or null if never checked
     * @param intervalSeconds the configured check interval in seconds, or null for default (60 seconds)
     * @param now the current timestamp for comparison
     * @return true if the entity should be checked, false otherwise
     */
    private boolean isDueForCheck(ZonedDateTime lastCheckAt, Integer intervalSeconds, ZonedDateTime now) {
        if (lastCheckAt == null) {
            return true; // Never checked before
        }
        int interval = intervalSeconds != null ? intervalSeconds : 60;
        return lastCheckAt.plusSeconds(interval).isBefore(now);
    }

    /**
     * Performs a health check on a single status app.
     * <p>
     * This method executes the configured health check for the app using the
     * HealthCheckService and updates the app's check result accordingly.
     * Any exceptions during the check are caught and recorded as failures.
     * </p>
     *
     * @param app the status app to check
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
     * <p>
     * This method executes the configured health check for the component using the
     * HealthCheckService and updates the component's check result accordingly.
     * Any exceptions during the check are caught and recorded as failures.
     * </p>
     *
     * @param component the status component to check
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
