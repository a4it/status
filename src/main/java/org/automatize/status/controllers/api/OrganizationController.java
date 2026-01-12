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

@RestController
@RequestMapping("/api/organizations")
@PreAuthorize("isAuthenticated()")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

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

    @GetMapping("/{id}")
    public ResponseEntity<Organization> getOrganizationById(@PathVariable UUID id) {
        Organization organization = organizationService.getOrganizationById(id);
        return ResponseEntity.ok(organization);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Organization> createOrganization(@Valid @RequestBody OrganizationRequest request) {
        Organization organization = organizationService.createOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(organization);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Organization> updateOrganization(
            @PathVariable UUID id,
            @Valid @RequestBody OrganizationRequest request) {
        Organization organization = organizationService.updateOrganization(id, request);
        return ResponseEntity.ok(organization);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteOrganization(@PathVariable UUID id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.ok(new MessageResponse("Organization deleted successfully", true));
    }

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Organization>> getOrganizationsByTenant(@PathVariable UUID tenantId) {
        List<Organization> organizations = organizationService.getOrganizationsByTenant(tenantId);
        return ResponseEntity.ok(organizations);
    }

    @GetMapping("/current")
    public ResponseEntity<Organization> getCurrentUserOrganization() {
        Organization organization = organizationService.getCurrentUserOrganization();
        return ResponseEntity.ok(organization);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Organization> updateOrganizationStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        Organization organization = organizationService.updateStatus(id, status);
        return ResponseEntity.ok(organization);
    }
}