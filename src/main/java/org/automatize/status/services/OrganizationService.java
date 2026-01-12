package org.automatize.status.services;

import org.automatize.status.api.request.OrganizationRequest;
import org.automatize.status.models.Organization;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.TenantRepository;
import org.automatize.status.repositories.UserRepository;
import org.automatize.status.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrganizationService {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<Organization> getAllOrganizations(UUID tenantId, String status, String search, Pageable pageable) {
        List<Organization> organizations;
        
        if (tenantId != null && status != null) {
            organizations = organizationRepository.findByTenantIdAndStatus(tenantId, status);
        } else if (tenantId != null) {
            organizations = organizationRepository.findByTenantId(tenantId);
        } else if (status != null) {
            organizations = organizationRepository.findByStatus(status);
        } else if (search != null && !search.isEmpty()) {
            organizations = organizationRepository.search(search);
        } else {
            return organizationRepository.findAll(pageable);
        }
        
        return new PageImpl<>(organizations, pageable, organizations.size());
    }

    @Transactional(readOnly = true)
    public Organization getOrganizationById(UUID id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Organization> getOrganizationsByTenant(UUID tenantId) {
        return organizationRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Organization getCurrentUserOrganization() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID organizationId = principal.getOrganizationId();
        
        if (organizationId == null) {
            throw new RuntimeException("User does not belong to any organization");
        }
        
        return getOrganizationById(organizationId);
    }

    public Organization createOrganization(OrganizationRequest request) {
        if (organizationRepository.existsByName(request.getName())) {
            throw new RuntimeException("Organization with name already exists: " + request.getName());
        }

        if (request.getEmail() != null && organizationRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Organization with email already exists: " + request.getEmail());
        }

        Organization organization = new Organization();
        mapRequestToOrganization(request, organization);
        
        if (request.getTenantId() != null) {
            Tenant tenant = tenantRepository.findById(request.getTenantId())
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));
            organization.setTenant(tenant);
        }
        
        String currentUser = getCurrentUsername();
        organization.setCreatedBy(currentUser);
        organization.setLastModifiedBy(currentUser);

        return organizationRepository.save(organization);
    }

    public Organization updateOrganization(UUID id, OrganizationRequest request) {
        Organization organization = getOrganizationById(id);

        if (!organization.getName().equals(request.getName()) && 
            organizationRepository.existsByName(request.getName())) {
            throw new RuntimeException("Organization with name already exists: " + request.getName());
        }

        if (request.getEmail() != null && 
            !request.getEmail().equals(organization.getEmail()) && 
            organizationRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Organization with email already exists: " + request.getEmail());
        }

        mapRequestToOrganization(request, organization);
        
        if (request.getTenantId() != null && 
            (organization.getTenant() == null || !organization.getTenant().getId().equals(request.getTenantId()))) {
            Tenant tenant = tenantRepository.findById(request.getTenantId())
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));
            organization.setTenant(tenant);
        }
        
        organization.setLastModifiedBy(getCurrentUsername());

        return organizationRepository.save(organization);
    }

    public Organization updateStatus(UUID id, String status) {
        Organization organization = getOrganizationById(id);
        organization.setStatus(status);
        organization.setLastModifiedBy(getCurrentUsername());
        return organizationRepository.save(organization);
    }

    public void deleteOrganization(UUID id) {
        Organization organization = getOrganizationById(id);
        
        Long userCount = userRepository.countByOrganizationId(id);
        if (userCount > 0) {
            throw new RuntimeException("Cannot delete organization with active users");
        }
        
        organizationRepository.delete(organization);
    }

    private void mapRequestToOrganization(OrganizationRequest request, Organization organization) {
        organization.setName(request.getName());
        organization.setDescription(request.getDescription());
        organization.setEmail(request.getEmail());
        organization.setPhone(request.getPhone());
        organization.setWebsite(request.getWebsite());
        organization.setAddress(request.getAddress());
        organization.setLogoUrl(request.getLogoUrl());
        organization.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        organization.setOrganizationType(request.getOrganizationType());
        organization.setVatNumber(request.getVatNumber());
        organization.setCountry(request.getCountry());
        organization.setPostalCode(request.getPostalCode());
        organization.setCommunity(request.getCommunity());
        organization.setType(request.getType());
        organization.setSubscriptionExempt(request.getSubscriptionExempt() != null ? request.getSubscriptionExempt() : false);
        organization.setThrottlingEnabled(request.getThrottlingEnabled() != null ? request.getThrottlingEnabled() : true);
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            return ((UserPrincipal) principal).getUsername();
        }
        return "system";
    }
}