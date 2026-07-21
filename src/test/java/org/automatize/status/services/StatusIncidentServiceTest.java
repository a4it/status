package org.automatize.status.services;

import org.automatize.status.api.request.StatusIncidentRequest;
import org.automatize.status.api.request.StatusIncidentUpdateRequest;
import org.automatize.status.api.response.StatusIncidentResponse;
import org.automatize.status.api.response.StatusIncidentUpdateResponse;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.models.StatusIncident;
import org.automatize.status.models.StatusIncidentComponent;
import org.automatize.status.models.StatusIncidentUpdate;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.automatize.status.repositories.StatusIncidentComponentRepository;
import org.automatize.status.repositories.StatusIncidentRepository;
import org.automatize.status.repositories.StatusIncidentUpdateRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StatusIncidentService}.
 */
@ExtendWith(MockitoExtension.class)
class StatusIncidentServiceTest {

    @Mock
    private StatusIncidentRepository statusIncidentRepository;
    @Mock
    private StatusIncidentUpdateRepository statusIncidentUpdateRepository;
    @Mock
    private StatusIncidentComponentRepository statusIncidentComponentRepository;
    @Mock
    private StatusAppRepository statusAppRepository;
    @Mock
    private StatusComponentRepository statusComponentRepository;
    @Mock
    private IncidentNotificationService incidentNotificationService;

    @InjectMocks
    private StatusIncidentService statusIncidentService;

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

    private StatusIncident newIncident(UUID id, StatusApp app, String status, String severity) {
        StatusIncident incident = new StatusIncident();
        incident.setId(id);
        incident.setApp(app);
        incident.setTitle("Outage");
        incident.setStatus(status);
        incident.setSeverity(severity);
        incident.setImpact("MINOR");
        incident.setIsPublic(true);
        return incident;
    }

    @Test
    void getIncidentById_existingId_returnsResponse() {
        UUID id = UUID.randomUUID();
        when(statusIncidentRepository.findById(id))
                .thenReturn(Optional.of(newIncident(id, newApp(UUID.randomUUID(), "OPERATIONAL"), "INVESTIGATING", "MINOR")));

        StatusIncidentResponse response = statusIncidentService.getIncidentById(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getStatus()).isEqualTo("INVESTIGATING");
    }

    @Test
    void getIncidentById_notFound_throwsRuntime() {
        UUID id = UUID.randomUUID();
        when(statusIncidentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusIncidentService.getIncidentById(id))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getAllIncidents_noFilters_returnsPageFromFindAll() {
        StatusIncident incident = newIncident(UUID.randomUUID(), newApp(UUID.randomUUID(), "OPERATIONAL"), "INVESTIGATING", "MINOR");
        when(statusIncidentRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(incident)));

        var page = statusIncidentService.getAllIncidents(null, null, null, null, null, pageable);

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void getActiveIncidents_filtersOutResolved() {
        StatusApp app = newApp(UUID.randomUUID(), "OPERATIONAL");
        StatusIncident active = newIncident(UUID.randomUUID(), app, "INVESTIGATING", "MINOR");
        StatusIncident resolved = newIncident(UUID.randomUUID(), app, "RESOLVED", "MINOR");
        when(statusIncidentRepository.findByResolvedAtIsNull()).thenReturn(List.of(active, resolved));

        List<StatusIncidentResponse> result = statusIncidentService.getActiveIncidents(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("INVESTIGATING");
    }

    @Test
    void getIncidentUpdates_returnsMappedUpdates() {
        UUID incidentId = UUID.randomUUID();
        StatusIncidentUpdate u1 = new StatusIncidentUpdate();
        u1.setId(UUID.randomUUID());
        u1.setStatus("INVESTIGATING");
        u1.setMessage("m1");
        when(statusIncidentUpdateRepository.findByIncidentIdOrderByUpdateTimeDesc(incidentId)).thenReturn(List.of(u1));

        List<StatusIncidentUpdateResponse> result = statusIncidentService.getIncidentUpdates(incidentId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessage()).isEqualTo("m1");
    }

    @Test
    void createIncident_happyPath_savesAndNotifies() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "OPERATIONAL");
        StatusIncidentRequest request = new StatusIncidentRequest();
        request.setAppId(appId);
        request.setTitle("Outage");
        request.setStatus("INVESTIGATING");
        request.setSeverity("MINOR");
        request.setImpact("MINOR");

        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusIncidentRepository.save(any(StatusIncident.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentRepository.countActiveIncidentsByAppId(appId)).thenReturn(0L);

        StatusIncidentResponse response = statusIncidentService.createIncident(request);

        assertThat(response.getTitle()).isEqualTo("Outage");
        verify(statusIncidentRepository).save(any(StatusIncident.class));
        verify(incidentNotificationService).notifySubscribersOfNewIncident(any(StatusIncident.class));
    }

    @Test
    void createIncident_withInitialMessageAndComponents_createsUpdateAndLinks() {
        UUID appId = UUID.randomUUID();
        UUID componentId = UUID.randomUUID();
        StatusApp app = newApp(appId, "OPERATIONAL");
        StatusComponent component = new StatusComponent();
        component.setId(componentId);
        component.setApp(app);
        component.setStatus("OPERATIONAL");

        StatusIncidentRequest request = new StatusIncidentRequest();
        request.setAppId(appId);
        request.setTitle("Outage");
        request.setStatus("INVESTIGATING");
        request.setSeverity("MINOR");
        request.setImpact("MINOR");
        request.setInitialMessage("initial");
        request.setAffectedComponentIds(List.of(componentId));

        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusIncidentRepository.save(any(StatusIncident.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentUpdateRepository.save(any(StatusIncidentUpdate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusComponentRepository.findById(componentId)).thenReturn(Optional.of(component));
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentComponentRepository.save(any(StatusIncidentComponent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentRepository.countActiveIncidentsByAppId(appId)).thenReturn(0L);

        statusIncidentService.createIncident(request);

        verify(statusIncidentUpdateRepository).save(any(StatusIncidentUpdate.class));
        verify(statusIncidentComponentRepository).save(any(StatusIncidentComponent.class));
        assertThat(component.getStatus()).isEqualTo("DEGRADED");
    }

    @Test
    void createIncident_appNotFound_throwsRuntime() {
        UUID appId = UUID.randomUUID();
        StatusIncidentRequest request = new StatusIncidentRequest();
        request.setAppId(appId);
        when(statusAppRepository.findById(appId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusIncidentService.createIncident(request))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void updateIncident_happyPath_updatesStatus() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(UUID.randomUUID(), "OPERATIONAL");
        StatusIncident incident = newIncident(id, app, "INVESTIGATING", "MINOR");

        StatusIncidentRequest request = new StatusIncidentRequest();
        request.setTitle("Outage");
        request.setStatus("IDENTIFIED");
        request.setSeverity("MINOR");
        request.setImpact("MINOR");

        when(statusIncidentRepository.findById(id)).thenReturn(Optional.of(incident));
        when(statusIncidentRepository.save(any(StatusIncident.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentRepository.countActiveIncidentsByAppId(app.getId())).thenReturn(0L);

        StatusIncidentResponse response = statusIncidentService.updateIncident(id, request);

        assertThat(response.getStatus()).isEqualTo("IDENTIFIED");
    }

    @Test
    void updateIncident_notFound_throwsRuntime() {
        UUID id = UUID.randomUUID();
        when(statusIncidentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusIncidentService.updateIncident(id, new StatusIncidentRequest()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void addIncidentUpdate_statusChanged_updatesIncident() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(UUID.randomUUID(), "OPERATIONAL");
        StatusIncident incident = newIncident(id, app, "INVESTIGATING", "MINOR");

        StatusIncidentUpdateRequest request = new StatusIncidentUpdateRequest();
        request.setStatus("IDENTIFIED");
        request.setMessage("progress");

        when(statusIncidentRepository.findById(id)).thenReturn(Optional.of(incident));
        when(statusIncidentUpdateRepository.save(any(StatusIncidentUpdate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentRepository.save(any(StatusIncident.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentRepository.countActiveIncidentsByAppId(app.getId())).thenReturn(0L);

        StatusIncidentUpdateResponse response = statusIncidentService.addIncidentUpdate(id, request);

        assertThat(response.getStatus()).isEqualTo("IDENTIFIED");
        assertThat(incident.getStatus()).isEqualTo("IDENTIFIED");
        verify(statusIncidentRepository).save(incident);
    }

    @Test
    void addIncidentUpdate_statusUnchanged_doesNotUpdateIncident() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(UUID.randomUUID(), "OPERATIONAL");
        StatusIncident incident = newIncident(id, app, "INVESTIGATING", "MINOR");

        StatusIncidentUpdateRequest request = new StatusIncidentUpdateRequest();
        request.setStatus("INVESTIGATING");
        request.setMessage("still working");

        when(statusIncidentRepository.findById(id)).thenReturn(Optional.of(incident));
        when(statusIncidentUpdateRepository.save(any(StatusIncidentUpdate.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusIncidentUpdateResponse response = statusIncidentService.addIncidentUpdate(id, request);

        assertThat(response.getMessage()).isEqualTo("still working");
        verify(statusIncidentRepository, never()).save(any());
    }

    @Test
    void resolveIncident_activeIncident_resolvesAndResetsComponents() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(UUID.randomUUID(), "DEGRADED");
        StatusIncident incident = newIncident(id, app, "INVESTIGATING", "MINOR");

        StatusComponent component = new StatusComponent();
        component.setId(UUID.randomUUID());
        component.setApp(app);
        component.setStatus("DEGRADED");
        StatusIncidentComponent ic = new StatusIncidentComponent();
        ic.setIncident(incident);
        ic.setComponent(component);

        when(statusIncidentRepository.findById(id)).thenReturn(Optional.of(incident));
        when(statusIncidentRepository.save(any(StatusIncident.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentUpdateRepository.save(any(StatusIncidentUpdate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentComponentRepository.findByIncidentId(id)).thenReturn(List.of(ic));
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentRepository.countActiveIncidentsByAppId(app.getId())).thenReturn(0L);
        when(statusAppRepository.save(any(StatusApp.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusIncidentResponse response = statusIncidentService.resolveIncident(id, "fixed");

        assertThat(response.getStatus()).isEqualTo("RESOLVED");
        assertThat(response.getResolvedAt()).isNotNull();
        assertThat(component.getStatus()).isEqualTo("OPERATIONAL");
        verify(incidentNotificationService).notifySubscribersOfIncidentResolution(any(StatusIncident.class), any());
    }

    @Test
    void resolveIncident_alreadyResolved_throwsRuntime() {
        UUID id = UUID.randomUUID();
        StatusIncident incident = newIncident(id, newApp(UUID.randomUUID(), "OPERATIONAL"), "RESOLVED", "MINOR");
        when(statusIncidentRepository.findById(id)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> statusIncidentService.resolveIncident(id, null))
                .isInstanceOf(RuntimeException.class);
        verify(statusIncidentRepository, never()).save(any());
    }

    @Test
    void deleteIncident_resolved_deletes() {
        UUID id = UUID.randomUUID();
        StatusIncident incident = newIncident(id, newApp(UUID.randomUUID(), "OPERATIONAL"), "RESOLVED", "MINOR");
        when(statusIncidentRepository.findById(id)).thenReturn(Optional.of(incident));

        statusIncidentService.deleteIncident(id);

        verify(statusIncidentRepository).delete(incident);
    }

    @Test
    void deleteIncident_active_throwsRuntime() {
        UUID id = UUID.randomUUID();
        StatusIncident incident = newIncident(id, newApp(UUID.randomUUID(), "OPERATIONAL"), "INVESTIGATING", "MINOR");
        when(statusIncidentRepository.findById(id)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> statusIncidentService.deleteIncident(id))
                .isInstanceOf(RuntimeException.class);
        verify(statusIncidentRepository, never()).delete(any());
    }

    @Test
    void createAutomatedIncident_nullApp_returnsNull() {
        StatusIncident result = statusIncidentService.createAutomatedIncident(null, "MAJOR", "down");

        assertThat(result).isNull();
    }

    @Test
    void createAutomatedIncident_noExisting_createsNewInvestigating() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "OPERATIONAL");
        when(statusIncidentRepository.findActiveAutomatedIncidents(appId, "system")).thenReturn(List.of());
        when(statusIncidentRepository.save(any(StatusIncident.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentUpdateRepository.save(any(StatusIncidentUpdate.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusIncident result = statusIncidentService.createAutomatedIncident(app, "MAJOR", "down");

        assertThat(result.getStatus()).isEqualTo("INVESTIGATING");
        assertThat(result.getImpact()).isEqualTo("PARTIAL_OUTAGE");
        assertThat(result.getTitle()).contains("Automated");
        verify(incidentNotificationService).notifySubscribersOfNewIncident(any(StatusIncident.class));
    }

    @Test
    void createAutomatedIncident_existingWorseSeverity_upgradesSeverity() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "DEGRADED");
        StatusIncident existing = newIncident(UUID.randomUUID(), app, "INVESTIGATING", "MINOR");
        when(statusIncidentRepository.findActiveAutomatedIncidents(appId, "system")).thenReturn(List.of(existing));
        when(statusIncidentRepository.save(any(StatusIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusIncident result = statusIncidentService.createAutomatedIncident(app, "CRITICAL", "worse");

        assertThat(result.getSeverity()).isEqualTo("CRITICAL");
        assertThat(result.getImpact()).isEqualTo("MAJOR");
        verify(statusIncidentRepository).save(existing);
    }

    @Test
    void createAutomatedIncident_existingNotWorse_doesNotUpgrade() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "MAJOR_OUTAGE");
        StatusIncident existing = newIncident(UUID.randomUUID(), app, "INVESTIGATING", "CRITICAL");
        when(statusIncidentRepository.findActiveAutomatedIncidents(appId, "system")).thenReturn(List.of(existing));

        StatusIncident result = statusIncidentService.createAutomatedIncident(app, "MINOR", "minor");

        assertThat(result.getSeverity()).isEqualTo("CRITICAL");
        verify(statusIncidentRepository, never()).save(any());
    }

    @Test
    void resolveAutomatedIncidents_resolvesAllAndNotifies() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "MAJOR_OUTAGE");
        StatusIncident incident = newIncident(UUID.randomUUID(), app, "INVESTIGATING", "CRITICAL");
        when(statusIncidentRepository.findActiveAutomatedIncidents(appId, "system")).thenReturn(List.of(incident));
        when(statusIncidentRepository.save(any(StatusIncident.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentUpdateRepository.save(any(StatusIncidentUpdate.class))).thenAnswer(inv -> inv.getArgument(0));

        statusIncidentService.resolveAutomatedIncidents(app);

        assertThat(incident.getStatus()).isEqualTo("RESOLVED");
        verify(statusIncidentRepository).save(incident);
        verify(incidentNotificationService).notifySubscribersOfIncidentResolution(any(StatusIncident.class), any());
    }

    @Test
    void resolveAutomatedIncidents_nullApp_noInteraction() {
        statusIncidentService.resolveAutomatedIncidents(null);

        verify(statusIncidentRepository, never()).save(any());
    }
}
