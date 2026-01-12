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

@RestController
@RequestMapping("/api/status-apps")
@PreAuthorize("isAuthenticated()")
public class StatusAppController {

    @Autowired
    private StatusAppService statusAppService;

    @GetMapping
    public ResponseEntity<Page<StatusAppResponse>> getAllStatusApps(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<StatusAppResponse> apps = statusAppService.getAllStatusApps(tenantId, organizationId, search, pageable);
        return ResponseEntity.ok(apps);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StatusAppResponse> getStatusAppById(@PathVariable UUID id) {
        StatusAppResponse app = statusAppService.getStatusAppById(id);
        return ResponseEntity.ok(app);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusAppResponse> createStatusApp(@Valid @RequestBody StatusAppRequest request) {
        StatusAppResponse app = statusAppService.createStatusApp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(app);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusAppResponse> updateStatusApp(
            @PathVariable UUID id,
            @Valid @RequestBody StatusAppRequest request) {
        StatusAppResponse app = statusAppService.updateStatusApp(id, request);
        return ResponseEntity.ok(app);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> deleteStatusApp(@PathVariable UUID id) {
        statusAppService.deleteStatusApp(id);
        return ResponseEntity.ok(new MessageResponse("Status app deleted successfully", true));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<StatusAppResponse> updateStatusAppStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        StatusAppResponse app = statusAppService.updateStatus(id, status);
        return ResponseEntity.ok(app);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<StatusAppResponse>> getStatusAppsByTenant(@PathVariable UUID tenantId) {
        List<StatusAppResponse> apps = statusAppService.getStatusAppsByTenant(tenantId);
        return ResponseEntity.ok(apps);
    }

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<StatusAppResponse>> getStatusAppsByOrganization(@PathVariable UUID organizationId) {
        List<StatusAppResponse> apps = statusAppService.getStatusAppsByOrganization(organizationId);
        return ResponseEntity.ok(apps);
    }
}