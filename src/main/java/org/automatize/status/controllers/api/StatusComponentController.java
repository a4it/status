package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.StatusComponentRequest;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.api.response.StatusComponentResponse;
import org.automatize.status.services.StatusComponentService;
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
 * REST API controller for status component management.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for status components</li>
 *   <li>Handle component filtering, status updates, and reordering</li>
 *   <li>Manage role-based access control for component operations</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see StatusComponentService
 * @see StatusComponentResponse
 */
@RestController
@RequestMapping("/api/components")
@PreAuthorize("isAuthenticated()")
public class StatusComponentController {

    @Autowired
    private StatusComponentService statusComponentService;

    /**
     * Retrieves a paginated list of all status components with optional filtering.
     *
     * @param appId optional filter by status application ID
     * @param search optional search term for filtering by name
     * @param pageable pagination parameters (page, size, sort)
     * @return ResponseEntity containing a page of status components
     */
    @GetMapping
    public ResponseEntity<Page<StatusComponentResponse>> getAllComponents(
            @RequestParam(required = false) UUID appId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<StatusComponentResponse> components = statusComponentService.getAllComponents(appId, search, pageable);
        return ResponseEntity.ok(components);
    }

    /**
     * Retrieves a status component by its unique identifier.
     *
     * @param id the UUID of the status component
     * @return ResponseEntity containing the status component details
     */
    @GetMapping("/{id}")
    public ResponseEntity<StatusComponentResponse> getComponentById(@PathVariable UUID id) {
        StatusComponentResponse component = statusComponentService.getComponentById(id);
        return ResponseEntity.ok(component);
    }

    /**
     * Creates a new status component.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param request the status component creation request
     * @return ResponseEntity containing the created status component with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusComponentResponse> createComponent(@Valid @RequestBody StatusComponentRequest request) {
        StatusComponentResponse component = statusComponentService.createComponent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(component);
    }

    /**
     * Updates an existing status component.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the status component to update
     * @param request the status component update request
     * @return ResponseEntity containing the updated status component
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusComponentResponse> updateComponent(
            @PathVariable UUID id,
            @Valid @RequestBody StatusComponentRequest request) {
        StatusComponentResponse component = statusComponentService.updateComponent(id, request);
        return ResponseEntity.ok(component);
    }

    /**
     * Deletes a status component by its unique identifier.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the status component to delete
     * @return ResponseEntity containing a success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> deleteComponent(@PathVariable UUID id) {
        statusComponentService.deleteComponent(id);
        return ResponseEntity.ok(new MessageResponse("Component deleted successfully", true));
    }

    /**
     * Updates the operational status of a status component.
     * <p>
     * This endpoint is accessible to users with ADMIN, MANAGER, or USER roles.
     * </p>
     *
     * @param id the UUID of the status component
     * @param status the new operational status value
     * @return ResponseEntity containing the updated status component
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<StatusComponentResponse> updateComponentStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        StatusComponentResponse component = statusComponentService.updateStatus(id, status);
        return ResponseEntity.ok(component);
    }

    /**
     * Retrieves all status components belonging to a specific status application.
     *
     * @param appId the UUID of the status application
     * @return ResponseEntity containing a list of status components
     */
    @GetMapping("/app/{appId}")
    public ResponseEntity<List<StatusComponentResponse>> getComponentsByApp(@PathVariable UUID appId) {
        List<StatusComponentResponse> components = statusComponentService.getComponentsByApp(appId);
        return ResponseEntity.ok(components);
    }

    /**
     * Reorders status components within their parent application.
     * <p>
     * This endpoint accepts a list of component order requests specifying the new
     * display position for each component. This endpoint is restricted to users
     * with ADMIN or MANAGER roles.
     * </p>
     *
     * @param orderRequests list of component order requests containing component IDs and new positions
     * @return ResponseEntity containing a success message
     */
    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> reorderComponents(@RequestBody List<ComponentOrderRequest> orderRequests) {
        statusComponentService.reorderComponents(orderRequests);
        return ResponseEntity.ok(new MessageResponse("Components reordered successfully", true));
    }
}