package org.automatize.status.services;

import org.automatize.status.api.request.TenantRequest;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.TenantRepository;
import org.automatize.status.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class TenantService {

    @Autowired
    private TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public Page<Tenant> getAllTenants(String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return tenantRepository.findAll(pageable); // Should implement search in repository
        }
        return tenantRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Tenant getTenantById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Tenant getTenantByName(String name) {
        return tenantRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Tenant not found with name: " + name));
    }

    public Tenant createTenant(TenantRequest request) {
        if (tenantRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tenant with name already exists: " + request.getName());
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.getName());
        tenant.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        String currentUser = getCurrentUsername();
        tenant.setCreatedBy(currentUser);
        tenant.setLastModifiedBy(currentUser);

        return tenantRepository.save(tenant);
    }

    public Tenant updateTenant(UUID id, TenantRequest request) {
        Tenant tenant = getTenantById(id);

        if (!tenant.getName().equals(request.getName()) &&
            tenantRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tenant with name already exists: " + request.getName());
        }

        tenant.setName(request.getName());
        tenant.setIsActive(request.getIsActive() != null ? request.getIsActive() : tenant.getIsActive());
        tenant.setLastModifiedBy(getCurrentUsername());

        return tenantRepository.save(tenant);
    }

    public void deleteTenant(UUID id) {
        Tenant tenant = getTenantById(id);
        tenantRepository.delete(tenant);
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            return ((UserPrincipal) principal).getUsername();
        }
        return "system";
    }
}