package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.StatusAppRequest;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.api.response.StatusAppResponse;
import org.automatize.status.services.StatusAppService;
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
 * <p>
 * REST API controller for status application management.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for status applications</li>
 *   <li>Handle filtering by tenant, organization, and search terms</li>
 *   <li>Manage application status updates with role-based access control</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see StatusAppService
 * @see StatusAppResponse
 */
@RestController
@RequestMapping("/api/status-apps")
@PreAuthorize("isAuthenticated()")
public class StatusAppController {

    @Autowired
    private StatusAppService statusAppService;

    /**
     * Retrieves a paginated list of all status applications with optional filtering.
     *
     * @param tenantId optional filter by tenant ID
     * @param organizationId optional filter by organization ID
     * @param search optional search term for filtering by name
     * @param pageable pagination parameters (page, size, sort)
     * @return ResponseEntity containing a page of status applications
     */
    @GetMapping
    public ResponseEntity<Page<StatusAppResponse>> getAllStatusApps(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<StatusAppResponse> apps = statusAppService.getAllStatusApps(tenantId, organizationId, search, pageable);
        return ResponseEntity.ok(apps);
    }

    /**
     * Retrieves a status application by its unique identifier.
     *
     * @param id the UUID of the status application
     * @return ResponseEntity containing the status application details
     */
    @GetMapping("/{id}")
    public ResponseEntity<StatusAppResponse> getStatusAppById(@PathVariable UUID id) {
        StatusAppResponse app = statusAppService.getStatusAppById(id);
        return ResponseEntity.ok(app);
    }

    /**
     * Creates a new status application.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param request the status application creation request
     * @return ResponseEntity containing the created status application with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusAppResponse> createStatusApp(@Valid @RequestBody StatusAppRequest request) {
        StatusAppResponse app = statusAppService.createStatusApp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(app);
    }

    /**
     * Updates an existing status application.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the status application to update
     * @param request the status application update request
     * @return ResponseEntity containing the updated status application
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusAppResponse> updateStatusApp(
            @PathVariable UUID id,
            @Valid @RequestBody StatusAppRequest request) {
        StatusAppResponse app = statusAppService.updateStatusApp(id, request);
        return ResponseEntity.ok(app);
    }

    /**
     * Deletes a status application by its unique identifier.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the status application to delete
     * @return ResponseEntity containing a success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> deleteStatusApp(@PathVariable UUID id) {
        statusAppService.deleteStatusApp(id);
        return ResponseEntity.ok(new MessageResponse("Status app deleted successfully", true));
    }

    /**
     * Updates the operational status of a status application.
     * <p>
     * This endpoint is accessible to users with ADMIN, MANAGER, or USER roles.
     * </p>
     *
     * @param id the UUID of the status application
     * @param status the new operational status value
     * @return ResponseEntity containing the updated status application
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<StatusAppResponse> updateStatusAppStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        StatusAppResponse app = statusAppService.updateStatus(id, status);
        return ResponseEntity.ok(app);
    }

    /**
     * Retrieves all status applications belonging to a specific tenant.
     *
     * @param tenantId the UUID of the tenant
     * @return ResponseEntity containing a list of status applications
     */
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<StatusAppResponse>> getStatusAppsByTenant(@PathVariable UUID tenantId) {
        List<StatusAppResponse> apps = statusAppService.getStatusAppsByTenant(tenantId);
        return ResponseEntity.ok(apps);
    }

    /**
     * Retrieves all status applications belonging to a specific organization.
     *
     * @param organizationId the UUID of the organization
     * @return ResponseEntity containing a list of status applications
     */
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<StatusAppResponse>> getStatusAppsByOrganization(@PathVariable UUID organizationId) {
        List<StatusAppResponse> apps = statusAppService.getStatusAppsByOrganization(organizationId);
        return ResponseEntity.ok(apps);
    }
}