package org.automatize.status.controllers.api;

import org.automatize.status.api.response.ComponentHistoryResponse;
import org.automatize.status.api.response.StatusAppResponse;
import org.automatize.status.api.response.StatusComponentResponse;
import org.automatize.status.api.response.StatusIncidentResponse;
import org.automatize.status.api.response.StatusIncidentUpdateResponse;
import org.automatize.status.api.response.StatusMaintenanceResponse;
import org.automatize.status.api.response.StatusSummaryResponse;
import org.automatize.status.api.response.UptimeHistoryResponse;
import org.automatize.status.api.response.UptimeResponse;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.services.PublicStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link PublicStatusController}. This controller exposes
 * unauthenticated, read-only endpoints; tests cover the GET mappings, JSON
 * contract, and {@code ResourceNotFoundException} -> 404 mapping. Security
 * filters are disabled ({@code addFilters = false}).
 */
@WebMvcTest(controllers = PublicStatusController.class)
class PublicStatusControllerTest extends AbstractApiControllerTest {

    private static final String APP_NAME = "Public App";
    private static final String APP_SLUG = "public-app";
    private static final String INCIDENT_TITLE = "Outage";
    private static final String JSON_PATH_FIRST_TITLE = "$[0].title";
    private static final String TYPE_COMPONENT = "COMPONENT";

    @MockitoBean
    private PublicStatusService publicStatusService;

    /**
     * Builds a sample {@link StatusAppResponse} fixture for stubbing service calls.
     *
     * @param id the identifier to assign
     * @return a public app response named "Public App" with slug "public-app"
     */
    private StatusAppResponse sampleApp(UUID id) {
        StatusAppResponse r = new StatusAppResponse();
        r.setId(id);
        r.setName(APP_NAME);
        r.setSlug(APP_SLUG);
        return r;
    }

    /**
     * Builds a sample {@link StatusComponentResponse} fixture for stubbing service calls.
     *
     * @param id the identifier to assign
     * @return a component response named "API"
     */
    private StatusComponentResponse sampleComponent(UUID id) {
        StatusComponentResponse r = new StatusComponentResponse();
        r.setId(id);
        r.setName("API");
        return r;
    }

    /**
     * Builds a sample {@link StatusIncidentResponse} fixture for stubbing service calls.
     *
     * @param id the identifier to assign
     * @return an incident response titled "Outage"
     */
    private StatusIncidentResponse sampleIncident(UUID id) {
        StatusIncidentResponse r = new StatusIncidentResponse();
        r.setId(id);
        r.setTitle(INCIDENT_TITLE);
        return r;
    }

    /**
     * Builds a sample {@link StatusMaintenanceResponse} fixture for stubbing service calls.
     *
     * @param id the identifier to assign
     * @return a maintenance response titled "Upgrade"
     */
    private StatusMaintenanceResponse sampleMaintenance(UUID id) {
        StatusMaintenanceResponse r = new StatusMaintenanceResponse();
        r.setId(id);
        r.setTitle("Upgrade");
        return r;
    }

    /**
     * Verifies GET /api/public/status/apps returns 200 with the list of public apps.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getAllPublicApps_returnsOk() throws Exception {
        when(publicStatusService.getAllPublicApps(isNull()))
                .thenReturn(List.of(sampleApp(UUID.randomUUID())));

        mockMvc.perform(get("/api/public/status/apps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(APP_NAME));
    }

    /**
     * Verifies GET /api/public/status/apps/{slug} returns 200 with the app when the slug resolves.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getAppBySlug_found_returnsOk() throws Exception {
        when(publicStatusService.getAppBySlug(eq(APP_SLUG), any()))
                .thenReturn(sampleApp(UUID.randomUUID()));

        mockMvc.perform(get("/api/public/status/apps/{slug}", APP_SLUG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(APP_SLUG));
    }

    /**
     * Verifies GET /api/public/status/apps/{slug} returns 404 when the service throws
     * {@code ResourceNotFoundException} for a non-public/unknown slug.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getAppBySlug_notFound_returns404() throws Exception {
        when(publicStatusService.getAppBySlug(eq("missing"), any()))
                .thenThrow(new ResourceNotFoundException("Status app is not public"));

        mockMvc.perform(get("/api/public/status/apps/{slug}", "missing"))
                .andExpect(status().isNotFound());
    }

    /**
     * Verifies GET /api/public/status/apps/{appId}/components returns 200 with the app's components.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getAppComponents_returnsOk() throws Exception {
        UUID appId = UUID.randomUUID();
        when(publicStatusService.getAppComponents(appId))
                .thenReturn(List.of(sampleComponent(UUID.randomUUID())));

        mockMvc.perform(get("/api/public/status/apps/{appId}/components", appId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("API"));
    }

    /**
     * Verifies GET /api/public/status/apps/{appId}/incidents returns 200 with recent incidents
     * (default 30-day window).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getAppIncidents_returnsOk() throws Exception {
        UUID appId = UUID.randomUUID();
        when(publicStatusService.getAppIncidents(eq(appId), eq(30)))
                .thenReturn(List.of(sampleIncident(UUID.randomUUID())));

        mockMvc.perform(get("/api/public/status/apps/{appId}/incidents", appId))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_FIRST_TITLE).value(INCIDENT_TITLE));
    }

    /**
     * Verifies GET /api/public/status/apps/{appId}/incidents/current returns 200 with active incidents.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getCurrentIncidents_returnsOk() throws Exception {
        UUID appId = UUID.randomUUID();
        when(publicStatusService.getCurrentIncidents(appId))
                .thenReturn(List.of(sampleIncident(UUID.randomUUID())));

        mockMvc.perform(get("/api/public/status/apps/{appId}/incidents/current", appId))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_FIRST_TITLE).value(INCIDENT_TITLE));
    }

    /**
     * Verifies GET /api/public/status/incidents/{incidentId} returns 200 with the incident when public.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getIncidentDetails_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(publicStatusService.getIncidentDetails(id)).thenReturn(sampleIncident(id));

        mockMvc.perform(get("/api/public/status/incidents/{incidentId}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    /**
     * Verifies GET /api/public/status/incidents/{incidentId} returns 404 when the incident is not public.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getIncidentDetails_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(publicStatusService.getIncidentDetails(id))
                .thenThrow(new ResourceNotFoundException("Incident is not public"));

        mockMvc.perform(get("/api/public/status/incidents/{incidentId}", id))
                .andExpect(status().isNotFound());
    }

    /**
     * Verifies GET /api/public/status/incidents/{incidentId}/updates returns 200 with the incident's
     * update timeline.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getIncidentUpdates_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        StatusIncidentUpdateResponse u = new StatusIncidentUpdateResponse();
        u.setId(UUID.randomUUID());
        u.setStatus("MONITORING");
        when(publicStatusService.getIncidentUpdates(id)).thenReturn(List.of(u));

        mockMvc.perform(get("/api/public/status/incidents/{incidentId}/updates", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("MONITORING"));
    }

    /**
     * Verifies GET /api/public/status/apps/{appId}/maintenance returns 200 with maintenance windows
     * (default "upcoming" filter).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getAppMaintenance_returnsOk() throws Exception {
        UUID appId = UUID.randomUUID();
        when(publicStatusService.getAppMaintenance(eq(appId), eq("upcoming")))
                .thenReturn(List.of(sampleMaintenance(UUID.randomUUID())));

        mockMvc.perform(get("/api/public/status/apps/{appId}/maintenance", appId))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_FIRST_TITLE).value("Upgrade"));
    }

    /**
     * Verifies GET /api/public/status/maintenance/{maintenanceId} returns 404 when the window is not public.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getMaintenanceDetails_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(publicStatusService.getMaintenanceDetails(id))
                .thenThrow(new ResourceNotFoundException("Maintenance is not public"));

        mockMvc.perform(get("/api/public/status/maintenance/{maintenanceId}", id))
                .andExpect(status().isNotFound());
    }

    /**
     * Verifies GET /api/public/status/summary returns 200 with the overall status summary.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getStatusSummary_returnsOk() throws Exception {
        StatusSummaryResponse summary = new StatusSummaryResponse();
        summary.setOverallStatus("OPERATIONAL");
        summary.setTotalApps(5);
        when(publicStatusService.getStatusSummary(isNull())).thenReturn(summary);

        mockMvc.perform(get("/api/public/status/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").value("OPERATIONAL"));
    }

    /**
     * Verifies GET /api/public/status/components/{componentId}/history returns 200 with the component's
     * history (default 7-day window).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getComponentHistory_returnsOk() throws Exception {
        UUID componentId = UUID.randomUUID();
        ComponentHistoryResponse history = new ComponentHistoryResponse();
        history.setComponentId(componentId);
        history.setComponentName("API");
        when(publicStatusService.getComponentHistory(eq(componentId), eq(7))).thenReturn(history);

        mockMvc.perform(get("/api/public/status/components/{componentId}/history", componentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.componentName").value("API"));
    }

    /**
     * Verifies GET /api/public/status/apps/{appId}/uptime returns 200 with the uptime percentage
     * (default 90-day window).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getAppUptime_returnsOk() throws Exception {
        UUID appId = UUID.randomUUID();
        UptimeResponse uptime = new UptimeResponse();
        uptime.setAppId(appId);
        uptime.setUptimePercentage(99.9);
        when(publicStatusService.getAppUptime(eq(appId), eq(90))).thenReturn(uptime);

        mockMvc.perform(get("/api/public/status/apps/{appId}/uptime", appId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uptimePercentage").value(99.9));
    }

    /**
     * Verifies GET /api/public/status/apps/{appId}/uptime-history returns 200 with the app-level
     * uptime history (type "APP").
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getAppUptimeHistory_returnsOk() throws Exception {
        UUID appId = UUID.randomUUID();
        UptimeHistoryResponse history = new UptimeHistoryResponse();
        history.setId(appId);
        history.setName(APP_NAME);
        history.setType("APP");
        when(publicStatusService.getAppUptimeHistory(eq(appId), eq(90))).thenReturn(history);

        mockMvc.perform(get("/api/public/status/apps/{appId}/uptime-history", appId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("APP"));
    }

    /**
     * Verifies GET /api/public/status/components/{componentId}/uptime-history returns 200 with the
     * component-level uptime history (type "COMPONENT").
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getComponentUptimeHistory_returnsOk() throws Exception {
        UUID componentId = UUID.randomUUID();
        UptimeHistoryResponse history = new UptimeHistoryResponse();
        history.setId(componentId);
        history.setType(TYPE_COMPONENT);
        when(publicStatusService.getComponentUptimeHistory(eq(componentId), eq(90))).thenReturn(history);

        mockMvc.perform(get("/api/public/status/components/{componentId}/uptime-history", componentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value(TYPE_COMPONENT));
    }

    /**
     * Verifies GET /api/public/status/apps/{appId}/components/uptime-history returns 200 with the
     * uptime history for all of the app's components.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getAllComponentsUptimeHistory_returnsOk() throws Exception {
        UUID appId = UUID.randomUUID();
        UptimeHistoryResponse history = new UptimeHistoryResponse();
        history.setId(UUID.randomUUID());
        history.setType(TYPE_COMPONENT);
        when(publicStatusService.getAllComponentsUptimeHistory(eq(appId), eq(90)))
                .thenReturn(List.of(history));

        mockMvc.perform(get("/api/public/status/apps/{appId}/components/uptime-history", appId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value(TYPE_COMPONENT));
    }
}
