package org.automatize.status.services;

import org.automatize.status.api.request.StatusAppRequest;
import org.automatize.status.api.response.StatusAppResponse;
import org.automatize.status.api.response.StatusComponentResponse;
import org.automatize.status.api.response.StatusIncidentResponse;
import org.automatize.status.api.response.StatusMaintenanceResponse;
import org.automatize.status.models.*;
import org.automatize.status.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class StatusAppService {

    @Autowired
    private StatusAppRepository statusAppRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private StatusComponentRepository statusComponentRepository;

    @Autowired
    private StatusIncidentRepository statusIncidentRepository;

    @Autowired
    private StatusMaintenanceRepository statusMaintenanceRepository;

    @Transactional(readOnly = true)
    public Page<StatusAppResponse> getAllStatusApps(UUID tenantId, UUID organizationId, String search, Pageable pageable) {
        List<StatusApp> apps;
        
        if (tenantId != null) {
            apps = statusAppRepository.findByTenantId(tenantId);
        } else if (organizationId != null) {
            apps = statusAppRepository.findByOrganizationId(organizationId);
        } else if (search != null && !search.isEmpty()) {
            apps = statusAppRepository.search(search);
        } else {
            return statusAppRepository.findAll(pageable).map(this::mapToResponse);
        }
        
        List<StatusAppResponse> responses = apps.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, responses.size());
    }

    @Transactional(readOnly = true)
    public StatusAppResponse getStatusAppById(UUID id) {
        StatusApp app = statusAppRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Status app not found with id: " + id));
        return mapToResponse(app);
    }

    @Transactional(readOnly = true)
    public List<StatusAppResponse> getStatusAppsByTenant(UUID tenantId) {
        return statusAppRepository.findByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StatusAppResponse> getStatusAppsByOrganization(UUID organizationId) {
        return statusAppRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public StatusAppResponse createStatusApp(StatusAppRequest request) {
        if (request.getTenantId() != null && 
            statusAppRepository.existsByTenantIdAndSlug(request.getTenantId(), request.getSlug())) {
            throw new RuntimeException("Status app with slug already exists in this tenant: " + request.getSlug());
        }

        StatusApp app = new StatusApp();
        mapRequestToStatusApp(request, app);
        
        if (request.getTenantId() != null) {
            Tenant tenant = tenantRepository.findById(request.getTenantId())
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));
            app.setTenant(tenant);
        }
        
        if (request.getOrganizationId() != null) {
            Organization organization = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            app.setOrganization(organization);
        }
        
        String currentUser = getCurrentUsername();
        app.setCreatedBy(currentUser);
        app.setLastModifiedBy(currentUser);

        StatusApp savedApp = statusAppRepository.save(app);
        return mapToResponse(savedApp);
    }

    public StatusAppResponse updateStatusApp(UUID id, StatusAppRequest request) {
        StatusApp app = statusAppRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Status app not found with id: " + id));

        if (!app.getSlug().equals(request.getSlug()) && 
            request.getTenantId() != null &&
            statusAppRepository.existsByTenantIdAndSlug(request.getTenantId(), request.getSlug())) {
            throw new RuntimeException("Status app with slug already exists in this tenant: " + request.getSlug());
        }

        mapRequestToStatusApp(request, app);
        
        if (request.getTenantId() != null && 
            (app.getTenant() == null || !app.getTenant().getId().equals(request.getTenantId()))) {
            Tenant tenant = tenantRepository.findById(request.getTenantId())
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));
            app.setTenant(tenant);
        }
        
        if (request.getOrganizationId() != null && 
            (app.getOrganization() == null || !app.getOrganization().getId().equals(request.getOrganizationId()))) {
            Organization organization = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            app.setOrganization(organization);
        }
        
        app.setLastModifiedBy(getCurrentUsername());

        StatusApp savedApp = statusAppRepository.save(app);
        return mapToResponse(savedApp);
    }

    public StatusAppResponse updateStatus(UUID id, String status) {
        StatusApp app = statusAppRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Status app not found with id: " + id));
        
        app.setStatus(status);
        app.setLastModifiedBy(getCurrentUsername());
        
        StatusApp savedApp = statusAppRepository.save(app);
        
        // Update all components status if app status is major outage
        if ("MAJOR_OUTAGE".equals(status)) {
            List<StatusComponent> components = statusComponentRepository.findByAppId(id);
            components.forEach(component -> {
                component.setStatus("MAJOR_OUTAGE");
                component.setLastModifiedBy(getCurrentUsername());
            });
            statusComponentRepository.saveAll(components);
        }
        
        return mapToResponse(savedApp);
    }

    public void deleteStatusApp(UUID id) {
        StatusApp app = statusAppRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Status app not found with id: " + id));
        
        // Check for active incidents
        Long activeIncidents = statusIncidentRepository.countActiveIncidentsByAppId(id);
        if (activeIncidents > 0) {
            throw new RuntimeException("Cannot delete app with active incidents");
        }
        
        // Check for upcoming maintenance
        Long activeMaintenance = statusMaintenanceRepository.countActiveMaintenanceByAppId(id);
        if (activeMaintenance > 0) {
            throw new RuntimeException("Cannot delete app with upcoming maintenance");
        }
        
        statusAppRepository.delete(app);
    }

    private StatusAppResponse mapToResponse(StatusApp app) {
        StatusAppResponse response = new StatusAppResponse();
        response.setId(app.getId());
        response.setName(app.getName());
        response.setDescription(app.getDescription());
        response.setSlug(app.getSlug());
        response.setStatus(app.getStatus());
        response.setIsPublic(app.getIsPublic());
        response.setLastUpdated(app.getLastModifiedDate());

        // Health check configuration
        response.setCheckEnabled(app.getCheckEnabled());
        response.setCheckType(app.getCheckType());
        response.setCheckUrl(app.getCheckUrl());
        response.setCheckIntervalSeconds(app.getCheckIntervalSeconds());
        response.setCheckTimeoutSeconds(app.getCheckTimeoutSeconds());
        response.setCheckExpectedStatus(app.getCheckExpectedStatus());
        response.setCheckFailureThreshold(app.getCheckFailureThreshold());
        response.setLastCheckAt(app.getLastCheckAt());
        response.setLastCheckSuccess(app.getLastCheckSuccess());
        response.setLastCheckMessage(app.getLastCheckMessage());
        response.setConsecutiveFailures(app.getConsecutiveFailures());

        // Load components
        List<StatusComponent> components = statusComponentRepository.findByAppIdOrderByPosition(app.getId());
        response.setComponents(components.stream()
                .map(this::mapComponentToResponse)
                .collect(Collectors.toList()));
        
        // Load current incidents
        List<StatusIncident> activeIncidents = statusIncidentRepository.findByAppIdAndStatus(app.getId(), "RESOLVED");
        activeIncidents = activeIncidents.stream()
                .filter(incident -> !incident.getStatus().equals("RESOLVED"))
                .collect(Collectors.toList());
        
        if (!activeIncidents.isEmpty()) {
            response.setCurrentIncident(mapIncidentToResponse(activeIncidents.get(0)));
        }
        
        // Load upcoming maintenances
        List<StatusMaintenance> upcomingMaintenances = statusMaintenanceRepository
                .findUpcomingMaintenanceByAppId(app.getId(), app.getLastModifiedDate());
        response.setUpcomingMaintenances(upcomingMaintenances.stream()
                .limit(3)
                .map(this::mapMaintenanceToResponse)
                .collect(Collectors.toList()));
        
        return response;
    }

    private void mapRequestToStatusApp(StatusAppRequest request, StatusApp app) {
        app.setName(request.getName());
        app.setDescription(request.getDescription());
        app.setSlug(request.getSlug());
        app.setStatus(request.getStatus() != null ? request.getStatus() : "OPERATIONAL");
        app.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : true);

        // Health check configuration
        app.setCheckEnabled(request.getCheckEnabled() != null ? request.getCheckEnabled() : false);
        app.setCheckType(request.getCheckType() != null ? request.getCheckType() : "NONE");
        app.setCheckUrl(request.getCheckUrl());
        app.setCheckIntervalSeconds(request.getCheckIntervalSeconds() != null ? request.getCheckIntervalSeconds() : 60);
        app.setCheckTimeoutSeconds(request.getCheckTimeoutSeconds() != null ? request.getCheckTimeoutSeconds() : 10);
        app.setCheckExpectedStatus(request.getCheckExpectedStatus() != null ? request.getCheckExpectedStatus() : 200);
        app.setCheckFailureThreshold(request.getCheckFailureThreshold() != null ? request.getCheckFailureThreshold() : 3);
    }

    private StatusComponentResponse mapComponentToResponse(StatusComponent component) {
        StatusComponentResponse response = new StatusComponentResponse();
        response.setId(component.getId());
        response.setAppId(component.getApp() != null ? component.getApp().getId() : null);
        response.setName(component.getName());
        response.setDescription(component.getDescription());
        response.setStatus(component.getStatus());
        response.setGroupName(component.getGroupName());
        response.setPosition(component.getPosition());

        // Health check configuration
        response.setCheckInheritFromApp(component.getCheckInheritFromApp());
        response.setCheckEnabled(component.getCheckEnabled());
        response.setCheckType(component.getCheckType());
        response.setCheckUrl(component.getCheckUrl());
        response.setCheckIntervalSeconds(component.getCheckIntervalSeconds());
        response.setCheckTimeoutSeconds(component.getCheckTimeoutSeconds());
        response.setCheckExpectedStatus(component.getCheckExpectedStatus());
        response.setCheckFailureThreshold(component.getCheckFailureThreshold());
        response.setLastCheckAt(component.getLastCheckAt());
        response.setLastCheckSuccess(component.getLastCheckSuccess());
        response.setLastCheckMessage(component.getLastCheckMessage());
        response.setConsecutiveFailures(component.getConsecutiveFailures());

        return response;
    }

    private StatusIncidentResponse mapIncidentToResponse(StatusIncident incident) {
        StatusIncidentResponse response = new StatusIncidentResponse();
        response.setId(incident.getId());
        response.setTitle(incident.getTitle());
        response.setDescription(incident.getDescription());
        response.setStatus(incident.getStatus());
        response.setSeverity(incident.getSeverity());
        response.setImpact(incident.getImpact());
        response.setStartedAt(incident.getStartedAt());
        response.setResolvedAt(incident.getResolvedAt());
        return response;
    }

    private StatusMaintenanceResponse mapMaintenanceToResponse(StatusMaintenance maintenance) {
        StatusMaintenanceResponse response = new StatusMaintenanceResponse();
        response.setId(maintenance.getId());
        response.setTitle(maintenance.getTitle());
        response.setDescription(maintenance.getDescription());
        response.setStatus(maintenance.getStatus());
        response.setStartsAt(maintenance.getStartsAt());
        response.setEndsAt(maintenance.getEndsAt());
        return response;
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            return (String) principal;
        }
        return "system";
    }
}