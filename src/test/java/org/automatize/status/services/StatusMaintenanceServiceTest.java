package org.automatize.status.services;

import org.automatize.status.api.request.StatusMaintenanceRequest;
import org.automatize.status.api.response.StatusMaintenanceResponse;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.models.StatusMaintenance;
import org.automatize.status.models.StatusMaintenanceComponent;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.automatize.status.repositories.StatusIncidentRepository;
import org.automatize.status.repositories.StatusMaintenanceComponentRepository;
import org.automatize.status.repositories.StatusMaintenanceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StatusMaintenanceService}.
 */
@ExtendWith(MockitoExtension.class)
class StatusMaintenanceServiceTest {

    @Mock
    private StatusMaintenanceRepository statusMaintenanceRepository;
    @Mock
    private StatusMaintenanceComponentRepository statusMaintenanceComponentRepository;
    @Mock
    private StatusAppRepository statusAppRepository;
    @Mock
    private StatusComponentRepository statusComponentRepository;
    @Mock
    private StatusIncidentRepository statusIncidentRepository;

    @InjectMocks
    private StatusMaintenanceService statusMaintenanceService;

    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private StatusApp newApp(UUID id, String status) {
        StatusApp app = new StatusApp();
        app.setId(id);
        app.setName("App");
        app.setStatus(status);
        return app;
    }

    private StatusMaintenance newMaintenance(UUID id, StatusApp app, String status) {
        StatusMaintenance m = new StatusMaintenance();
        m.setId(id);
        m.setApp(app);
        m.setTitle("Window");
        m.setStatus(status);
        m.setStartsAt(ZonedDateTime.now().plusDays(1));
        m.setEndsAt(ZonedDateTime.now().plusDays(1).plusHours(2));
        m.setIsPublic(true);
        return m;
    }

    private StatusMaintenanceRequest validRequest(UUID appId) {
        StatusMaintenanceRequest request = new StatusMaintenanceRequest();
        request.setAppId(appId);
        request.setTitle("Window");
        request.setStartsAt(ZonedDateTime.now().plusDays(1));
        request.setEndsAt(ZonedDateTime.now().plusDays(1).plusHours(2));
        return request;
    }

    @Test
    void getMaintenanceById_existingId_returnsResponse() {
        UUID id = UUID.randomUUID();
        when(statusMaintenanceRepository.findById(id))
                .thenReturn(Optional.of(newMaintenance(id, newApp(UUID.randomUUID(), "OPERATIONAL"), "SCHEDULED")));

        StatusMaintenanceResponse response = statusMaintenanceService.getMaintenanceById(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getStatus()).isEqualTo("SCHEDULED");
    }

    @Test
    void getMaintenanceById_notFound_throwsRuntime() {
        UUID id = UUID.randomUUID();
        when(statusMaintenanceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusMaintenanceService.getMaintenanceById(id))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getAllMaintenance_noFilters_returnsPageFromFindAll() {
        StatusMaintenance m = newMaintenance(UUID.randomUUID(), newApp(UUID.randomUUID(), "OPERATIONAL"), "SCHEDULED");
        when(statusMaintenanceRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(m)));

        var page = statusMaintenanceService.getAllMaintenance(null, null, null, null, null, pageable);

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void createMaintenance_validWindow_savesScheduled() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "OPERATIONAL");
        StatusMaintenanceRequest request = validRequest(appId);

        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusMaintenanceRepository.save(any(StatusMaintenance.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusMaintenanceResponse response = statusMaintenanceService.createMaintenance(request);

        assertThat(response.getStatus()).isEqualTo("SCHEDULED");
    }

    @Test
    void createMaintenance_startAfterEnd_throwsRuntime() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "OPERATIONAL");
        StatusMaintenanceRequest request = validRequest(appId);
        request.setStartsAt(ZonedDateTime.now().plusDays(2));
        request.setEndsAt(ZonedDateTime.now().plusDays(1));

        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> statusMaintenanceService.createMaintenance(request))
                .isInstanceOf(RuntimeException.class);
        verify(statusMaintenanceRepository, never()).save(any());
    }

    @Test
    void createMaintenance_appNotFound_throwsRuntime() {
        UUID appId = UUID.randomUUID();
        when(statusAppRepository.findById(appId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusMaintenanceService.createMaintenance(validRequest(appId)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void createMaintenance_withComponents_linksComponents() {
        UUID appId = UUID.randomUUID();
        UUID componentId = UUID.randomUUID();
        StatusApp app = newApp(appId, "OPERATIONAL");
        StatusComponent component = new StatusComponent();
        component.setId(componentId);
        component.setApp(app);

        StatusMaintenanceRequest request = validRequest(appId);
        request.setAffectedComponentIds(List.of(componentId));

        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusMaintenanceRepository.save(any(StatusMaintenance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusComponentRepository.findById(componentId)).thenReturn(Optional.of(component));
        when(statusMaintenanceComponentRepository.save(any(StatusMaintenanceComponent.class))).thenAnswer(inv -> inv.getArgument(0));

        statusMaintenanceService.createMaintenance(request);

        verify(statusMaintenanceComponentRepository).save(any(StatusMaintenanceComponent.class));
    }

    @Test
    void updateMaintenance_scheduled_updates() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(UUID.randomUUID(), "OPERATIONAL");
        StatusMaintenance maintenance = newMaintenance(id, app, "SCHEDULED");
        StatusMaintenanceRequest request = validRequest(null);
        request.setTitle("Updated");

        when(statusMaintenanceRepository.findById(id)).thenReturn(Optional.of(maintenance));
        when(statusMaintenanceRepository.save(any(StatusMaintenance.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusMaintenanceResponse response = statusMaintenanceService.updateMaintenance(id, request);

        assertThat(response.getTitle()).isEqualTo("Updated");
    }

    @Test
    void updateMaintenance_completed_throwsRuntime() {
        UUID id = UUID.randomUUID();
        StatusMaintenance maintenance = newMaintenance(id, newApp(UUID.randomUUID(), "OPERATIONAL"), "COMPLETED");
        when(statusMaintenanceRepository.findById(id)).thenReturn(Optional.of(maintenance));

        assertThatThrownBy(() -> statusMaintenanceService.updateMaintenance(id, validRequest(null)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void updateMaintenance_startAfterEnd_throwsRuntime() {
        UUID id = UUID.randomUUID();
        StatusMaintenance maintenance = newMaintenance(id, newApp(UUID.randomUUID(), "OPERATIONAL"), "SCHEDULED");
        StatusMaintenanceRequest request = validRequest(null);
        request.setStartsAt(ZonedDateTime.now().plusDays(2));
        request.setEndsAt(ZonedDateTime.now().plusDays(1));

        when(statusMaintenanceRepository.findById(id)).thenReturn(Optional.of(maintenance));

        assertThatThrownBy(() -> statusMaintenanceService.updateMaintenance(id, request))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void updateMaintenance_notFound_throwsRuntime() {
        UUID id = UUID.randomUUID();
        when(statusMaintenanceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusMaintenanceService.updateMaintenance(id, validRequest(null)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void updateStatus_updatesAndSaves() {
        UUID id = UUID.randomUUID();
        StatusMaintenance maintenance = newMaintenance(id, newApp(UUID.randomUUID(), "OPERATIONAL"), "SCHEDULED");
        when(statusMaintenanceRepository.findById(id)).thenReturn(Optional.of(maintenance));
        when(statusMaintenanceRepository.save(any(StatusMaintenance.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusMaintenanceResponse response = statusMaintenanceService.updateStatus(id, "CANCELLED");

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void startMaintenance_scheduled_setsInProgressAndUpdatesComponents() {
        UUID id = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "OPERATIONAL");
        StatusMaintenance maintenance = newMaintenance(id, app, "SCHEDULED");

        StatusComponent component = new StatusComponent();
        component.setId(UUID.randomUUID());
        component.setApp(app);
        component.setStatus("OPERATIONAL");
        StatusMaintenanceComponent mc = new StatusMaintenanceComponent();
        mc.setMaintenance(maintenance);
        mc.setComponent(component);

        when(statusMaintenanceRepository.findById(id)).thenReturn(Optional.of(maintenance));
        when(statusMaintenanceRepository.save(any(StatusMaintenance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusMaintenanceComponentRepository.findByMaintenanceId(id)).thenReturn(List.of(mc));
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusMaintenanceRepository.countActiveMaintenanceByAppId(appId)).thenReturn(1L);
        when(statusAppRepository.save(any(StatusApp.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusMaintenanceResponse response = statusMaintenanceService.startMaintenance(id);

        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(component.getStatus()).isEqualTo("UNDER_MAINTENANCE");
        assertThat(app.getStatus()).isEqualTo("UNDER_MAINTENANCE");
    }

    @Test
    void startMaintenance_notScheduled_throwsRuntime() {
        UUID id = UUID.randomUUID();
        StatusMaintenance maintenance = newMaintenance(id, newApp(UUID.randomUUID(), "OPERATIONAL"), "IN_PROGRESS");
        when(statusMaintenanceRepository.findById(id)).thenReturn(Optional.of(maintenance));

        assertThatThrownBy(() -> statusMaintenanceService.startMaintenance(id))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void completeMaintenance_inProgress_setsCompletedAndResetsComponents() {
        UUID id = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "UNDER_MAINTENANCE");
        StatusMaintenance maintenance = newMaintenance(id, app, "IN_PROGRESS");

        StatusComponent component = new StatusComponent();
        component.setId(UUID.randomUUID());
        component.setApp(app);
        component.setStatus("UNDER_MAINTENANCE");
        StatusMaintenanceComponent mc = new StatusMaintenanceComponent();
        mc.setMaintenance(maintenance);
        mc.setComponent(component);

        when(statusMaintenanceRepository.findById(id)).thenReturn(Optional.of(maintenance));
        when(statusMaintenanceRepository.save(any(StatusMaintenance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusMaintenanceComponentRepository.findByMaintenanceId(id)).thenReturn(List.of(mc));
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusMaintenanceRepository.countActiveMaintenanceByAppId(appId)).thenReturn(0L);
        when(statusIncidentRepository.countActiveIncidentsByAppId(appId)).thenReturn(0L);
        when(statusAppRepository.save(any(StatusApp.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusMaintenanceResponse response = statusMaintenanceService.completeMaintenance(id);

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(component.getStatus()).isEqualTo("OPERATIONAL");
        assertThat(app.getStatus()).isEqualTo("OPERATIONAL");
    }

    @Test
    void completeMaintenance_notInProgress_throwsRuntime() {
        UUID id = UUID.randomUUID();
        StatusMaintenance maintenance = newMaintenance(id, newApp(UUID.randomUUID(), "OPERATIONAL"), "SCHEDULED");
        when(statusMaintenanceRepository.findById(id)).thenReturn(Optional.of(maintenance));

        assertThatThrownBy(() -> statusMaintenanceService.completeMaintenance(id))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void deleteMaintenance_scheduled_deletes() {
        UUID id = UUID.randomUUID();
        StatusMaintenance maintenance = newMaintenance(id, newApp(UUID.randomUUID(), "OPERATIONAL"), "SCHEDULED");
        when(statusMaintenanceRepository.findById(id)).thenReturn(Optional.of(maintenance));

        statusMaintenanceService.deleteMaintenance(id);

        verify(statusMaintenanceRepository).delete(maintenance);
    }

    @Test
    void deleteMaintenance_inProgress_throwsRuntime() {
        UUID id = UUID.randomUUID();
        StatusMaintenance maintenance = newMaintenance(id, newApp(UUID.randomUUID(), "OPERATIONAL"), "IN_PROGRESS");
        when(statusMaintenanceRepository.findById(id)).thenReturn(Optional.of(maintenance));

        assertThatThrownBy(() -> statusMaintenanceService.deleteMaintenance(id))
                .isInstanceOf(RuntimeException.class);
        verify(statusMaintenanceRepository, never()).delete(any());
    }

    @Test
    void getUpcomingMaintenance_byApp_returnsOnlyScheduled() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "OPERATIONAL");
        StatusMaintenance scheduled = newMaintenance(UUID.randomUUID(), app, "SCHEDULED");
        StatusMaintenance inProgress = newMaintenance(UUID.randomUUID(), app, "IN_PROGRESS");
        when(statusMaintenanceRepository.findUpcomingMaintenanceByAppId(eq(appId), any()))
                .thenReturn(List.of(scheduled, inProgress));

        List<StatusMaintenanceResponse> result = statusMaintenanceService.getUpcomingMaintenance(appId, null, 7);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("SCHEDULED");
    }

    @Test
    void getActiveMaintenance_byApp_filtersByApp() {
        UUID appId = UUID.randomUUID();
        StatusApp matching = newApp(appId, "OPERATIONAL");
        StatusApp other = newApp(UUID.randomUUID(), "OPERATIONAL");
        StatusMaintenance m1 = newMaintenance(UUID.randomUUID(), matching, "IN_PROGRESS");
        StatusMaintenance m2 = newMaintenance(UUID.randomUUID(), other, "IN_PROGRESS");
        when(statusMaintenanceRepository.findActiveMaintenance(any())).thenReturn(List.of(m1, m2));

        List<StatusMaintenanceResponse> result = statusMaintenanceService.getActiveMaintenance(appId, null);

        assertThat(result).hasSize(1);
    }
}
