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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

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
class StatusComponentServiceTest extends AbstractServiceTest {

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

    private static final String OPERATIONAL = "OPERATIONAL";
    private static final String DATABASE = "Database";
    private static final String MAJOR_OUTAGE = "MAJOR_OUTAGE";
    private static final String DEGRADED = "DEGRADED";

    /**
     * Builds a minimal {@link StatusComponent} fixture linked to the given app.
     *
     * @param id     the identifier to assign to the component
     * @param app    the owning {@link StatusApp}
     * @param name   the component name
     * @param status the status value to assign to the component
     * @return a populated {@link StatusComponent} instance
     */
    private StatusComponent newComponent(UUID id, StatusApp app, String name, String status) {
        StatusComponent component = new StatusComponent();
        component.setId(id);
        component.setApp(app);
        component.setName(name);
        component.setStatus(status);
        return component;
    }

    /**
     * Verifies that requesting an existing component by id returns a response mapping its id and name.
     */
    @Test
    void getComponentById_existingId_returnsResponse() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(UUID.randomUUID(), OPERATIONAL);
        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(newComponent(id, app, "API", OPERATIONAL)));

        StatusComponentResponse response = statusComponentService.getComponentById(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getName()).isEqualTo("API");
    }

    /**
     * Verifies that requesting a component whose id does not exist throws {@link ResourceNotFoundException}.
     */
    @Test
    void getComponentById_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(statusComponentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusComponentService.getComponentById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that creating a component with a unique name persists it, assigns the next
     * position (count + 1) and generates a non-blank API key.
     */
    @Test
    void createComponent_uniqueName_savesAndAssignsPosition() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, OPERATIONAL);
        StatusComponentRequest request = new StatusComponentRequest();
        request.setAppId(appId);
        request.setName(DATABASE);

        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusComponentRepository.existsByAppIdAndName(appId, DATABASE)).thenReturn(false);
        when(statusComponentRepository.countByAppId(appId)).thenReturn(2L);
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusComponentResponse response = statusComponentService.createComponent(request);

        assertThat(response.getName()).isEqualTo(DATABASE);
        assertThat(response.getPosition()).isEqualTo(3);
        assertThat(response.getApiKey()).isNotBlank();
    }

    /**
     * Verifies that creating a component for a non-existent app throws {@link ResourceNotFoundException}.
     */
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

    /**
     * Verifies that creating a component whose name already exists within the app throws
     * {@link DuplicateResourceException} and never persists the component.
     */
    @Test
    void createComponent_duplicateName_throwsDuplicateResource() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, OPERATIONAL);
        StatusComponentRequest request = new StatusComponentRequest();
        request.setAppId(appId);
        request.setName("Dup");
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusComponentRepository.existsByAppIdAndName(appId, "Dup")).thenReturn(true);

        assertThatThrownBy(() -> statusComponentService.createComponent(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(statusComponentRepository, never()).save(any());
    }

    /**
     * Verifies that updating a component while keeping the same name applies the new field values
     * and saves the component.
     */
    @Test
    void updateComponent_sameName_updatesAndSaves() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(UUID.randomUUID(), OPERATIONAL);
        StatusComponent component = newComponent(id, app, "API", OPERATIONAL);
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

    /**
     * Verifies that updating a component whose id does not exist throws {@link ResourceNotFoundException}.
     */
    @Test
    void updateComponent_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(statusComponentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusComponentService.updateComponent(id, new StatusComponentRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that renaming a component to a name already used by another component in the same app
     * throws {@link DuplicateResourceException}.
     */
    @Test
    void updateComponent_renameToExistingName_throwsDuplicateResource() {
        UUID id = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, OPERATIONAL);
        StatusComponent component = newComponent(id, app, "old", OPERATIONAL);

        StatusComponentRequest request = new StatusComponentRequest();
        request.setName("new");

        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(component));
        when(statusComponentRepository.existsByAppIdAndName(appId, "new")).thenReturn(true);

        assertThatThrownBy(() -> statusComponentService.updateComponent(id, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    /**
     * Verifies that setting a component to MAJOR_OUTAGE recomputes and persists the parent app status as MAJOR_OUTAGE.
     */
    @Test
    void updateStatus_componentMajorOutage_setsAppMajorOutage() {
        UUID id = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, OPERATIONAL);
        StatusComponent component = newComponent(id, app, "API", OPERATIONAL);

        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(component));
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));
        StatusComponent outage = newComponent(UUID.randomUUID(), app, "A", MAJOR_OUTAGE);
        when(statusComponentRepository.findByAppId(appId)).thenReturn(List.of(outage));
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusAppRepository.save(any(StatusApp.class))).thenAnswer(inv -> inv.getArgument(0));

        statusComponentService.updateStatus(id, MAJOR_OUTAGE);

        assertThat(app.getStatus()).isEqualTo(MAJOR_OUTAGE);
        verify(statusAppRepository).save(app);
    }

    /**
     * Verifies that setting a component to PARTIAL_OUTAGE recomputes and persists the parent app status as DEGRADED.
     */
    @Test
    void updateStatus_componentDegraded_setsAppDegraded() {
        UUID id = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, OPERATIONAL);
        StatusComponent component = newComponent(id, app, "API", OPERATIONAL);

        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(component));
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));
        StatusComponent degraded = newComponent(UUID.randomUUID(), app, "A", "PARTIAL_OUTAGE");
        when(statusComponentRepository.findByAppId(appId)).thenReturn(List.of(degraded));
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusAppRepository.save(any(StatusApp.class))).thenAnswer(inv -> inv.getArgument(0));

        statusComponentService.updateStatus(id, "PARTIAL_OUTAGE");

        assertThat(app.getStatus()).isEqualTo(DEGRADED);
        verify(statusAppRepository).save(app);
    }

    /**
     * Verifies that when all components are OPERATIONAL the parent app status is recomputed and persisted as OPERATIONAL.
     */
    @Test
    void updateStatus_allOperational_setsAppOperational() {
        UUID id = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, DEGRADED);
        StatusComponent component = newComponent(id, app, "API", DEGRADED);

        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(component));
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));
        StatusComponent operational = newComponent(UUID.randomUUID(), app, "A", OPERATIONAL);
        when(statusComponentRepository.findByAppId(appId)).thenReturn(List.of(operational));
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusAppRepository.save(any(StatusApp.class))).thenAnswer(inv -> inv.getArgument(0));

        statusComponentService.updateStatus(id, OPERATIONAL);

        assertThat(app.getStatus()).isEqualTo(OPERATIONAL);
        verify(statusAppRepository).save(app);
    }

    /**
     * Verifies that updating a component's status when the app has no components leaves the app
     * untouched (never saved).
     */
    @Test
    void updateStatus_noComponents_leavesAppUntouched() {
        UUID id = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, OPERATIONAL);
        StatusComponent component = newComponent(id, app, "API", OPERATIONAL);

        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(component));
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusComponentRepository.findByAppId(appId)).thenReturn(List.of());

        statusComponentService.updateStatus(id, OPERATIONAL);

        verify(statusAppRepository, never()).save(any());
    }

    /**
     * Verifies that a component with no linked incidents or maintenances is deleted.
     */
    @Test
    void deleteComponent_noIncidentsOrMaintenance_deletes() {
        UUID id = UUID.randomUUID();
        StatusComponent component = newComponent(id, newApp(UUID.randomUUID(), OPERATIONAL), "API", OPERATIONAL);
        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(component));
        when(statusIncidentComponentRepository.countByComponentId(id)).thenReturn(0L);
        when(statusMaintenanceComponentRepository.countByComponentId(id)).thenReturn(0L);

        statusComponentService.deleteComponent(id);

        verify(statusComponentRepository).delete(component);
    }

    /**
     * Verifies that deleting a component still linked to incidents throws {@link BusinessRuleException}
     * and never deletes the component.
     */
    @Test
    void deleteComponent_activeIncidents_throwsBusinessRule() {
        UUID id = UUID.randomUUID();
        StatusComponent component = newComponent(id, newApp(UUID.randomUUID(), OPERATIONAL), "API", OPERATIONAL);
        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(component));
        when(statusIncidentComponentRepository.countByComponentId(id)).thenReturn(1L);

        assertThatThrownBy(() -> statusComponentService.deleteComponent(id))
                .isInstanceOf(BusinessRuleException.class);
        verify(statusComponentRepository, never()).delete(any());
    }

    /**
     * Verifies that deleting a component still linked to maintenances throws {@link BusinessRuleException}
     * and never deletes the component.
     */
    @Test
    void deleteComponent_scheduledMaintenance_throwsBusinessRule() {
        UUID id = UUID.randomUUID();
        StatusComponent component = newComponent(id, newApp(UUID.randomUUID(), OPERATIONAL), "API", OPERATIONAL);
        when(statusComponentRepository.findById(id)).thenReturn(Optional.of(component));
        when(statusIncidentComponentRepository.countByComponentId(id)).thenReturn(0L);
        when(statusMaintenanceComponentRepository.countByComponentId(id)).thenReturn(3L);

        assertThatThrownBy(() -> statusComponentService.deleteComponent(id))
                .isInstanceOf(BusinessRuleException.class);
        verify(statusComponentRepository, never()).delete(any());
    }

    /**
     * Verifies that reordering components applies the requested position to each component and saves them all.
     */
    @Test
    void reorderComponents_updatesPositionsForEach() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        StatusApp app = newApp(UUID.randomUUID(), OPERATIONAL);
        StatusComponent c1 = newComponent(id1, app, "A", OPERATIONAL);
        StatusComponent c2 = newComponent(id2, app, "B", OPERATIONAL);

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

    /**
     * Verifies that reordering fails with {@link ResourceNotFoundException} when a referenced component id does not exist.
     */
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

    /**
     * Verifies that creating a component without optional fields applies the expected defaults:
     * OPERATIONAL status and check-inheritance from the app enabled.
     */
    @Test
    void createComponent_defaultsAppliedForOptionalFields() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId, OPERATIONAL);
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
        assertThat(captor.getValue().getStatus()).isEqualTo(OPERATIONAL);
        assertThat(captor.getValue().getCheckInheritFromApp()).isTrue();
    }
}
