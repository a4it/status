package org.automatize.status.services;

import org.automatize.status.api.request.OrganizationRequest;
import org.automatize.status.models.Organization;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.TenantRepository;
import org.automatize.status.repositories.UserRepository;
import org.automatize.status.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service responsible for managing organization entities within the multi-tenant hierarchy.
 * <p>
 * Organizations represent the middle tier in the Tenant -> Organization -> User hierarchy.
 * This service provides CRUD operations for organizations, including tenant association,
 * status management, and validation of business rules such as unique names and emails.
 * </p>
 *
 * @author Status Monitoring Team
 * @since 1.0
 */
@Service
@Transactional
public class OrganizationService {

    /**
     * Repository for organization data access operations.
     */
    @Autowired
    private OrganizationRepository organizationRepository;

    /**
     * Repository for tenant data access operations.
     */
    @Autowired
    private TenantRepository tenantRepository;

    /**
     * Repository for user data access operations.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Retrieves a paginated list of organizations with optional filtering.
     * <p>
     * Supports filtering by tenant ID, status, or search term. When multiple
     * filters are provided, they are applied in priority order.
     * </p>
     *
     * @param tenantId optional tenant ID to filter organizations
     * @param status optional status to filter organizations
     * @param search optional search term for name matching
     * @param pageable pagination information
     * @return a page of Organization entities matching the criteria
     */
    @Transactional(readOnly = true)
    public Page<Organization> getAllOrganizations(UUID tenantId, String status, String search, Pageable pageable) {
        List<Organization> organizations;
        
        if (tenantId != null && status != null) {
            organizations = organizationRepository.findByTenantIdAndStatus(tenantId, status);
        } else if (tenantId != null) {
            organizations = organizationRepository.findByTenantId(tenantId);
        } else if (status != null) {
            organizations = organizationRepository.findByStatus(status);
        } else if (search != null && !search.isEmpty()) {
            organizations = organizationRepository.search(search);
        } else {
            return organizationRepository.findAll(pageable);
        }
        
        return new PageImpl<>(organizations, pageable, organizations.size());
    }

    /**
     * Retrieves an organization by its unique identifier.
     *
     * @param id the UUID of the organization to retrieve
     * @return the Organization entity
     * @throws RuntimeException if no organization is found with the given ID
     */
    @Transactional(readOnly = true)
    public Organization getOrganizationById(UUID id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found with id: " + id));
    }

    /**
     * Retrieves all organizations belonging to a specific tenant.
     *
     * @param tenantId the UUID of the tenant
     * @return a list of Organization entities belonging to the tenant
     */
    @Transactional(readOnly = true)
    public List<Organization> getOrganizationsByTenant(UUID tenantId) {
        return organizationRepository.findByTenantId(tenantId);
    }

    /**
     * Retrieves the organization of the currently authenticated user.
     *
     * @return the Organization entity associated with the current user
     * @throws RuntimeException if the user does not belong to any organization
     */
    @Transactional(readOnly = true)
    public Organization getCurrentUserOrganization() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID organizationId = principal.getOrganizationId();
        
        if (organizationId == null) {
            throw new RuntimeException("User does not belong to any organization");
        }
        
        return getOrganizationById(organizationId);
    }

    /**
     * Creates a new organization with the provided details.
     * <p>
     * Validates that the organization name and email (if provided) are unique
     * before creating the organization.
     * </p>
     *
     * @param request the organization creation request containing organization details
     * @return the newly created Organization entity
     * @throws RuntimeException if an organization with the same name or email already exists
     * @throws RuntimeException if the specified tenant is not found
     */
    public Organization createOrganization(OrganizationRequest request) {
        if (organizationRepository.existsByName(request.getName())) {
            throw new RuntimeException("Organization with name already exists: " + request.getName());
        }

        if (request.getEmail() != null && organizationRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Organization with email already exists: " + request.getEmail());
        }

        Organization organization = new Organization();
        mapRequestToOrganization(request, organization);
        
        if (request.getTenantId() != null) {
            Tenant tenant = tenantRepository.findById(request.getTenantId())
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));
            organization.setTenant(tenant);
        }
        
        String currentUser = getCurrentUsername();
        organization.setCreatedBy(currentUser);
        organization.setLastModifiedBy(currentUser);

        return organizationRepository.save(organization);
    }

    /**
     * Updates an existing organization with the provided details.
     * <p>
     * Validates that the new name and email (if changed) do not conflict with
     * existing organizations.
     * </p>
     *
     * @param id the UUID of the organization to update
     * @param request the organization update request containing new details
     * @return the updated Organization entity
     * @throws RuntimeException if the organization is not found
     * @throws RuntimeException if an organization with the same name or email already exists
     */
    public Organization updateOrganization(UUID id, OrganizationRequest request) {
        Organization organization = getOrganizationById(id);

        if (!organization.getName().equals(request.getName()) && 
            organizationRepository.existsByName(request.getName())) {
            throw new RuntimeException("Organization with name already exists: " + request.getName());
        }

        if (request.getEmail() != null && 
            !request.getEmail().equals(organization.getEmail()) && 
            organizationRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Organization with email already exists: " + request.getEmail());
        }

        mapRequestToOrganization(request, organization);
        
        if (request.getTenantId() != null && 
            (organization.getTenant() == null || !organization.getTenant().getId().equals(request.getTenantId()))) {
            Tenant tenant = tenantRepository.findById(request.getTenantId())
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));
            organization.setTenant(tenant);
        }
        
        organization.setLastModifiedBy(getCurrentUsername());

        return organizationRepository.save(organization);
    }

    /**
     * Updates the status of an organization.
     *
     * @param id the UUID of the organization to update
     * @param status the new status value
     * @return the updated Organization entity
     * @throws RuntimeException if the organization is not found
     */
    public Organization updateStatus(UUID id, String status) {
        Organization organization = getOrganizationById(id);
        organization.setStatus(status);
        organization.setLastModifiedBy(getCurrentUsername());
        return organizationRepository.save(organization);
    }

    /**
     * Deletes an organization by its unique identifier.
     * <p>
     * This method will fail if the organization has any active users associated with it.
     * </p>
     *
     * @param id the UUID of the organization to delete
     * @throws RuntimeException if the organization is not found
     * @throws RuntimeException if the organization has active users
     */
    public void deleteOrganization(UUID id) {
        Organization organization = getOrganizationById(id);
        
        Long userCount = userRepository.countByOrganizationId(id);
        if (userCount > 0) {
            throw new RuntimeException("Cannot delete organization with active users");
        }
        
        organizationRepository.delete(organization);
    }

    /**
     * Maps fields from an OrganizationRequest to an Organization entity.
     *
     * @param request the source request containing organization data
     * @param organization the target Organization entity to populate
     */
    private void mapRequestToOrganization(OrganizationRequest request, Organization organization) {
        organization.setName(request.getName());
        organization.setDescription(request.getDescription());
        organization.setEmail(request.getEmail());
        organization.setPhone(request.getPhone());
        organization.setWebsite(request.getWebsite());
        organization.setAddress(request.getAddress());
        organization.setLogoUrl(request.getLogoUrl());
        organization.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        organization.setOrganizationType(request.getOrganizationType());
        organization.setVatNumber(request.getVatNumber());
        organization.setCountry(request.getCountry());
        organization.setPostalCode(request.getPostalCode());
        organization.setCommunity(request.getCommunity());
        organization.setType(request.getType());
        organization.setSubscriptionExempt(request.getSubscriptionExempt() != null ? request.getSubscriptionExempt() : false);
        organization.setThrottlingEnabled(request.getThrottlingEnabled() != null ? request.getThrottlingEnabled() : true);
    }

    /**
     * Retrieves the username of the currently authenticated user.
     *
     * @return the username, or "system" if no user is authenticated
     */
    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            return ((UserPrincipal) principal).getUsername();
        }
        return "system";
    }
}