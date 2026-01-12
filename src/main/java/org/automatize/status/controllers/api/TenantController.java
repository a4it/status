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

@RestController
@RequestMapping("/api/tenants")
@PreAuthorize("hasRole('ADMIN')")
public class TenantController {

    @Autowired
    private TenantService tenantService;

    @GetMapping
    public ResponseEntity<Page<Tenant>> getAllTenants(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<Tenant> tenants = tenantService.getAllTenants(search, pageable);
        return ResponseEntity.ok(tenants);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tenant> getTenantById(@PathVariable UUID id) {
        Tenant tenant = tenantService.getTenantById(id);
        return ResponseEntity.ok(tenant);
    }

    @PostMapping
    public ResponseEntity<Tenant> createTenant(@Valid @RequestBody TenantRequest request) {
        Tenant tenant = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(tenant);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tenant> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody TenantRequest request) {
        Tenant tenant = tenantService.updateTenant(id, request);
        return ResponseEntity.ok(tenant);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteTenant(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.ok(new MessageResponse("Tenant deleted successfully", true));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<Tenant> getTenantByName(@PathVariable String name) {
        Tenant tenant = tenantService.getTenantByName(name);
        return ResponseEntity.ok(tenant);
    }
}