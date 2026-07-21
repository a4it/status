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

/**
 * <p>
 * REST API controller for managing the active tenant/organization context.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Expose the tenants and organizations available for context switching</li>
 *   <li>Switch the current user's active tenant/organization context</li>
 *   <li>Report the currently active context</li>
 * </ul>
 * </p>
 *
 * <p>
 * Base route: {@code /api/context}. Enumeration and switching are restricted to the
 * SUPERADMIN role; reading the current context requires any authenticated user.
 * </p>
 *
 * @see TenantContextService
 */
@RestController
@RequestMapping("/api/context")
public class TenantContextController {

    @Autowired
    private TenantContextService tenantContextService;

    /**
     * Lists all active tenants available for context switching.
     * <p>
     * HTTP GET {@code /api/context/tenants}. Restricted to the SUPERADMIN role.
     * </p>
     *
     * @return ResponseEntity containing a list of active tenants
     */
    @GetMapping("/tenants")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<List<Tenant>> getTenants() {
        return ResponseEntity.ok(tenantContextService.getActiveTenants());
    }

    /**
     * Lists the organizations belonging to a given tenant.
     * <p>
     * HTTP GET {@code /api/context/tenants/{tenantId}/organizations}. Restricted to the SUPERADMIN role.
     * </p>
     *
     * @param tenantId the UUID of the tenant whose organizations are requested
     * @return ResponseEntity containing a list of organizations for the tenant
     */
    @GetMapping("/tenants/{tenantId}/organizations")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<List<Organization>> getOrganizations(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(tenantContextService.getOrganizationsForTenant(tenantId));
    }

    /**
     * Switches the current user's active tenant/organization context.
     * <p>
     * HTTP POST {@code /api/context/switch}. Restricted to the SUPERADMIN role.
     * </p>
     *
     * @param request the target tenant and organization to switch to
     * @param principal the currently authenticated user
     * @return ResponseEntity containing the updated context
     */
    @PostMapping("/switch")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ContextResponse> switchContext(
            @RequestBody SwitchContextRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tenantContextService.switchContext(
                principal.getId(), request.getTenantId(), request.getOrganizationId()));
    }

    /**
     * Returns the currently active tenant/organization context for the user.
     * <p>
     * HTTP GET {@code /api/context/current}. Requires an authenticated user.
     * </p>
     *
     * @param principal the currently authenticated user
     * @return ResponseEntity containing the current context
     */
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContextResponse> getCurrentContext(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tenantContextService.getCurrentContext(principal));
    }
}
