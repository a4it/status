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

@RestController
@RequestMapping("/api/maintenance")
@PreAuthorize("isAuthenticated()")
public class StatusMaintenanceController {

    @Autowired
    private StatusMaintenanceService statusMaintenanceService;

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

    @GetMapping("/{id}")
    public ResponseEntity<StatusMaintenanceResponse> getMaintenanceById(@PathVariable UUID id) {
        StatusMaintenanceResponse maintenance = statusMaintenanceService.getMaintenanceById(id);
        return ResponseEntity.ok(maintenance);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusMaintenanceResponse> createMaintenance(@Valid @RequestBody StatusMaintenanceRequest request) {
        StatusMaintenanceResponse maintenance = statusMaintenanceService.createMaintenance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(maintenance);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusMaintenanceResponse> updateMaintenance(
            @PathVariable UUID id,
            @Valid @RequestBody StatusMaintenanceRequest request) {
        StatusMaintenanceResponse maintenance = statusMaintenanceService.updateMaintenance(id, request);
        return ResponseEntity.ok(maintenance);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> deleteMaintenance(@PathVariable UUID id) {
        statusMaintenanceService.deleteMaintenance(id);
        return ResponseEntity.ok(new MessageResponse("Maintenance deleted successfully", true));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusMaintenanceResponse> updateMaintenanceStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        StatusMaintenanceResponse maintenance = statusMaintenanceService.updateStatus(id, status);
        return ResponseEntity.ok(maintenance);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<StatusMaintenanceResponse>> getUpcomingMaintenance(
            @RequestParam(required = false) UUID appId,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(defaultValue = "30") int days) {
        List<StatusMaintenanceResponse> maintenances = statusMaintenanceService.getUpcomingMaintenance(appId, tenantId, days);
        return ResponseEntity.ok(maintenances);
    }

    @GetMapping("/active")
    public ResponseEntity<List<StatusMaintenanceResponse>> getActiveMaintenance(
            @RequestParam(required = false) UUID appId,
            @RequestParam(required = false) UUID tenantId) {
        List<StatusMaintenanceResponse> maintenances = statusMaintenanceService.getActiveMaintenance(appId, tenantId);
        return ResponseEntity.ok(maintenances);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusMaintenanceResponse> startMaintenance(@PathVariable UUID id) {
        StatusMaintenanceResponse maintenance = statusMaintenanceService.startMaintenance(id);
        return ResponseEntity.ok(maintenance);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusMaintenanceResponse> completeMaintenance(@PathVariable UUID id) {
        StatusMaintenanceResponse maintenance = statusMaintenanceService.completeMaintenance(id);
        return ResponseEntity.ok(maintenance);
    }
}