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
public class StatusComponentService {

    @Autowired
    private StatusComponentRepository statusComponentRepository;

    @Autowired
    private StatusAppRepository statusAppRepository;

    @Autowired
    private StatusIncidentComponentRepository statusIncidentComponentRepository;

    @Autowired
    private StatusMaintenanceComponentRepository statusMaintenanceComponentRepository;

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

    @Transactional(readOnly = true)
    public StatusComponentResponse getComponentById(UUID id) {
        StatusComponent component = statusComponentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Component not found with id: " + id));
        return mapToResponse(component);
    }

    @Transactional(readOnly = true)
    public List<StatusComponentResponse> getComponentsByApp(UUID appId) {
        return statusComponentRepository.findByAppIdOrderByPosition(appId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

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

        StatusComponent savedComponent = statusComponentRepository.save(component);
        return mapToResponse(savedComponent);
    }

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

        StatusComponent savedComponent = statusComponentRepository.save(component);
        return mapToResponse(savedComponent);
    }

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

        return response;
    }

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

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            return (String) principal;
        }
        return "system";
    }
}