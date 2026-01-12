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

@Service
@Transactional
public class StatusMaintenanceService {

    @Autowired
    private StatusMaintenanceRepository statusMaintenanceRepository;

    @Autowired
    private StatusMaintenanceComponentRepository statusMaintenanceComponentRepository;

    @Autowired
    private StatusAppRepository statusAppRepository;

    @Autowired
    private StatusComponentRepository statusComponentRepository;

    @Autowired
    private StatusIncidentRepository statusIncidentRepository;

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

    @Transactional(readOnly = true)
    public StatusMaintenanceResponse getMaintenanceById(UUID id) {
        StatusMaintenance maintenance = statusMaintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found with id: " + id));
        return mapToResponse(maintenance);
    }

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

    public StatusMaintenanceResponse updateStatus(UUID id, String status) {
        StatusMaintenance maintenance = statusMaintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found with id: " + id));
        
        maintenance.setStatus(status);
        maintenance.setLastModifiedBy(getCurrentUsername());
        
        StatusMaintenance savedMaintenance = statusMaintenanceRepository.save(maintenance);
        return mapToResponse(savedMaintenance);
    }

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

    public void deleteMaintenance(UUID id) {
        StatusMaintenance maintenance = statusMaintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found with id: " + id));
        
        if ("IN_PROGRESS".equals(maintenance.getStatus())) {
            throw new RuntimeException("Cannot delete in-progress maintenance");
        }
        
        statusMaintenanceRepository.delete(maintenance);
    }

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

    private void mapRequestToMaintenance(StatusMaintenanceRequest request, StatusMaintenance maintenance) {
        maintenance.setTitle(request.getTitle());
        maintenance.setDescription(request.getDescription());
        maintenance.setStatus(request.getStatus());
        maintenance.setStartsAt(request.getStartsAt());
        maintenance.setEndsAt(request.getEndsAt());
        maintenance.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : true);
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            return (String) principal;
        }
        return "system";
    }
}