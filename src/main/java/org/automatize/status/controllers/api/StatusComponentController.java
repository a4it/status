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

@RestController
@RequestMapping("/api/components")
@PreAuthorize("isAuthenticated()")
public class StatusComponentController {

    @Autowired
    private StatusComponentService statusComponentService;

    @GetMapping
    public ResponseEntity<Page<StatusComponentResponse>> getAllComponents(
            @RequestParam(required = false) UUID appId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<StatusComponentResponse> components = statusComponentService.getAllComponents(appId, search, pageable);
        return ResponseEntity.ok(components);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StatusComponentResponse> getComponentById(@PathVariable UUID id) {
        StatusComponentResponse component = statusComponentService.getComponentById(id);
        return ResponseEntity.ok(component);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusComponentResponse> createComponent(@Valid @RequestBody StatusComponentRequest request) {
        StatusComponentResponse component = statusComponentService.createComponent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(component);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusComponentResponse> updateComponent(
            @PathVariable UUID id,
            @Valid @RequestBody StatusComponentRequest request) {
        StatusComponentResponse component = statusComponentService.updateComponent(id, request);
        return ResponseEntity.ok(component);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> deleteComponent(@PathVariable UUID id) {
        statusComponentService.deleteComponent(id);
        return ResponseEntity.ok(new MessageResponse("Component deleted successfully", true));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<StatusComponentResponse> updateComponentStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        StatusComponentResponse component = statusComponentService.updateStatus(id, status);
        return ResponseEntity.ok(component);
    }

    @GetMapping("/app/{appId}")
    public ResponseEntity<List<StatusComponentResponse>> getComponentsByApp(@PathVariable UUID appId) {
        List<StatusComponentResponse> components = statusComponentService.getComponentsByApp(appId);
        return ResponseEntity.ok(components);
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> reorderComponents(@RequestBody List<ComponentOrderRequest> orderRequests) {
        statusComponentService.reorderComponents(orderRequests);
        return ResponseEntity.ok(new MessageResponse("Components reordered successfully", true));
    }
}