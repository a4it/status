package org.automatize.status.services;

import org.automatize.status.api.response.ContextResponse;
import org.automatize.status.exceptions.BusinessRuleException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.Organization;
import org.automatize.status.models.Tenant;
import org.automatize.status.models.User;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.TenantRepository;
import org.automatize.status.repositories.UserRepository;
import org.automatize.status.security.JwtUtils;
import org.automatize.status.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 * Service responsible for managing the active tenant/organization context for SUPERADMIN users.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>List active tenants and their active organizations for context selection</li>
 *   <li>Switch a SUPERADMIN's context, issuing a new JWT scoped to the chosen tenant and organization</li>
 *   <li>Expose the caller's current context derived from their principal</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
@Service
@Transactional
public class TenantContextService {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Retrieves all active tenants available for context selection.
     *
     * @return a list of active Tenant entities
     */
    public List<Tenant> getActiveTenants() {
        return tenantRepository.findByIsActive(true);
    }

    /**
     * Retrieves the active organizations belonging to the given tenant.
     *
     * @param tenantId the UUID of the tenant
     * @return a list of active Organization entities for the tenant
     */
    public List<Organization> getOrganizationsForTenant(UUID tenantId) {
        return organizationRepository.findByTenantIdAndStatus(tenantId, "ACTIVE");
    }

    /**
     * Switches a SUPERADMIN user's active context to the given tenant and organization,
     * validating that both are active and correctly related, and issues a new context-scoped JWT.
     *
     * @param userId the UUID of the user switching context
     * @param tenantId the UUID of the target tenant
     * @param organizationId the UUID of the target organization
     * @return a ContextResponse containing the new access token and selected context details
     * @throws ResourceNotFoundException if the user, tenant, or organization is not found
     * @throws AccessDeniedException if the user is not a SUPERADMIN
     * @throws BusinessRuleException if the tenant or organization is inactive, or the organization does not belong to the tenant
     */
    public ContextResponse switchContext(UUID userId, UUID tenantId, UUID organizationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only SUPERADMIN users are permitted to switch context
        if (!"SUPERADMIN".equals(user.getRole())) {
            throw new AccessDeniedException("Only SUPERADMIN can switch context");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        // Reject switching into an inactive tenant
        if (!Boolean.TRUE.equals(tenant.getIsActive())) {
            throw new BusinessRuleException("Tenant is not active");
        }

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        // Reject switching into an inactive organization
        if (!"ACTIVE".equals(org.getStatus())) {
            throw new BusinessRuleException("Organization is not active");
        }
        // Ensure the organization actually belongs to the selected tenant
        if (org.getTenant() == null || !tenantId.equals(org.getTenant().getId())) {
            throw new BusinessRuleException("Organization does not belong to the selected tenant");
        }

        String newToken = jwtUtils.generateJwtTokenWithContext(
                user.getId(), user.getUsername(), user.getEmail(),
                organizationId, user.getRole(), tenantId
        );

        ContextResponse response = new ContextResponse();
        response.setAccessToken(newToken);
        response.setTenantId(tenant.getId());
        response.setTenantName(tenant.getName());
        response.setOrganizationId(org.getId());
        response.setOrganizationName(org.getName());
        response.setSuperadmin(true);
        response.setHasSelectedContext(true);
        return response;
    }

    /**
     * Builds the current context for the given authenticated principal, resolving tenant and
     * organization names where available.
     *
     * @param principal the currently authenticated user principal
     * @return a ContextResponse describing the caller's current tenant/organization context
     */
    public ContextResponse getCurrentContext(UserPrincipal principal) {
        ContextResponse response = new ContextResponse();
        response.setOrganizationId(principal.getOrganizationId());
        response.setTenantId(principal.getTenantId());
        response.setSuperadmin("SUPERADMIN".equals(principal.getRole()));
        response.setHasSelectedContext(principal.hasSelectedContext());

        // Resolve the tenant name only when the principal has a tenant in context
        if (principal.getTenantId() != null) {
            tenantRepository.findById(principal.getTenantId())
                    .ifPresent(t -> response.setTenantName(t.getName()));
        }
        // Resolve the organization name only when the principal has an organization in context
        if (principal.getOrganizationId() != null) {
            organizationRepository.findById(principal.getOrganizationId())
                    .ifPresent(o -> response.setOrganizationName(o.getName()));
        }
        return response;
    }
}
