package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.StatusMaintenanceRequest;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.api.response.StatusMaintenanceResponse;
import org.automatize.status.services.StatusMaintenanceService;
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
 * REST API controller for scheduled maintenance management.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for maintenance windows</li>
 *   <li>Handle maintenance lifecycle management (scheduled, in-progress, completed)</li>
 *   <li>Manage role-based access control for maintenance operations</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see StatusMaintenanceService
 * @see StatusMaintenanceResponse
 */
@RestController
@RequestMapping("/api/maintenance")
@PreAuthorize("isAuthenticated()")
public class StatusMaintenanceController {

    @Autowired
    private StatusMaintenanceService statusMaintenanceService;

    /**
     * Retrieves a paginated list of all maintenance windows with optional filtering.
     *
     * @param appId optional filter by status application ID
     * @param status optional filter by maintenance status
     * @param startDate optional filter for maintenance windows starting after this date
     * @param endDate optional filter for maintenance windows ending before this date
     * @param search optional search term for filtering by title or description
     * @param pageable pagination parameters (page, size, sort)
     * @return ResponseEntity containing a page of maintenance windows
     */
    @GetMapping
    public ResponseEntity<Page<StatusMaintenanceResponse>> getAllMaintenance(
            @RequestParam(required = false) UUID appId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<StatusMaintenanceResponse> maintenances = statusMaintenanceService.getAllMaintenance(
                appId, status, startDate, endDate, search, pageable);
        return ResponseEntity.ok(maintenances);
    }

    /**
     * Retrieves a maintenance window by its unique identifier.
     *
     * @param id the UUID of the maintenance window
     * @return ResponseEntity containing the maintenance window details
     */
    @GetMapping("/{id}")
    public ResponseEntity<StatusMaintenanceResponse> getMaintenanceById(@PathVariable UUID id) {
        StatusMaintenanceResponse maintenance = statusMaintenanceService.getMaintenanceById(id);
        return ResponseEntity.ok(maintenance);
    }

    /**
     * Creates a new scheduled maintenance window.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param request the maintenance creation request containing maintenance details
     * @return ResponseEntity containing the created maintenance window with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusMaintenanceResponse> createMaintenance(@Valid @RequestBody StatusMaintenanceRequest request) {
        StatusMaintenanceResponse maintenance = statusMaintenanceService.createMaintenance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(maintenance);
    }

    /**
     * Updates an existing maintenance window.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the maintenance window to update
     * @param request the maintenance update request containing new details
     * @return ResponseEntity containing the updated maintenance window
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusMaintenanceResponse> updateMaintenance(
            @PathVariable UUID id,
            @Valid @RequestBody StatusMaintenanceRequest request) {
        StatusMaintenanceResponse maintenance = statusMaintenanceService.updateMaintenance(id, request);
        return ResponseEntity.ok(maintenance);
    }

    /**
     * Deletes a maintenance window by its unique identifier.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the maintenance window to delete
     * @return ResponseEntity containing a success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> deleteMaintenance(@PathVariable UUID id) {
        statusMaintenanceService.deleteMaintenance(id);
        return ResponseEntity.ok(new MessageResponse("Maintenance deleted successfully", true));
    }

    /**
     * Updates the status of a maintenance window.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the maintenance window
     * @param status the new status value
     * @return ResponseEntity containing the updated maintenance window
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusMaintenanceResponse> updateMaintenanceStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        StatusMaintenanceResponse maintenance = statusMaintenanceService.updateStatus(id, status);
        return ResponseEntity.ok(maintenance);
    }

    /**
     * Retrieves upcoming scheduled maintenance windows within a time range.
     *
     * @param appId optional filter by status application ID
     * @param tenantId optional filter by tenant ID
     * @param days the number of days in the future to look (default: 30)
     * @return ResponseEntity containing a list of upcoming maintenance windows
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<StatusMaintenanceResponse>> getUpcomingMaintenance(
            @RequestParam(required = false) UUID appId,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(defaultValue = "30") int days) {
        List<StatusMaintenanceResponse> maintenances = statusMaintenanceService.getUpcomingMaintenance(appId, tenantId, days);
        return ResponseEntity.ok(maintenances);
    }

    /**
     * Retrieves currently active (in-progress) maintenance windows.
     *
     * @param appId optional filter by status application ID
     * @param tenantId optional filter by tenant ID
     * @return ResponseEntity containing a list of active maintenance windows
     */
    @GetMapping("/active")
    public ResponseEntity<List<StatusMaintenanceResponse>> getActiveMaintenance(
            @RequestParam(required = false) UUID appId,
            @RequestParam(required = false) UUID tenantId) {
        List<StatusMaintenanceResponse> maintenances = statusMaintenanceService.getActiveMaintenance(appId, tenantId);
        return ResponseEntity.ok(maintenances);
    }

    /**
     * Starts a scheduled maintenance window.
     * <p>
     * This transitions the maintenance window from scheduled to in-progress status.
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the maintenance window to start
     * @return ResponseEntity containing the updated maintenance window
     */
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusMaintenanceResponse> startMaintenance(@PathVariable UUID id) {
        StatusMaintenanceResponse maintenance = statusMaintenanceService.startMaintenance(id);
        return ResponseEntity.ok(maintenance);
    }

    /**
     * Completes an active maintenance window.
     * <p>
     * This transitions the maintenance window from in-progress to completed status.
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the maintenance window to complete
     * @return ResponseEntity containing the updated maintenance window
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusMaintenanceResponse> completeMaintenance(@PathVariable UUID id) {
        StatusMaintenanceResponse maintenance = statusMaintenanceService.completeMaintenance(id);
        return ResponseEntity.ok(maintenance);
    }
}