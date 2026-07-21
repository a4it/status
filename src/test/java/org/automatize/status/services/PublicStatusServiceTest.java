package org.automatize.status.services;

import org.automatize.status.api.response.ComponentHistoryResponse;
import org.automatize.status.api.response.StatusAppResponse;
import org.automatize.status.api.response.StatusComponentResponse;
import org.automatize.status.api.response.StatusIncidentResponse;
import org.automatize.status.api.response.StatusMaintenanceResponse;
import org.automatize.status.api.response.StatusSummaryResponse;
import org.automatize.status.api.response.UptimeHistoryResponse;
import org.automatize.status.api.response.UptimeResponse;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.models.StatusIncident;
import org.automatize.status.models.StatusMaintenance;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.automatize.status.repositories.StatusIncidentComponentRepository;
import org.automatize.status.repositories.StatusIncidentRepository;
import org.automatize.status.repositories.StatusIncidentUpdateRepository;
import org.automatize.status.repositories.StatusMaintenanceComponentRepository;
import org.automatize.status.repositories.StatusMaintenanceRepository;
import org.automatize.status.repositories.StatusUptimeHistoryRepository;
import org.automatize.status.repositories.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PublicStatusService}.
 */
@ExtendWith(MockitoExtension.class)
class PublicStatusServiceTest {

    @Mock
    private StatusAppRepository statusAppRepository;
    @Mock
    private StatusComponentRepository statusComponentRepository;
    @Mock
    private StatusIncidentRepository statusIncidentRepository;
    @Mock
    private StatusIncidentUpdateRepository statusIncidentUpdateRepository;
    @Mock
    private StatusIncidentComponentRepository statusIncidentComponentRepository;
    @Mock
    private StatusMaintenanceRepository statusMaintenanceRepository;
    @Mock
    private StatusMaintenanceComponentRepository statusMaintenanceComponentRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private StatusUptimeHistoryRepository statusUptimeHistoryRepository;

    @InjectMocks
    private PublicStatusService publicStatusService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private StatusApp newApp(UUID id, String slug, String status, boolean isPublic) {
        StatusApp app = new StatusApp();
        app.setId(id);
        app.setName("App " + slug);
        app.setSlug(slug);
        app.setStatus(status);
        app.setIsPublic(isPublic);
        return app;
    }

    private StatusComponent newComponent(UUID id, StatusApp app, String status) {
        StatusComponent component = new StatusComponent();
        component.setId(id);
        component.setApp(app);
        component.setName("Comp");
        component.setStatus(status);
        return component;
    }

    private StatusIncident newIncident(UUID id, StatusApp app, String status, boolean isPublic) {
        StatusIncident incident = new StatusIncident();
        incident.setId(id);
        incident.setApp(app);
        incident.setTitle("Incident");
        incident.setStatus(status);
        incident.setSeverity("MINOR");
        incident.setImpact("MINOR");
        incident.setIsPublic(isPublic);
        return incident;
    }

    private StatusMaintenance newMaintenance(UUID id, StatusApp app, String status, boolean isPublic) {
        StatusMaintenance m = new StatusMaintenance();
        m.setId(id);
        m.setApp(app);
        m.setTitle("Window");
        m.setStatus(status);
        m.setStartsAt(ZonedDateTime.now().plusDays(1));
        m.setEndsAt(ZonedDateTime.now().plusDays(1).plusHours(1));
        m.setIsPublic(isPublic);
        return m;
    }

    @Test
    void getAllPublicApps_noTenant_returnsPublicApps() {
        when(statusAppRepository.findByIsPublic(true))
                .thenReturn(List.of(newApp(UUID.randomUUID(), "web", "OPERATIONAL", true)));

        List<StatusAppResponse> result = publicStatusService.getAllPublicApps(null);

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllPublicApps_tenantNotFound_throwsRuntime() {
        when(tenantRepository.findByName("acme")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicStatusService.getAllPublicApps("acme"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getAllPublicApps_withTenant_returnsTenantScopedApps() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        when(tenantRepository.findByName("acme")).thenReturn(Optional.of(tenant));
        when(statusAppRepository.findByTenantIdAndIsPublic(tenantId, true))
                .thenReturn(List.of(newApp(UUID.randomUUID(), "web", "OPERATIONAL", true)));

        List<StatusAppResponse> result = publicStatusService.getAllPublicApps("acme");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAppBySlug_publicApp_returnsResponse() {
        StatusApp app = newApp(UUID.randomUUID(), "web", "OPERATIONAL", true);
        when(statusAppRepository.findBySlug("web")).thenReturn(Optional.of(app));

        StatusAppResponse response = publicStatusService.getAppBySlug("web", null);

        assertThat(response.getSlug()).isEqualTo("web");
    }

    @Test
    void getAppBySlug_notPublic_throwsResourceNotFound() {
        StatusApp app = newApp(UUID.randomUUID(), "web", "OPERATIONAL", false);
        when(statusAppRepository.findBySlug("web")).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> publicStatusService.getAppBySlug("web", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAppBySlug_notFound_throwsRuntime() {
        when(statusAppRepository.findBySlug("web")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicStatusService.getAppBySlug("web", null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getAppComponents_publicApp_returnsComponents() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "web", "OPERATIONAL", true);
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusComponentRepository.findByAppIdOrderByPosition(appId))
                .thenReturn(List.of(newComponent(UUID.randomUUID(), app, "OPERATIONAL"),
                        newComponent(UUID.randomUUID(), app, "DEGRADED")));

        List<StatusComponentResponse> result = publicStatusService.getAppComponents(appId);

        assertThat(result).hasSize(2);
    }

    @Test
    void getAppComponents_notPublic_throwsResourceNotFound() {
        UUID appId = UUID.randomUUID();
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(newApp(appId, "web", "OPERATIONAL", false)));

        assertThatThrownBy(() -> publicStatusService.getAppComponents(appId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCurrentIncidents_returnsOnlyPublicUnresolved() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "web", "DEGRADED", true);
        StatusIncident publicIncident = newIncident(UUID.randomUUID(), app, "INVESTIGATING", true);
        StatusIncident privateIncident = newIncident(UUID.randomUUID(), app, "INVESTIGATING", false);
        when(statusIncidentRepository.findByAppIdAndStatusNot(appId, "RESOLVED"))
                .thenReturn(List.of(publicIncident, privateIncident));

        List<StatusIncidentResponse> result = publicStatusService.getCurrentIncidents(appId);

        assertThat(result).hasSize(1);
    }

    @Test
    void getAppMaintenance_upcoming_returnsOnlyPublic() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "web", "OPERATIONAL", true);
        StatusMaintenance publicM = newMaintenance(UUID.randomUUID(), app, "SCHEDULED", true);
        StatusMaintenance privateM = newMaintenance(UUID.randomUUID(), app, "SCHEDULED", false);
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusMaintenanceRepository.findUpcomingMaintenanceByAppId(eq(appId), any()))
                .thenReturn(List.of(publicM, privateM));

        List<StatusMaintenanceResponse> result = publicStatusService.getAppMaintenance(appId, "upcoming");

        assertThat(result).hasSize(1);
    }

    @Test
    void getStatusSummary_allOperational_overallOperational() {
        StatusApp a1 = newApp(UUID.randomUUID(), "a1", "OPERATIONAL", true);
        StatusApp a2 = newApp(UUID.randomUUID(), "a2", "OPERATIONAL", true);
        when(statusAppRepository.findByIsPublic(true)).thenReturn(List.of(a1, a2));

        StatusSummaryResponse summary = publicStatusService.getStatusSummary(null);

        assertThat(summary.getTotalApps()).isEqualTo(2);
        assertThat(summary.getOperationalApps()).isEqualTo(2);
        assertThat(summary.getAppsWithIssues()).isZero();
        assertThat(summary.getOverallStatus()).isEqualTo("OPERATIONAL");
    }

    @Test
    void getStatusSummary_majorityDown_overallMajorOutage() {
        StatusApp a1 = newApp(UUID.randomUUID(), "a1", "OPERATIONAL", true);
        StatusApp a2 = newApp(UUID.randomUUID(), "a2", "MAJOR_OUTAGE", true);
        when(statusAppRepository.findByIsPublic(true)).thenReturn(List.of(a1, a2));

        StatusSummaryResponse summary = publicStatusService.getStatusSummary(null);

        assertThat(summary.getAppsWithIssues()).isEqualTo(1);
        assertThat(summary.getOverallStatus()).isEqualTo("MAJOR_OUTAGE");
    }

    @Test
    void getStatusSummary_minorityDown_overallDegraded() {
        StatusApp a1 = newApp(UUID.randomUUID(), "a1", "OPERATIONAL", true);
        StatusApp a2 = newApp(UUID.randomUUID(), "a2", "OPERATIONAL", true);
        StatusApp a3 = newApp(UUID.randomUUID(), "a3", "MAJOR_OUTAGE", true);
        when(statusAppRepository.findByIsPublic(true)).thenReturn(List.of(a1, a2, a3));

        StatusSummaryResponse summary = publicStatusService.getStatusSummary(null);

        assertThat(summary.getOverallStatus()).isEqualTo("DEGRADED");
    }

    @Test
    void getAppUptime_publicApp_calculatesUptimeFromIncidents() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "web", "OPERATIONAL", true);
        StatusIncident incident = newIncident(UUID.randomUUID(), app, "RESOLVED", true);
        incident.setStartedAt(ZonedDateTime.now().minusHours(2));
        incident.setResolvedAt(incident.getStartedAt().plusMinutes(60));

        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusIncidentRepository.findRecentPublicIncidentsByAppId(eq(appId), any()))
                .thenReturn(List.of(incident));
        when(statusMaintenanceRepository.findByAppIdAndStatus(appId, "COMPLETED")).thenReturn(List.of());

        UptimeResponse uptime = publicStatusService.getAppUptime(appId, 1);

        assertThat(uptime.getTotalIncidents()).isEqualTo(1);
        assertThat(uptime.getTotalOutageMinutes()).isEqualTo(60);
        assertThat(uptime.getUptimePercentage()).isEqualTo(95.83);
    }

    @Test
    void getAppUptime_notPublic_throwsResourceNotFound() {
        UUID appId = UUID.randomUUID();
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(newApp(appId, "web", "OPERATIONAL", false)));

        assertThatThrownBy(() -> publicStatusService.getAppUptime(appId, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getComponentHistory_publicApp_buildsDailyHistory() {
        UUID componentId = UUID.randomUUID();
        StatusApp app = newApp(UUID.randomUUID(), "web", "OPERATIONAL", true);
        StatusComponent component = newComponent(componentId, app, "OPERATIONAL");
        when(statusComponentRepository.findById(componentId)).thenReturn(Optional.of(component));
        when(statusIncidentComponentRepository.countByComponentId(componentId)).thenReturn(5L);

        ComponentHistoryResponse history = publicStatusService.getComponentHistory(componentId, 7);

        assertThat(history.getComponentId()).isEqualTo(componentId);
        assertThat(history.getTotalIncidents()).isEqualTo(5);
        assertThat(history.getHistory()).hasSize(8);
    }

    @Test
    void getComponentHistory_appNotPublic_throwsResourceNotFound() {
        UUID componentId = UUID.randomUUID();
        StatusApp app = newApp(UUID.randomUUID(), "web", "OPERATIONAL", false);
        when(statusComponentRepository.findById(componentId)).thenReturn(Optional.of(newComponent(componentId, app, "OPERATIONAL")));

        assertThatThrownBy(() -> publicStatusService.getComponentHistory(componentId, 7))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAppUptimeHistory_noRecords_fillsDefaultsAsFullUptime() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "web", "OPERATIONAL", true);
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusUptimeHistoryRepository.findAppUptimeHistory(eq(appId), any(), any())).thenReturn(List.of());

        UptimeHistoryResponse history = publicStatusService.getAppUptimeHistory(appId, 7);

        assertThat(history.getDaysInRange()).isEqualTo(7);
        assertThat(history.getDailyHistory()).hasSize(7);
        assertThat(history.getOverallUptimePercentage().doubleValue()).isEqualTo(100.0);
    }
}
