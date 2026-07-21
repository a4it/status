package org.automatize.status.controllers.api;

import org.automatize.status.api.response.StatusIncidentResponse;
import org.automatize.status.api.response.StatusIncidentUpdateResponse;
import org.automatize.status.exceptions.BusinessRuleException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.services.StatusIncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link StatusIncidentController}. Security filters are
 * disabled ({@code addFilters = false}); focus is request mapping, bean
 * validation (400), JSON contract, {@code @ResponseStatus} exception mapping
 * (404/409), and delegation to the (mocked) service layer.
 */
@WebMvcTest(controllers = StatusIncidentController.class)
class StatusIncidentControllerTest extends AbstractApiControllerTest {

    private static final String INCIDENT_TITLE = "DB outage";
    private static final String INCIDENTS_PATH = "/api/incidents";
    private static final String INCIDENT_BY_ID_PATH = "/api/incidents/{id}";
    private static final String INCIDENT_UPDATES_PATH = "/api/incidents/{id}/updates";

    @MockitoBean
    private StatusIncidentService statusIncidentService;

    /**
     * Builds a sample {@link StatusIncidentResponse} for stubbing service returns.
     *
     * @param id the identifier to assign to the response
     * @return a sample incident response with representative field values
     */
    private StatusIncidentResponse sampleResponse(UUID id) {
        StatusIncidentResponse r = new StatusIncidentResponse();
        r.setId(id);
        r.setTitle(INCIDENT_TITLE);
        r.setStatus("INVESTIGATING");
        r.setSeverity("MAJOR");
        return r;
    }

    /**
     * Builds a sample {@link StatusIncidentUpdateResponse} for stubbing service returns.
     *
     * @param id the identifier to assign to the update
     * @return a sample incident update response with representative field values
     */
    private StatusIncidentUpdateResponse sampleUpdate(UUID id) {
        StatusIncidentUpdateResponse u = new StatusIncidentUpdateResponse();
        u.setId(id);
        u.setStatus("MONITORING");
        u.setMessage("Recovering");
        return u;
    }

    /**
     * Provides a minimal valid JSON request body that satisfies bean validation for incident creation.
     *
     * @return a JSON string with the required incident fields
     */
    private String validBody() {
        return "{\"appId\":\"" + UUID.randomUUID() + "\",\"title\":\"DB outage\","
                + "\"status\":\"INVESTIGATING\",\"severity\":\"MAJOR\","
                + "\"startedAt\":\"2026-01-01T10:00:00Z\"}";
    }

    @Test
    /**
     * Verifies GET /api/incidents returns 200 OK with a paged JSON body of incidents.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getAllIncidents_returnsOkPage() throws Exception {
        when(statusIncidentService.getAllIncidents(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse(UUID.randomUUID()))));

        mockMvc.perform(get(INCIDENTS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value(INCIDENT_TITLE));
    }

    @Test
    /**
     * Verifies GET /api/incidents/{id} returns 200 OK with the incident when it exists.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getIncidentById_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusIncidentService.getIncidentById(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get("/api/incidents/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("MAJOR"));
    }

    @Test
    /**
     * Verifies GET /api/incidents/{id} maps {@link ResourceNotFoundException} to 404 Not Found.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getIncidentById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusIncidentService.getIncidentById(id))
                .thenThrow(new ResourceNotFoundException("Incident not found with id: " + id));

        mockMvc.perform(get("/api/incidents/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    /**
     * Verifies POST /api/incidents with a valid body returns 201 Created and echoes the incident.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void createIncident_valid_returns201() throws Exception {
        when(statusIncidentService.createIncident(any())).thenReturn(sampleResponse(UUID.randomUUID()));

        mockMvc.perform(post(INCIDENTS_PATH).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(INCIDENT_TITLE));
    }

    @Test
    /**
     * Verifies POST /api/incidents with a missing title fails bean validation with 400 Bad Request.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void createIncident_missingTitle_returns400() throws Exception {
        String body = "{\"appId\":\"" + UUID.randomUUID() + "\",\"status\":\"INVESTIGATING\","
                + "\"severity\":\"MAJOR\",\"startedAt\":\"2026-01-01T10:00:00Z\"}";
        mockMvc.perform(post("/api/incidents").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    /**
     * Verifies POST /api/incidents with a missing appId fails bean validation with 400 Bad Request.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void createIncident_missingAppId_returns400() throws Exception {
        String body = "{\"title\":\"DB outage\",\"status\":\"INVESTIGATING\","
                + "\"severity\":\"MAJOR\",\"startedAt\":\"2026-01-01T10:00:00Z\"}";
        mockMvc.perform(post("/api/incidents").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    /**
     * Verifies PUT /api/incidents/{id} with a valid body returns 200 OK with the updated incident.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void updateIncident_valid_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusIncidentService.updateIncident(eq(id), any())).thenReturn(sampleResponse(id));

        mockMvc.perform(put("/api/incidents/{id}", id).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    /**
     * Verifies DELETE /api/incidents/{id} returns 200 OK with a success message and delegates to the service.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void deleteIncident_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/incidents/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(statusIncidentService).deleteIncident(id);
    }

    @Test
    /**
     * Verifies POST /api/incidents/{id}/updates with a valid body returns 201 Created and echoes the update.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void addIncidentUpdate_valid_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusIncidentService.addIncidentUpdate(eq(id), any())).thenReturn(sampleUpdate(UUID.randomUUID()));

        String body = "{\"status\":\"MONITORING\",\"message\":\"Recovering\"}";
        mockMvc.perform(post("/api/incidents/{id}/updates", id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Recovering"));
    }

    @Test
    /**
     * Verifies POST /api/incidents/{id}/updates with a missing message fails bean validation with 400 Bad Request.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void addIncidentUpdate_missingMessage_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        String body = "{\"status\":\"MONITORING\"}";
        mockMvc.perform(post("/api/incidents/{id}/updates", id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    /**
     * Verifies GET /api/incidents/{id}/updates returns 200 OK with the incident's updates.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getIncidentUpdates_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusIncidentService.getIncidentUpdates(id)).thenReturn(List.of(sampleUpdate(UUID.randomUUID())));

        mockMvc.perform(get("/api/incidents/{id}/updates", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("MONITORING"));
    }

    @Test
    /**
     * Verifies PATCH /api/incidents/{id}/resolve returns 200 OK with the resolved incident.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void resolveIncident_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusIncidentService.resolveIncident(eq(id), any())).thenReturn(sampleResponse(id));

        mockMvc.perform(patch("/api/incidents/{id}/resolve", id)
                        .contentType(MediaType.TEXT_PLAIN).content("Resolved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    /**
     * Verifies PATCH /api/incidents/{id}/resolve maps {@link BusinessRuleException} for an already-resolved incident to 409 Conflict.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void resolveIncident_alreadyResolved_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusIncidentService.resolveIncident(eq(id), any()))
                .thenThrow(new BusinessRuleException("Incident is already resolved"));

        mockMvc.perform(patch("/api/incidents/{id}/resolve", id)
                        .contentType(MediaType.TEXT_PLAIN).content("Resolved"))
                .andExpect(status().isConflict());
    }

    @Test
    /**
     * Verifies GET /api/incidents/active returns 200 OK with the currently active incidents.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getActiveIncidents_returnsOk() throws Exception {
        when(statusIncidentService.getActiveIncidents(any(), any()))
                .thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/incidents/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value(INCIDENT_TITLE));
    }
}
