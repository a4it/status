package org.automatize.status.controllers.api;

import org.automatize.status.api.request.SwitchContextRequest;
import org.automatize.status.api.response.ContextResponse;
import org.automatize.status.models.Organization;
import org.automatize.status.models.Tenant;
import org.automatize.status.security.UserPrincipal;
import org.automatize.status.services.TenantContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/context")
public class TenantContextController {

    @Autowired
    private TenantContextService tenantContextService;

    @GetMapping("/tenants")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<List<Tenant>> getTenants() {
        return ResponseEntity.ok(tenantContextService.getActiveTenants());
    }

    @GetMapping("/tenants/{tenantId}/organizations")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<List<Organization>> getOrganizations(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(tenantContextService.getOrganizationsForTenant(tenantId));
    }

    @PostMapping("/switch")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ContextResponse> switchContext(
            @RequestBody SwitchContextRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tenantContextService.switchContext(
                principal.getId(), request.getTenantId(), request.getOrganizationId()));
    }

    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContextResponse> getCurrentContext(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tenantContextService.getCurrentContext(principal));
    }
}
