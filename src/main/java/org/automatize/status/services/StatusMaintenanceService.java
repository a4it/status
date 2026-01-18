package org.automatize.status.services;

import org.automatize.status.api.request.StatusMaintenanceRequest;
import org.automatize.status.api.response.StatusComponentResponse;
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

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <p>
 * Service responsible for managing scheduled maintenance windows.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Manage maintenance lifecycle from scheduling through completion</li>
 *   <li>Handle component status updates during maintenance periods</li>
 *   <li>Provide upcoming and active maintenance queries</li>
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
public class StatusMaintenanceService {

    /**
     * Repository for maintenance data access operations.
     */
    @Autowired
    private StatusMaintenanceRepository statusMaintenanceRepository;

    /**
     * Repository for maintenance-component relationship data access.
     */
    @Autowired
    private StatusMaintenanceComponentRepository statusMaintenanceComponentRepository;

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
     * Repository for status incident data access operations.
     */
    @Autowired
    private StatusIncidentRepository statusIncidentRepository;

    /**
     * Retrieves a paginated list of maintenance windows with optional filtering.
     *
     * @param appId optional app ID to filter maintenance windows
     * @param status optional status to filter maintenance windows
     * @param startDate optional start date for date range filter
     * @param endDate optional end date for date range filter
     * @param search optional search term
     * @param pageable pagination information
     * @return a page of StatusMaintenanceResponse objects matching the criteria
     */
    @Transactional(readOnly = true)
    public Page<StatusMaintenanceResponse> getAllMaintenance(UUID appId, String status, ZonedDateTime startDate,
                                                           ZonedDateTime endDate, String search, Pageable pageable) {
        List<StatusMaintenance> maintenances;
        
        if (appId != null && status != null) {
            maintenances = statusMaintenanceRepository.findByAppIdAndStatus(appId, status);
        } else if (appId != null) {
            maintenances = statusMaintenanceRepository.findByAppId(appId);
        } else if (status != null) {
            maintenances = statusMaintenanceRepository.findByStatus(status);
        } else if (startDate != null && endDate != null) {
            maintenances = statusMaintenanceRepository.findByStartsAtBetween(startDate, endDate);
        } else if (search != null && !search.isEmpty()) {
            maintenances = statusMaintenanceRepository.search(search);
        } else {
            return statusMaintenanceRepository.findAll(pageable).map(this::mapToResponse);
        }
        
        List<StatusMaintenanceResponse> responses = maintenances.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, responses.size());
    }

    /**
     * Retrieves a maintenance window by its unique identifier.
     *
     * @param id the UUID of the maintenance window
     * @return the StatusMaintenanceResponse for the requested maintenance
     * @throws RuntimeException if the maintenance is not found
     */
    @Transactional(readOnly = true)
    public StatusMaintenanceResponse getMaintenanceById(UUID id) {
        StatusMaintenance maintenance = statusMaintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found with id: " + id));
        return mapToResponse(maintenance);
    }

    /**
     * Retrieves upcoming scheduled maintenance windows within a specified number of days.
     *
     * @param appId optional app ID to filter maintenance windows
     * @param tenantId optional tenant ID to filter maintenance windows
     * @param days the number of days to look ahead
     * @return a list of StatusMaintenanceResponse objects for scheduled maintenance
     */
    @Transactional(readOnly = true)
    public List<StatusMaintenanceResponse> getUpcomingMaintenance(UUID appId, UUID tenantId, int days) {
        ZonedDateTime startDate = ZonedDateTime.now();
        ZonedDateTime endDate = startDate.plusDays(days);
        
        List<StatusMaintenance> maintenances;
        if (appId != null) {
            maintenances = statusMaintenanceRepository.findUpcomingMaintenanceByAppId(appId, startDate);
        } else if (tenantId != null) {
            maintenances = statusMaintenanceRepository.findByTenantId(tenantId);
            maintenances = maintenances.stream()
                    .filter(m -> m.getStartsAt().isAfter(startDate) && m.getStartsAt().isBefore(endDate))
                    .collect(Collectors.toList());
        } else {
            maintenances = statusMaintenanceRepository.findByStartsAtBetween(startDate, endDate);
        }
        
        return maintenances.stream()
                .filter(m -> "SCHEDULED".equals(m.getStatus()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves currently active (in-progress) maintenance windows.
     *
     * @param appId optional app ID to filter maintenance windows
     * @param tenantId optional tenant ID to filter maintenance windows
     * @return a list of StatusMaintenanceResponse objects for active maintenance
     */
    @Transactional(readOnly = true)
    public List<StatusMaintenanceResponse> getActiveMaintenance(UUID appId, UUID tenantId) {
        ZonedDateTime currentTime = ZonedDateTime.now();
        List<StatusMaintenance> maintenances = statusMaintenanceRepository.findActiveMaintenance(currentTime);
        
        if (appId != null) {
            maintenances = maintenances.stream()
                    .filter(m -> m.getApp().getId().equals(appId))
                    .collect(Collectors.toList());
        } else if (tenantId != null) {
            maintenances = maintenances.stream()
                    .filter(m -> m.getApp().getTenant() != null && m.getApp().getTenant().getId().equals(tenantId))
                    .collect(Collectors.toList());
        }
        
        return maintenances.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new scheduled maintenance window.
     * <p>
     * Validates that the start time is before the end time before creating.
     * </p>
     *
     * @param request the maintenance creation request
     * @return the newly created StatusMaintenanceResponse
     * @throws RuntimeException if the app is not found
     * @throws RuntimeException if the start time is after the end time
     */
    public StatusMaintenanceResponse createMaintenance(StatusMaintenanceRequest request) {
        StatusApp app = statusAppRepository.findById(request.getAppId())
                .orElseThrow(() -> new RuntimeException("Status app not found"));

        if (request.getStartsAt().isAfter(request.getEndsAt())) {
            throw new RuntimeException("Start time must be before end time");
        }

        StatusMaintenance maintenance = new StatusMaintenance();
        mapRequestToMaintenance(request, maintenance);
        maintenance.setApp(app);
        maintenance.setStatus("SCHEDULED");
        
        String currentUser = getCurrentUsername();
        maintenance.setCreatedBy(currentUser);
        maintenance.setLastModifiedBy(currentUser);

        StatusMaintenance savedMaintenance = statusMaintenanceRepository.save(maintenance);
        
        // Link affected components
        if (request.getAffectedComponentIds() != null && !request.getAffectedComponentIds().isEmpty()) {
            linkAffectedComponents(savedMaintenance, request.getAffectedComponentIds());
        }
        
        return mapToResponse(savedMaintenance);
    }

    /**
     * Updates an existing maintenance window.
     * <p>
     * Cannot update completed or cancelled maintenance.
     * </p>
     *
     * @param id the UUID of the maintenance to update
     * @param request the maintenance update request
     * @return the updated StatusMaintenanceResponse
     * @throws RuntimeException if the maintenance is not found
     * @throws RuntimeException if the maintenance is completed or cancelled
     * @throws RuntimeException if the start time is after the end time
     */
    public StatusMaintenanceResponse updateMaintenance(UUID id, StatusMaintenanceRequest request) {
        StatusMaintenance maintenance = statusMaintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found with id: " + id));

        if ("COMPLETED".equals(maintenance.getStatus()) || "CANCELLED".equals(maintenance.getStatus())) {
            throw new RuntimeException("Cannot update completed or cancelled maintenance");
        }

        if (request.getStartsAt().isAfter(request.getEndsAt())) {
            throw new RuntimeException("Start time must be before end time");
        }

        mapRequestToMaintenance(request, maintenance);
        
        if (request.getAppId() != null && !maintenance.getApp().getId().equals(request.getAppId())) {
            StatusApp app = statusAppRepository.findById(request.getAppId())
                    .orElseThrow(() -> new RuntimeException("Status app not found"));
            maintenance.setApp(app);
        }
        
        maintenance.setLastModifiedBy(getCurrentUsername());

        StatusMaintenance savedMaintenance = statusMaintenanceRepository.save(maintenance);
        
        // Update affected components if changed
        if (request.getAffectedComponentIds() != null) {
            statusMaintenanceComponentRepository.deleteByMaintenanceId(id);
            linkAffectedComponents(savedMaintenance, request.getAffectedComponentIds());
        }
        
        return mapToResponse(savedMaintenance);
    }

    /**
     * Updates the status of a maintenance window.
     *
     * @param id the UUID of the maintenance
     * @param status the new status value
     * @return the updated StatusMaintenanceResponse
     * @throws RuntimeException if the maintenance is not found
     */
    public StatusMaintenanceResponse updateStatus(UUID id, String status) {
        StatusMaintenance maintenance = statusMaintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found with id: " + id));
        
        maintenance.setStatus(status);
        maintenance.setLastModifiedBy(getCurrentUsername());
        
        StatusMaintenance savedMaintenance = statusMaintenanceRepository.save(maintenance);
        return mapToResponse(savedMaintenance);
    }

    /**
     * Starts a scheduled maintenance window.
     * <p>
     * This method:
     * <ul>
     *   <li>Sets the maintenance status to IN_PROGRESS</li>
     *   <li>Updates affected components to UNDER_MAINTENANCE status</li>
     *   <li>Updates the parent app's status</li>
     * </ul>
     * </p>
     *
     * @param id the UUID of the maintenance to start
     * @return the started StatusMaintenanceResponse
     * @throws RuntimeException if the maintenance is not found
     * @throws RuntimeException if the maintenance is not in SCHEDULED status
     */
    public StatusMaintenanceResponse startMaintenance(UUID id) {
        StatusMaintenance maintenance = statusMaintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found with id: " + id));

        if (!"SCHEDULED".equals(maintenance.getStatus())) {
            throw new RuntimeException("Can only start scheduled maintenance");
        }

        maintenance.setStatus("IN_PROGRESS");
        maintenance.setLastModifiedBy(getCurrentUsername());
        
        StatusMaintenance savedMaintenance = statusMaintenanceRepository.save(maintenance);
        
        // Update affected components status
        List<StatusMaintenanceComponent> maintenanceComponents = statusMaintenanceComponentRepository
                .findByMaintenanceId(id);
        
        for (StatusMaintenanceComponent mc : maintenanceComponents) {
            StatusComponent component = mc.getComponent();
            component.setStatus("UNDER_MAINTENANCE");
            component.setLastModifiedBy(getCurrentUsername());
            statusComponentRepository.save(component);
        }
        
        // Update app status
        updateAppStatusForMaintenance(savedMaintenance.getApp());
        
        return mapToResponse(savedMaintenance);
    }

    /**
     * Completes an in-progress maintenance window.
     * <p>
     * This method:
     * <ul>
     *   <li>Sets the maintenance status to COMPLETED</li>
     *   <li>Resets affected components to OPERATIONAL status</li>
     *   <li>Updates the parent app's status</li>
     * </ul>
     * </p>
     *
     * @param id the UUID of the maintenance to complete
     * @return the completed StatusMaintenanceResponse
     * @throws RuntimeException if the maintenance is not found
     * @throws RuntimeException if the maintenance is not in IN_PROGRESS status
     */
    public StatusMaintenanceResponse completeMaintenance(UUID id) {
        StatusMaintenance maintenance = statusMaintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found with id: " + id));

        if (!"IN_PROGRESS".equals(maintenance.getStatus())) {
            throw new RuntimeException("Can only complete in-progress maintenance");
        }

        maintenance.setStatus("COMPLETED");
        maintenance.setLastModifiedBy(getCurrentUsername());
        
        StatusMaintenance savedMaintenance = statusMaintenanceRepository.save(maintenance);
        
        // Reset affected components status
        List<StatusMaintenanceComponent> maintenanceComponents = statusMaintenanceComponentRepository
                .findByMaintenanceId(id);
        
        for (StatusMaintenanceComponent mc : maintenanceComponents) {
            StatusComponent component = mc.getComponent();
            component.setStatus("OPERATIONAL");
            component.setLastModifiedBy(getCurrentUsername());
            statusComponentRepository.save(component);
        }
        
        // Update app status
        updateAppStatusForMaintenance(savedMaintenance.getApp());
        
        return mapToResponse(savedMaintenance);
    }

    /**
     * Deletes a maintenance window by its unique identifier.
     * <p>
     * In-progress maintenance cannot be deleted.
     * </p>
     *
     * @param id the UUID of the maintenance to delete
     * @throws RuntimeException if the maintenance is not found
     * @throws RuntimeException if the maintenance is in progress
     */
    public void deleteMaintenance(UUID id) {
        StatusMaintenance maintenance = statusMaintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found with id: " + id));
        
        if ("IN_PROGRESS".equals(maintenance.getStatus())) {
            throw new RuntimeException("Cannot delete in-progress maintenance");
        }
        
        statusMaintenanceRepository.delete(maintenance);
    }

    /**
     * Links components to a maintenance window.
     *
     * @param maintenance the maintenance to link components to
     * @param componentIds the list of component UUIDs to link
     * @throws RuntimeException if any component is not found
     */
    private void linkAffectedComponents(StatusMaintenance maintenance, List<UUID> componentIds) {
        for (UUID componentId : componentIds) {
            StatusComponent component = statusComponentRepository.findById(componentId)
                    .orElseThrow(() -> new RuntimeException("Component not found"));
            
            StatusMaintenanceComponent maintenanceComponent = new StatusMaintenanceComponent();
            maintenanceComponent.setMaintenance(maintenance);
            maintenanceComponent.setComponent(component);
            
            statusMaintenanceComponentRepository.save(maintenanceComponent);
        }
    }

    /**
     * Updates the app's status based on active maintenance.
     * <p>
     * Sets the app to UNDER_MAINTENANCE if there is active maintenance,
     * or OPERATIONAL if maintenance completes and there are no active incidents.
     * </p>
     *
     * @param app the status app to update
     */
    private void updateAppStatusForMaintenance(StatusApp app) {
        Long activeMaintenance = statusMaintenanceRepository.countActiveMaintenanceByAppId(app.getId());
        
        if (activeMaintenance > 0 && "OPERATIONAL".equals(app.getStatus())) {
            app.setStatus("UNDER_MAINTENANCE");
            app.setLastModifiedBy(getCurrentUsername());
            statusAppRepository.save(app);
        } else if (activeMaintenance == 0 && "UNDER_MAINTENANCE".equals(app.getStatus())) {
            // Check for active incidents before setting to operational
            Long activeIncidents = statusIncidentRepository.countActiveIncidentsByAppId(app.getId());
            if (activeIncidents == 0) {
                app.setStatus("OPERATIONAL");
                app.setLastModifiedBy(getCurrentUsername());
                statusAppRepository.save(app);
            }
        }
    }

    /**
     * Maps a StatusMaintenance entity to a StatusMaintenanceResponse DTO.
     *
     * @param maintenance the StatusMaintenance entity to map
     * @return the mapped StatusMaintenanceResponse
     */
    private StatusMaintenanceResponse mapToResponse(StatusMaintenance maintenance) {
        StatusMaintenanceResponse response = new StatusMaintenanceResponse();
        response.setId(maintenance.getId());
        response.setTitle(maintenance.getTitle());
        response.setDescription(maintenance.getDescription());
        response.setStatus(maintenance.getStatus());
        response.setStartsAt(maintenance.getStartsAt());
        response.setEndsAt(maintenance.getEndsAt());
        
        // Load affected components
        List<StatusMaintenanceComponent> maintenanceComponents = statusMaintenanceComponentRepository
                .findByMaintenanceId(maintenance.getId());
        response.setAffectedComponents(maintenanceComponents.stream()
                .map(mc -> mapComponentToResponse(mc.getComponent()))
                .collect(Collectors.toList()));
        
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
        response.setName(component.getName());
        response.setDescription(component.getDescription());
        response.setStatus(component.getStatus());
        response.setGroupName(component.getGroupName());
        response.setPosition(component.getPosition());
        return response;
    }

    /**
     * Maps fields from a StatusMaintenanceRequest to a StatusMaintenance entity.
     *
     * @param request the source request containing maintenance data
     * @param maintenance the target StatusMaintenance entity to populate
     */
    private void mapRequestToMaintenance(StatusMaintenanceRequest request, StatusMaintenance maintenance) {
        maintenance.setTitle(request.getTitle());
        maintenance.setDescription(request.getDescription());
        maintenance.setStatus(request.getStatus());
        maintenance.setStartsAt(request.getStartsAt());
        maintenance.setEndsAt(request.getEndsAt());
        maintenance.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : true);
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