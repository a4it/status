package org.automatize.status.services;

import org.automatize.status.api.response.StatusPlatformResponse;
import org.automatize.status.models.Organization;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusPlatform;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusPlatformRepository;
import org.automatize.status.repositories.TenantRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
 * Unit tests for {@link StatusPlatformService}.
 */
class StatusPlatformServiceTest extends AbstractServiceTest {

    private static final String SLUG_CLOUD = "cloud";
    private static final String STATUS_OPERATIONAL = "OPERATIONAL";

    @Mock
    private StatusPlatformRepository statusPlatformRepository;
    @Mock
    private StatusAppRepository statusAppRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private StatusPlatformService statusPlatformService;

    private final Pageable pageable = PageRequest.of(0, 10);

    /**
     * Builds a minimal public {@link StatusPlatform} fixture for use in tests.
     *
     * @param id     the identifier to assign to the platform
     * @param slug   the platform slug (also used to derive its name)
     * @param status the status value to assign to the platform
     * @return a populated {@link StatusPlatform} instance
     */
    private StatusPlatform newPlatform(UUID id, String slug, String status) {
        StatusPlatform platform = new StatusPlatform();
        platform.setId(id);
        platform.setName("Platform " + slug);
        platform.setSlug(slug);
        platform.setStatus(status);
        platform.setIsPublic(true);
        platform.setPosition(0);
        return platform;
    }

    /**
     * Verifies that requesting an existing platform by id returns a response mapping its id and slug.
     */
    @Test
    void getPlatformById_existingId_returnsResponse() {
        UUID id = UUID.randomUUID();
        when(statusPlatformRepository.findById(id)).thenReturn(Optional.of(newPlatform(id, SLUG_CLOUD, STATUS_OPERATIONAL)));

        StatusPlatformResponse response = statusPlatformService.getPlatformById(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getSlug()).isEqualTo(SLUG_CLOUD);
    }

    /**
     * Verifies that requesting a platform whose id does not exist throws a {@link RuntimeException}.
     */
    @Test
    void getPlatformById_notFound_throwsRuntime() {
        UUID id = UUID.randomUUID();
        when(statusPlatformRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusPlatformService.getPlatformById(id))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * Verifies that requesting a platform by an existing slug returns the matching response.
     */
    @Test
    void getPlatformBySlug_found_returnsResponse() {
        when(statusPlatformRepository.findBySlug(SLUG_CLOUD))
                .thenReturn(Optional.of(newPlatform(UUID.randomUUID(), SLUG_CLOUD, STATUS_OPERATIONAL)));

        StatusPlatformResponse response = statusPlatformService.getPlatformBySlug(SLUG_CLOUD);

        assertThat(response.getSlug()).isEqualTo(SLUG_CLOUD);
    }

    /**
     * Verifies that requesting a platform by an unknown slug throws a {@link RuntimeException}.
     */
    @Test
    void getPlatformBySlug_notFound_throwsRuntime() {
        when(statusPlatformRepository.findBySlug("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusPlatformService.getPlatformBySlug("x"))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * Verifies that requesting all platforms without a filter delegates to the repository's paged findAll
     * and returns the resulting page.
     */
    @Test
    void getAllPlatforms_noFilter_returnsPageFromFindAll() {
        when(statusPlatformRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(newPlatform(UUID.randomUUID(), SLUG_CLOUD, STATUS_OPERATIONAL))));

        var page = statusPlatformService.getAllPlatforms(null, null, null, pageable);

        assertThat(page.getContent()).hasSize(1);
    }

    /**
     * Verifies that fetching all platforms ordered returns the position-ordered list from the repository.
     */
    @Test
    void getAllPlatformsOrdered_returnsOrderedList() {
        when(statusPlatformRepository.findAllByOrderByPosition())
                .thenReturn(List.of(newPlatform(UUID.randomUUID(), SLUG_CLOUD, STATUS_OPERATIONAL)));

        List<StatusPlatformResponse> result = statusPlatformService.getAllPlatformsOrdered();

        assertThat(result).hasSize(1);
    }

    /**
     * Verifies that creating a platform without a tenant persists it directly.
     */
    @Test
    void createPlatform_noTenant_saves() {
        StatusPlatform platform = newPlatform(null, SLUG_CLOUD, STATUS_OPERATIONAL);
        when(statusPlatformRepository.save(any(StatusPlatform.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusPlatformResponse response = statusPlatformService.createPlatform(platform);

        assertThat(response.getSlug()).isEqualTo(SLUG_CLOUD);
        verify(statusPlatformRepository).save(platform);
    }

    /**
     * Verifies that creating a platform whose slug already exists within its tenant throws a
     * {@link RuntimeException} and never persists it.
     */
    @Test
    void createPlatform_duplicateSlugInTenant_throwsRuntime() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        StatusPlatform platform = newPlatform(null, "dup", STATUS_OPERATIONAL);
        platform.setTenant(tenant);
        when(statusPlatformRepository.existsByTenantIdAndSlug(tenantId, "dup")).thenReturn(true);

        assertThatThrownBy(() -> statusPlatformService.createPlatform(platform))
                .isInstanceOf(RuntimeException.class);
        verify(statusPlatformRepository, never()).save(any());
    }

    /**
     * Verifies that creating a platform with explicit tenant and organization ids resolves both entities,
     * assigns them to the platform and persists it.
     */
    @Test
    void createPlatform_withTenantAndOrganizationIds_resolvesAndSaves() {
        UUID tenantId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        Organization org = new Organization();
        org.setId(orgId);
        StatusPlatform platform = newPlatform(null, SLUG_CLOUD, STATUS_OPERATIONAL);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(statusPlatformRepository.existsByTenantIdAndSlug(tenantId, SLUG_CLOUD)).thenReturn(false);
        when(statusPlatformRepository.save(any(StatusPlatform.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusPlatformResponse response = statusPlatformService.createPlatform(platform, tenantId, orgId);

        assertThat(response.getSlug()).isEqualTo(SLUG_CLOUD);
        assertThat(platform.getTenant()).isEqualTo(tenant);
        assertThat(platform.getOrganization()).isEqualTo(org);
    }

    /**
     * Verifies that updating a platform while keeping the same slug applies the new field values (name, status).
     */
    @Test
    void updatePlatform_sameSlug_updatesFields() {
        UUID id = UUID.randomUUID();
        StatusPlatform existing = newPlatform(id, SLUG_CLOUD, STATUS_OPERATIONAL);
        StatusPlatform updated = newPlatform(null, SLUG_CLOUD, "DEGRADED");
        updated.setName("Renamed");

        when(statusPlatformRepository.findById(id)).thenReturn(Optional.of(existing));
        when(statusPlatformRepository.save(any(StatusPlatform.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusPlatformResponse response = statusPlatformService.updatePlatform(id, updated);

        assertThat(response.getName()).isEqualTo("Renamed");
        assertThat(response.getStatus()).isEqualTo("DEGRADED");
    }

    /**
     * Verifies that updating a platform whose id does not exist throws a {@link RuntimeException}.
     */
    @Test
    void updatePlatform_notFound_throwsRuntime() {
        UUID id = UUID.randomUUID();
        when(statusPlatformRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusPlatformService.updatePlatform(id, newPlatform(null, "x", STATUS_OPERATIONAL)))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * Verifies that changing a platform's status (e.g. to MAJOR_OUTAGE) updates and saves the platform.
     */
    @Test
    void updateStatus_updatesAndSaves() {
        UUID id = UUID.randomUUID();
        StatusPlatform platform = newPlatform(id, SLUG_CLOUD, STATUS_OPERATIONAL);
        when(statusPlatformRepository.findById(id)).thenReturn(Optional.of(platform));
        when(statusPlatformRepository.save(any(StatusPlatform.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusPlatformResponse response = statusPlatformService.updateStatus(id, "MAJOR_OUTAGE");

        assertThat(response.getStatus()).isEqualTo("MAJOR_OUTAGE");
    }

    /**
     * Verifies that a platform with no associated apps can be deleted.
     */
    @Test
    void deletePlatform_noAssociatedApps_deletes() {
        UUID id = UUID.randomUUID();
        StatusPlatform platform = newPlatform(id, SLUG_CLOUD, STATUS_OPERATIONAL);
        when(statusPlatformRepository.findById(id)).thenReturn(Optional.of(platform));
        when(statusAppRepository.findByPlatformId(id)).thenReturn(List.of());

        statusPlatformService.deletePlatform(id);

        verify(statusPlatformRepository).delete(platform);
    }

    /**
     * Verifies that attempting to delete a platform that still has associated apps throws a
     * {@link RuntimeException} and never deletes it.
     */
    @Test
    void deletePlatform_withAssociatedApps_throwsRuntime() {
        UUID id = UUID.randomUUID();
        StatusPlatform platform = newPlatform(id, SLUG_CLOUD, STATUS_OPERATIONAL);
        when(statusPlatformRepository.findById(id)).thenReturn(Optional.of(platform));
        when(statusAppRepository.findByPlatformId(id)).thenReturn(List.of(new StatusApp()));

        assertThatThrownBy(() -> statusPlatformService.deletePlatform(id))
                .isInstanceOf(RuntimeException.class);
        verify(statusPlatformRepository, never()).delete(any());
    }

    /**
     * Verifies that attempting to delete a platform whose id does not exist throws a {@link RuntimeException}.
     */
    @Test
    void deletePlatform_notFound_throwsRuntime() {
        UUID id = UUID.randomUUID();
        when(statusPlatformRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusPlatformService.deletePlatform(id))
                .isInstanceOf(RuntimeException.class);
    }
}
