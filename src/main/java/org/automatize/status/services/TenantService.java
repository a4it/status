package org.automatize.status.services;

import org.automatize.status.api.request.TenantRequest;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.TenantRepository;
import org.automatize.status.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service responsible for managing tenant entities.
 * <p>
 * Tenants represent the top level of the multi-tenant hierarchy (Tenant -> Organization -> User).
 * This service provides CRUD operations for tenants, including name validation and
 * active/inactive status management.
 * </p>
 *
 * @author Status Monitoring Team
 * @since 1.0
 */
@Service
@Transactional
public class TenantService {

    /**
     * Repository for tenant data access operations.
     */
    @Autowired
    private TenantRepository tenantRepository;

    /**
     * Retrieves a paginated list of tenants with optional search filtering.
     *
     * @param search optional search term (currently not implemented in repository)
     * @param pageable pagination information
     * @return a page of Tenant entities
     */
    @Transactional(readOnly = true)
    public Page<Tenant> getAllTenants(String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return tenantRepository.findAll(pageable); // Should implement search in repository
        }
        return tenantRepository.findAll(pageable);
    }

    /**
     * Retrieves a tenant by its unique identifier.
     *
     * @param id the UUID of the tenant
     * @return the Tenant entity
     * @throws RuntimeException if the tenant is not found
     */
    @Transactional(readOnly = true)
    public Tenant getTenantById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found with id: " + id));
    }

    /**
     * Retrieves a tenant by its name.
     *
     * @param name the name of the tenant
     * @return the Tenant entity
     * @throws RuntimeException if the tenant is not found
     */
    @Transactional(readOnly = true)
    public Tenant getTenantByName(String name) {
        return tenantRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Tenant not found with name: " + name));
    }

    /**
     * Creates a new tenant with the provided details.
     * <p>
     * Validates that the tenant name is unique before creating.
     * </p>
     *
     * @param request the tenant creation request containing name and active status
     * @return the newly created Tenant entity
     * @throws RuntimeException if a tenant with the same name already exists
     */
    public Tenant createTenant(TenantRequest request) {
        if (tenantRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tenant with name already exists: " + request.getName());
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.getName());
        tenant.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        String currentUser = getCurrentUsername();
        tenant.setCreatedBy(currentUser);
        tenant.setLastModifiedBy(currentUser);

        return tenantRepository.save(tenant);
    }

    /**
     * Updates an existing tenant with the provided details.
     * <p>
     * Validates that the new name (if changed) is unique before updating.
     * </p>
     *
     * @param id the UUID of the tenant to update
     * @param request the tenant update request
     * @return the updated Tenant entity
     * @throws RuntimeException if the tenant is not found
     * @throws RuntimeException if a tenant with the new name already exists
     */
    public Tenant updateTenant(UUID id, TenantRequest request) {
        Tenant tenant = getTenantById(id);

        if (!tenant.getName().equals(request.getName()) &&
            tenantRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tenant with name already exists: " + request.getName());
        }

        tenant.setName(request.getName());
        tenant.setIsActive(request.getIsActive() != null ? request.getIsActive() : tenant.getIsActive());
        tenant.setLastModifiedBy(getCurrentUsername());

        return tenantRepository.save(tenant);
    }

    /**
     * Deletes a tenant by its unique identifier.
     *
     * @param id the UUID of the tenant to delete
     * @throws RuntimeException if the tenant is not found
     */
    public void deleteTenant(UUID id) {
        Tenant tenant = getTenantById(id);
        tenantRepository.delete(tenant);
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