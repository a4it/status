package org.automatize.status.services;

import org.automatize.status.api.request.StatusIncidentRequest;
import org.automatize.status.api.request.StatusIncidentUpdateRequest;
import org.automatize.status.api.response.StatusComponentResponse;
import org.automatize.status.api.response.StatusIncidentResponse;
import org.automatize.status.api.response.StatusIncidentUpdateResponse;
import org.automatize.status.models.*;
import org.automatize.status.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class StatusIncidentService {

    @Autowired
    private StatusIncidentRepository statusIncidentRepository;

    @Autowired
    private StatusIncidentUpdateRepository statusIncidentUpdateRepository;

    @Autowired
    private StatusIncidentComponentRepository statusIncidentComponentRepository;

    @Autowired
    private StatusAppRepository statusAppRepository;

    @Autowired
    private StatusComponentRepository statusComponentRepository;

    @Transactional(readOnly = true)
    public Page<StatusIncidentResponse> getAllIncidents(UUID appId, String status, ZonedDateTime startDate, 
                                                       ZonedDateTime endDate, String search, Pageable pageable) {
        List<StatusIncident> incidents;
        
        if (appId != null && status != null) {
            incidents = statusIncidentRepository.findByAppIdAndStatus(appId, status);
        } else if (appId != null) {
            incidents = statusIncidentRepository.findByAppId(appId);
        } else if (status != null) {
            incidents = statusIncidentRepository.findByStatus(status);
        } else if (startDate != null && endDate != null) {
            incidents = statusIncidentRepository.findByStartedAtBetween(startDate, endDate);
        } else if (search != null && !search.isEmpty()) {
            incidents = statusIncidentRepository.search(search);
        } else {
            return statusIncidentRepository.findAll(pageable).map(this::mapToResponse);
        }
        
        List<StatusIncidentResponse> responses = incidents.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, responses.size());
    }

    @Transactional(readOnly = true)
    public StatusIncidentResponse getIncidentById(UUID id) {
        StatusIncident incident = statusIncidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found with id: " + id));
        return mapToResponse(incident);
    }

    @Transactional(readOnly = true)
    public List<StatusIncidentResponse> getActiveIncidents(UUID appId, UUID tenantId) {
        List<StatusIncident> incidents;
        
        if (appId != null) {
            incidents = statusIncidentRepository.findByAppId(appId);
        } else if (tenantId != null) {
            incidents = statusIncidentRepository.findByTenantId(tenantId);
        } else {
            incidents = statusIncidentRepository.findByResolvedAtIsNull();
        }
        
        return incidents.stream()
                .filter(incident -> !"RESOLVED".equals(incident.getStatus()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StatusIncidentUpdateResponse> getIncidentUpdates(UUID incidentId) {
        List<StatusIncidentUpdate> updates = statusIncidentUpdateRepository
                .findByIncidentIdOrderByUpdateTimeDesc(incidentId);
        
        return updates.stream()
                .map(this::mapUpdateToResponse)
                .collect(Collectors.toList());
    }

    public StatusIncidentResponse createIncident(StatusIncidentRequest request) {
        StatusApp app = statusAppRepository.findById(request.getAppId())
                .orElseThrow(() -> new RuntimeException("Status app not found"));

        StatusIncident incident = new StatusIncident();
        mapRequestToIncident(request, incident);
        incident.setApp(app);
        
        String currentUser = getCurrentUsername();
        incident.setCreatedBy(currentUser);
        incident.setLastModifiedBy(currentUser);

        StatusIncident savedIncident = statusIncidentRepository.save(incident);
        
        // Create initial incident update if message provided
        if (request.getInitialMessage() != null && !request.getInitialMessage().isEmpty()) {
            createIncidentUpdate(savedIncident, request.getStatus(), request.getInitialMessage());
        }
        
        // Link affected components
        if (request.getAffectedComponentIds() != null && !request.getAffectedComponentIds().isEmpty()) {
            linkAffectedComponents(savedIncident, request.getAffectedComponentIds());
        }
        
        // Update app status
        updateAppStatusForIncident(app);
        
        return mapToResponse(savedIncident);
    }

    public StatusIncidentResponse updateIncident(UUID id, StatusIncidentRequest request) {
        StatusIncident incident = statusIncidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found with id: " + id));

        String oldStatus = incident.getStatus();
        mapRequestToIncident(request, incident);
        
        if (request.getAppId() != null && !incident.getApp().getId().equals(request.getAppId())) {
            StatusApp app = statusAppRepository.findById(request.getAppId())
                    .orElseThrow(() -> new RuntimeException("Status app not found"));
            incident.setApp(app);
        }
        
        incident.setLastModifiedBy(getCurrentUsername());

        StatusIncident savedIncident = statusIncidentRepository.save(incident);
        
        // Update affected components if changed
        if (request.getAffectedComponentIds() != null) {
            statusIncidentComponentRepository.deleteByIncidentId(id);
            linkAffectedComponents(savedIncident, request.getAffectedComponentIds());
        }
        
        // Update app status
        updateAppStatusForIncident(savedIncident.getApp());
        
        return mapToResponse(savedIncident);
    }

    public StatusIncidentUpdateResponse addIncidentUpdate(UUID incidentId, StatusIncidentUpdateRequest request) {
        StatusIncident incident = statusIncidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found with id: " + incidentId));

        StatusIncidentUpdate update = createIncidentUpdate(incident, request.getStatus(), request.getMessage());
        
        // Update incident status if changed
        if (!incident.getStatus().equals(request.getStatus())) {
            incident.setStatus(request.getStatus());
            incident.setLastModifiedBy(getCurrentUsername());
            statusIncidentRepository.save(incident);
            
            // Update app status
            updateAppStatusForIncident(incident.getApp());
        }
        
        return mapUpdateToResponse(update);
    }

    public StatusIncidentResponse resolveIncident(UUID id, String message) {
        StatusIncident incident = statusIncidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found with id: " + id));

        if ("RESOLVED".equals(incident.getStatus())) {
            throw new RuntimeException("Incident is already resolved");
        }

        incident.setStatus("RESOLVED");
        incident.setResolvedAt(ZonedDateTime.now());
        incident.setLastModifiedBy(getCurrentUsername());

        StatusIncident savedIncident = statusIncidentRepository.save(incident);
        
        // Create resolution update
        String resolvedMessage = message != null ? message : "Incident has been resolved";
        createIncidentUpdate(savedIncident, "RESOLVED", resolvedMessage);
        
        // Reset component statuses
        List<StatusIncidentComponent> affectedComponents = statusIncidentComponentRepository.findByIncidentId(id);
        for (StatusIncidentComponent ic : affectedComponents) {
            StatusComponent component = ic.getComponent();
            component.setStatus("OPERATIONAL");
            component.setLastModifiedBy(getCurrentUsername());
            statusComponentRepository.save(component);
        }
        
        // Update app status
        updateAppStatusForIncident(savedIncident.getApp());
        
        return mapToResponse(savedIncident);
    }

    public void deleteIncident(UUID id) {
        StatusIncident incident = statusIncidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found with id: " + id));
        
        if (!"RESOLVED".equals(incident.getStatus())) {
            throw new RuntimeException("Cannot delete active incident. Resolve it first.");
        }
        
        statusIncidentRepository.delete(incident);
    }

    private StatusIncidentUpdate createIncidentUpdate(StatusIncident incident, String status, String message) {
        StatusIncidentUpdate update = new StatusIncidentUpdate();
        update.setIncident(incident);
        update.setStatus(status);
        update.setMessage(message);
        update.setUpdateTime(ZonedDateTime.now());
        update.setCreatedBy(getCurrentUsername());
        update.setLastModifiedBy(getCurrentUsername());
        
        return statusIncidentUpdateRepository.save(update);
    }

    private void linkAffectedComponents(StatusIncident incident, List<UUID> componentIds) {
        for (UUID componentId : componentIds) {
            StatusComponent component = statusComponentRepository.findById(componentId)
                    .orElseThrow(() -> new RuntimeException("Component not found"));
            
            StatusIncidentComponent incidentComponent = new StatusIncidentComponent();
            incidentComponent.setIncident(incident);
            incidentComponent.setComponent(component);
            incidentComponent.setComponentStatus(component.getStatus());
            
            statusIncidentComponentRepository.save(incidentComponent);
            
            // Update component status based on incident severity
            updateComponentStatusForIncident(component, incident);
        }
    }

    private void updateComponentStatusForIncident(StatusComponent component, StatusIncident incident) {
        String newStatus = "DEGRADED";
        
        if ("CRITICAL".equals(incident.getSeverity()) || "MAJOR_OUTAGE".equals(incident.getImpact())) {
            newStatus = "MAJOR_OUTAGE";
        } else if ("MAJOR".equals(incident.getSeverity()) || "PARTIAL_OUTAGE".equals(incident.getImpact())) {
            newStatus = "PARTIAL_OUTAGE";
        }
        
        component.setStatus(newStatus);
        component.setLastModifiedBy(getCurrentUsername());
        statusComponentRepository.save(component);
    }

    private void updateAppStatusForIncident(StatusApp app) {
        Long activeIncidents = statusIncidentRepository.countActiveIncidentsByAppId(app.getId());
        
        String newStatus = "OPERATIONAL";
        if (activeIncidents > 0) {
            List<StatusIncident> incidents = statusIncidentRepository.findByAppId(app.getId());
            incidents = incidents.stream()
                    .filter(i -> !"RESOLVED".equals(i.getStatus()))
                    .collect(Collectors.toList());
            
            boolean hasCritical = incidents.stream()
                    .anyMatch(i -> "CRITICAL".equals(i.getSeverity()) || "MAJOR_OUTAGE".equals(i.getImpact()));
            
            if (hasCritical) {
                newStatus = "MAJOR_OUTAGE";
            } else {
                newStatus = "DEGRADED";
            }
        }
        
        if (!app.getStatus().equals(newStatus)) {
            app.setStatus(newStatus);
            app.setLastModifiedBy(getCurrentUsername());
            statusAppRepository.save(app);
        }
    }

    private StatusIncidentResponse mapToResponse(StatusIncident incident) {
        StatusIncidentResponse response = new StatusIncidentResponse();
        response.setId(incident.getId());
        response.setAppId(incident.getApp() != null ? incident.getApp().getId() : null);
        response.setTitle(incident.getTitle());
        response.setDescription(incident.getDescription());
        response.setStatus(incident.getStatus());
        response.setSeverity(incident.getSeverity());
        response.setImpact(incident.getImpact());
        response.setStartedAt(incident.getStartedAt());
        response.setResolvedAt(incident.getResolvedAt());
        response.setIsPublic(incident.getIsPublic());

        // Load updates
        List<StatusIncidentUpdate> updates = statusIncidentUpdateRepository
                .findByIncidentIdOrderByUpdateTimeDesc(incident.getId());
        response.setUpdates(updates.stream()
                .map(this::mapUpdateToResponse)
                .collect(Collectors.toList()));
        
        // Load affected components
        List<StatusIncidentComponent> incidentComponents = statusIncidentComponentRepository
                .findByIncidentId(incident.getId());
        response.setAffectedComponents(incidentComponents.stream()
                .map(ic -> mapComponentToResponse(ic.getComponent()))
                .collect(Collectors.toList()));
        
        return response;
    }

    private StatusIncidentUpdateResponse mapUpdateToResponse(StatusIncidentUpdate update) {
        StatusIncidentUpdateResponse response = new StatusIncidentUpdateResponse();
        response.setId(update.getId());
        response.setStatus(update.getStatus());
        response.setMessage(update.getMessage());
        response.setUpdateTime(update.getUpdateTime());
        response.setCreatedBy(update.getCreatedBy());
        return response;
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
        return response;
    }

    private void mapRequestToIncident(StatusIncidentRequest request, StatusIncident incident) {
        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setStatus(request.getStatus());
        incident.setSeverity(request.getSeverity());
        incident.setImpact(request.getImpact());
        incident.setStartedAt(request.getStartedAt());
        incident.setResolvedAt(request.getResolvedAt());
        incident.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : true);
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            return (String) principal;
        }
        return "system";
    }
}