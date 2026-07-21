package org.automatize.status.services;

import org.automatize.status.api.request.StatusAppRequest;
import org.automatize.status.api.response.StatusAppResponse;
import org.automatize.status.exceptions.BusinessRuleException;
import org.automatize.status.exceptions.DuplicateResourceException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.automatize.status.repositories.StatusIncidentRepository;
import org.automatize.status.repositories.StatusMaintenanceRepository;
import org.automatize.status.repositories.StatusPlatformRepository;
import org.automatize.status.repositories.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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
 * Unit tests for {@link StatusAppService}.
 */
@ExtendWith(MockitoExtension.class)
class StatusAppServiceTest {

    @Mock
    private StatusAppRepository statusAppRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private StatusComponentRepository statusComponentRepository;
    @Mock
    private StatusIncidentRepository statusIncidentRepository;
    @Mock
    private StatusMaintenanceRepository statusMaintenanceRepository;
    @Mock
    private StatusPlatformRepository statusPlatformRepository;

    @InjectMocks
    private StatusAppService statusAppService;

    private final Pageable pageable = PageRequest.of(0, 10);

    /**
     * Establishes an authenticated security context before each test so that
     * service calls relying on the current principal succeed.
     */
    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", null, List.of()));
    }

    /**
     * Clears the security context after each test to avoid leaking authentication
     * state between tests.
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Builds a public {@link StatusApp} fixture.
     *
     * @param id     the application id
     * @param slug   the URL slug
     * @param status the current status (e.g. OPERATIONAL)
     * @return a populated {@link StatusApp} instance
     */
    private StatusApp newApp(UUID id, String slug, String status) {
        StatusApp app = new StatusApp();
        app.setId(id);
        app.setName("App " + slug);
        app.setSlug(slug);
        app.setStatus(status);
        app.setIsPublic(true);
        return app;
    }

    /**
     * Verifies that looking up an existing app by id returns a response with the
     * matching id and status.
     */
    @Test
    void getStatusAppById_existingId_returnsResponse() {
        UUID id = UUID.randomUUID();
        when(statusAppRepository.findById(id)).thenReturn(Optional.of(newApp(id, "web", "OPERATIONAL")));

        StatusAppResponse response = statusAppService.getStatusAppById(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getStatus()).isEqualTo("OPERATIONAL");
    }

    /**
     * Verifies that looking up a missing app by id raises
     * {@link ResourceNotFoundException}.
     */
    @Test
    void getStatusAppById_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(statusAppRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusAppService.getStatusAppById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that with no filters the service returns a page sourced from the
     * repository's paged {@code findAll}.
     */
    @Test
    void getAllStatusApps_noFilters_returnsPageFromFindAll() {
        StatusApp app = newApp(UUID.randomUUID(), "web", "OPERATIONAL");
        when(statusAppRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(app)));

        var page = statusAppService.getAllStatusApps(null, null, null, pageable);

        assertThat(page.getContent()).hasSize(1);
    }

    /**
     * Verifies that filtering by tenant returns a page mapped from the tenant-scoped
     * repository lookup.
     */
    @Test
    void getAllStatusApps_byTenant_returnsMappedList() {
        UUID tenantId = UUID.randomUUID();
        when(statusAppRepository.findByTenantId(tenantId))
                .thenReturn(List.of(newApp(UUID.randomUUID(), "web", "OPERATIONAL")));

        var page = statusAppService.getAllStatusApps(tenantId, null, null, pageable);

        assertThat(page.getContent()).hasSize(1);
    }

    /**
     * Verifies that creating an app with a unique slug persists it and returns a
     * response carrying the slug and a generated API key.
     */
    @Test
    void createStatusApp_uniqueSlug_savesAndReturns() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        StatusAppRequest request = new StatusAppRequest();
        request.setName("New App");
        request.setSlug("new-app");
        request.setTenantId(tenantId);

        when(statusAppRepository.existsByTenantIdAndSlug(tenantId, "new-app")).thenReturn(false);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(statusAppRepository.save(any(StatusApp.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusAppResponse response = statusAppService.createStatusApp(request);

        assertThat(response.getSlug()).isEqualTo("new-app");
        assertThat(response.getApiKey()).isNotBlank();
        verify(statusAppRepository).save(any(StatusApp.class));
    }

    /**
     * Verifies that creating an app with a slug already used within the tenant raises
     * {@link DuplicateResourceException} and never persists.
     */
    @Test
    void createStatusApp_duplicateSlug_throwsDuplicateResource() {
        UUID tenantId = UUID.randomUUID();
        StatusAppRequest request = new StatusAppRequest();
        request.setSlug("dup");
        request.setTenantId(tenantId);
        when(statusAppRepository.existsByTenantIdAndSlug(tenantId, "dup")).thenReturn(true);

        assertThatThrownBy(() -> statusAppService.createStatusApp(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(statusAppRepository, never()).save(any());
    }

    /**
     * Verifies that creating an app referencing a non-existent tenant raises
     * {@link ResourceNotFoundException}.
     */
    @Test
    void createStatusApp_tenantNotFound_throwsResourceNotFound() {
        UUID tenantId = UUID.randomUUID();
        StatusAppRequest request = new StatusAppRequest();
        request.setSlug("s");
        request.setName("n");
        request.setTenantId(tenantId);
        when(statusAppRepository.existsByTenantIdAndSlug(tenantId, "s")).thenReturn(false);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusAppService.createStatusApp(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that updating a missing app raises {@link ResourceNotFoundException}.
     */
    @Test
    void updateStatusApp_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        StatusAppRequest request = new StatusAppRequest();
        when(statusAppRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusAppService.updateStatusApp(id, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that updating an app while keeping its slug applies the new fields and
     * persists the existing entity.
     */
    @Test
    void updateStatusApp_sameSlug_updatesAndSaves() {
        UUID id = UUID.randomUUID();
        StatusApp existing = newApp(id, "web", "OPERATIONAL");
        existing.setApiKey("existing-key");

        StatusAppRequest request = new StatusAppRequest();
        request.setName("Renamed");
        request.setSlug("web");

        when(statusAppRepository.findById(id)).thenReturn(Optional.of(existing));
        when(statusAppRepository.save(any(StatusApp.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusAppResponse response = statusAppService.updateStatusApp(id, request);

        assertThat(response.getName()).isEqualTo("Renamed");
        verify(statusAppRepository).save(existing);
    }

    /**
     * Verifies that setting an app to MAJOR_OUTAGE cascades the status to all its
     * components and saves them.
     */
    @Test
    void updateStatus_majorOutage_cascadesToComponents() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(id, "web", "OPERATIONAL");
        StatusComponent c1 = new StatusComponent();
        c1.setStatus("OPERATIONAL");
        StatusComponent c2 = new StatusComponent();
        c2.setStatus("DEGRADED");

        when(statusAppRepository.findById(id)).thenReturn(Optional.of(app));
        when(statusAppRepository.save(any(StatusApp.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusComponentRepository.findByAppId(id)).thenReturn(List.of(c1, c2));

        statusAppService.updateStatus(id, "MAJOR_OUTAGE");

        assertThat(c1.getStatus()).isEqualTo("MAJOR_OUTAGE");
        assertThat(c2.getStatus()).isEqualTo("MAJOR_OUTAGE");
        verify(statusComponentRepository).saveAll(List.of(c1, c2));
    }

    /**
     * Verifies that setting an app to OPERATIONAL does not cascade to or query its
     * components.
     */
    @Test
    void updateStatus_operational_doesNotCascade() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(id, "web", "MAJOR_OUTAGE");
        when(statusAppRepository.findById(id)).thenReturn(Optional.of(app));
        when(statusAppRepository.save(any(StatusApp.class))).thenAnswer(inv -> inv.getArgument(0));

        statusAppService.updateStatus(id, "OPERATIONAL");

        verify(statusComponentRepository, never()).saveAll(any());
        verify(statusComponentRepository, never()).findByAppId(id);
    }

    /**
     * Verifies that an app with no active incidents or maintenance is deleted.
     */
    @Test
    void deleteStatusApp_noActiveIncidentsOrMaintenance_deletes() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(id, "web", "OPERATIONAL");
        when(statusAppRepository.findById(id)).thenReturn(Optional.of(app));
        when(statusIncidentRepository.countActiveIncidentsByAppId(id)).thenReturn(0L);
        when(statusMaintenanceRepository.countActiveMaintenanceByAppId(id)).thenReturn(0L);

        statusAppService.deleteStatusApp(id);

        verify(statusAppRepository).delete(app);
    }

    @Test
    void deleteStatusApp_activeIncidents_throwsBusinessRule() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(id, "web", "MAJOR_OUTAGE");
        when(statusAppRepository.findById(id)).thenReturn(Optional.of(app));
        when(statusIncidentRepository.countActiveIncidentsByAppId(id)).thenReturn(1L);

        assertThatThrownBy(() -> statusAppService.deleteStatusApp(id))
                .isInstanceOf(BusinessRuleException.class);
        verify(statusAppRepository, never()).delete(any());
    }

    @Test
    void deleteStatusApp_upcomingMaintenance_throwsBusinessRule() {
        UUID id = UUID.randomUUID();
        StatusApp app = newApp(id, "web", "OPERATIONAL");
        when(statusAppRepository.findById(id)).thenReturn(Optional.of(app));
        when(statusIncidentRepository.countActiveIncidentsByAppId(id)).thenReturn(0L);
        when(statusMaintenanceRepository.countActiveMaintenanceByAppId(id)).thenReturn(2L);

        assertThatThrownBy(() -> statusAppService.deleteStatusApp(id))
                .isInstanceOf(BusinessRuleException.class);
        verify(statusAppRepository, never()).delete(any());
    }

    @Test
    void deleteStatusApp_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(statusAppRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusAppService.deleteStatusApp(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createStatusApp_defaultsAppliedForOptionalFields() {
        StatusAppRequest request = new StatusAppRequest();
        request.setName("Minimal");
        request.setSlug("minimal");
        when(statusAppRepository.save(any(StatusApp.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<StatusApp> captor = ArgumentCaptor.forClass(StatusApp.class);
        statusAppService.createStatusApp(request);

        verify(statusAppRepository).save(captor.capture());
        StatusApp saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("OPERATIONAL");
        assertThat(saved.getIsPublic()).isTrue();
        assertThat(saved.getCheckType()).isEqualTo("NONE");
    }
}
