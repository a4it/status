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
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
class StatusIncidentServiceTest extends AbstractServiceTest {

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

    private static final String IMPACT_MAJOR = "MAJOR";
    private static final String MINOR = "MINOR";
    private static final String STATUS_INVESTIGATING = "INVESTIGATING";
    private static final String STATUS_OPERATIONAL = "OPERATIONAL";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String STATUS_DEGRADED = "DEGRADED";
    private static final String STATUS_IDENTIFIED = "IDENTIFIED";
    private static final String SEVERITY_CRITICAL = "CRITICAL";
    private static final String TITLE_OUTAGE = "Outage";
    private static final String SOURCE_SYSTEM = "system";

    private final Pageable pageable = PageRequest.of(0, 10);

    /**
     * Builds a minimal {@link StatusIncident} fixture linked to the given app.
     *
     * @param id       the identifier to assign to the incident
     * @param app      the owning {@link StatusApp}
     * @param status   the incident status
     * @param severity the incident severity
     * @return a populated {@link StatusIncident} instance
     */
    private StatusIncident newIncident(UUID id, StatusApp app, String status, String severity) {
        StatusIncident incident = new StatusIncident();
        incident.setId(id);
        incident.setApp(app);
        incident.setTitle(TITLE_OUTAGE);
        incident.setStatus(status);
        incident.setSeverity(severity);
        incident.setImpact(MINOR);
        incident.setIsPublic(true);
        return incident;
    }

    /**
     * Verifies that requesting an existing incident by id returns a response mapping its id and status.
     */
    @Test
    void getIncidentById_existingId_returnsResponse() {
        UUID id = UUID.randomUUID();
        when(statusIncidentRepository.findById(id))
                .thenReturn(Optional.of(newIncident(id, newApp(UUID.randomUUID(), STATUS_OPERATIONAL), STATUS_INVESTIGATING, MINOR)));

        StatusIncidentResponse response = statusIncidentService.getIncidentById(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getStatus()).isEqualTo(STATUS_INVESTIGATING);
    }

    /**
     * Verifies that requesting an incident whose id does not exist throws a {@link RuntimeException}.
     */
    @Test
    void getIncidentById_notFound_throwsRuntime() {
        UUID id = UUID.randomUUID();
        when(statusIncidentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusIncidentService.getIncidentById(id))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * Verifies that requesting all incidents without filters delegates to the repository's paged findAll
     * and returns the resulting page.
     */
    @Test
    void getAllIncidents_noFilters_returnsPageFromFindAll() {
        StatusIncident incident = newIncident(UUID.randomUUID(), newApp(UUID.randomUUID(), STATUS_OPERATIONAL), STATUS_INVESTIGATING, MINOR);
        when(statusIncidentRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(incident)));

        var page = statusIncidentService.getAllIncidents(null, null, null, null, null, pageable);

        assertThat(page.getContent()).hasSize(1);
    }

    /**
     * Verifies that fetching active incidents excludes those in the RESOLVED status.
     */
    @Test
    void getActiveIncidents_filtersOutResolved() {
        StatusApp app = newApp(UUID.randomUUID(), STATUS_OPERATIONAL);
        StatusIncident active = newIncident(UUID.randomUUID(), app, STATUS_INVESTIGATING, MINOR);
        StatusIncident resolved = newIncident(UUID.randomUUID(), app, STATUS_RESOLVED, MINOR);
        when(statusIncidentRepository.findByResolvedAtIsNull()).thenReturn(List.of(active, resolved));

        List<StatusIncidentResponse> result = statusIncidentService.getActiveIncidents(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(STATUS_INVESTIGATING);
    }

    /**
     * Verifies that fetching an incident's updates returns them mapped into response objects.
     */
    @Test
    void getIncidentUpdates_returnsMappedUpdates() {
        UUID incidentId = UUID.randomUUID();
        StatusIncidentUpdate u1 = new StatusIncidentUpdate();
        u1.setId(UUID.randomUUID());
        u1.setStatus(STATUS_INVESTIGATING);
        u1.setMessage("m1");
        when(statusIncidentUpdateRepository.findByIncidentIdOrderByUpdateTimeDesc(incidentId)).thenReturn(List.of(u1));

        List<StatusIncidentUpdateResponse> result = statusIncidentService.getIncidentUpdates(incidentId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessage()).isEqualTo("m1");
    }

    /**
     * Verifies that creating an incident persists it and notifies subscribers of the new incident.
     */
    @Test
    void createIncident_happyPath_savesAndNotifies() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, STATUS_OPERATIONAL);
        StatusIncidentRequest request = new StatusIncidentRequest();
        request.setAppId(appId);
        request.setTitle(TITLE_OUTAGE);
        request.setStatus(STATUS_INVESTIGATING);
        request.setSeverity(MINOR);
        request.setImpact(MINOR);

        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusIncidentRepository.save(any(StatusIncident.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentRepository.countActiveIncidentsByAppId(appId)).thenReturn(0L);

        StatusIncidentResponse response = statusIncidentService.createIncident(request);

        assertThat(response.getTitle()).isEqualTo(TITLE_OUTAGE);
        verify(statusIncidentRepository).save(any(StatusIncident.class));
        verify(incidentNotificationService).notifySubscribersOfNewIncident(any(StatusIncident.class));
    }

    /**
     * Verifies that creating an incident with an initial message and affected components creates an
     * incident update, links the components and degrades the affected component's status.
     */
    @Test
    void createIncident_withInitialMessageAndComponents_createsUpdateAndLinks() {
        UUID appId = UUID.randomUUID();
        UUID componentId = UUID.randomUUID();
        StatusApp app = newApp(appId, STATUS_OPERATIONAL);
        StatusComponent component = new StatusComponent();
        component.setId(componentId);
        component.setApp(app);
        component.setStatus(STATUS_OPERATIONAL);

        StatusIncidentRequest request = new StatusIncidentRequest();
        request.setAppId(appId);
        request.setTitle(TITLE_OUTAGE);
        request.setStatus(STATUS_INVESTIGATING);
        request.setSeverity(MINOR);
        request.setImpact(MINOR);
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
        assertThat(component.getStatus()).isEqualTo(STATUS_DEGRADED);
    }

    /**
     * Verifies that creating an incident for a non-existent app throws a {@link RuntimeException}.
     */
    @Test
    void createIncident_appNotFound_throwsRuntime() {
        UUID appId = UUID.randomUUID();
        StatusIncidentRequest request = new StatusIncidentRequest();
        request.setAppId(appId);
        when(statusAppRepository.findById(appId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusIncidentService.createIncident(request))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * Verifies that updating an incident applies the new status (e.g. from INVESTIGATING to IDENTIFIED).
     */
    @Test
    void updateIncident_happyPath_updatesStatus() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(UUID.randomUUID(), STATUS_OPERATIONAL);
        StatusIncident incident = newIncident(id, app, STATUS_INVESTIGATING, MINOR);

        StatusIncidentRequest request = new StatusIncidentRequest();
        request.setTitle(TITLE_OUTAGE);
        request.setStatus(STATUS_IDENTIFIED);
        request.setSeverity(MINOR);
        request.setImpact(MINOR);

        when(statusIncidentRepository.findById(id)).thenReturn(Optional.of(incident));
        when(statusIncidentRepository.save(any(StatusIncident.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentRepository.countActiveIncidentsByAppId(app.getId())).thenReturn(0L);

        StatusIncidentResponse response = statusIncidentService.updateIncident(id, request);

        assertThat(response.getStatus()).isEqualTo(STATUS_IDENTIFIED);
    }

    /**
     * Verifies that updating an incident whose id does not exist throws a {@link RuntimeException}.
     */
    @Test
    void updateIncident_notFound_throwsRuntime() {
        UUID id = UUID.randomUUID();
        when(statusIncidentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusIncidentService.updateIncident(id, new StatusIncidentRequest()))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * Verifies that adding an update carrying a new status persists the update and propagates the new
     * status onto the incident.
     */
    @Test
    void addIncidentUpdate_statusChanged_updatesIncident() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(UUID.randomUUID(), STATUS_OPERATIONAL);
        StatusIncident incident = newIncident(id, app, STATUS_INVESTIGATING, MINOR);

        StatusIncidentUpdateRequest request = new StatusIncidentUpdateRequest();
        request.setStatus(STATUS_IDENTIFIED);
        request.setMessage("progress");

        when(statusIncidentRepository.findById(id)).thenReturn(Optional.of(incident));
        when(statusIncidentUpdateRepository.save(any(StatusIncidentUpdate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentRepository.save(any(StatusIncident.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentRepository.countActiveIncidentsByAppId(app.getId())).thenReturn(0L);

        StatusIncidentUpdateResponse response = statusIncidentService.addIncidentUpdate(id, request);

        assertThat(response.getStatus()).isEqualTo(STATUS_IDENTIFIED);
        assertThat(incident.getStatus()).isEqualTo(STATUS_IDENTIFIED);
        verify(statusIncidentRepository).save(incident);
    }

    /**
     * Verifies that adding an update whose status matches the current incident status persists the update
     * but does not re-save the incident.
     */
    @Test
    void addIncidentUpdate_statusUnchanged_doesNotUpdateIncident() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(UUID.randomUUID(), STATUS_OPERATIONAL);
        StatusIncident incident = newIncident(id, app, STATUS_INVESTIGATING, MINOR);

        StatusIncidentUpdateRequest request = new StatusIncidentUpdateRequest();
        request.setStatus(STATUS_INVESTIGATING);
        request.setMessage("still working");

        when(statusIncidentRepository.findById(id)).thenReturn(Optional.of(incident));
        when(statusIncidentUpdateRepository.save(any(StatusIncidentUpdate.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusIncidentUpdateResponse response = statusIncidentService.addIncidentUpdate(id, request);

        assertThat(response.getMessage()).isEqualTo("still working");
        verify(statusIncidentRepository, never()).save(any());
    }

    /**
     * Verifies that resolving an active incident sets its status to RESOLVED, stamps the resolved time,
     * resets affected components back to OPERATIONAL and notifies subscribers of the resolution.
     */
    @Test
    void resolveIncident_activeIncident_resolvesAndResetsComponents() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(UUID.randomUUID(), STATUS_DEGRADED);
        StatusIncident incident = newIncident(id, app, STATUS_INVESTIGATING, MINOR);

        StatusComponent component = new StatusComponent();
        component.setId(UUID.randomUUID());
        component.setApp(app);
        component.setStatus(STATUS_DEGRADED);
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

        assertThat(response.getStatus()).isEqualTo(STATUS_RESOLVED);
        assertThat(response.getResolvedAt()).isNotNull();
        assertThat(component.getStatus()).isEqualTo(STATUS_OPERATIONAL);
        verify(incidentNotificationService).notifySubscribersOfIncidentResolution(any(StatusIncident.class), any());
    }

    /**
     * Verifies that attempting to resolve an already-resolved incident throws a {@link RuntimeException}
     * and never persists changes.
     */
    @Test
    void resolveIncident_alreadyResolved_throwsRuntime() {
        UUID id = UUID.randomUUID();
        StatusIncident incident = newIncident(id, newApp(UUID.randomUUID(), STATUS_OPERATIONAL), STATUS_RESOLVED, MINOR);
        when(statusIncidentRepository.findById(id)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> statusIncidentService.resolveIncident(id, null))
                .isInstanceOf(RuntimeException.class);
        verify(statusIncidentRepository, never()).save(any());
    }

    /**
     * Verifies that a resolved incident can be deleted.
     */
    @Test
    void deleteIncident_resolved_deletes() {
        UUID id = UUID.randomUUID();
        StatusIncident incident = newIncident(id, newApp(UUID.randomUUID(), STATUS_OPERATIONAL), STATUS_RESOLVED, MINOR);
        when(statusIncidentRepository.findById(id)).thenReturn(Optional.of(incident));

        statusIncidentService.deleteIncident(id);

        verify(statusIncidentRepository).delete(incident);
    }

    /**
     * Verifies that attempting to delete an active (unresolved) incident throws a {@link RuntimeException}
     * and never deletes it.
     */
    @Test
    void deleteIncident_active_throwsRuntime() {
        UUID id = UUID.randomUUID();
        StatusIncident incident = newIncident(id, newApp(UUID.randomUUID(), STATUS_OPERATIONAL), STATUS_INVESTIGATING, MINOR);
        when(statusIncidentRepository.findById(id)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> statusIncidentService.deleteIncident(id))
                .isInstanceOf(RuntimeException.class);
        verify(statusIncidentRepository, never()).delete(any());
    }

    /**
     * Verifies that creating an automated incident with a null app returns null (no incident created).
     */
    @Test
    void createAutomatedIncident_nullApp_returnsNull() {
        StatusIncident result = statusIncidentService.createAutomatedIncident(null, IMPACT_MAJOR, "down");

        assertThat(result).isNull();
    }

    /**
     * Verifies that when no automated incident exists a new one is created in the INVESTIGATING status
     * with the mapped impact and an "Automated" title, and subscribers are notified.
     */
    @Test
    void createAutomatedIncident_noExisting_createsNewInvestigating() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, STATUS_OPERATIONAL);
        when(statusIncidentRepository.findActiveAutomatedIncidents(appId, SOURCE_SYSTEM)).thenReturn(List.of());
        when(statusIncidentRepository.save(any(StatusIncident.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentUpdateRepository.save(any(StatusIncidentUpdate.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusIncident result = statusIncidentService.createAutomatedIncident(app, IMPACT_MAJOR, "down");

        assertThat(result.getStatus()).isEqualTo(STATUS_INVESTIGATING);
        assertThat(result.getImpact()).isEqualTo("PARTIAL_OUTAGE");
        assertThat(result.getTitle()).contains("Automated");
        verify(incidentNotificationService).notifySubscribersOfNewIncident(any(StatusIncident.class));
    }

    /**
     * Verifies that an existing automated incident is upgraded (severity and impact raised) when the new
     * report carries a worse severity, and the existing incident is saved.
     */
    @Test
    void createAutomatedIncident_existingWorseSeverity_upgradesSeverity() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, STATUS_DEGRADED);
        StatusIncident existing = newIncident(UUID.randomUUID(), app, STATUS_INVESTIGATING, MINOR);
        when(statusIncidentRepository.findActiveAutomatedIncidents(appId, SOURCE_SYSTEM)).thenReturn(List.of(existing));
        when(statusIncidentRepository.save(any(StatusIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusIncident result = statusIncidentService.createAutomatedIncident(app, SEVERITY_CRITICAL, "worse");

        assertThat(result.getSeverity()).isEqualTo(SEVERITY_CRITICAL);
        assertThat(result.getImpact()).isEqualTo(IMPACT_MAJOR);
        verify(statusIncidentRepository).save(existing);
    }

    /**
     * Verifies that an existing automated incident is left unchanged (not saved) when the new report's
     * severity is not worse than the current one.
     */
    @Test
    void createAutomatedIncident_existingNotWorse_doesNotUpgrade() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "MAJOR_OUTAGE");
        StatusIncident existing = newIncident(UUID.randomUUID(), app, STATUS_INVESTIGATING, SEVERITY_CRITICAL);
        when(statusIncidentRepository.findActiveAutomatedIncidents(appId, SOURCE_SYSTEM)).thenReturn(List.of(existing));

        StatusIncident result = statusIncidentService.createAutomatedIncident(app, MINOR, "minor");

        assertThat(result.getSeverity()).isEqualTo(SEVERITY_CRITICAL);
        verify(statusIncidentRepository, never()).save(any());
    }

    /**
     * Verifies that resolving automated incidents for an app resolves each active automated incident
     * and notifies subscribers of the resolution.
     */
    @Test
    void resolveAutomatedIncidents_resolvesAllAndNotifies() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "MAJOR_OUTAGE");
        StatusIncident incident = newIncident(UUID.randomUUID(), app, STATUS_INVESTIGATING, SEVERITY_CRITICAL);
        when(statusIncidentRepository.findActiveAutomatedIncidents(appId, SOURCE_SYSTEM)).thenReturn(List.of(incident));
        when(statusIncidentRepository.save(any(StatusIncident.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusIncidentUpdateRepository.save(any(StatusIncidentUpdate.class))).thenAnswer(inv -> inv.getArgument(0));

        statusIncidentService.resolveAutomatedIncidents(app);

        assertThat(incident.getStatus()).isEqualTo(STATUS_RESOLVED);
        verify(statusIncidentRepository).save(incident);
        verify(incidentNotificationService).notifySubscribersOfIncidentResolution(any(StatusIncident.class), any());
    }

    /**
     * Verifies that resolving automated incidents with a null app performs no repository interaction.
     */
    @Test
    void resolveAutomatedIncidents_nullApp_noInteraction() {
        statusIncidentService.resolveAutomatedIncidents(null);

        verify(statusIncidentRepository, never()).save(any());
    }
}
