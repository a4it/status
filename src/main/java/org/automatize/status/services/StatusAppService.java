package org.automatize.status.services;

import org.automatize.status.api.request.StatusAppRequest;
import org.automatize.status.api.response.StatusAppResponse;
import org.automatize.status.api.response.StatusComponentResponse;
import org.automatize.status.api.response.StatusIncidentResponse;
import org.automatize.status.api.response.StatusMaintenanceResponse;
import org.automatize.status.models.*;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.automatize.status.repositories.StatusIncidentRepository;
import org.automatize.status.repositories.StatusMaintenanceRepository;
import org.automatize.status.repositories.StatusPlatformRepository;
import org.automatize.status.repositories.TenantRepository;
import org.automatize.status.util.ApiKeyGenerator;
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

/**
 * <p>
 * Service responsible for managing status applications.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for status application entities</li>
 *   <li>Manage health check configuration and status updates</li>
 *   <li>Handle tenant and organization associations</li>
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
public class StatusAppService {

    /**
     * Repository for status app data access operations.
     */
    @Autowired
    private StatusAppRepository statusAppRepository;

    /**
     * Repository for tenant data access operations.
     */
    @Autowired
    private TenantRepository tenantRepository;

    /**
     * Repository for organization data access operations.
     */
    @Autowired
    private OrganizationRepository organizationRepository;

    /**
     * Repository for status component data access operations.
     */
    @Autowired
    private StatusComponentRepository statusComponentRepository;

    /**
     * Repository for status incident data access operations.
     */
    @Autowired
    private StatusIncidentRepository statusIncidentRepository;

    /**
     * Repository for status maintenance data access operations.
     */
    @Autowired
    private StatusMaintenanceRepository statusMaintenanceRepository;

    @Autowired
    private StatusPlatformRepository statusPlatformRepository;

    /**
     * Retrieves a paginated list of status apps with optional filtering.
     *
     * @param tenantId optional tenant ID to filter apps
     * @param organizationId optional organization ID to filter apps
     * @param search optional search term for name matching
     * @param pageable pagination information
     * @return a page of StatusAppResponse objects matching the criteria
     */
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

    /**
     * Retrieves a status app by its unique identifier.
     *
     * @param id the UUID of the status app
     * @return the StatusAppResponse for the requested app
     * @throws RuntimeException if the app is not found
     */
    @Transactional(readOnly = true)
    public StatusAppResponse getStatusAppById(UUID id) {
        StatusApp app = statusAppRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Status app not found with id: " + id));
        return mapToResponse(app);
    }

    /**
     * Retrieves all status apps belonging to a specific tenant.
     *
     * @param tenantId the UUID of the tenant
     * @return a list of StatusAppResponse objects for the tenant's apps
     */
    @Transactional(readOnly = true)
    public List<StatusAppResponse> getStatusAppsByTenant(UUID tenantId) {
        return statusAppRepository.findByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all status apps belonging to a specific organization.
     *
     * @param organizationId the UUID of the organization
     * @return a list of StatusAppResponse objects for the organization's apps
     */
    @Transactional(readOnly = true)
    public List<StatusAppResponse> getStatusAppsByOrganization(UUID organizationId) {
        return statusAppRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new status app with the provided details.
     * <p>
     * Validates that the slug is unique within the tenant before creating.
     * </p>
     *
     * @param request the status app creation request
     * @return the newly created StatusAppResponse
     * @throws RuntimeException if the slug already exists in the tenant
     * @throws RuntimeException if the tenant or organization is not found
     */
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

        if (request.getPlatformId() != null) {
            StatusPlatform platform = statusPlatformRepository.findById(request.getPlatformId())
                    .orElseThrow(() -> new RuntimeException("Platform not found"));
            app.setPlatform(platform);
        }

        String currentUser = getCurrentUsername();
        app.setCreatedBy(currentUser);
        app.setLastModifiedBy(currentUser);

        // Generate API key for event logging
        app.setApiKey(ApiKeyGenerator.generateApiKey());

        StatusApp savedApp = statusAppRepository.save(app);
        return mapToResponse(savedApp);
    }

    /**
     * Updates an existing status app with the provided details.
     *
     * @param id the UUID of the status app to update
     * @param request the status app update request
     * @return the updated StatusAppResponse
     * @throws RuntimeException if the app is not found
     * @throws RuntimeException if the new slug conflicts with an existing app
     */
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

        if (request.getPlatformId() != null) {
            if (app.getPlatform() == null || !app.getPlatform().getId().equals(request.getPlatformId())) {
                StatusPlatform platform = statusPlatformRepository.findById(request.getPlatformId())
                        .orElseThrow(() -> new RuntimeException("Platform not found"));
                app.setPlatform(platform);
            }
        } else {
            app.setPlatform(null);
        }

        app.setLastModifiedBy(getCurrentUsername());

        // Generate API key if empty
        if (app.getApiKey() == null || app.getApiKey().isEmpty()) {
            app.setApiKey(ApiKeyGenerator.generateApiKey());
        }

        StatusApp savedApp = statusAppRepository.save(app);
        return mapToResponse(savedApp);
    }

    /**
     * Updates the status of a status app.
     * <p>
     * When the status is set to MAJOR_OUTAGE, all components of the app are
     * automatically updated to MAJOR_OUTAGE status as well.
     * </p>
     *
     * @param id the UUID of the status app
     * @param status the new status value
     * @return the updated StatusAppResponse
     * @throws RuntimeException if the app is not found
     */
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

    /**
     * Deletes a status app by its unique identifier.
     * <p>
     * Deletion is prevented if the app has active incidents or upcoming maintenance.
     * </p>
     *
     * @param id the UUID of the status app to delete
     * @throws RuntimeException if the app is not found
     * @throws RuntimeException if the app has active incidents
     * @throws RuntimeException if the app has upcoming maintenance
     */
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

    /**
     * Maps a StatusApp entity to a StatusAppResponse DTO.
     * <p>
     * Includes associated components, current incident, and upcoming maintenance.
     * </p>
     *
     * @param app the StatusApp entity to map
     * @return the mapped StatusAppResponse
     */
    private StatusAppResponse mapToResponse(StatusApp app) {
        StatusAppResponse response = new StatusAppResponse();
        response.setId(app.getId());
        response.setName(app.getName());
        response.setDescription(app.getDescription());
        response.setSlug(app.getSlug());
        response.setStatus(app.getStatus());
        response.setIsPublic(app.getIsPublic());
        response.setLastUpdated(app.getLastModifiedDate());

        // Platform information
        if (app.getPlatform() != null) {
            response.setPlatformId(app.getPlatform().getId());
            response.setPlatformName(app.getPlatform().getName());
        }

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

        // API key for event logging
        response.setApiKey(app.getApiKey());

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

    /**
     * Maps fields from a StatusAppRequest to a StatusApp entity.
     *
     * @param request the source request containing app data
     * @param app the target StatusApp entity to populate
     */
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

    /**
     * Maps a StatusComponent entity to a StatusComponentResponse DTO.
     *
     * @param component the StatusComponent entity to map
     * @return the mapped StatusComponentResponse
     */
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

    /**
     * Maps a StatusIncident entity to a StatusIncidentResponse DTO.
     *
     * @param incident the StatusIncident entity to map
     * @return the mapped StatusIncidentResponse
     */
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

    /**
     * Maps a StatusMaintenance entity to a StatusMaintenanceResponse DTO.
     *
     * @param maintenance the StatusMaintenance entity to map
     * @return the mapped StatusMaintenanceResponse
     */
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

    /**
     * Retrieves the username of the currently authenticated user.
     *
     * @return the username, or "system" if no user is authenticated
     */
    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            return (String) principal;
        }
        return "system";
    }
}