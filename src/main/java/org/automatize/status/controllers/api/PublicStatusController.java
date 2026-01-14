package org.automatize.status.controllers.api;

import org.automatize.status.api.response.*;
import org.automatize.status.services.PublicStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST API controller for public status page endpoints.
 * <p>
 * This controller provides unauthenticated access to status information intended
 * for public consumption. It exposes read-only endpoints for viewing application
 * status, components, incidents, maintenance schedules, and uptime statistics.
 * All endpoints support CORS for cross-origin access from public status pages.
 * </p>
 *
 * @see PublicStatusService
 */
@RestController
@RequestMapping("/api/public/status")
@CrossOrigin(origins = "*")
public class PublicStatusController {

    @Autowired
    private PublicStatusService publicStatusService;

    /**
     * Retrieves all publicly visible status applications.
     *
     * @param tenantDomain optional tenant domain to filter applications
     * @return ResponseEntity containing a list of public status applications
     */
    @GetMapping("/apps")
    public ResponseEntity<List<StatusAppResponse>> getAllPublicApps(
            @RequestParam(required = false) String tenantDomain) {
        List<StatusAppResponse> apps = publicStatusService.getAllPublicApps(tenantDomain);
        return ResponseEntity.ok(apps);
    }

    /**
     * Retrieves a specific status application by its URL slug.
     *
     * @param slug the URL-friendly slug identifier of the application
     * @param tenantDomain optional tenant domain for multi-tenant filtering
     * @return ResponseEntity containing the status application details
     */
    @GetMapping("/apps/{slug}")
    public ResponseEntity<StatusAppResponse> getAppBySlug(
            @PathVariable String slug,
            @RequestParam(required = false) String tenantDomain) {
        StatusAppResponse app = publicStatusService.getAppBySlug(slug, tenantDomain);
        return ResponseEntity.ok(app);
    }

    /**
     * Retrieves all components for a specific status application.
     *
     * @param appId the UUID of the status application
     * @return ResponseEntity containing a list of status components
     */
    @GetMapping("/apps/{appId}/components")
    public ResponseEntity<List<StatusComponentResponse>> getAppComponents(@PathVariable UUID appId) {
        List<StatusComponentResponse> components = publicStatusService.getAppComponents(appId);
        return ResponseEntity.ok(components);
    }

    /**
     * Retrieves incidents for a specific status application within a time range.
     *
     * @param appId the UUID of the status application
     * @param days the number of past days to include (default: 30)
     * @return ResponseEntity containing a list of incidents
     */
    @GetMapping("/apps/{appId}/incidents")
    public ResponseEntity<List<StatusIncidentResponse>> getAppIncidents(
            @PathVariable UUID appId,
            @RequestParam(defaultValue = "30") int days) {
        List<StatusIncidentResponse> incidents = publicStatusService.getAppIncidents(appId, days);
        return ResponseEntity.ok(incidents);
    }

    /**
     * Retrieves currently active (unresolved) incidents for a status application.
     *
     * @param appId the UUID of the status application
     * @return ResponseEntity containing a list of current incidents
     */
    @GetMapping("/apps/{appId}/incidents/current")
    public ResponseEntity<List<StatusIncidentResponse>> getCurrentIncidents(@PathVariable UUID appId) {
        List<StatusIncidentResponse> incidents = publicStatusService.getCurrentIncidents(appId);
        return ResponseEntity.ok(incidents);
    }

    /**
     * Retrieves detailed information about a specific incident.
     *
     * @param incidentId the UUID of the incident
     * @return ResponseEntity containing the incident details
     */
    @GetMapping("/incidents/{incidentId}")
    public ResponseEntity<StatusIncidentResponse> getIncidentDetails(@PathVariable UUID incidentId) {
        StatusIncidentResponse incident = publicStatusService.getIncidentDetails(incidentId);
        return ResponseEntity.ok(incident);
    }

    /**
     * Retrieves all status updates for a specific incident.
     *
     * @param incidentId the UUID of the incident
     * @return ResponseEntity containing a list of incident updates
     */
    @GetMapping("/incidents/{incidentId}/updates")
    public ResponseEntity<List<StatusIncidentUpdateResponse>> getIncidentUpdates(@PathVariable UUID incidentId) {
        List<StatusIncidentUpdateResponse> updates = publicStatusService.getIncidentUpdates(incidentId);
        return ResponseEntity.ok(updates);
    }

    /**
     * Retrieves maintenance windows for a specific status application.
     *
     * @param appId the UUID of the status application
     * @param type the type of maintenance to retrieve (default: "upcoming")
     * @return ResponseEntity containing a list of maintenance windows
     */
    @GetMapping("/apps/{appId}/maintenance")
    public ResponseEntity<List<StatusMaintenanceResponse>> getAppMaintenance(
            @PathVariable UUID appId,
            @RequestParam(defaultValue = "upcoming") String type) {
        List<StatusMaintenanceResponse> maintenances = publicStatusService.getAppMaintenance(appId, type);
        return ResponseEntity.ok(maintenances);
    }

    /**
     * Retrieves detailed information about a specific maintenance window.
     *
     * @param maintenanceId the UUID of the maintenance window
     * @return ResponseEntity containing the maintenance details
     */
    @GetMapping("/maintenance/{maintenanceId}")
    public ResponseEntity<StatusMaintenanceResponse> getMaintenanceDetails(@PathVariable UUID maintenanceId) {
        StatusMaintenanceResponse maintenance = publicStatusService.getMaintenanceDetails(maintenanceId);
        return ResponseEntity.ok(maintenance);
    }

    /**
     * Retrieves a summary of the overall system status.
     *
     * @param tenantDomain optional tenant domain for multi-tenant filtering
     * @return ResponseEntity containing the status summary
     */
    @GetMapping("/summary")
    public ResponseEntity<StatusSummaryResponse> getStatusSummary(
            @RequestParam(required = false) String tenantDomain) {
        StatusSummaryResponse summary = publicStatusService.getStatusSummary(tenantDomain);
        return ResponseEntity.ok(summary);
    }

    /**
     * Retrieves the status history for a specific component.
     *
     * @param componentId the UUID of the component
     * @param days the number of past days to include (default: 7)
     * @return ResponseEntity containing the component status history
     */
    @GetMapping("/components/{componentId}/history")
    public ResponseEntity<ComponentHistoryResponse> getComponentHistory(
            @PathVariable UUID componentId,
            @RequestParam(defaultValue = "7") int days) {
        ComponentHistoryResponse history = publicStatusService.getComponentHistory(componentId, days);
        return ResponseEntity.ok(history);
    }

    /**
     * Retrieves the uptime statistics for a specific status application.
     *
     * @param appId the UUID of the status application
     * @param days the number of past days to calculate uptime for (default: 90)
     * @return ResponseEntity containing the uptime statistics
     */
    @GetMapping("/apps/{appId}/uptime")
    public ResponseEntity<UptimeResponse> getAppUptime(
            @PathVariable UUID appId,
            @RequestParam(defaultValue = "90") int days) {
        UptimeResponse uptime = publicStatusService.getAppUptime(appId, days);
        return ResponseEntity.ok(uptime);
    }

    /**
     * Retrieves the historical uptime data for a specific status application.
     *
     * @param appId the UUID of the status application
     * @param days the number of past days to include (default: 90)
     * @return ResponseEntity containing the uptime history
     */
    @GetMapping("/apps/{appId}/uptime-history")
    public ResponseEntity<UptimeHistoryResponse> getAppUptimeHistory(
            @PathVariable UUID appId,
            @RequestParam(defaultValue = "90") int days) {
        UptimeHistoryResponse history = publicStatusService.getAppUptimeHistory(appId, days);
        return ResponseEntity.ok(history);
    }

    /**
     * Retrieves the historical uptime data for a specific component.
     *
     * @param componentId the UUID of the component
     * @param days the number of past days to include (default: 90)
     * @return ResponseEntity containing the component uptime history
     */
    @GetMapping("/components/{componentId}/uptime-history")
    public ResponseEntity<UptimeHistoryResponse> getComponentUptimeHistory(
            @PathVariable UUID componentId,
            @RequestParam(defaultValue = "90") int days) {
        UptimeHistoryResponse history = publicStatusService.getComponentUptimeHistory(componentId, days);
        return ResponseEntity.ok(history);
    }

    /**
     * Retrieves the historical uptime data for all components of a status application.
     *
     * @param appId the UUID of the status application
     * @param days the number of past days to include (default: 90)
     * @return ResponseEntity containing a list of uptime histories for all components
     */
    @GetMapping("/apps/{appId}/components/uptime-history")
    public ResponseEntity<List<UptimeHistoryResponse>> getAllComponentsUptimeHistory(
            @PathVariable UUID appId,
            @RequestParam(defaultValue = "90") int days) {
        List<UptimeHistoryResponse> histories = publicStatusService.getAllComponentsUptimeHistory(appId, days);
        return ResponseEntity.ok(histories);
    }
}