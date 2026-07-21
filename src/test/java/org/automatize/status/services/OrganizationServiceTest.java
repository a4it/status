package org.automatize.status.services;

import org.automatize.status.api.request.OrganizationRequest;
import org.automatize.status.exceptions.BusinessRuleException;
import org.automatize.status.exceptions.DuplicateResourceException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.Organization;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.TenantRepository;
import org.automatize.status.repositories.UserRepository;
import org.automatize.status.security.UserPrincipal;
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

import java.util.Collections;
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
 * Unit tests for {@link OrganizationService}.
 *
 * <p>Testing approach: pure Mockito unit tests. The organization, tenant and user repositories are
 * mocked and injected into the service, so filtering/paging, CRUD rules, duplicate and not-found
 * handling, tenant association, and delete-guard logic are verified without a database. A
 * {@link SecurityContextHolder} authentication (optionally carrying a {@link UserPrincipal}) is
 * seeded per test for the current-user scenarios and cleared afterwards.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrganizationService organizationService;

    /**
     * Seeds the security context with a basic "tester" authentication before each test.
     */
    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", null, java.util.List.of()));
    }

    /**
     * Clears the security context after each test to avoid leaking authentication state.
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Replaces the authentication with one whose principal is a {@link UserPrincipal} bound to the
     * given organization id, enabling current-user organization scenarios.
     *
     * @param orgId the organization id to associate with the principal (may be {@code null})
     */
    private void setPrincipal(UUID orgId) {
        UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(), "tester", "tester@example.com", "pw",
                "ADMIN", orgId, true, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList()));
    }

    /**
     * Builds an ACTIVE {@link Organization} fixture with the given id and name plus a derived email.
     *
     * @param id   the identifier to assign
     * @param name the organization name (also used to derive the email)
     * @return a populated {@link Organization}
     */
    private Organization buildOrg(UUID id, String name) {
        Organization org = new Organization();
        org.setId(id);
        org.setName(name);
        org.setEmail(name + "@example.com");
        org.setStatus("ACTIVE");
        return org;
    }

    /**
     * Builds an {@link OrganizationRequest} carrying only the given name.
     *
     * @param name the organization name to set on the request
     * @return a populated {@link OrganizationRequest}
     */
    private OrganizationRequest buildRequest(String name) {
        OrganizationRequest request = new OrganizationRequest();
        request.setName(name);
        return request;
    }

    /**
     * Verifies that supplying both tenant and status filters queries the repository by tenant id and
     * status, returning the matching organizations as a page.
     */
    @Test
    void getAllOrganizations_tenantAndStatus_filtersByBoth() {
        UUID tenantId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        List<Organization> orgs = List.of(buildOrg(UUID.randomUUID(), "Org1"));
        when(organizationRepository.findByTenantIdAndStatus(tenantId, "ACTIVE")).thenReturn(orgs);

        Page<Organization> result = organizationService.getAllOrganizations(tenantId, "ACTIVE", null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    /**
     * Verifies that supplying only a tenant filter queries the repository by tenant id.
     */
    @Test
    void getAllOrganizations_tenantOnly_filtersByTenant() {
        UUID tenantId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        when(organizationRepository.findByTenantId(tenantId))
                .thenReturn(List.of(buildOrg(UUID.randomUUID(), "Org1")));

        Page<Organization> result = organizationService.getAllOrganizations(tenantId, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    /**
     * Verifies that supplying only a status filter queries the repository by status.
     */
    @Test
    void getAllOrganizations_statusOnly_filtersByStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        when(organizationRepository.findByStatus("ACTIVE"))
                .thenReturn(List.of(buildOrg(UUID.randomUUID(), "Org1")));

        Page<Organization> result = organizationService.getAllOrganizations(null, "ACTIVE", null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    /**
     * Verifies that supplying only a search term routes to the repository's search query.
     */
    @Test
    void getAllOrganizations_searchOnly_usesSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        when(organizationRepository.search("acme"))
                .thenReturn(List.of(buildOrg(UUID.randomUUID(), "Acme")));

        Page<Organization> result = organizationService.getAllOrganizations(null, null, "acme", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    /**
     * Verifies that when no filters are provided the service returns the repository's paged findAll
     * result directly.
     */
    @Test
    void getAllOrganizations_noFilters_returnsFindAllPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Organization> page = new PageImpl<>(List.of(buildOrg(UUID.randomUUID(), "Org1")));
        when(organizationRepository.findAll(pageable)).thenReturn(page);

        Page<Organization> result = organizationService.getAllOrganizations(null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    /**
     * Verifies that fetching an existing organization by id returns that organization instance.
     */
    @Test
    void getOrganizationById_existing_returnsOrganization() {
        UUID id = UUID.randomUUID();
        Organization org = buildOrg(id, "Org1");
        when(organizationRepository.findById(id)).thenReturn(Optional.of(org));

        assertThat(organizationService.getOrganizationById(id)).isSameAs(org);
    }

    /**
     * Verifies that fetching a non-existent organization by id throws
     * {@link ResourceNotFoundException}.
     */
    @Test
    void getOrganizationById_missing_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(organizationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.getOrganizationById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that {@code getOrganizationsByTenant} returns the repository's list for the tenant id
     * unchanged.
     */
    @Test
    void getOrganizationsByTenant_delegatesToRepository() {
        UUID tenantId = UUID.randomUUID();
        List<Organization> orgs = List.of(buildOrg(UUID.randomUUID(), "Org1"));
        when(organizationRepository.findByTenantId(tenantId)).thenReturn(orgs);

        assertThat(organizationService.getOrganizationsByTenant(tenantId)).isEqualTo(orgs);
    }

    /**
     * Verifies that when the current principal has an organization id, the corresponding
     * organization is looked up and returned.
     */
    @Test
    void getCurrentUserOrganization_withOrganization_returnsIt() {
        UUID orgId = UUID.randomUUID();
        setPrincipal(orgId);
        Organization org = buildOrg(orgId, "Org1");
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

        assertThat(organizationService.getCurrentUserOrganization()).isSameAs(org);
    }

    /**
     * Verifies that when the current principal has no organization id, requesting the current user's
     * organization throws {@link BusinessRuleException}.
     */
    @Test
    void getCurrentUserOrganization_noOrganization_throwsBusinessRuleException() {
        setPrincipal(null);

        assertThatThrownBy(() -> organizationService.getCurrentUserOrganization())
                .isInstanceOf(BusinessRuleException.class);
    }

    /**
     * Verifies that creating an organization with a unique name and email persists it with an ACTIVE
     * status and returns the saved instance.
     */
    @Test
    void createOrganization_uniqueNameAndEmail_savesOrganization() {
        OrganizationRequest request = buildRequest("Org1");
        request.setEmail("org1@example.com");

        when(organizationRepository.existsByName("Org1")).thenReturn(false);
        when(organizationRepository.existsByEmail("org1@example.com")).thenReturn(false);
        when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Organization result = organizationService.createOrganization(request);

        assertThat(result.getName()).isEqualTo("Org1");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(organizationRepository).save(any(Organization.class));
    }

    /**
     * Verifies that creating an organization with an already-used name throws
     * {@link DuplicateResourceException} and never saves.
     */
    @Test
    void createOrganization_duplicateName_throwsDuplicateResourceException() {
        OrganizationRequest request = buildRequest("Org1");
        when(organizationRepository.existsByName("Org1")).thenReturn(true);

        assertThatThrownBy(() -> organizationService.createOrganization(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void createOrganization_duplicateEmail_throwsDuplicateResourceException() {
        OrganizationRequest request = buildRequest("Org1");
        request.setEmail("dup@example.com");

        when(organizationRepository.existsByName("Org1")).thenReturn(false);
        when(organizationRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> organizationService.createOrganization(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void createOrganization_withTenant_associatesTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        OrganizationRequest request = buildRequest("Org1");
        request.setTenantId(tenantId);

        when(organizationRepository.existsByName("Org1")).thenReturn(false);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Organization result = organizationService.createOrganization(request);

        assertThat(result.getTenant()).isSameAs(tenant);
    }

    @Test
    void createOrganization_tenantNotFound_throwsResourceNotFoundException() {
        UUID tenantId = UUID.randomUUID();
        OrganizationRequest request = buildRequest("Org1");
        request.setTenantId(tenantId);

        when(organizationRepository.existsByName("Org1")).thenReturn(false);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.createOrganization(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateOrganization_sameNameAndEmail_updates() {
        UUID id = UUID.randomUUID();
        Organization org = buildOrg(id, "Org1");

        OrganizationRequest request = buildRequest("Org1");
        request.setEmail("Org1@example.com");
        request.setDescription("updated");

        when(organizationRepository.findById(id)).thenReturn(Optional.of(org));
        when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Organization result = organizationService.updateOrganization(id, request);

        assertThat(result.getDescription()).isEqualTo("updated");
    }

    @Test
    void updateOrganization_newDuplicateName_throwsDuplicateResourceException() {
        UUID id = UUID.randomUUID();
        Organization org = buildOrg(id, "Org1");

        OrganizationRequest request = buildRequest("Other");

        when(organizationRepository.findById(id)).thenReturn(Optional.of(org));
        when(organizationRepository.existsByName("Other")).thenReturn(true);

        assertThatThrownBy(() -> organizationService.updateOrganization(id, request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void updateOrganization_newDuplicateEmail_throwsDuplicateResourceException() {
        UUID id = UUID.randomUUID();
        Organization org = buildOrg(id, "Org1");

        OrganizationRequest request = buildRequest("Org1");
        request.setEmail("new@example.com");

        when(organizationRepository.findById(id)).thenReturn(Optional.of(org));
        when(organizationRepository.existsByEmail("new@example.com")).thenReturn(true);

        assertThatThrownBy(() -> organizationService.updateOrganization(id, request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void updateOrganization_changeTenant_reassignsTenant() {
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Organization org = buildOrg(id, "Org1");
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        OrganizationRequest request = buildRequest("Org1");
        request.setEmail("Org1@example.com");
        request.setTenantId(tenantId);

        when(organizationRepository.findById(id)).thenReturn(Optional.of(org));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Organization result = organizationService.updateOrganization(id, request);

        assertThat(result.getTenant()).isSameAs(tenant);
    }

    @Test
    void updateStatus_setsStatusAndSaves() {
        UUID id = UUID.randomUUID();
        Organization org = buildOrg(id, "Org1");

        when(organizationRepository.findById(id)).thenReturn(Optional.of(org));
        when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Organization result = organizationService.updateStatus(id, "SUSPENDED");

        assertThat(result.getStatus()).isEqualTo("SUSPENDED");
    }

    @Test
    void deleteOrganization_noUsers_deletes() {
        UUID id = UUID.randomUUID();
        Organization org = buildOrg(id, "Org1");

        when(organizationRepository.findById(id)).thenReturn(Optional.of(org));
        when(userRepository.countByOrganizationId(id)).thenReturn(0L);

        organizationService.deleteOrganization(id);

        verify(organizationRepository).delete(org);
    }

    @Test
    void deleteOrganization_withUsers_throwsBusinessRuleException() {
        UUID id = UUID.randomUUID();
        Organization org = buildOrg(id, "Org1");

        when(organizationRepository.findById(id)).thenReturn(Optional.of(org));
        when(userRepository.countByOrganizationId(id)).thenReturn(3L);

        assertThatThrownBy(() -> organizationService.deleteOrganization(id))
                .isInstanceOf(BusinessRuleException.class);
        verify(organizationRepository, never()).delete(any());
    }
}
