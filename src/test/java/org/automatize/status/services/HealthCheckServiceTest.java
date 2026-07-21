package org.automatize.status.services;

import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.automatize.status.services.HealthCheckService.HealthCheckResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link HealthCheckService}.
 *
 * <p>Only the pure / branching logic is exercised here: dispatch, SSRF validation,
 * and the status-transition bookkeeping in {@code updateAppCheckResult} /
 * {@code updateComponentCheckResult}. The actual PING / HTTP_GET / SPRING_BOOT_HEALTH /
 * TCP_PORT network paths open real sockets and are not injectable, so they require
 * integration coverage and are intentionally not tested here.</p>
 */
@ExtendWith(MockitoExtension.class)
class HealthCheckServiceTest {

    @Mock
    private StatusAppRepository statusAppRepository;

    @Mock
    private StatusComponentRepository statusComponentRepository;

    @Mock
    private StatusIncidentService statusIncidentService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private HealthCheckService healthCheckService;

    // ------------------------------------------------------ performCheck dispatch

    @Test
    void performCheck_nullCheckType_returnsSuccessNoCheck() {
        HealthCheckResult result = healthCheckService.performCheck(null, "http://8.8.8.8", 5, 200);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("No check configured");
    }

    @Test
    void performCheck_noneCheckType_returnsSuccessNoCheck() {
        HealthCheckResult result = healthCheckService.performCheck("NONE", "http://8.8.8.8", 5, 200);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("No check configured");
    }

    @Test
    void performCheck_unknownCheckType_returnsFailure() {
        // 8.8.8.8 is a literal, publicly routable IP so SSRF validation passes without DNS.
        HealthCheckResult result = healthCheckService.performCheck("BOGUS", "http://8.8.8.8", 5, 200);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Unknown check type: BOGUS");
    }

    @Test
    void performCheck_loopbackHost_blockedBySsrf() {
        HealthCheckResult result = healthCheckService.performCheck("HTTP_GET", "http://127.0.0.1", 5, 200);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).startsWith("Blocked:");
    }

    @Test
    void performCheck_privateHost_blockedBySsrf() {
        HealthCheckResult result = healthCheckService.performCheck("HTTP_GET", "http://10.0.0.1", 5, 200);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).startsWith("Blocked:");
    }

    @Test
    void performCheck_tcpWithInvalidPort_returnsInvalidPort() {
        // Host resolves (literal public IP, SSRF passes); port parse fails deterministically, no socket opened.
        HealthCheckResult result = healthCheckService.performCheck("TCP_PORT", "tcp://8.8.8.8:notaport", 5, null);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Invalid port number");
    }

    // --------------------------------------------------- updateAppCheckResult

    private StatusApp appWith(String status, int consecutiveFailures, int threshold) {
        StatusApp app = new StatusApp();
        app.setName("app");
        app.setStatus(status);
        app.setConsecutiveFailures(consecutiveFailures);
        app.setCheckFailureThreshold(threshold);
        return app;
    }

    @Test
    void updateAppCheckResult_success_resetsFailuresAndSaves() {
        StatusApp app = appWith("OPERATIONAL", 2, 3);

        healthCheckService.updateAppCheckResult(app, new HealthCheckResult(true, "ok"));

        assertThat(app.getConsecutiveFailures()).isZero();
        assertThat(app.getLastCheckSuccess()).isTrue();
        assertThat(app.getLastCheckMessage()).isEqualTo("ok");
        assertThat(app.getLastCheckAt()).isNotNull();
        assertThat(app.getStatus()).isEqualTo("OPERATIONAL");
        verify(statusAppRepository).save(app);
        verifyNoInteractions(statusIncidentService);
    }

    @Test
    void updateAppCheckResult_successWhenDegraded_restoresAndResolvesIncidents() {
        StatusApp app = appWith("DEGRADED_PERFORMANCE", 5, 3);

        healthCheckService.updateAppCheckResult(app, new HealthCheckResult(true, "recovered"));

        assertThat(app.getStatus()).isEqualTo("OPERATIONAL");
        verify(statusIncidentService).resolveAutomatedIncidents(app);
        verify(statusAppRepository).save(app);
    }

    @Test
    void updateAppCheckResult_successWhenMajorOutage_restoresAndResolvesIncidents() {
        StatusApp app = appWith("MAJOR_OUTAGE", 8, 3);

        healthCheckService.updateAppCheckResult(app, new HealthCheckResult(true, "recovered"));

        assertThat(app.getStatus()).isEqualTo("OPERATIONAL");
        verify(statusIncidentService).resolveAutomatedIncidents(app);
    }

    @Test
    void updateAppCheckResult_failureBelowThreshold_noStatusChange() {
        StatusApp app = appWith("OPERATIONAL", 0, 3);

        healthCheckService.updateAppCheckResult(app, new HealthCheckResult(false, "down"));

        assertThat(app.getConsecutiveFailures()).isEqualTo(1);
        assertThat(app.getStatus()).isEqualTo("OPERATIONAL");
        verify(statusIncidentService, never()).createAutomatedIncident(any(), any(), any());
        verify(statusAppRepository).save(app);
    }

    @Test
    void updateAppCheckResult_failureAtThreshold_degradesAndCreatesMajorIncident() {
        StatusApp app = appWith("OPERATIONAL", 2, 3);

        healthCheckService.updateAppCheckResult(app, new HealthCheckResult(false, "down"));

        assertThat(app.getConsecutiveFailures()).isEqualTo(3);
        assertThat(app.getStatus()).isEqualTo("DEGRADED_PERFORMANCE");
        verify(statusIncidentService).createAutomatedIncident(app, "MAJOR", "down");
    }

    @Test
    void updateAppCheckResult_failureAtDoubleThreshold_majorOutageAndCreatesCriticalIncident() {
        StatusApp app = appWith("DEGRADED_PERFORMANCE", 5, 3);

        healthCheckService.updateAppCheckResult(app, new HealthCheckResult(false, "down"));

        assertThat(app.getConsecutiveFailures()).isEqualTo(6);
        assertThat(app.getStatus()).isEqualTo("MAJOR_OUTAGE");
        verify(statusIncidentService).createAutomatedIncident(app, "CRITICAL", "down");
    }

    @Test
    void updateAppCheckResult_nullConsecutiveAndThreshold_usesDefaults() {
        StatusApp app = new StatusApp();
        app.setName("app");
        app.setStatus("OPERATIONAL");
        // consecutiveFailures and checkFailureThreshold left null -> defaults 0 and 3

        healthCheckService.updateAppCheckResult(app, new HealthCheckResult(false, "down"));

        assertThat(app.getConsecutiveFailures()).isEqualTo(1);
        assertThat(app.getStatus()).isEqualTo("OPERATIONAL");
    }

    // ------------------------------------------- updateComponentCheckResult

    private StatusComponent componentWith(String status, int consecutiveFailures, int threshold) {
        StatusComponent component = new StatusComponent();
        component.setName("component");
        component.setStatus(status);
        component.setConsecutiveFailures(consecutiveFailures);
        component.setCheckFailureThreshold(threshold);
        return component;
    }

    @Test
    void updateComponentCheckResult_success_resetsFailuresAndSaves() {
        StatusComponent component = componentWith("OPERATIONAL", 2, 3);

        healthCheckService.updateComponentCheckResult(component, new HealthCheckResult(true, "ok"));

        assertThat(component.getConsecutiveFailures()).isZero();
        assertThat(component.getLastCheckSuccess()).isTrue();
        verify(statusComponentRepository).save(component);
    }

    @Test
    void updateComponentCheckResult_successWhenDegraded_restoresToOperational() {
        StatusComponent component = componentWith("DEGRADED_PERFORMANCE", 5, 3);

        healthCheckService.updateComponentCheckResult(component, new HealthCheckResult(true, "recovered"));

        assertThat(component.getStatus()).isEqualTo("OPERATIONAL");
        verify(statusComponentRepository).save(component);
    }

    @Test
    void updateComponentCheckResult_failureAtThreshold_degrades() {
        StatusComponent component = componentWith("OPERATIONAL", 2, 3);

        healthCheckService.updateComponentCheckResult(component, new HealthCheckResult(false, "down"));

        assertThat(component.getConsecutiveFailures()).isEqualTo(3);
        assertThat(component.getStatus()).isEqualTo("DEGRADED_PERFORMANCE");
    }

    @Test
    void updateComponentCheckResult_failureAtDoubleThreshold_majorOutage() {
        StatusComponent component = componentWith("DEGRADED_PERFORMANCE", 5, 3);

        healthCheckService.updateComponentCheckResult(component, new HealthCheckResult(false, "down"));

        assertThat(component.getConsecutiveFailures()).isEqualTo(6);
        assertThat(component.getStatus()).isEqualTo("MAJOR_OUTAGE");
    }

    @Test
    void updateComponentCheckResult_failureBelowThreshold_noStatusChange() {
        StatusComponent component = componentWith("OPERATIONAL", 0, 3);

        healthCheckService.updateComponentCheckResult(component, new HealthCheckResult(false, "down"));

        assertThat(component.getConsecutiveFailures()).isEqualTo(1);
        assertThat(component.getStatus()).isEqualTo("OPERATIONAL");
        verify(statusComponentRepository).save(component);
    }
}
