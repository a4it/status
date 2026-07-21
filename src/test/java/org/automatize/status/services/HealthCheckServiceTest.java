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

    private static final String URL_PUBLIC_IP = "http://8.8.8.8";
    private static final String STATUS_OPERATIONAL = "OPERATIONAL";
    private static final String STATUS_DEGRADED_PERFORMANCE = "DEGRADED_PERFORMANCE";
    private static final String STATUS_MAJOR_OUTAGE = "MAJOR_OUTAGE";
    private static final String MESSAGE_RECOVERED = "recovered";

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

    /**
     * Verifies that a null check type is treated as "no check configured" and reported as a
     * successful, no-op result.
     */
    @Test
    void performCheck_nullCheckType_returnsSuccessNoCheck() {
        HealthCheckResult result = healthCheckService.performCheck(null, URL_PUBLIC_IP, 5, 200);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("No check configured");
    }

    /**
     * Verifies that an explicit "NONE" check type is treated as "no check configured" and
     * reported as a successful, no-op result.
     */
    @Test
    void performCheck_noneCheckType_returnsSuccessNoCheck() {
        HealthCheckResult result = healthCheckService.performCheck("NONE", URL_PUBLIC_IP, 5, 200);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("No check configured");
    }

    /**
     * Verifies that an unrecognised check type yields a failure result naming the unknown
     * type.
     */
    @Test
    void performCheck_unknownCheckType_returnsFailure() {
        // 8.8.8.8 is a literal, publicly routable IP so SSRF validation passes without DNS.
        HealthCheckResult result = healthCheckService.performCheck("BOGUS", URL_PUBLIC_IP, 5, 200);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Unknown check type: BOGUS");
    }

    /**
     * Verifies that a loopback host is rejected by SSRF validation with a "Blocked:" result.
     */
    @Test
    void performCheck_loopbackHost_blockedBySsrf() {
        HealthCheckResult result = healthCheckService.performCheck("HTTP_GET", "http://127.0.0.1", 5, 200);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).startsWith("Blocked:");
    }

    /**
     * Verifies that a private-range host is rejected by SSRF validation with a "Blocked:"
     * result.
     */
    @Test
    void performCheck_privateHost_blockedBySsrf() {
        HealthCheckResult result = healthCheckService.performCheck("HTTP_GET", "http://10.0.0.1", 5, 200);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).startsWith("Blocked:");
    }

    /**
     * Verifies that a TCP check whose URL contains a non-numeric port fails with an
     * "Invalid port number" result before any socket is opened.
     */
    @Test
    void performCheck_tcpWithInvalidPort_returnsInvalidPort() {
        // Host resolves (literal public IP, SSRF passes); port parse fails deterministically, no socket opened.
        HealthCheckResult result = healthCheckService.performCheck("TCP_PORT", "tcp://8.8.8.8:notaport", 5, null);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Invalid port number");
    }

    // --------------------------------------------------- updateAppCheckResult

    /**
     * Builds a {@link StatusApp} fixture with the given status, consecutive-failure count,
     * and failure threshold for exercising the check-result bookkeeping.
     *
     * @param status              the initial status of the app
     * @param consecutiveFailures the initial consecutive-failure counter
     * @param threshold           the failure threshold at which status degrades
     * @return a configured {@link StatusApp} test fixture
     */
    private StatusApp appWith(String status, int consecutiveFailures, int threshold) {
        StatusApp app = new StatusApp();
        app.setName("app");
        app.setStatus(status);
        app.setConsecutiveFailures(consecutiveFailures);
        app.setCheckFailureThreshold(threshold);
        return app;
    }

    /**
     * Verifies that a successful check on an operational app resets its failure counter,
     * records the check outcome, saves it, and creates no incident.
     */
    @Test
    void updateAppCheckResult_success_resetsFailuresAndSaves() {
        StatusApp app = appWith(STATUS_OPERATIONAL, 2, 3);

        healthCheckService.updateAppCheckResult(app, new HealthCheckResult(true, "ok"));

        assertThat(app.getConsecutiveFailures()).isZero();
        assertThat(app.getLastCheckSuccess()).isTrue();
        assertThat(app.getLastCheckMessage()).isEqualTo("ok");
        assertThat(app.getLastCheckAt()).isNotNull();
        assertThat(app.getStatus()).isEqualTo(STATUS_OPERATIONAL);
        verify(statusAppRepository).save(app);
        verifyNoInteractions(statusIncidentService);
    }

    /**
     * Verifies that a successful check on a degraded app restores it to operational and
     * resolves its automated incidents.
     */
    @Test
    void updateAppCheckResult_successWhenDegraded_restoresAndResolvesIncidents() {
        StatusApp app = appWith(STATUS_DEGRADED_PERFORMANCE, 5, 3);

        healthCheckService.updateAppCheckResult(app, new HealthCheckResult(true, MESSAGE_RECOVERED));

        assertThat(app.getStatus()).isEqualTo(STATUS_OPERATIONAL);
        verify(statusIncidentService).resolveAutomatedIncidents(app);
        verify(statusAppRepository).save(app);
    }

    /**
     * Verifies that a successful check on an app in major outage restores it to operational
     * and resolves its automated incidents.
     */
    @Test
    void updateAppCheckResult_successWhenMajorOutage_restoresAndResolvesIncidents() {
        StatusApp app = appWith(STATUS_MAJOR_OUTAGE, 8, 3);

        healthCheckService.updateAppCheckResult(app, new HealthCheckResult(true, MESSAGE_RECOVERED));

        assertThat(app.getStatus()).isEqualTo(STATUS_OPERATIONAL);
        verify(statusIncidentService).resolveAutomatedIncidents(app);
    }

    /**
     * Verifies that a failed check that keeps the failure count below the threshold
     * increments the counter, leaves the status unchanged, creates no incident, and saves.
     */
    @Test
    void updateAppCheckResult_failureBelowThreshold_noStatusChange() {
        StatusApp app = appWith(STATUS_OPERATIONAL, 0, 3);

        healthCheckService.updateAppCheckResult(app, new HealthCheckResult(false, "down"));

        assertThat(app.getConsecutiveFailures()).isEqualTo(1);
        assertThat(app.getStatus()).isEqualTo(STATUS_OPERATIONAL);
        verify(statusIncidentService, never()).createAutomatedIncident(any(), any(), any());
        verify(statusAppRepository).save(app);
    }

    /**
     * Verifies that a failed check reaching the threshold degrades the app's performance and
     * creates a MAJOR automated incident.
     */
    @Test
    void updateAppCheckResult_failureAtThreshold_degradesAndCreatesMajorIncident() {
        StatusApp app = appWith(STATUS_OPERATIONAL, 2, 3);

        healthCheckService.updateAppCheckResult(app, new HealthCheckResult(false, "down"));

        assertThat(app.getConsecutiveFailures()).isEqualTo(3);
        assertThat(app.getStatus()).isEqualTo(STATUS_DEGRADED_PERFORMANCE);
        verify(statusIncidentService).createAutomatedIncident(app, "MAJOR", "down");
    }

    /**
     * Verifies that a failed check reaching twice the threshold escalates the app to major
     * outage and creates a CRITICAL automated incident.
     */
    @Test
    void updateAppCheckResult_failureAtDoubleThreshold_majorOutageAndCreatesCriticalIncident() {
        StatusApp app = appWith(STATUS_DEGRADED_PERFORMANCE, 5, 3);

        healthCheckService.updateAppCheckResult(app, new HealthCheckResult(false, "down"));

        assertThat(app.getConsecutiveFailures()).isEqualTo(6);
        assertThat(app.getStatus()).isEqualTo(STATUS_MAJOR_OUTAGE);
        verify(statusIncidentService).createAutomatedIncident(app, "CRITICAL", "down");
    }

    /**
     * Verifies that when the app's consecutive-failure count and threshold are null, a
     * failed check applies the defaults (0 and 3): count becomes 1 with no status change.
     */
    @Test
    void updateAppCheckResult_nullConsecutiveAndThreshold_usesDefaults() {
        StatusApp app = new StatusApp();
        app.setName("app");
        app.setStatus(STATUS_OPERATIONAL);
        // consecutiveFailures and checkFailureThreshold left null -> defaults 0 and 3

        healthCheckService.updateAppCheckResult(app, new HealthCheckResult(false, "down"));

        assertThat(app.getConsecutiveFailures()).isEqualTo(1);
        assertThat(app.getStatus()).isEqualTo(STATUS_OPERATIONAL);
    }

    // ------------------------------------------- updateComponentCheckResult

    /**
     * Builds a {@link StatusComponent} fixture with the given status, consecutive-failure
     * count, and failure threshold for exercising the check-result bookkeeping.
     *
     * @param status              the initial status of the component
     * @param consecutiveFailures the initial consecutive-failure counter
     * @param threshold           the failure threshold at which status degrades
     * @return a configured {@link StatusComponent} test fixture
     */
    private StatusComponent componentWith(String status, int consecutiveFailures, int threshold) {
        StatusComponent component = new StatusComponent();
        component.setName("component");
        component.setStatus(status);
        component.setConsecutiveFailures(consecutiveFailures);
        component.setCheckFailureThreshold(threshold);
        return component;
    }

    /**
     * Verifies that a successful check on an operational component resets its failure
     * counter, records success, and saves it.
     */
    @Test
    void updateComponentCheckResult_success_resetsFailuresAndSaves() {
        StatusComponent component = componentWith(STATUS_OPERATIONAL, 2, 3);

        healthCheckService.updateComponentCheckResult(component, new HealthCheckResult(true, "ok"));

        assertThat(component.getConsecutiveFailures()).isZero();
        assertThat(component.getLastCheckSuccess()).isTrue();
        verify(statusComponentRepository).save(component);
    }

    /**
     * Verifies that a successful check on a degraded component restores it to operational
     * and saves it.
     */
    @Test
    void updateComponentCheckResult_successWhenDegraded_restoresToOperational() {
        StatusComponent component = componentWith(STATUS_DEGRADED_PERFORMANCE, 5, 3);

        healthCheckService.updateComponentCheckResult(component, new HealthCheckResult(true, MESSAGE_RECOVERED));

        assertThat(component.getStatus()).isEqualTo(STATUS_OPERATIONAL);
        verify(statusComponentRepository).save(component);
    }

    /**
     * Verifies that a failed check reaching the threshold degrades the component's
     * performance.
     */
    @Test
    void updateComponentCheckResult_failureAtThreshold_degrades() {
        StatusComponent component = componentWith(STATUS_OPERATIONAL, 2, 3);

        healthCheckService.updateComponentCheckResult(component, new HealthCheckResult(false, "down"));

        assertThat(component.getConsecutiveFailures()).isEqualTo(3);
        assertThat(component.getStatus()).isEqualTo(STATUS_DEGRADED_PERFORMANCE);
    }

    /**
     * Verifies that a failed check reaching twice the threshold escalates the component to
     * major outage.
     */
    @Test
    void updateComponentCheckResult_failureAtDoubleThreshold_majorOutage() {
        StatusComponent component = componentWith(STATUS_DEGRADED_PERFORMANCE, 5, 3);

        healthCheckService.updateComponentCheckResult(component, new HealthCheckResult(false, "down"));

        assertThat(component.getConsecutiveFailures()).isEqualTo(6);
        assertThat(component.getStatus()).isEqualTo(STATUS_MAJOR_OUTAGE);
    }

    /**
     * Verifies that a failed check keeping the component's failure count below the threshold
     * increments the counter, leaves the status unchanged, and saves it.
     */
    @Test
    void updateComponentCheckResult_failureBelowThreshold_noStatusChange() {
        StatusComponent component = componentWith(STATUS_OPERATIONAL, 0, 3);

        healthCheckService.updateComponentCheckResult(component, new HealthCheckResult(false, "down"));

        assertThat(component.getConsecutiveFailures()).isEqualTo(1);
        assertThat(component.getStatus()).isEqualTo(STATUS_OPERATIONAL);
        verify(statusComponentRepository).save(component);
    }
}
