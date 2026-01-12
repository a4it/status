package org.automatize.status.controllers.api;

import org.automatize.status.api.response.*;
import org.automatize.status.services.PublicStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/status")
@CrossOrigin(origins = "*")
public class PublicStatusController {

    @Autowired
    private PublicStatusService publicStatusService;

    @GetMapping("/apps")
    public ResponseEntity<List<StatusAppResponse>> getAllPublicApps(
            @RequestParam(required = false) String tenantDomain) {
        List<StatusAppResponse> apps = publicStatusService.getAllPublicApps(tenantDomain);
        return ResponseEntity.ok(apps);
    }

    @GetMapping("/apps/{slug}")
    public ResponseEntity<StatusAppResponse> getAppBySlug(
            @PathVariable String slug,
            @RequestParam(required = false) String tenantDomain) {
        StatusAppResponse app = publicStatusService.getAppBySlug(slug, tenantDomain);
        return ResponseEntity.ok(app);
    }

    @GetMapping("/apps/{appId}/components")
    public ResponseEntity<List<StatusComponentResponse>> getAppComponents(@PathVariable UUID appId) {
        List<StatusComponentResponse> components = publicStatusService.getAppComponents(appId);
        return ResponseEntity.ok(components);
    }

    @GetMapping("/apps/{appId}/incidents")
    public ResponseEntity<List<StatusIncidentResponse>> getAppIncidents(
            @PathVariable UUID appId,
            @RequestParam(defaultValue = "30") int days) {
        List<StatusIncidentResponse> incidents = publicStatusService.getAppIncidents(appId, days);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/apps/{appId}/incidents/current")
    public ResponseEntity<List<StatusIncidentResponse>> getCurrentIncidents(@PathVariable UUID appId) {
        List<StatusIncidentResponse> incidents = publicStatusService.getCurrentIncidents(appId);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/incidents/{incidentId}")
    public ResponseEntity<StatusIncidentResponse> getIncidentDetails(@PathVariable UUID incidentId) {
        StatusIncidentResponse incident = publicStatusService.getIncidentDetails(incidentId);
        return ResponseEntity.ok(incident);
    }

    @GetMapping("/incidents/{incidentId}/updates")
    public ResponseEntity<List<StatusIncidentUpdateResponse>> getIncidentUpdates(@PathVariable UUID incidentId) {
        List<StatusIncidentUpdateResponse> updates = publicStatusService.getIncidentUpdates(incidentId);
        return ResponseEntity.ok(updates);
    }

    @GetMapping("/apps/{appId}/maintenance")
    public ResponseEntity<List<StatusMaintenanceResponse>> getAppMaintenance(
            @PathVariable UUID appId,
            @RequestParam(defaultValue = "upcoming") String type) {
        List<StatusMaintenanceResponse> maintenances = publicStatusService.getAppMaintenance(appId, type);
        return ResponseEntity.ok(maintenances);
    }

    @GetMapping("/maintenance/{maintenanceId}")
    public ResponseEntity<StatusMaintenanceResponse> getMaintenanceDetails(@PathVariable UUID maintenanceId) {
        StatusMaintenanceResponse maintenance = publicStatusService.getMaintenanceDetails(maintenanceId);
        return ResponseEntity.ok(maintenance);
    }

    @GetMapping("/summary")
    public ResponseEntity<StatusSummaryResponse> getStatusSummary(
            @RequestParam(required = false) String tenantDomain) {
        StatusSummaryResponse summary = publicStatusService.getStatusSummary(tenantDomain);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/components/{componentId}/history")
    public ResponseEntity<ComponentHistoryResponse> getComponentHistory(
            @PathVariable UUID componentId,
            @RequestParam(defaultValue = "7") int days) {
        ComponentHistoryResponse history = publicStatusService.getComponentHistory(componentId, days);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/apps/{appId}/uptime")
    public ResponseEntity<UptimeResponse> getAppUptime(
            @PathVariable UUID appId,
            @RequestParam(defaultValue = "90") int days) {
        UptimeResponse uptime = publicStatusService.getAppUptime(appId, days);
        return ResponseEntity.ok(uptime);
    }

    @GetMapping("/apps/{appId}/uptime-history")
    public ResponseEntity<UptimeHistoryResponse> getAppUptimeHistory(
            @PathVariable UUID appId,
            @RequestParam(defaultValue = "90") int days) {
        UptimeHistoryResponse history = publicStatusService.getAppUptimeHistory(appId, days);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/components/{componentId}/uptime-history")
    public ResponseEntity<UptimeHistoryResponse> getComponentUptimeHistory(
            @PathVariable UUID componentId,
            @RequestParam(defaultValue = "90") int days) {
        UptimeHistoryResponse history = publicStatusService.getComponentUptimeHistory(componentId, days);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/apps/{appId}/components/uptime-history")
    public ResponseEntity<List<UptimeHistoryResponse>> getAllComponentsUptimeHistory(
            @PathVariable UUID appId,
            @RequestParam(defaultValue = "90") int days) {
        List<UptimeHistoryResponse> histories = publicStatusService.getAllComponentsUptimeHistory(appId, days);
        return ResponseEntity.ok(histories);
    }
}