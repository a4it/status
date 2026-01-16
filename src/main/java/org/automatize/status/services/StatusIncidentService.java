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

/**
 * Service responsible for managing status incidents.
 * <p>
 * Incidents represent service disruptions or issues affecting monitored applications.
 * This service provides comprehensive incident lifecycle management including creation,
 * updates, resolution, and component impact tracking. It also manages automatic status
 * propagation to affected components and parent applications.
 * </p>
 *
 * @author Status Monitoring Team
 * @since 1.0
 */
@Service
@Transactional
public class StatusIncidentService {

    /**
     * Repository for status incident data access operations.
     */
    @Autowired
    private StatusIncidentRepository statusIncidentRepository;

    /**
     * Repository for incident update data access operations.
     */
    @Autowired
    private StatusIncidentUpdateRepository statusIncidentUpdateRepository;

    /**
     * Repository for incident-component relationship data access.
     */
    @Autowired
    private StatusIncidentComponentRepository statusIncidentComponentRepository;

    /**
     * Repository for status app data access operations.
     */
    @Autowired
    private StatusAppRepository statusAppRepository;

    /**
     * Repository for status component data access operations.
     */
    @Autowired
    private StatusComponentRepository statusComponentRepository;

    /**
     * Service for sending incident notifications to subscribers.
     */
    @Autowired
    private IncidentNotificationService incidentNotificationService;

    /**
     * Retrieves a paginated list of incidents with optional filtering.
     *
     * @param appId optional app ID to filter incidents
     * @param status optional status to filter incidents
     * @param startDate optional start date for date range filter
     * @param endDate optional end date for date range filter
     * @param search optional search term
     * @param pageable pagination information
     * @return a page of StatusIncidentResponse objects matching the criteria
     */
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

    /**
     * Retrieves an incident by its unique identifier.
     *
     * @param id the UUID of the incident
     * @return the StatusIncidentResponse for the requested incident
     * @throws RuntimeException if the incident is not found
     */
    @Transactional(readOnly = true)
    public StatusIncidentResponse getIncidentById(UUID id) {
        StatusIncident incident = statusIncidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found with id: " + id));
        return mapToResponse(incident);
    }

    /**
     * Retrieves all active (unresolved) incidents, optionally filtered by app or tenant.
     *
     * @param appId optional app ID to filter incidents
     * @param tenantId optional tenant ID to filter incidents
     * @return a list of StatusIncidentResponse objects for active incidents
     */
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

    /**
     * Retrieves all updates for a specific incident, ordered by update time descending.
     *
     * @param incidentId the UUID of the incident
     * @return a list of StatusIncidentUpdateResponse objects
     */
    @Transactional(readOnly = true)
    public List<StatusIncidentUpdateResponse> getIncidentUpdates(UUID incidentId) {
        List<StatusIncidentUpdate> updates = statusIncidentUpdateRepository
                .findByIncidentIdOrderByUpdateTimeDesc(incidentId);
        
        return updates.stream()
                .map(this::mapUpdateToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new incident for a status app.
     * <p>
     * This method also:
     * <ul>
     *   <li>Creates an initial incident update if a message is provided</li>
     *   <li>Links affected components to the incident</li>
     *   <li>Updates component statuses based on incident severity</li>
     *   <li>Updates the parent app's status</li>
     * </ul>
     * </p>
     *
     * @param request the incident creation request
     * @return the newly created StatusIncidentResponse
     * @throws RuntimeException if the app is not found
     */
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

        // Notify subscribers about the new incident
        incidentNotificationService.notifySubscribersOfNewIncident(savedIncident);

        return mapToResponse(savedIncident);
    }

    /**
     * Updates an existing incident with the provided details.
     * <p>
     * If the affected components list is provided, it replaces the existing list.
     * </p>
     *
     * @param id the UUID of the incident to update
     * @param request the incident update request
     * @return the updated StatusIncidentResponse
     * @throws RuntimeException if the incident is not found
     */
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

    /**
     * Adds a new update to an existing incident.
     * <p>
     * If the update changes the incident status, the app status is also updated accordingly.
     * </p>
     *
     * @param incidentId the UUID of the incident
     * @param request the incident update request containing status and message
     * @return the newly created StatusIncidentUpdateResponse
     * @throws RuntimeException if the incident is not found
     */
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

    /**
     * Resolves an incident and resets affected component statuses.
     * <p>
     * This method:
     * <ul>
     *   <li>Sets the incident status to RESOLVED</li>
     *   <li>Records the resolution timestamp</li>
     *   <li>Creates a resolution update entry</li>
     *   <li>Resets all affected components to OPERATIONAL status</li>
     *   <li>Updates the parent app's status</li>
     * </ul>
     * </p>
     *
     * @param id the UUID of the incident to resolve
     * @param message optional resolution message
     * @return the resolved StatusIncidentResponse
     * @throws RuntimeException if the incident is not found
     * @throws RuntimeException if the incident is already resolved
     */
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

        // Notify subscribers about the incident resolution
        incidentNotificationService.notifySubscribersOfIncidentResolution(savedIncident, resolvedMessage);

        return mapToResponse(savedIncident);
    }

    /**
     * Deletes an incident by its unique identifier.
     * <p>
     * Only resolved incidents can be deleted.
     * </p>
     *
     * @param id the UUID of the incident to delete
     * @throws RuntimeException if the incident is not found
     * @throws RuntimeException if the incident is still active
     */
    public void deleteIncident(UUID id) {
        StatusIncident incident = statusIncidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found with id: " + id));
        
        if (!"RESOLVED".equals(incident.getStatus())) {
            throw new RuntimeException("Cannot delete active incident. Resolve it first.");
        }
        
        statusIncidentRepository.delete(incident);
    }

    /**
     * Creates an incident update entry for timeline tracking.
     *
     * @param incident the incident to add the update to
     * @param status the status at the time of the update
     * @param message the update message
     * @return the newly created StatusIncidentUpdate
     */
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

    /**
     * Links components to an incident and updates their status.
     *
     * @param incident the incident to link components to
     * @param componentIds the list of component UUIDs to link
     * @throws RuntimeException if any component is not found
     */
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

    /**
     * Updates a component's status based on the incident's severity and impact.
     *
     * @param component the component to update
     * @param incident the incident affecting the component
     */
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

    /**
     * Updates the app's status based on active incidents.
     * <p>
     * Sets the app to MAJOR_OUTAGE if any active incident is critical,
     * DEGRADED if there are other active incidents, or OPERATIONAL if resolved.
     * </p>
     *
     * @param app the status app to update
     */
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

    /**
     * Maps a StatusIncident entity to a StatusIncidentResponse DTO.
     *
     * @param incident the StatusIncident entity to map
     * @return the mapped StatusIncidentResponse
     */
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

    /**
     * Maps a StatusIncidentUpdate entity to a StatusIncidentUpdateResponse DTO.
     *
     * @param update the StatusIncidentUpdate entity to map
     * @return the mapped StatusIncidentUpdateResponse
     */
    private StatusIncidentUpdateResponse mapUpdateToResponse(StatusIncidentUpdate update) {
        StatusIncidentUpdateResponse response = new StatusIncidentUpdateResponse();
        response.setId(update.getId());
        response.setStatus(update.getStatus());
        response.setMessage(update.getMessage());
        response.setUpdateTime(update.getUpdateTime());
        response.setCreatedBy(update.getCreatedBy());
        return response;
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
        return response;
    }

    /**
     * Maps fields from a StatusIncidentRequest to a StatusIncident entity.
     *
     * @param request the source request containing incident data
     * @param incident the target StatusIncident entity to populate
     */
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