package org.automatize.status.services;

import org.automatize.status.api.response.*;
import org.automatize.status.models.*;
import org.automatize.status.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <p>
 * Service responsible for providing public status information without authentication.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide read-only public status page data</li>
 *   <li>Calculate uptime history and statistics</li>
 *   <li>Aggregate overall system status summaries</li>
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
@Transactional(readOnly = true)
public class PublicStatusService {

    /**
     * Repository for status app data access.
     */
    @Autowired
    private StatusAppRepository statusAppRepository;

    /**
     * Repository for status component data access.
     */
    @Autowired
    private StatusComponentRepository statusComponentRepository;

    /**
     * Repository for status incident data access.
     */
    @Autowired
    private StatusIncidentRepository statusIncidentRepository;

    /**
     * Repository for incident update data access.
     */
    @Autowired
    private StatusIncidentUpdateRepository statusIncidentUpdateRepository;

    /**
     * Repository for incident-component relationship data access.
     */
    @Autowired
    private StatusIncidentComponentRepository statusIncidentComponentRepository;

    /**
     * Repository for maintenance data access.
     */
    @Autowired
    private StatusMaintenanceRepository statusMaintenanceRepository;

    /**
     * Repository for maintenance-component relationship data access.
     */
    @Autowired
    private StatusMaintenanceComponentRepository statusMaintenanceComponentRepository;

    /**
     * Repository for tenant data access.
     */
    @Autowired
    private TenantRepository tenantRepository;

    /**
     * Repository for uptime history data access.
     */
    @Autowired
    private StatusUptimeHistoryRepository statusUptimeHistoryRepository;

    /**
     * Retrieves all public status apps, optionally filtered by tenant.
     *
     * @param tenantName optional tenant name to filter apps
     * @return a list of StatusAppResponse objects for public apps
     * @throws RuntimeException if the specified tenant is not found
     */
    public List<StatusAppResponse> getAllPublicApps(String tenantName) {
        List<StatusApp> apps;
        if (tenantName != null) {
            Tenant tenant = tenantRepository.findByName(tenantName)
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));
            apps = statusAppRepository.findByTenantIdAndIsPublic(tenant.getId(), true);
        } else {
            apps = statusAppRepository.findByIsPublic(true);
        }

        return apps.stream()
                .map(this::mapToStatusAppResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a public status app by its slug identifier.
     *
     * @param slug the unique slug of the status app
     * @param tenantName optional tenant name for scoping the search
     * @return the StatusAppResponse for the requested app
     * @throws RuntimeException if the app is not found or is not public
     */
    public StatusAppResponse getAppBySlug(String slug, String tenantName) {
        StatusApp app;
        if (tenantName != null) {
            Tenant tenant = tenantRepository.findByName(tenantName)
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));
            app = statusAppRepository.findByTenantIdAndSlug(tenant.getId(), slug)
                    .orElseThrow(() -> new RuntimeException("Status app not found"));
        } else {
            app = statusAppRepository.findBySlug(slug)
                    .orElseThrow(() -> new RuntimeException("Status app not found"));
        }

        if (!app.getIsPublic()) {
            throw new RuntimeException("Status app is not public");
        }

        return mapToStatusAppResponse(app);
    }

    /**
     * Retrieves all components for a public status app.
     *
     * @param appId the UUID of the status app
     * @return a list of StatusComponentResponse objects for the app's components
     * @throws RuntimeException if the app is not found or is not public
     */
    public List<StatusComponentResponse> getAppComponents(UUID appId) {
        StatusApp app = statusAppRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Status app not found"));
        
        if (!app.getIsPublic()) {
            throw new RuntimeException("Status app is not public");
        }

        List<StatusComponent> components = statusComponentRepository.findByAppIdOrderByPosition(appId);
        return components.stream()
                .map(this::mapToStatusComponentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves public incidents for an app within a specified time range.
     *
     * @param appId the UUID of the status app
     * @param days the number of days to look back for incidents
     * @return a list of StatusIncidentResponse objects for public incidents
     * @throws RuntimeException if the app is not found or is not public
     */
    public List<StatusIncidentResponse> getAppIncidents(UUID appId, int days) {
        StatusApp app = statusAppRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Status app not found"));
        
        if (!app.getIsPublic()) {
            throw new RuntimeException("Status app is not public");
        }

        ZonedDateTime startDate = ZonedDateTime.now().minusDays(days);
        List<StatusIncident> incidents = statusIncidentRepository.findRecentIncidentsByAppId(appId, startDate);
        
        return incidents.stream()
                .filter(StatusIncident::getIsPublic)
                .map(this::mapToStatusIncidentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all currently active (unresolved) public incidents for an app.
     *
     * @param appId the UUID of the status app
     * @return a list of StatusIncidentResponse objects for active public incidents
     */
    public List<StatusIncidentResponse> getCurrentIncidents(UUID appId) {
        List<StatusIncident> incidents = statusIncidentRepository.findByAppIdAndStatus(appId, "RESOLVED");
        
        return incidents.stream()
                .filter(incident -> !incident.getStatus().equals("RESOLVED") && incident.getIsPublic())
                .map(this::mapToStatusIncidentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves detailed information about a specific public incident.
     *
     * @param incidentId the UUID of the incident
     * @return the StatusIncidentResponse with full incident details
     * @throws RuntimeException if the incident is not found or is not public
     */
    public StatusIncidentResponse getIncidentDetails(UUID incidentId) {
        StatusIncident incident = statusIncidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));
        
        if (!incident.getIsPublic()) {
            throw new RuntimeException("Incident is not public");
        }

        return mapToStatusIncidentResponse(incident);
    }

    /**
     * Retrieves all updates for a specific public incident.
     *
     * @param incidentId the UUID of the incident
     * @return a list of StatusIncidentUpdateResponse objects ordered by update time descending
     * @throws RuntimeException if the incident is not found or is not public
     */
    public List<StatusIncidentUpdateResponse> getIncidentUpdates(UUID incidentId) {
        StatusIncident incident = statusIncidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));
        
        if (!incident.getIsPublic()) {
            throw new RuntimeException("Incident is not public");
        }

        List<StatusIncidentUpdate> updates = statusIncidentUpdateRepository.findByIncidentIdOrderByUpdateTimeDesc(incidentId);
        return updates.stream()
                .map(this::mapToStatusIncidentUpdateResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves public maintenance windows for an app.
     *
     * @param appId the UUID of the status app
     * @param type the type filter: "upcoming" for scheduled, "past" for completed, or null for all
     * @return a list of StatusMaintenanceResponse objects for public maintenance windows
     * @throws RuntimeException if the app is not found or is not public
     */
    public List<StatusMaintenanceResponse> getAppMaintenance(UUID appId, String type) {
        StatusApp app = statusAppRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Status app not found"));
        
        if (!app.getIsPublic()) {
            throw new RuntimeException("Status app is not public");
        }

        List<StatusMaintenance> maintenances;
        if ("upcoming".equals(type)) {
            maintenances = statusMaintenanceRepository.findUpcomingMaintenanceByAppId(appId, ZonedDateTime.now());
        } else if ("past".equals(type)) {
            maintenances = statusMaintenanceRepository.findByAppIdAndStatus(appId, "COMPLETED");
        } else {
            maintenances = statusMaintenanceRepository.findByAppId(appId);
        }

        return maintenances.stream()
                .filter(StatusMaintenance::getIsPublic)
                .map(this::mapToStatusMaintenanceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves detailed information about a specific public maintenance window.
     *
     * @param maintenanceId the UUID of the maintenance
     * @return the StatusMaintenanceResponse with full maintenance details
     * @throws RuntimeException if the maintenance is not found or is not public
     */
    public StatusMaintenanceResponse getMaintenanceDetails(UUID maintenanceId) {
        StatusMaintenance maintenance = statusMaintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new RuntimeException("Maintenance not found"));
        
        if (!maintenance.getIsPublic()) {
            throw new RuntimeException("Maintenance is not public");
        }

        return mapToStatusMaintenanceResponse(maintenance);
    }

    /**
     * Generates an overall status summary for public apps.
     * <p>
     * The summary includes counts of operational and affected apps, active incidents,
     * upcoming maintenance, and an overall system status determination.
     * </p>
     *
     * @param tenantName optional tenant name to filter the summary
     * @return a StatusSummaryResponse with aggregated status information
     * @throws RuntimeException if the specified tenant is not found
     */
    public StatusSummaryResponse getStatusSummary(String tenantName) {
        List<StatusApp> apps;
        if (tenantName != null) {
            Tenant tenant = tenantRepository.findByName(tenantName)
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));
            apps = statusAppRepository.findByTenantIdAndIsPublic(tenant.getId(), true);
        } else {
            apps = statusAppRepository.findByIsPublic(true);
        }

        StatusSummaryResponse summary = new StatusSummaryResponse();
        summary.setTotalApps(apps.size());
        
        long operationalCount = apps.stream()
                .filter(app -> "OPERATIONAL".equals(app.getStatus()))
                .count();
        summary.setOperationalApps((int) operationalCount);
        summary.setAppsWithIssues(apps.size() - (int) operationalCount);

        int activeIncidents = 0;
        int upcomingMaintenances = 0;
        
        for (StatusApp app : apps) {
            activeIncidents += statusIncidentRepository.countActiveIncidentsByAppId(app.getId()).intValue();
            upcomingMaintenances += statusMaintenanceRepository.countActiveMaintenanceByAppId(app.getId()).intValue();
        }
        
        summary.setActiveIncidents(activeIncidents);
        summary.setUpcomingMaintenances(upcomingMaintenances);
        
        if (apps.isEmpty() || operationalCount == apps.size()) {
            summary.setOverallStatus("OPERATIONAL");
        } else if (operationalCount > apps.size() / 2) {
            summary.setOverallStatus("DEGRADED");
        } else {
            summary.setOverallStatus("MAJOR_OUTAGE");
        }
        
        summary.setApps(apps.stream()
                .map(this::mapToStatusAppResponse)
                .collect(Collectors.toList()));
        
        return summary;
    }

    /**
     * Retrieves historical status data for a component over a specified period.
     *
     * @param componentId the UUID of the component
     * @param days the number of days of history to retrieve
     * @return a ComponentHistoryResponse with daily status history and uptime statistics
     * @throws RuntimeException if the component is not found or its app is not public
     */
    public ComponentHistoryResponse getComponentHistory(UUID componentId, int days) {
        StatusComponent component = statusComponentRepository.findById(componentId)
                .orElseThrow(() -> new RuntimeException("Component not found"));
        
        if (!component.getApp().getIsPublic()) {
            throw new RuntimeException("Component's app is not public");
        }

        ComponentHistoryResponse history = new ComponentHistoryResponse();
        history.setComponentId(componentId);
        history.setComponentName(component.getName());
        
        List<ComponentHistoryResponse.DailyStatus> dailyStatuses = new ArrayList<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            ComponentHistoryResponse.DailyStatus dailyStatus = new ComponentHistoryResponse.DailyStatus();
            dailyStatus.setDate(date);
            dailyStatus.setStatus("OPERATIONAL");
            dailyStatus.setIncidents(0);
            dailyStatus.setMaintenanceMinutes(0);
            dailyStatuses.add(dailyStatus);
        }
        
        history.setHistory(dailyStatuses);
        history.setUptimePercentage(99.9); // Calculate based on actual data
        history.setTotalIncidents(statusIncidentComponentRepository.countByComponentId(componentId).intValue());
        
        return history;
    }

    /**
     * Calculates uptime statistics for an app over a specified period.
     *
     * @param appId the UUID of the status app
     * @param days the number of days to calculate uptime for
     * @return an UptimeResponse with uptime percentage and outage/maintenance minutes
     * @throws RuntimeException if the app is not found or is not public
     */
    public UptimeResponse getAppUptime(UUID appId, int days) {
        StatusApp app = statusAppRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Status app not found"));
        
        if (!app.getIsPublic()) {
            throw new RuntimeException("Status app is not public");
        }

        UptimeResponse uptime = new UptimeResponse();
        uptime.setAppId(appId);
        uptime.setAppName(app.getName());
        uptime.setDaysCalculated(days);
        
        ZonedDateTime startDate = ZonedDateTime.now().minusDays(days);
        List<StatusIncident> incidents = statusIncidentRepository.findRecentPublicIncidentsByAppId(appId, startDate);

        uptime.setTotalIncidents(incidents.size());
        
        int totalMinutes = days * 24 * 60;
        int outageMinutes = calculateOutageMinutes(incidents);
        int maintenanceMinutes = calculateMaintenanceMinutes(appId, startDate);
        
        uptime.setTotalOutageMinutes(outageMinutes);
        uptime.setTotalMaintenanceMinutes(maintenanceMinutes);
        
        double uptimePercentage = ((double) (totalMinutes - outageMinutes - maintenanceMinutes) / totalMinutes) * 100;
        uptime.setUptimePercentage(Math.round(uptimePercentage * 100.0) / 100.0);
        
        return uptime;
    }

    /**
     * Maps a StatusApp entity to a StatusAppResponse DTO.
     *
     * @param app the StatusApp entity to map
     * @return the mapped StatusAppResponse
     */
    private StatusAppResponse mapToStatusAppResponse(StatusApp app) {
        StatusAppResponse response = new StatusAppResponse();
        response.setId(app.getId());
        response.setName(app.getName());
        response.setDescription(app.getDescription());
        response.setSlug(app.getSlug());
        response.setStatus(app.getStatus());
        response.setIsPublic(app.getIsPublic());
        response.setLastUpdated(app.getLastModifiedDate());
        
        List<StatusComponent> components = statusComponentRepository.findByAppIdOrderByPosition(app.getId());
        response.setComponents(components.stream()
                .map(this::mapToStatusComponentResponse)
                .collect(Collectors.toList()));
        
        List<StatusIncident> activeIncidents = statusIncidentRepository.findByAppIdAndStatus(app.getId(), "RESOLVED");
        if (!activeIncidents.isEmpty()) {
            response.setCurrentIncident(mapToStatusIncidentResponse(activeIncidents.get(0)));
        }
        
        List<StatusMaintenance> upcomingMaintenances = statusMaintenanceRepository
                .findUpcomingMaintenanceByAppId(app.getId(), ZonedDateTime.now());
        response.setUpcomingMaintenances(upcomingMaintenances.stream()
                .limit(3)
                .map(this::mapToStatusMaintenanceResponse)
                .collect(Collectors.toList()));
        
        return response;
    }

    /**
     * Maps a StatusComponent entity to a StatusComponentResponse DTO.
     *
     * @param component the StatusComponent entity to map
     * @return the mapped StatusComponentResponse
     */
    private StatusComponentResponse mapToStatusComponentResponse(StatusComponent component) {
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
     * Maps a StatusIncident entity to a StatusIncidentResponse DTO.
     *
     * @param incident the StatusIncident entity to map
     * @return the mapped StatusIncidentResponse
     */
    private StatusIncidentResponse mapToStatusIncidentResponse(StatusIncident incident) {
        StatusIncidentResponse response = new StatusIncidentResponse();
        response.setId(incident.getId());
        response.setTitle(incident.getTitle());
        response.setDescription(incident.getDescription());
        response.setStatus(incident.getStatus());
        response.setSeverity(incident.getSeverity());
        response.setImpact(incident.getImpact());
        response.setStartedAt(incident.getStartedAt());
        response.setResolvedAt(incident.getResolvedAt());
        
        List<StatusIncidentUpdate> updates = statusIncidentUpdateRepository
                .findByIncidentIdOrderByUpdateTimeDesc(incident.getId());
        response.setUpdates(updates.stream()
                .map(this::mapToStatusIncidentUpdateResponse)
                .collect(Collectors.toList()));
        
        List<StatusIncidentComponent> incidentComponents = statusIncidentComponentRepository
                .findByIncidentId(incident.getId());
        response.setAffectedComponents(incidentComponents.stream()
                .map(ic -> mapToStatusComponentResponse(ic.getComponent()))
                .collect(Collectors.toList()));
        
        return response;
    }

    /**
     * Maps a StatusIncidentUpdate entity to a StatusIncidentUpdateResponse DTO.
     *
     * @param update the StatusIncidentUpdate entity to map
     * @return the mapped StatusIncidentUpdateResponse
     */
    private StatusIncidentUpdateResponse mapToStatusIncidentUpdateResponse(StatusIncidentUpdate update) {
        StatusIncidentUpdateResponse response = new StatusIncidentUpdateResponse();
        response.setId(update.getId());
        response.setStatus(update.getStatus());
        response.setMessage(update.getMessage());
        response.setUpdateTime(update.getUpdateTime());
        response.setCreatedBy(update.getCreatedBy());
        return response;
    }

    /**
     * Maps a StatusMaintenance entity to a StatusMaintenanceResponse DTO.
     *
     * @param maintenance the StatusMaintenance entity to map
     * @return the mapped StatusMaintenanceResponse
     */
    private StatusMaintenanceResponse mapToStatusMaintenanceResponse(StatusMaintenance maintenance) {
        StatusMaintenanceResponse response = new StatusMaintenanceResponse();
        response.setId(maintenance.getId());
        response.setTitle(maintenance.getTitle());
        response.setDescription(maintenance.getDescription());
        response.setStatus(maintenance.getStatus());
        response.setStartsAt(maintenance.getStartsAt());
        response.setEndsAt(maintenance.getEndsAt());
        
        List<StatusMaintenanceComponent> maintenanceComponents = statusMaintenanceComponentRepository
                .findByMaintenanceId(maintenance.getId());
        response.setAffectedComponents(maintenanceComponents.stream()
                .map(mc -> mapToStatusComponentResponse(mc.getComponent()))
                .collect(Collectors.toList()));
        
        return response;
    }

    /**
     * Calculates the total outage duration in minutes from a list of incidents.
     *
     * @param incidents the list of incidents to calculate outage time from
     * @return the total outage duration in minutes
     */
    private int calculateOutageMinutes(List<StatusIncident> incidents) {
        int totalMinutes = 0;
        for (StatusIncident incident : incidents) {
            if (incident.getResolvedAt() != null) {
                long minutes = ChronoUnit.MINUTES.between(incident.getStartedAt(), incident.getResolvedAt());
                totalMinutes += minutes;
            }
        }
        return totalMinutes;
    }

    /**
     * Calculates the total maintenance duration in minutes for an app since a given start date.
     *
     * @param appId the UUID of the status app
     * @param startDate the start date for the calculation period
     * @return the total maintenance duration in minutes
     */
    private int calculateMaintenanceMinutes(UUID appId, ZonedDateTime startDate) {
        List<StatusMaintenance> maintenances = statusMaintenanceRepository.findByAppIdAndStatus(appId, "COMPLETED");
        int totalMinutes = 0;
        for (StatusMaintenance maintenance : maintenances) {
            if (maintenance.getStartsAt().isAfter(startDate)) {
                long minutes = ChronoUnit.MINUTES.between(maintenance.getStartsAt(), maintenance.getEndsAt());
                totalMinutes += minutes;
            }
        }
        return totalMinutes;
    }

    /**
     * Retrieves daily uptime history for an app over a specified period.
     *
     * @param appId the UUID of the status app
     * @param days the number of days of history to retrieve
     * @return an UptimeHistoryResponse with daily uptime data
     * @throws RuntimeException if the app is not found or is not public
     */
    public UptimeHistoryResponse getAppUptimeHistory(UUID appId, int days) {
        StatusApp app = statusAppRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Status app not found"));

        if (!app.getIsPublic()) {
            throw new RuntimeException("Status app is not public");
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<StatusUptimeHistory> historyRecords = statusUptimeHistoryRepository
                .findAppUptimeHistory(appId, startDate, endDate);

        return buildUptimeHistoryResponse(appId, app.getName(), "APP", days, startDate, endDate, historyRecords);
    }

    /**
     * Retrieves daily uptime history for a component over a specified period.
     *
     * @param componentId the UUID of the component
     * @param days the number of days of history to retrieve
     * @return an UptimeHistoryResponse with daily uptime data
     * @throws RuntimeException if the component is not found or its app is not public
     */
    public UptimeHistoryResponse getComponentUptimeHistory(UUID componentId, int days) {
        StatusComponent component = statusComponentRepository.findById(componentId)
                .orElseThrow(() -> new RuntimeException("Component not found"));

        if (!component.getApp().getIsPublic()) {
            throw new RuntimeException("Component's app is not public");
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<StatusUptimeHistory> historyRecords = statusUptimeHistoryRepository
                .findComponentUptimeHistory(componentId, startDate, endDate);

        return buildUptimeHistoryResponse(componentId, component.getName(), "COMPONENT", days, startDate, endDate, historyRecords);
    }

    /**
     * Builds an UptimeHistoryResponse from history records, filling in missing days with default values.
     *
     * @param id the UUID of the app or component
     * @param name the name of the app or component
     * @param type the type identifier ("APP" or "COMPONENT")
     * @param days the number of days in the range
     * @param startDate the start date of the history range
     * @param endDate the end date of the history range
     * @param historyRecords the retrieved history records
     * @return a fully populated UptimeHistoryResponse
     */
    private UptimeHistoryResponse buildUptimeHistoryResponse(UUID id, String name, String type, int days,
            LocalDate startDate, LocalDate endDate, List<StatusUptimeHistory> historyRecords) {

        UptimeHistoryResponse response = new UptimeHistoryResponse();
        response.setId(id);
        response.setName(name);
        response.setType(type);
        response.setDaysInRange(days);

        Map<LocalDate, StatusUptimeHistory> historyMap = new HashMap<>();
        for (StatusUptimeHistory record : historyRecords) {
            historyMap.put(record.getRecordDate(), record);
        }

        List<DailyUptimeResponse> dailyHistory = new ArrayList<>();
        BigDecimal totalUptime = BigDecimal.ZERO;
        int totalIncidents = 0;
        int daysWithData = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DailyUptimeResponse daily = new DailyUptimeResponse();
            daily.setDate(date);

            StatusUptimeHistory record = historyMap.get(date);
            if (record != null) {
                daily.setStatus(record.getStatus());
                daily.setUptimePercentage(record.getUptimePercentage());
                daily.setIncidentCount(record.getIncidentCount());
                daily.setMaintenanceCount(record.getMaintenanceCount());
                totalUptime = totalUptime.add(record.getUptimePercentage());
                totalIncidents += record.getIncidentCount();
                daysWithData++;
            } else {
                daily.setStatus("OPERATIONAL");
                daily.setUptimePercentage(new BigDecimal("100.000"));
                daily.setIncidentCount(0);
                daily.setMaintenanceCount(0);
                totalUptime = totalUptime.add(new BigDecimal("100.000"));
                daysWithData++;
            }

            dailyHistory.add(daily);
        }

        response.setDailyHistory(dailyHistory);
        response.setTotalIncidents(totalIncidents);

        if (daysWithData > 0) {
            response.setOverallUptimePercentage(totalUptime.divide(new BigDecimal(daysWithData), 3, RoundingMode.HALF_UP));
        } else {
            response.setOverallUptimePercentage(new BigDecimal("100.000"));
        }

        return response;
    }

    /**
     * Retrieves uptime history for all components of an app.
     *
     * @param appId the UUID of the status app
     * @param days the number of days of history to retrieve
     * @return a list of UptimeHistoryResponse objects, one for each component
     * @throws RuntimeException if the app is not found or is not public
     */
    public List<UptimeHistoryResponse> getAllComponentsUptimeHistory(UUID appId, int days) {
        StatusApp app = statusAppRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Status app not found"));

        if (!app.getIsPublic()) {
            throw new RuntimeException("Status app is not public");
        }

        List<StatusComponent> components = statusComponentRepository.findByAppIdOrderByPosition(appId);
        List<UptimeHistoryResponse> responses = new ArrayList<>();

        for (StatusComponent component : components) {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days - 1);

            List<StatusUptimeHistory> historyRecords = statusUptimeHistoryRepository
                    .findComponentUptimeHistory(component.getId(), startDate, endDate);

            UptimeHistoryResponse response = buildUptimeHistoryResponse(
                    component.getId(), component.getName(), "COMPONENT", days, startDate, endDate, historyRecords);
            responses.add(response);
        }

        return responses;
    }
}