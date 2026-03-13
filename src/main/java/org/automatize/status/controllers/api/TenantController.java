package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.TenantRequest;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.models.Tenant;
import org.automatize.status.services.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * <p>
 * REST API controller for tenant management operations.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for tenants</li>
 *   <li>Handle tenant filtering and search functionality</li>
 *   <li>Restrict access to ADMIN role only</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see TenantService
 * @see Tenant
 */
@RestController
@RequestMapping("/api/tenants")
@PreAuthorize("hasRole('ADMIN')")
public class TenantController {

    @Autowired
    private TenantService tenantService;

    /**
     * Retrieves a paginated list of all tenants with optional search filtering.
     *
     * @param search optional search term for filtering by name
     * @param pageable pagination parameters (page, size, sort)
     * @return ResponseEntity containing a page of tenants
     */
    @GetMapping
    public ResponseEntity<Page<Tenant>> getAllTenants(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<Tenant> tenants = tenantService.getAllTenants(search, pageable);
        return ResponseEntity.ok(tenants);
    }

    /**
     * Retrieves a tenant by its unique identifier.
     *
     * @param id the UUID of the tenant
     * @return ResponseEntity containing the tenant details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Tenant> getTenantById(@PathVariable UUID id) {
        Tenant tenant = tenantService.getTenantById(id);
        return ResponseEntity.ok(tenant);
    }

    /**
     * Creates a new tenant.
     *
     * @param request the tenant creation request containing tenant details
     * @return ResponseEntity containing the created tenant with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<Tenant> createTenant(@Valid @RequestBody TenantRequest request) {
        Tenant tenant = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(tenant);
    }

    /**
     * Updates an existing tenant.
     *
     * @param id the UUID of the tenant to update
     * @param request the tenant update request containing new details
     * @return ResponseEntity containing the updated tenant
     */
    @PutMapping("/{id}")
    public ResponseEntity<Tenant> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody TenantRequest request) {
        Tenant tenant = tenantService.updateTenant(id, request);
        return ResponseEntity.ok(tenant);
    }

    /**
     * Deletes a tenant by its unique identifier.
     * <p>
     * Note: Deleting a tenant will cascade to all associated organizations and users.
     * </p>
     *
     * @param id the UUID of the tenant to delete
     * @return ResponseEntity containing a success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteTenant(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.ok(new MessageResponse("Tenant deleted successfully", true));
    }

    /**
     * Retrieves a tenant by its unique name.
     *
     * @param name the unique name of the tenant
     * @return ResponseEntity containing the tenant details
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<Tenant> getTenantByName(@PathVariable String name) {
        Tenant tenant = tenantService.getTenantByName(name);
        return ResponseEntity.ok(tenant);
    }
}