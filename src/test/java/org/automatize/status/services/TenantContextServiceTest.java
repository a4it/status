package org.automatize.status.services;

import org.automatize.status.api.response.ContextResponse;
import org.automatize.status.exceptions.BusinessRuleException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.Organization;
import org.automatize.status.models.Tenant;
import org.automatize.status.models.User;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.TenantRepository;
import org.automatize.status.repositories.UserRepository;
import org.automatize.status.security.JwtUtils;
import org.automatize.status.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TenantContextService}.
 */
@ExtendWith(MockitoExtension.class)
class TenantContextServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private TenantContextService tenantContextService;

    private User buildUser(UUID id, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername("tester");
        user.setEmail("tester@example.com");
        user.setRole(role);
        return user;
    }

    private Tenant buildTenant(UUID id, boolean active) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName("Acme");
        tenant.setIsActive(active);
        return tenant;
    }

    private Organization buildOrg(UUID id, String status, Tenant tenant) {
        Organization org = new Organization();
        org.setId(id);
        org.setName("Org1");
        org.setStatus(status);
        org.setTenant(tenant);
        return org;
    }

    @Test
    void getActiveTenants_delegatesToRepository() {
        List<Tenant> tenants = List.of(buildTenant(UUID.randomUUID(), true));
        when(tenantRepository.findByIsActive(true)).thenReturn(tenants);

        assertThat(tenantContextService.getActiveTenants()).isEqualTo(tenants);
    }

    @Test
    void getOrganizationsForTenant_returnsActiveOrganizations() {
        UUID tenantId = UUID.randomUUID();
        List<Organization> orgs = List.of(buildOrg(UUID.randomUUID(), "ACTIVE", null));
        when(organizationRepository.findByTenantIdAndStatus(tenantId, "ACTIVE")).thenReturn(orgs);

        assertThat(tenantContextService.getOrganizationsForTenant(tenantId)).isEqualTo(orgs);
    }

    @Test
    void switchContext_userNotFound_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantContextService.switchContext(userId, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void switchContext_nonSuperadmin_throwsAccessDeniedException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser(userId, "USER")));

        assertThatThrownBy(() -> tenantContextService.switchContext(userId, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void switchContext_tenantNotFound_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser(userId, "SUPERADMIN")));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantContextService.switchContext(userId, tenantId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void switchContext_inactiveTenant_throwsBusinessRuleException() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser(userId, "SUPERADMIN")));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(buildTenant(tenantId, false)));

        assertThatThrownBy(() -> tenantContextService.switchContext(userId, tenantId, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Tenant is not active");
    }

    @Test
    void switchContext_organizationNotFound_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser(userId, "SUPERADMIN")));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(buildTenant(tenantId, true)));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantContextService.switchContext(userId, tenantId, orgId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void switchContext_inactiveOrganization_throwsBusinessRuleException() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Tenant tenant = buildTenant(tenantId, true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser(userId, "SUPERADMIN")));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(buildOrg(orgId, "INACTIVE", tenant)));

        assertThatThrownBy(() -> tenantContextService.switchContext(userId, tenantId, orgId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Organization is not active");
    }

    @Test
    void switchContext_organizationNotInTenant_throwsBusinessRuleException() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Tenant tenant = buildTenant(tenantId, true);
        Tenant otherTenant = buildTenant(UUID.randomUUID(), true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser(userId, "SUPERADMIN")));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(buildOrg(orgId, "ACTIVE", otherTenant)));

        assertThatThrownBy(() -> tenantContextService.switchContext(userId, tenantId, orgId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Organization does not belong to the selected tenant");
    }

    @Test
    void switchContext_validRequest_returnsContextResponse() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        User user = buildUser(userId, "SUPERADMIN");
        Tenant tenant = buildTenant(tenantId, true);
        Organization org = buildOrg(orgId, "ACTIVE", tenant);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(jwtUtils.generateJwtTokenWithContext(eq(userId), eq("tester"), eq("tester@example.com"),
                eq(orgId), eq("SUPERADMIN"), eq(tenantId))).thenReturn("context-token");

        ContextResponse response = tenantContextService.switchContext(userId, tenantId, orgId);

        assertThat(response.getAccessToken()).isEqualTo("context-token");
        assertThat(response.getTenantId()).isEqualTo(tenantId);
        assertThat(response.getTenantName()).isEqualTo("Acme");
        assertThat(response.getOrganizationId()).isEqualTo(orgId);
        assertThat(response.getOrganizationName()).isEqualTo("Org1");
        assertThat(response.isSuperadmin()).isTrue();
        assertThat(response.isHasSelectedContext()).isTrue();
    }

    @Test
    void getCurrentContext_withSelectedContext_populatesNames() {
        UUID tenantId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        User user = buildUser(UUID.randomUUID(), "SUPERADMIN");
        UserPrincipal principal = UserPrincipal.createWithContext(user, tenantId, orgId);

        Tenant tenant = buildTenant(tenantId, true);
        Organization org = buildOrg(orgId, "ACTIVE", tenant);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

        ContextResponse response = tenantContextService.getCurrentContext(principal);

        assertThat(response.getTenantId()).isEqualTo(tenantId);
        assertThat(response.getTenantName()).isEqualTo("Acme");
        assertThat(response.getOrganizationId()).isEqualTo(orgId);
        assertThat(response.getOrganizationName()).isEqualTo("Org1");
        assertThat(response.isSuperadmin()).isTrue();
        assertThat(response.isHasSelectedContext()).isTrue();
    }

    @Test
    void getCurrentContext_noContext_returnsResponseWithoutLookups() {
        User user = buildUser(UUID.randomUUID(), "USER");
        UserPrincipal principal = UserPrincipal.create(user);

        ContextResponse response = tenantContextService.getCurrentContext(principal);

        assertThat(response.getTenantId()).isNull();
        assertThat(response.getTenantName()).isNull();
        assertThat(response.isSuperadmin()).isFalse();
    }
}
