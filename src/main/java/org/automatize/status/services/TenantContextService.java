package org.automatize.status.services;

import org.automatize.status.api.response.ContextResponse;
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

    public List<Tenant> getActiveTenants() {
        return tenantRepository.findByIsActive(true);
    }

    public List<Organization> getOrganizationsForTenant(UUID tenantId) {
        return organizationRepository.findByTenantIdAndStatus(tenantId, "ACTIVE");
    }

    public ContextResponse switchContext(UUID userId, UUID tenantId, UUID organizationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"SUPERADMIN".equals(user.getRole())) {
            throw new AccessDeniedException("Only SUPERADMIN can switch context");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        if (!Boolean.TRUE.equals(tenant.getIsActive())) {
            throw new RuntimeException("Tenant is not active");
        }

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        if (!"ACTIVE".equals(org.getStatus())) {
            throw new RuntimeException("Organization is not active");
        }
        if (org.getTenant() == null || !tenantId.equals(org.getTenant().getId())) {
            throw new RuntimeException("Organization does not belong to the selected tenant");
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

    public ContextResponse getCurrentContext(UserPrincipal principal) {
        ContextResponse response = new ContextResponse();
        response.setOrganizationId(principal.getOrganizationId());
        response.setTenantId(principal.getTenantId());
        response.setSuperadmin("SUPERADMIN".equals(principal.getRole()));
        response.setHasSelectedContext(principal.hasSelectedContext());

        if (principal.getTenantId() != null) {
            tenantRepository.findById(principal.getTenantId())
                    .ifPresent(t -> response.setTenantName(t.getName()));
        }
        if (principal.getOrganizationId() != null) {
            organizationRepository.findById(principal.getOrganizationId())
                    .ifPresent(o -> response.setOrganizationName(o.getName()));
        }
        return response;
    }
}
