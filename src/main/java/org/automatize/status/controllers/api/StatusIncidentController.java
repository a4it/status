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

@RestController
@RequestMapping("/api/incidents")
@PreAuthorize("isAuthenticated()")
public class StatusIncidentController {

    @Autowired
    private StatusIncidentService statusIncidentService;

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

    @GetMapping("/{id}")
    public ResponseEntity<StatusIncidentResponse> getIncidentById(@PathVariable UUID id) {
        StatusIncidentResponse incident = statusIncidentService.getIncidentById(id);
        return ResponseEntity.ok(incident);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<StatusIncidentResponse> createIncident(@Valid @RequestBody StatusIncidentRequest request) {
        StatusIncidentResponse incident = statusIncidentService.createIncident(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(incident);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<StatusIncidentResponse> updateIncident(
            @PathVariable UUID id,
            @Valid @RequestBody StatusIncidentRequest request) {
        StatusIncidentResponse incident = statusIncidentService.updateIncident(id, request);
        return ResponseEntity.ok(incident);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> deleteIncident(@PathVariable UUID id) {
        statusIncidentService.deleteIncident(id);
        return ResponseEntity.ok(new MessageResponse("Incident deleted successfully", true));
    }

    @PostMapping("/{id}/updates")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<StatusIncidentUpdateResponse> addIncidentUpdate(
            @PathVariable UUID id,
            @Valid @RequestBody StatusIncidentUpdateRequest request) {
        StatusIncidentUpdateResponse update = statusIncidentService.addIncidentUpdate(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(update);
    }

    @GetMapping("/{id}/updates")
    public ResponseEntity<List<StatusIncidentUpdateResponse>> getIncidentUpdates(@PathVariable UUID id) {
        List<StatusIncidentUpdateResponse> updates = statusIncidentService.getIncidentUpdates(id);
        return ResponseEntity.ok(updates);
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<StatusIncidentResponse> resolveIncident(
            @PathVariable UUID id,
            @RequestBody(required = false) String message) {
        StatusIncidentResponse incident = statusIncidentService.resolveIncident(id, message);
        return ResponseEntity.ok(incident);
    }

    @GetMapping("/active")
    public ResponseEntity<List<StatusIncidentResponse>> getActiveIncidents(
            @RequestParam(required = false) UUID appId,
            @RequestParam(required = false) UUID tenantId) {
        List<StatusIncidentResponse> incidents = statusIncidentService.getActiveIncidents(appId, tenantId);
        return ResponseEntity.ok(incidents);
    }
}