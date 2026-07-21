package org.automatize.status.services;

import org.automatize.status.api.request.StatusComponentRequest;
import org.automatize.status.api.response.StatusComponentResponse;
import org.automatize.status.controllers.api.ComponentOrderRequest;
import org.automatize.status.exceptions.BusinessRuleException;
import org.automatize.status.exceptions.DuplicateResourceException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.automatize.status.repositories.StatusIncidentComponentRepository;
import org.automatize.status.repositories.StatusMaintenanceComponentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StatusComponentService}.
 */
@ExtendWith(MockitoExtension.class)
class StatusComponentServiceTest {

    @Mock
    private StatusComponentRepository statusComponentRepository;
    @Mock
    private StatusAppRepository statusAppRepository;
    @Mock
    private StatusIncidentComponentRepository statusIncidentComponentRepository;
    @Mock
    private StatusMaintenanceComponentRepository statusMaintenanceComponentRepository;

    @InjectMocks
    private StatusComponentService statusComponentService;

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

    private StatusComponent newComponent(UUID id, StatusApp app, String name, String status) {
        StatusComponent component = new StatusComponent();
        component.setId(id);
        component.setApp(app);
        component.setName(name);
        component.setStatus(status);
        return component;
    }

    @Test
    void getComponentById_existingId_returnsResponse() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(UUID.randomUUID(), "OPERATIONAL");
        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(newComponent(id, app, "API", "OPERATIONAL")));

        StatusComponentResponse response = statusComponentService.getComponentById(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getName()).isEqualTo("API");
    }

    @Test
    void getComponentById_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(statusComponentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusComponentService.getComponentById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createComponent_uniqueName_savesAndAssignsPosition() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "OPERATIONAL");
        StatusComponentRequest request = new StatusComponentRequest();
        request.setAppId(appId);
        request.setName("Database");

        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusComponentRepository.existsByAppIdAndName(appId, "Database")).thenReturn(false);
        when(statusComponentRepository.countByAppId(appId)).thenReturn(2L);
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusComponentResponse response = statusComponentService.createComponent(request);

        assertThat(response.getName()).isEqualTo("Database");
        assertThat(response.getPosition()).isEqualTo(3);
        assertThat(response.getApiKey()).isNotBlank();
    }

    @Test
    void createComponent_appNotFound_throwsResourceNotFound() {
        UUID appId = UUID.randomUUID();
        StatusComponentRequest request = new StatusComponentRequest();
        request.setAppId(appId);
        request.setName("X");
        when(statusAppRepository.findById(appId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusComponentService.createComponent(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createComponent_duplicateName_throwsDuplicateResource() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "OPERATIONAL");
        StatusComponentRequest request = new StatusComponentRequest();
        request.setAppId(appId);
        request.setName("Dup");
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusComponentRepository.existsByAppIdAndName(appId, "Dup")).thenReturn(true);

        assertThatThrownBy(() -> statusComponentService.createComponent(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(statusComponentRepository, never()).save(any());
    }

    @Test
    void updateComponent_sameName_updatesAndSaves() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(UUID.randomUUID(), "OPERATIONAL");
        StatusComponent component = newComponent(id, app, "API", "OPERATIONAL");
        component.setApiKey("existing");

        StatusComponentRequest request = new StatusComponentRequest();
        request.setName("API");
        request.setDescription("updated");

        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(component));
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusComponentResponse response = statusComponentService.updateComponent(id, request);

        assertThat(response.getDescription()).isEqualTo("updated");
        verify(statusComponentRepository).save(component);
    }

    @Test
    void updateComponent_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(statusComponentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusComponentService.updateComponent(id, new StatusComponentRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateComponent_renameToExistingName_throwsDuplicateResource() {
        UUID id = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "OPERATIONAL");
        StatusComponent component = newComponent(id, app, "old", "OPERATIONAL");

        StatusComponentRequest request = new StatusComponentRequest();
        request.setName("new");

        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(component));
        when(statusComponentRepository.existsByAppIdAndName(appId, "new")).thenReturn(true);

        assertThatThrownBy(() -> statusComponentService.updateComponent(id, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateStatus_componentMajorOutage_setsAppMajorOutage() {
        UUID id = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "OPERATIONAL");
        StatusComponent component = newComponent(id, app, "API", "OPERATIONAL");

        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(component));
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));
        StatusComponent outage = newComponent(UUID.randomUUID(), app, "A", "MAJOR_OUTAGE");
        when(statusComponentRepository.findByAppId(appId)).thenReturn(List.of(outage));
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusAppRepository.save(any(StatusApp.class))).thenAnswer(inv -> inv.getArgument(0));

        statusComponentService.updateStatus(id, "MAJOR_OUTAGE");

        assertThat(app.getStatus()).isEqualTo("MAJOR_OUTAGE");
        verify(statusAppRepository).save(app);
    }

    @Test
    void updateStatus_componentDegraded_setsAppDegraded() {
        UUID id = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "OPERATIONAL");
        StatusComponent component = newComponent(id, app, "API", "OPERATIONAL");

        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(component));
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));
        StatusComponent degraded = newComponent(UUID.randomUUID(), app, "A", "PARTIAL_OUTAGE");
        when(statusComponentRepository.findByAppId(appId)).thenReturn(List.of(degraded));
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusAppRepository.save(any(StatusApp.class))).thenAnswer(inv -> inv.getArgument(0));

        statusComponentService.updateStatus(id, "PARTIAL_OUTAGE");

        assertThat(app.getStatus()).isEqualTo("DEGRADED");
        verify(statusAppRepository).save(app);
    }

    @Test
    void updateStatus_allOperational_setsAppOperational() {
        UUID id = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "DEGRADED");
        StatusComponent component = newComponent(id, app, "API", "DEGRADED");

        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(component));
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));
        StatusComponent operational = newComponent(UUID.randomUUID(), app, "A", "OPERATIONAL");
        when(statusComponentRepository.findByAppId(appId)).thenReturn(List.of(operational));
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusAppRepository.save(any(StatusApp.class))).thenAnswer(inv -> inv.getArgument(0));

        statusComponentService.updateStatus(id, "OPERATIONAL");

        assertThat(app.getStatus()).isEqualTo("OPERATIONAL");
        verify(statusAppRepository).save(app);
    }

    @Test
    void updateStatus_noComponents_leavesAppUntouched() {
        UUID id = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "OPERATIONAL");
        StatusComponent component = newComponent(id, app, "API", "OPERATIONAL");

        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(component));
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusComponentRepository.findByAppId(appId)).thenReturn(List.of());

        statusComponentService.updateStatus(id, "OPERATIONAL");

        verify(statusAppRepository, never()).save(any());
    }

    @Test
    void deleteComponent_noIncidentsOrMaintenance_deletes() {
        UUID id = UUID.randomUUID();
        StatusComponent component = newComponent(id, newApp(UUID.randomUUID(), "OPERATIONAL"), "API", "OPERATIONAL");
        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(component));
        when(statusIncidentComponentRepository.countByComponentId(id)).thenReturn(0L);
        when(statusMaintenanceComponentRepository.countByComponentId(id)).thenReturn(0L);

        statusComponentService.deleteComponent(id);

        verify(statusComponentRepository).delete(component);
    }

    @Test
    void deleteComponent_activeIncidents_throwsBusinessRule() {
        UUID id = UUID.randomUUID();
        StatusComponent component = newComponent(id, newApp(UUID.randomUUID(), "OPERATIONAL"), "API", "OPERATIONAL");
        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(component));
        when(statusIncidentComponentRepository.countByComponentId(id)).thenReturn(1L);

        assertThatThrownBy(() -> statusComponentService.deleteComponent(id))
                .isInstanceOf(BusinessRuleException.class);
        verify(statusComponentRepository, never()).delete(any());
    }

    @Test
    void deleteComponent_scheduledMaintenance_throwsBusinessRule() {
        UUID id = UUID.randomUUID();
        StatusComponent component = newComponent(id, newApp(UUID.randomUUID(), "OPERATIONAL"), "API", "OPERATIONAL");
        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(component));
        when(statusIncidentComponentRepository.countByComponentId(id)).thenReturn(0L);
        when(statusMaintenanceComponentRepository.countByComponentId(id)).thenReturn(3L);

        assertThatThrownBy(() -> statusComponentService.deleteComponent(id))
                .isInstanceOf(BusinessRuleException.class);
        verify(statusComponentRepository, never()).delete(any());
    }

    @Test
    void reorderComponents_updatesPositionsForEach() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        StatusApp app = newApp(UUID.randomUUID(), "OPERATIONAL");
        StatusComponent c1 = newComponent(id1, app, "A", "OPERATIONAL");
        StatusComponent c2 = newComponent(id2, app, "B", "OPERATIONAL");

        ComponentOrderRequest o1 = new ComponentOrderRequest();
        o1.setComponentId(id1);
        o1.setPosition(5);
        ComponentOrderRequest o2 = new ComponentOrderRequest();
        o2.setComponentId(id2);
        o2.setPosition(2);

        when(statusComponentRepository.findById(id1)).thenReturn(Optional.of(c1));
        when(statusComponentRepository.findById(id2)).thenReturn(Optional.of(c2));
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));

        statusComponentService.reorderComponents(List.of(o1, o2));

        assertThat(c1.getPosition()).isEqualTo(5);
        assertThat(c2.getPosition()).isEqualTo(2);
        verify(statusComponentRepository, times(2)).save(any(StatusComponent.class));
    }

    @Test
    void reorderComponents_componentNotFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        ComponentOrderRequest o = new ComponentOrderRequest();
        o.setComponentId(id);
        o.setPosition(1);
        when(statusComponentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusComponentService.reorderComponents(List.of(o)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createComponent_defaultsAppliedForOptionalFields() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, "OPERATIONAL");
        StatusComponentRequest request = new StatusComponentRequest();
        request.setAppId(appId);
        request.setName("Cache");

        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusComponentRepository.existsByAppIdAndName(appId, "Cache")).thenReturn(false);
        when(statusComponentRepository.countByAppId(appId)).thenReturn(0L);
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<StatusComponent> captor = ArgumentCaptor.forClass(StatusComponent.class);
        statusComponentService.createComponent(request);

        verify(statusComponentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("OPERATIONAL");
        assertThat(captor.getValue().getCheckInheritFromApp()).isTrue();
    }
}
