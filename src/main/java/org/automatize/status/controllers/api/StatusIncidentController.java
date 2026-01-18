package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.StatusIncidentRequest;
import org.automatize.status.api.request.StatusIncidentUpdateRequest;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.api.response.StatusIncidentResponse;
import org.automatize.status.api.response.StatusIncidentUpdateResponse;
import org.automatize.status.services.StatusIncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * REST API controller for incident management.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for status incidents</li>
 *   <li>Handle incident updates, resolution, and filtering</li>
 *   <li>Manage role-based access control for incident operations</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see StatusIncidentService
 * @see StatusIncidentResponse
 */
@RestController
@RequestMapping("/api/incidents")
@PreAuthorize("isAuthenticated()")
public class StatusIncidentController {

    @Autowired
    private StatusIncidentService statusIncidentService;

    /**
     * Retrieves a paginated list of all incidents with optional filtering.
     *
     * @param appId optional filter by status application ID
     * @param status optional filter by incident status
     * @param startDate optional filter for incidents starting after this date
     * @param endDate optional filter for incidents ending before this date
     * @param search optional search term for filtering by title or description
     * @param pageable pagination parameters (page, size, sort)
     * @return ResponseEntity containing a page of incidents
     */
    @GetMapping
    public ResponseEntity<Page<StatusIncidentResponse>> getAllIncidents(
            @RequestParam(required = false) UUID appId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<StatusIncidentResponse> incidents = statusIncidentService.getAllIncidents(
                appId, status, startDate, endDate, search, pageable);
        return ResponseEntity.ok(incidents);
    }

    /**
     * Retrieves an incident by its unique identifier.
     *
     * @param id the UUID of the incident
     * @return ResponseEntity containing the incident details
     */
    @GetMapping("/{id}")
    public ResponseEntity<StatusIncidentResponse> getIncidentById(@PathVariable UUID id) {
        StatusIncidentResponse incident = statusIncidentService.getIncidentById(id);
        return ResponseEntity.ok(incident);
    }

    /**
     * Creates a new incident.
     * <p>
     * This endpoint is accessible to users with ADMIN, MANAGER, or USER roles.
     * </p>
     *
     * @param request the incident creation request containing incident details
     * @return ResponseEntity containing the created incident with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<StatusIncidentResponse> createIncident(@Valid @RequestBody StatusIncidentRequest request) {
        StatusIncidentResponse incident = statusIncidentService.createIncident(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(incident);
    }

    /**
     * Updates an existing incident.
     * <p>
     * This endpoint is accessible to users with ADMIN, MANAGER, or USER roles.
     * </p>
     *
     * @param id the UUID of the incident to update
     * @param request the incident update request containing new details
     * @return ResponseEntity containing the updated incident
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<StatusIncidentResponse> updateIncident(
            @PathVariable UUID id,
            @Valid @RequestBody StatusIncidentRequest request) {
        StatusIncidentResponse incident = statusIncidentService.updateIncident(id, request);
        return ResponseEntity.ok(incident);
    }

    /**
     * Deletes an incident by its unique identifier.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the incident to delete
     * @return ResponseEntity containing a success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> deleteIncident(@PathVariable UUID id) {
        statusIncidentService.deleteIncident(id);
        return ResponseEntity.ok(new MessageResponse("Incident deleted successfully", true));
    }

    /**
     * Adds a status update to an existing incident.
     * <p>
     * Incident updates are used to communicate progress, changes in status,
     * or additional information during incident resolution. This endpoint is
     * accessible to users with ADMIN, MANAGER, or USER roles.
     * </p>
     *
     * @param id the UUID of the incident to update
     * @param request the incident update request containing the update message
     * @return ResponseEntity containing the created incident update with HTTP 201 status
     */
    @PostMapping("/{id}/updates")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<StatusIncidentUpdateResponse> addIncidentUpdate(
            @PathVariable UUID id,
            @Valid @RequestBody StatusIncidentUpdateRequest request) {
        StatusIncidentUpdateResponse update = statusIncidentService.addIncidentUpdate(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(update);
    }

    /**
     * Retrieves all status updates for a specific incident.
     *
     * @param id the UUID of the incident
     * @return ResponseEntity containing a list of incident updates
     */
    @GetMapping("/{id}/updates")
    public ResponseEntity<List<StatusIncidentUpdateResponse>> getIncidentUpdates(@PathVariable UUID id) {
        List<StatusIncidentUpdateResponse> updates = statusIncidentService.getIncidentUpdates(id);
        return ResponseEntity.ok(updates);
    }

    /**
     * Marks an incident as resolved.
     * <p>
     * This endpoint sets the incident status to resolved and optionally adds
     * a resolution message. This endpoint is accessible to users with ADMIN,
     * MANAGER, or USER roles.
     * </p>
     *
     * @param id the UUID of the incident to resolve
     * @param message optional resolution message to add as the final update
     * @return ResponseEntity containing the resolved incident
     */
    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<StatusIncidentResponse> resolveIncident(
            @PathVariable UUID id,
            @RequestBody(required = false) String message) {
        StatusIncidentResponse incident = statusIncidentService.resolveIncident(id, message);
        return ResponseEntity.ok(incident);
    }

    /**
     * Retrieves all currently active (unresolved) incidents.
     *
     * @param appId optional filter by status application ID
     * @param tenantId optional filter by tenant ID
     * @return ResponseEntity containing a list of active incidents
     */
    @GetMapping("/active")
    public ResponseEntity<List<StatusIncidentResponse>> getActiveIncidents(
            @RequestParam(required = false) UUID appId,
            @RequestParam(required = false) UUID tenantId) {
        List<StatusIncidentResponse> incidents = statusIncidentService.getActiveIncidents(appId, tenantId);
        return ResponseEntity.ok(incidents);
    }
}