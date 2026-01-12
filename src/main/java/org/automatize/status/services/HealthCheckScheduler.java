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

@Service
public class HealthCheckScheduler {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckScheduler.class);

    private final StatusAppRepository statusAppRepository;
    private final StatusComponentRepository statusComponentRepository;
    private final HealthCheckService healthCheckService;
    private final ExecutorService executorService;

    @Value("${health-check.enabled:true}")
    private boolean healthCheckEnabled;

    public HealthCheckScheduler(StatusAppRepository statusAppRepository,
                                StatusComponentRepository statusComponentRepository,
                                HealthCheckService healthCheckService,
                                @Value("${health-check.thread-pool-size:10}") int threadPoolSize) {
        this.statusAppRepository = statusAppRepository;
        this.statusComponentRepository = statusComponentRepository;
        this.healthCheckService = healthCheckService;
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

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

    private boolean isDueForCheck(ZonedDateTime lastCheckAt, Integer intervalSeconds, ZonedDateTime now) {
        if (lastCheckAt == null) {
            return true; // Never checked before
        }
        int interval = intervalSeconds != null ? intervalSeconds : 60;
        return lastCheckAt.plusSeconds(interval).isBefore(now);
    }

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
