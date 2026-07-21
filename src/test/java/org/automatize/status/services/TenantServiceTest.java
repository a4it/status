package org.automatize.status.services;

import org.automatize.status.api.request.TenantRequest;
import org.automatize.status.exceptions.DuplicateResourceException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
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
 * Unit tests for {@link TenantService}.
 */
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    /**
     * Establishes an authenticated security context before each test so that auditing
     * fields can resolve the current username.
     */
    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", null, java.util.List.of()));
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
     * Builds an active {@link Tenant} test fixture with the given identifier and name.
     *
     * @param id   the tenant identifier to assign
     * @param name the tenant name to assign
     * @return a populated {@link Tenant} instance for use in tests
     */
    private Tenant buildTenant(UUID id, String name) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName(name);
        tenant.setIsActive(true);
        return tenant;
    }

    /**
     * Verifies that {@code getAllTenants} returns the full paged result from the
     * repository when no search term is supplied.
     */
    @Test
    void getAllTenants_noSearch_returnsAllTenantsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Tenant> page = new PageImpl<>(List.of(buildTenant(UUID.randomUUID(), "Acme")));
        when(tenantRepository.findAll(pageable)).thenReturn(page);

        Page<Tenant> result = tenantService.getAllTenants(null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    /**
     * Verifies that {@code getAllTenants} returns a paged result when a search term
     * is supplied.
     */
    @Test
    void getAllTenants_withSearch_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Tenant> page = new PageImpl<>(List.of(buildTenant(UUID.randomUUID(), "Acme")));
        when(tenantRepository.findAll(pageable)).thenReturn(page);

        Page<Tenant> result = tenantService.getAllTenants("acme", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    /**
     * Verifies that {@code getTenantById} returns the tenant when one exists for the id.
     */
    @Test
    void getTenantById_existing_returnsTenant() {
        UUID id = UUID.randomUUID();
        Tenant tenant = buildTenant(id, "Acme");
        when(tenantRepository.findById(id)).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.getTenantById(id);

        assertThat(result).isSameAs(tenant);
    }

    /**
     * Verifies that {@code getTenantById} throws {@link ResourceNotFoundException}
     * when no tenant exists for the id.
     */
    @Test
    void getTenantById_missing_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(tenantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getTenantById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that {@code getTenantByName} returns the tenant when one exists for the name.
     */
    @Test
    void getTenantByName_existing_returnsTenant() {
        Tenant tenant = buildTenant(UUID.randomUUID(), "Acme");
        when(tenantRepository.findByName("Acme")).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.getTenantByName("Acme");

        assertThat(result).isSameAs(tenant);
    }

    /**
     * Verifies that {@code getTenantByName} throws {@link ResourceNotFoundException}
     * when no tenant exists for the name.
     */
    @Test
    void getTenantByName_missing_throwsResourceNotFoundException() {
        when(tenantRepository.findByName("Nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getTenantByName("Nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that {@code createTenant} persists a new tenant when the name is unique,
     * defaulting the active flag to true and stamping the creating user.
     */
    @Test
    void createTenant_uniqueName_savesTenant() {
        TenantRequest request = new TenantRequest();
        request.setName("Acme");
        request.setIsActive(null);

        when(tenantRepository.existsByName("Acme")).thenReturn(false);
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tenant result = tenantService.createTenant(request);

        assertThat(result.getName()).isEqualTo("Acme");
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getCreatedBy()).isEqualTo("system");
        verify(tenantRepository).save(any(Tenant.class));
    }

    /**
     * Verifies that {@code createTenant} throws {@link DuplicateResourceException} and
     * saves nothing when a tenant with the same name already exists.
     */
    @Test
    void createTenant_duplicateName_throwsDuplicateResourceException() {
        TenantRequest request = new TenantRequest();
        request.setName("Acme");

        when(tenantRepository.existsByName("Acme")).thenReturn(true);

        assertThatThrownBy(() -> tenantService.createTenant(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(tenantRepository, never()).save(any());
    }

    /**
     * Verifies that {@code updateTenant} updates the tenant without a duplicate-name
     * check when the name is unchanged, and stamps the modifying user.
     */
    @Test
    void updateTenant_sameName_updatesWithoutDuplicateCheck() {
        UUID id = UUID.randomUUID();
        Tenant tenant = buildTenant(id, "Acme");

        TenantRequest request = new TenantRequest();
        request.setName("Acme");
        request.setIsActive(false);

        when(tenantRepository.findById(id)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tenant result = tenantService.updateTenant(id, request);

        assertThat(result.getIsActive()).isFalse();
        assertThat(result.getLastModifiedBy()).isEqualTo("system");
    }

    /**
     * Verifies that {@code updateTenant} throws {@link DuplicateResourceException} and
     * saves nothing when the name is changed to one already taken by another tenant.
     */
    @Test
    void updateTenant_newDuplicateName_throwsDuplicateResourceException() {
        UUID id = UUID.randomUUID();
        Tenant tenant = buildTenant(id, "Acme");

        TenantRequest request = new TenantRequest();
        request.setName("Other");

        when(tenantRepository.findById(id)).thenReturn(Optional.of(tenant));
        when(tenantRepository.existsByName("Other")).thenReturn(true);

        assertThatThrownBy(() -> tenantService.updateTenant(id, request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(tenantRepository, never()).save(any());
    }

    /**
     * Verifies that {@code updateTenant} throws {@link ResourceNotFoundException}
     * when no tenant exists for the id.
     */
    @Test
    void updateTenant_missing_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        TenantRequest request = new TenantRequest();
        request.setName("Acme");

        when(tenantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.updateTenant(id, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that {@code deleteTenant} deletes the tenant when one exists for the id.
     */
    @Test
    void deleteTenant_existing_deletesTenant() {
        UUID id = UUID.randomUUID();
        Tenant tenant = buildTenant(id, "Acme");
        when(tenantRepository.findById(id)).thenReturn(Optional.of(tenant));

        tenantService.deleteTenant(id);

        verify(tenantRepository).delete(tenant);
    }

    /**
     * Verifies that {@code deleteTenant} throws {@link ResourceNotFoundException} and
     * deletes nothing when no tenant exists for the id.
     */
    @Test
    void deleteTenant_missing_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(tenantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.deleteTenant(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(tenantRepository, never()).delete(any());
    }
}
