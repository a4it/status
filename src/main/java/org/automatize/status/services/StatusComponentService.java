package org.automatize.status.services;

import org.automatize.status.api.request.StatusComponentRequest;
import org.automatize.status.api.response.StatusComponentResponse;
import org.automatize.status.controllers.api.ComponentOrderRequest;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.automatize.status.repositories.StatusIncidentComponentRepository;
import org.automatize.status.repositories.StatusMaintenanceComponentRepository;
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
 * Service responsible for managing status components within status apps.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for component entities</li>
 *   <li>Manage component ordering, positioning, and grouping</li>
 *   <li>Handle cascading status updates to parent applications</li>
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
public class StatusComponentService {

    /**
     * Repository for status component data access operations.
     */
    @Autowired
    private StatusComponentRepository statusComponentRepository;

    /**
     * Repository for status app data access operations.
     */
    @Autowired
    private StatusAppRepository statusAppRepository;

    /**
     * Repository for incident-component relationship data access.
     */
    @Autowired
    private StatusIncidentComponentRepository statusIncidentComponentRepository;

    /**
     * Repository for maintenance-component relationship data access.
     */
    @Autowired
    private StatusMaintenanceComponentRepository statusMaintenanceComponentRepository;

    /**
     * Retrieves a paginated list of components with optional filtering.
     *
     * @param appId optional app ID to filter components
     * @param search optional search term for name matching
     * @param pageable pagination information
     * @return a page of StatusComponentResponse objects matching the criteria
     */
    @Transactional(readOnly = true)
    public Page<StatusComponentResponse> getAllComponents(UUID appId, String search, Pageable pageable) {
        List<StatusComponent> components;
        
        if (appId != null && search != null && !search.isEmpty()) {
            components = statusComponentRepository.searchByAppId(appId, search);
        } else if (appId != null) {
            components = statusComponentRepository.findByAppId(appId);
        } else if (search != null && !search.isEmpty()) {
            components = statusComponentRepository.search(search);
        } else {
            return statusComponentRepository.findAll(pageable).map(this::mapToResponse);
        }
        
        List<StatusComponentResponse> responses = components.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, responses.size());
    }

    /**
     * Retrieves a component by its unique identifier.
     *
     * @param id the UUID of the component
     * @return the StatusComponentResponse for the requested component
     * @throws RuntimeException if the component is not found
     */
    @Transactional(readOnly = true)
    public StatusComponentResponse getComponentById(UUID id) {
        StatusComponent component = statusComponentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Component not found with id: " + id));
        return mapToResponse(component);
    }

    /**
     * Retrieves all components belonging to a specific app, ordered by position.
     *
     * @param appId the UUID of the status app
     * @return a list of StatusComponentResponse objects ordered by position
     */
    @Transactional(readOnly = true)
    public List<StatusComponentResponse> getComponentsByApp(UUID appId) {
        return statusComponentRepository.findByAppIdOrderByPosition(appId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new component within a status app.
     * <p>
     * Validates that the component name is unique within the app.
     * If no position is specified, the component is added at the end.
     * </p>
     *
     * @param request the component creation request
     * @return the newly created StatusComponentResponse
     * @throws RuntimeException if the app is not found
     * @throws RuntimeException if a component with the same name already exists in the app
     */
    public StatusComponentResponse createComponent(StatusComponentRequest request) {
        StatusApp app = statusAppRepository.findById(request.getAppId())
                .orElseThrow(() -> new RuntimeException("Status app not found"));

        if (statusComponentRepository.existsByAppIdAndName(request.getAppId(), request.getName())) {
            throw new RuntimeException("Component with name already exists in this app: " + request.getName());
        }

        StatusComponent component = new StatusComponent();
        mapRequestToComponent(request, component);
        component.setApp(app);
        
        String currentUser = getCurrentUsername();
        component.setCreatedBy(currentUser);
        component.setLastModifiedBy(currentUser);
        
        // Set position if not provided
        if (component.getPosition() == null || component.getPosition() == 0) {
            Long componentCount = statusComponentRepository.countByAppId(app.getId());
            component.setPosition((int)(componentCount + 1));
        }

        // Generate API key for event logging
        component.setApiKey(ApiKeyGenerator.generateApiKey());

        StatusComponent savedComponent = statusComponentRepository.save(component);
        return mapToResponse(savedComponent);
    }

    /**
     * Updates an existing component with the provided details.
     *
     * @param id the UUID of the component to update
     * @param request the component update request
     * @return the updated StatusComponentResponse
     * @throws RuntimeException if the component is not found
     * @throws RuntimeException if the new name conflicts with an existing component
     */
    public StatusComponentResponse updateComponent(UUID id, StatusComponentRequest request) {
        StatusComponent component = statusComponentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Component not found with id: " + id));

        if (!component.getName().equals(request.getName()) && 
            statusComponentRepository.existsByAppIdAndName(component.getApp().getId(), request.getName())) {
            throw new RuntimeException("Component with name already exists in this app: " + request.getName());
        }

        mapRequestToComponent(request, component);
        
        if (request.getAppId() != null && !component.getApp().getId().equals(request.getAppId())) {
            StatusApp app = statusAppRepository.findById(request.getAppId())
                    .orElseThrow(() -> new RuntimeException("Status app not found"));
            component.setApp(app);
        }
        
        component.setLastModifiedBy(getCurrentUsername());

        // Generate API key if empty
        if (component.getApiKey() == null || component.getApiKey().isEmpty()) {
            component.setApiKey(ApiKeyGenerator.generateApiKey());
        }

        StatusComponent savedComponent = statusComponentRepository.save(component);
        return mapToResponse(savedComponent);
    }

    /**
     * Updates the status of a component.
     * <p>
     * After updating the component status, the parent app's status is
     * recalculated based on the statuses of all its components.
     * </p>
     *
     * @param id the UUID of the component
     * @param status the new status value
     * @return the updated StatusComponentResponse
     * @throws RuntimeException if the component is not found
     */
    public StatusComponentResponse updateStatus(UUID id, String status) {
        StatusComponent component = statusComponentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Component not found with id: " + id));
        
        component.setStatus(status);
        component.setLastModifiedBy(getCurrentUsername());
        
        StatusComponent savedComponent = statusComponentRepository.save(component);
        
        // Update app status based on component statuses
        updateAppStatusBasedOnComponents(component.getApp().getId());
        
        return mapToResponse(savedComponent);
    }

    /**
     * Deletes a component by its unique identifier.
     * <p>
     * Deletion is prevented if the component has active incidents or scheduled maintenance.
     * </p>
     *
     * @param id the UUID of the component to delete
     * @throws RuntimeException if the component is not found
     * @throws RuntimeException if the component has active incidents
     * @throws RuntimeException if the component has scheduled maintenance
     */
    public void deleteComponent(UUID id) {
        StatusComponent component = statusComponentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Component not found with id: " + id));
        
        // Check for active incidents
        Long activeIncidents = statusIncidentComponentRepository.countByComponentId(id);
        if (activeIncidents > 0) {
            throw new RuntimeException("Cannot delete component with active incidents");
        }
        
        // Check for upcoming maintenance
        Long activeMaintenance = statusMaintenanceComponentRepository.countByComponentId(id);
        if (activeMaintenance > 0) {
            throw new RuntimeException("Cannot delete component with scheduled maintenance");
        }
        
        statusComponentRepository.delete(component);
    }

    /**
     * Reorders components based on the provided order requests.
     *
     * @param orderRequests a list of ComponentOrderRequest objects specifying new positions
     * @throws RuntimeException if any component in the list is not found
     */
    @Transactional
    public void reorderComponents(List<ComponentOrderRequest> orderRequests) {
        for (ComponentOrderRequest orderRequest : orderRequests) {
            StatusComponent component = statusComponentRepository.findById(orderRequest.getComponentId())
                    .orElseThrow(() -> new RuntimeException("Component not found"));
            
            component.setPosition(orderRequest.getPosition());
            component.setLastModifiedBy(getCurrentUsername());
            statusComponentRepository.save(component);
        }
    }

    /**
     * Updates the parent app's status based on the aggregated status of its components.
     * <p>
     * The app status is determined as follows:
     * <ul>
     *   <li>MAJOR_OUTAGE if any component has MAJOR_OUTAGE status</li>
     *   <li>DEGRADED if any component has DEGRADED or PARTIAL_OUTAGE status</li>
     *   <li>OPERATIONAL otherwise</li>
     * </ul>
     * </p>
     *
     * @param appId the UUID of the status app
     */
    private void updateAppStatusBasedOnComponents(UUID appId) {
        List<StatusComponent> components = statusComponentRepository.findByAppId(appId);
        
        if (components.isEmpty()) {
            return;
        }
        
        String appStatus = "OPERATIONAL";
        long majorOutageCount = components.stream()
                .filter(c -> "MAJOR_OUTAGE".equals(c.getStatus()))
                .count();
        long degradedCount = components.stream()
                .filter(c -> "DEGRADED".equals(c.getStatus()) || "PARTIAL_OUTAGE".equals(c.getStatus()))
                .count();
        
        if (majorOutageCount > 0) {
            appStatus = "MAJOR_OUTAGE";
        } else if (degradedCount > 0) {
            appStatus = "DEGRADED";
        }
        
        StatusApp app = statusAppRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Status app not found"));
        
        if (!app.getStatus().equals(appStatus)) {
            app.setStatus(appStatus);
            app.setLastModifiedBy(getCurrentUsername());
            statusAppRepository.save(app);
        }
    }

    /**
     * Maps a StatusComponent entity to a StatusComponentResponse DTO.
     *
     * @param component the StatusComponent entity to map
     * @return the mapped StatusComponentResponse
     */
    private StatusComponentResponse mapToResponse(StatusComponent component) {
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

        // API key for event logging
        response.setApiKey(component.getApiKey());

        return response;
    }

    /**
     * Maps fields from a StatusComponentRequest to a StatusComponent entity.
     *
     * @param request the source request containing component data
     * @param component the target StatusComponent entity to populate
     */
    private void mapRequestToComponent(StatusComponentRequest request, StatusComponent component) {
        component.setName(request.getName());
        component.setDescription(request.getDescription());
        component.setStatus(request.getStatus() != null ? request.getStatus() : "OPERATIONAL");
        component.setPosition(request.getPosition() != null ? request.getPosition() : 0);
        component.setGroupName(request.getGroupName());

        // Health check configuration
        component.setCheckInheritFromApp(request.getCheckInheritFromApp() != null ? request.getCheckInheritFromApp() : true);
        component.setCheckEnabled(request.getCheckEnabled() != null ? request.getCheckEnabled() : false);
        component.setCheckType(request.getCheckType() != null ? request.getCheckType() : "NONE");
        component.setCheckUrl(request.getCheckUrl());
        component.setCheckIntervalSeconds(request.getCheckIntervalSeconds() != null ? request.getCheckIntervalSeconds() : 60);
        component.setCheckTimeoutSeconds(request.getCheckTimeoutSeconds() != null ? request.getCheckTimeoutSeconds() : 10);
        component.setCheckExpectedStatus(request.getCheckExpectedStatus() != null ? request.getCheckExpectedStatus() : 200);
        component.setCheckFailureThreshold(request.getCheckFailureThreshold() != null ? request.getCheckFailureThreshold() : 3);
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