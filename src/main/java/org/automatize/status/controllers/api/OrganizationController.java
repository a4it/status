package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.OrganizationRequest;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.models.Organization;
import org.automatize.status.services.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API controller for organization management operations.
 * <p>
 * This controller provides endpoints for managing organizations within the multi-tenant
 * hierarchy. Organizations belong to tenants and contain users. Access to endpoints
 * is controlled based on user roles (ADMIN, MANAGER).
 * </p>
 *
 * @see OrganizationService
 * @see Organization
 */
@RestController
@RequestMapping("/api/organizations")
@PreAuthorize("isAuthenticated()")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

    /**
     * Retrieves a paginated list of all organizations with optional filtering.
     * <p>
     * This endpoint is restricted to users with ADMIN role.
     * </p>
     *
     * @param tenantId optional filter by tenant ID
     * @param status optional filter by organization status
     * @param search optional search term for filtering by name
     * @param pageable pagination parameters (page, size, sort)
     * @return ResponseEntity containing a page of organizations
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Organization>> getAllOrganizations(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<Organization> organizations = organizationService.getAllOrganizations(tenantId, status, search, pageable);
        return ResponseEntity.ok(organizations);
    }

    /**
     * Retrieves an organization by its unique identifier.
     *
     * @param id the UUID of the organization to retrieve
     * @return ResponseEntity containing the organization
     */
    @GetMapping("/{id}")
    public ResponseEntity<Organization> getOrganizationById(@PathVariable UUID id) {
        Organization organization = organizationService.getOrganizationById(id);
        return ResponseEntity.ok(organization);
    }

    /**
     * Creates a new organization.
     * <p>
     * This endpoint is restricted to users with ADMIN role.
     * </p>
     *
     * @param request the organization creation request containing organization details
     * @return ResponseEntity containing the created organization with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Organization> createOrganization(@Valid @RequestBody OrganizationRequest request) {
        Organization organization = organizationService.createOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(organization);
    }

    /**
     * Updates an existing organization.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the organization to update
     * @param request the organization update request containing new details
     * @return ResponseEntity containing the updated organization
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Organization> updateOrganization(
            @PathVariable UUID id,
            @Valid @RequestBody OrganizationRequest request) {
        Organization organization = organizationService.updateOrganization(id, request);
        return ResponseEntity.ok(organization);
    }

    /**
     * Deletes an organization by its unique identifier.
     * <p>
     * This endpoint is restricted to users with ADMIN role.
     * </p>
     *
     * @param id the UUID of the organization to delete
     * @return ResponseEntity containing a success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteOrganization(@PathVariable UUID id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.ok(new MessageResponse("Organization deleted successfully", true));
    }

    /**
     * Retrieves all organizations belonging to a specific tenant.
     * <p>
     * This endpoint is restricted to users with ADMIN role.
     * </p>
     *
     * @param tenantId the UUID of the tenant
     * @return ResponseEntity containing a list of organizations
     */
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Organization>> getOrganizationsByTenant(@PathVariable UUID tenantId) {
        List<Organization> organizations = organizationService.getOrganizationsByTenant(tenantId);
        return ResponseEntity.ok(organizations);
    }

    /**
     * Retrieves the organization of the currently authenticated user.
     *
     * @return ResponseEntity containing the current user's organization
     */
    @GetMapping("/current")
    public ResponseEntity<Organization> getCurrentUserOrganization() {
        Organization organization = organizationService.getCurrentUserOrganization();
        return ResponseEntity.ok(organization);
    }

    /**
     * Updates the status of an organization.
     * <p>
     * This endpoint is restricted to users with ADMIN role.
     * </p>
     *
     * @param id the UUID of the organization to update
     * @param status the new status value
     * @return ResponseEntity containing the updated organization
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Organization> updateOrganizationStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        Organization organization = organizationService.updateStatus(id, status);
        return ResponseEntity.ok(organization);
    }
}