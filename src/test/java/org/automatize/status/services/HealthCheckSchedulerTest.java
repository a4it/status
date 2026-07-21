package org.automatize.status.services;

import org.automatize.status.api.response.HealthCheckTriggerResponse;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.automatize.status.services.HealthCheckService.HealthCheckResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HealthCheckScheduler}.
 *
 * <p>Testing approach: the repository and service collaborators are Mockito mocks, but the
 * scheduler is constructed by hand in {@link #setUp()} because its constructor takes a
 * primitive thread-pool size that {@code @InjectMocks} cannot supply. Because checks are
 * submitted to an executor, asynchronous side effects are asserted with Mockito
 * {@code timeout(...)} verifications. Tests cover the scheduled {@code runHealthChecks}
 * gating, the bulk {@code triggerAllChecks} count, and every validation branch of the
 * single-target {@code triggerAppCheck} / {@code triggerComponentCheck} entry points.</p>
 */
@ExtendWith(MockitoExtension.class)
class HealthCheckSchedulerTest {

    private static final String CHECK_TYPE_HTTP_GET = "HTTP_GET";
    private static final String CHECK_URL = "http://8.8.8.8";
    private static final String HTTP_200_MESSAGE = "HTTP 200";

    @Mock
    private StatusAppRepository statusAppRepository;

    @Mock
    private StatusComponentRepository statusComponentRepository;

    @Mock
    private HealthCheckService healthCheckService;

    @Mock
    private HealthCheckSettingsService settingsService;

    private HealthCheckScheduler scheduler;

    /**
     * Constructs the scheduler under test with the mocked collaborators and a small
     * thread-pool size, then enables the default health-check flag.
     */
    @BeforeEach
    void setUp() {
        // Constructor takes a primitive thread-pool size which Mockito cannot inject,
        // so the scheduler is wired up manually.
        scheduler = new HealthCheckScheduler(statusAppRepository, statusComponentRepository,
                healthCheckService, settingsService, 2);
        ReflectionTestUtils.setField(scheduler, "healthCheckEnabledDefault", true);
    }

    /**
     * Builds a fully check-enabled {@link StatusApp} fixture (HTTP GET against a public IP)
     * with no prior check timestamp, so it is considered due.
     *
     * @return a check-enabled {@link StatusApp} test fixture
     */
    private StatusApp checkableApp() {
        StatusApp app = new StatusApp();
        app.setName("app");
        app.setCheckEnabled(true);
        app.setCheckType(CHECK_TYPE_HTTP_GET);
        app.setCheckUrl(CHECK_URL);
        app.setCheckTimeoutSeconds(5);
        app.setCheckExpectedStatus(200);
        return app;
    }

    /**
     * Builds a fully check-enabled {@link StatusComponent} fixture that does not inherit its
     * check configuration from its parent app.
     *
     * @return a check-enabled {@link StatusComponent} test fixture
     */
    private StatusComponent checkableComponent() {
        StatusComponent component = new StatusComponent();
        component.setName("component");
        component.setCheckInheritFromApp(false);
        component.setCheckEnabled(true);
        component.setCheckType(CHECK_TYPE_HTTP_GET);
        component.setCheckUrl(CHECK_URL);
        component.setCheckTimeoutSeconds(5);
        component.setCheckExpectedStatus(200);
        return component;
    }

    // --------------------------------------------------------- runHealthChecks

    /**
     * Verifies that when health checks are globally disabled, the scheduled run queries no
     * repositories and performs no checks.
     */
    @Test
    void runHealthChecks_whenDisabled_doesNothing() {
        when(settingsService.isEnabled()).thenReturn(false);

        scheduler.runHealthChecks();

        verify(statusAppRepository, never()).findCheckEnabledApps();
        verify(statusComponentRepository, never()).findCheckEnabledComponents();
    }

    /**
     * Verifies that when health checks are enabled, a due app is checked and its result is
     * persisted via {@code updateAppCheckResult} (asserted asynchronously).
     */
    @Test
    void runHealthChecks_whenEnabled_submitsChecksForDueApps() {
        StatusApp app = checkableApp(); // lastCheckAt null -> due
        when(settingsService.isEnabled()).thenReturn(true);
        when(statusAppRepository.findCheckEnabledApps()).thenReturn(List.of(app));
        when(statusComponentRepository.findCheckEnabledComponents()).thenReturn(List.of());
        when(healthCheckService.performCheck(any(), any(), anyInt(), any()))
                .thenReturn(new HealthCheckResult(true, "ok"));

        scheduler.runHealthChecks();

        verify(healthCheckService, timeout(2000)).updateAppCheckResult(eq(app), any());
    }

    // -------------------------------------------------------- triggerAllChecks

    /**
     * Verifies that triggering all checks returns the total number of enabled targets and
     * submits a check for both the app and the component (results persisted asynchronously).
     */
    @Test
    void triggerAllChecks_returnsCountAndSubmitsChecks() {
        StatusApp app = checkableApp();
        StatusComponent component = checkableComponent();
        when(statusAppRepository.findCheckEnabledApps()).thenReturn(List.of(app));
        when(statusComponentRepository.findCheckEnabledComponents()).thenReturn(List.of(component));
        when(healthCheckService.performCheck(any(), any(), anyInt(), any()))
                .thenReturn(new HealthCheckResult(true, "ok"));

        int count = scheduler.triggerAllChecks();

        assertThat(count).isEqualTo(2);
        verify(healthCheckService, timeout(2000)).updateAppCheckResult(eq(app), any());
        verify(healthCheckService, timeout(2000)).updateComponentCheckResult(eq(component), any());
    }

    // --------------------------------------------------------- triggerAppCheck

    /**
     * Verifies that triggering a check for an unknown app id returns an unsuccessful
     * response whose message reports the app was not found.
     */
    @Test
    void triggerAppCheck_appNotFound_returnsFailure() {
        UUID id = UUID.randomUUID();
        when(statusAppRepository.findById(id)).thenReturn(java.util.Optional.empty());

        HealthCheckTriggerResponse response = scheduler.triggerAppCheck(id);

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getMessage()).contains("App not found");
    }

    /**
     * Verifies that triggering a check for an app with checks disabled returns an
     * unsuccessful response whose message reports the check is not enabled.
     */
    @Test
    void triggerAppCheck_checkDisabled_returnsFailure() {
        UUID id = UUID.randomUUID();
        StatusApp app = checkableApp();
        app.setCheckEnabled(false);
        when(statusAppRepository.findById(id)).thenReturn(java.util.Optional.of(app));

        HealthCheckTriggerResponse response = scheduler.triggerAppCheck(id);

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getMessage()).contains("not enabled");
    }

    /**
     * Verifies that triggering a check for an app with no configured check type returns an
     * unsuccessful response whose message reports no check type.
     */
    @Test
    void triggerAppCheck_noCheckType_returnsFailure() {
        UUID id = UUID.randomUUID();
        StatusApp app = checkableApp();
        app.setCheckType(null);
        when(statusAppRepository.findById(id)).thenReturn(java.util.Optional.of(app));

        HealthCheckTriggerResponse response = scheduler.triggerAppCheck(id);

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getMessage()).contains("No check type");
    }

    /**
     * Verifies that triggering a check for an app whose check URL is blank returns an
     * unsuccessful response whose message reports no check URL.
     */
    @Test
    void triggerAppCheck_noCheckUrl_returnsFailure() {
        UUID id = UUID.randomUUID();
        StatusApp app = checkableApp();
        app.setCheckUrl("  ");
        when(statusAppRepository.findById(id)).thenReturn(java.util.Optional.of(app));

        HealthCheckTriggerResponse response = scheduler.triggerAppCheck(id);

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getMessage()).contains("No check URL");
    }

    /**
     * Verifies that a valid app check returns a successful response carrying the check
     * message and a non-null duration, and that the result is persisted.
     */
    @Test
    void triggerAppCheck_happyPath_returnsSuccessWithDuration() {
        UUID id = UUID.randomUUID();
        StatusApp app = checkableApp();
        when(statusAppRepository.findById(id)).thenReturn(java.util.Optional.of(app));
        when(healthCheckService.performCheck(eq(CHECK_TYPE_HTTP_GET), eq(CHECK_URL), eq(5), eq(200)))
                .thenReturn(new HealthCheckResult(true, HTTP_200_MESSAGE));

        HealthCheckTriggerResponse response = scheduler.triggerAppCheck(id);

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo(HTTP_200_MESSAGE);
        assertThat(response.getDurationMs()).isNotNull();
        verify(healthCheckService).updateAppCheckResult(eq(app), any());
    }

    /**
     * Verifies that when the underlying check throws, the app trigger returns an
     * unsuccessful response whose message is prefixed with "Check error:".
     */
    @Test
    void triggerAppCheck_whenPerformCheckThrows_returnsCheckError() {
        UUID id = UUID.randomUUID();
        StatusApp app = checkableApp();
        when(statusAppRepository.findById(id)).thenReturn(java.util.Optional.of(app));
        when(healthCheckService.performCheck(any(), any(), anyInt(), any()))
                .thenThrow(new RuntimeException("boom"));

        HealthCheckTriggerResponse response = scheduler.triggerAppCheck(id);

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Check error:");
    }

    // --------------------------------------------------- triggerComponentCheck

    /**
     * Verifies that triggering a check for an unknown component id returns an unsuccessful
     * response whose message reports the component was not found.
     */
    @Test
    void triggerComponentCheck_notFound_returnsFailure() {
        UUID id = UUID.randomUUID();
        when(statusComponentRepository.findById(id)).thenReturn(java.util.Optional.empty());

        HealthCheckTriggerResponse response = scheduler.triggerComponentCheck(id);

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Component not found");
    }

    /**
     * Verifies that triggering a check for a component configured to inherit its check from
     * its app returns an unsuccessful response explaining the inheritance.
     */
    @Test
    void triggerComponentCheck_inheritsFromApp_returnsFailure() {
        UUID id = UUID.randomUUID();
        StatusComponent component = checkableComponent();
        component.setCheckInheritFromApp(true);
        when(statusComponentRepository.findById(id)).thenReturn(java.util.Optional.of(component));

        HealthCheckTriggerResponse response = scheduler.triggerComponentCheck(id);

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getMessage()).contains("inherits check from app");
    }

    /**
     * Verifies that triggering a check for a component with checks disabled returns an
     * unsuccessful response whose message reports the check is not enabled.
     */
    @Test
    void triggerComponentCheck_disabled_returnsFailure() {
        UUID id = UUID.randomUUID();
        StatusComponent component = checkableComponent();
        component.setCheckEnabled(false);
        when(statusComponentRepository.findById(id)).thenReturn(java.util.Optional.of(component));

        HealthCheckTriggerResponse response = scheduler.triggerComponentCheck(id);

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getMessage()).contains("not enabled");
    }

    /**
     * Verifies that a valid component check returns a successful response carrying the check
     * message, and that the result is persisted.
     */
    @Test
    void triggerComponentCheck_happyPath_returnsSuccess() {
        UUID id = UUID.randomUUID();
        StatusComponent component = checkableComponent();
        when(statusComponentRepository.findById(id)).thenReturn(java.util.Optional.of(component));
        when(healthCheckService.performCheck(eq("HTTP_GET"), eq("http://8.8.8.8"), eq(5), eq(200)))
                .thenReturn(new HealthCheckResult(true, "HTTP 200"));

        HealthCheckTriggerResponse response = scheduler.triggerComponentCheck(id);

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("HTTP 200");
        verify(healthCheckService).updateComponentCheckResult(eq(component), any());
    }
}
