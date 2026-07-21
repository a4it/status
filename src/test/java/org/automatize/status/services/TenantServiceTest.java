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

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", null, java.util.List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Tenant buildTenant(UUID id, String name) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName(name);
        tenant.setIsActive(true);
        return tenant;
    }

    @Test
    void getAllTenants_noSearch_returnsAllTenantsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Tenant> page = new PageImpl<>(List.of(buildTenant(UUID.randomUUID(), "Acme")));
        when(tenantRepository.findAll(pageable)).thenReturn(page);

        Page<Tenant> result = tenantService.getAllTenants(null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getAllTenants_withSearch_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Tenant> page = new PageImpl<>(List.of(buildTenant(UUID.randomUUID(), "Acme")));
        when(tenantRepository.findAll(pageable)).thenReturn(page);

        Page<Tenant> result = tenantService.getAllTenants("acme", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getTenantById_existing_returnsTenant() {
        UUID id = UUID.randomUUID();
        Tenant tenant = buildTenant(id, "Acme");
        when(tenantRepository.findById(id)).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.getTenantById(id);

        assertThat(result).isSameAs(tenant);
    }

    @Test
    void getTenantById_missing_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(tenantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getTenantById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTenantByName_existing_returnsTenant() {
        Tenant tenant = buildTenant(UUID.randomUUID(), "Acme");
        when(tenantRepository.findByName("Acme")).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.getTenantByName("Acme");

        assertThat(result).isSameAs(tenant);
    }

    @Test
    void getTenantByName_missing_throwsResourceNotFoundException() {
        when(tenantRepository.findByName("Nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getTenantByName("Nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

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

    @Test
    void createTenant_duplicateName_throwsDuplicateResourceException() {
        TenantRequest request = new TenantRequest();
        request.setName("Acme");

        when(tenantRepository.existsByName("Acme")).thenReturn(true);

        assertThatThrownBy(() -> tenantService.createTenant(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(tenantRepository, never()).save(any());
    }

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

    @Test
    void updateTenant_missing_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        TenantRequest request = new TenantRequest();
        request.setName("Acme");

        when(tenantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.updateTenant(id, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteTenant_existing_deletesTenant() {
        UUID id = UUID.randomUUID();
        Tenant tenant = buildTenant(id, "Acme");
        when(tenantRepository.findById(id)).thenReturn(Optional.of(tenant));

        tenantService.deleteTenant(id);

        verify(tenantRepository).delete(tenant);
    }

    @Test
    void deleteTenant_missing_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(tenantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.deleteTenant(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(tenantRepository, never()).delete(any());
    }
}
