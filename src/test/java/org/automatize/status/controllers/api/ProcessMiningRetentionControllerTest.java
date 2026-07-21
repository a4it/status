package org.automatize.status.controllers.api;

import org.automatize.status.api.response.ProcessMiningRetentionResponse;
import org.automatize.status.services.ProcessMiningRetentionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link ProcessMiningRetentionController} (CRUD + manual run
 * of log retention rules).
 */
@WebMvcTest(controllers = ProcessMiningRetentionController.class)
class ProcessMiningRetentionControllerTest extends AbstractApiControllerTest {

    private static final String RETENTION_BY_ID_PATH = RETENTION_BY_ID_PATH;

    @MockitoBean
    private ProcessMiningRetentionService service;

    /**
     * Builds a fully-populated {@link ProcessMiningRetentionResponse} fixture for stubbing service calls.
     *
     * @param id the identifier to assign to the response
     * @return a sample retention response with 30-day retention, enabled, for "All Platforms"
     */
    private ProcessMiningRetentionResponse sampleResponse(UUID id) {
        ProcessMiningRetentionResponse r = new ProcessMiningRetentionResponse();
        r.setId(id);
        r.setRetentionDays(30);
        r.setEnabled(true);
        r.setPlatformName("All Platforms");
        return r;
    }

    /**
     * Verifies GET /api/process-mining/retention returns 200 with the JSON list of retention rules
     * supplied by the service.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void findAll_returnsOk() throws Exception {
        when(service.findAll()).thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/process-mining/retention"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].retentionDays").value(30))
                .andExpect(jsonPath("$[0].enabled").value(true));
    }

    /**
     * Verifies GET /api/process-mining/retention/{id} returns 200 with the matching rule when found.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void findById_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get(RETENTION_BY_ID_PATH, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platformName").value("All Platforms"));
    }

    /**
     * Verifies that when the service throws {@code NoSuchElementException} (which carries no
     * {@code @ResponseStatus}), the exception propagates out of {@code mockMvc.perform} rather than
     * being mapped to a status code.
     */
    @Test
    void findById_notFound_propagates() {
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenThrow(new NoSuchElementException("Retention rule not found: " + id));

        // Service throws NoSuchElementException (no @ResponseStatus); with no matching
        // resolver the exception propagates out of perform().
        Assertions.assertThrows(Exception.class,
                () -> mockMvc.perform(get(RETENTION_BY_ID_PATH, id)));
    }

    /**
     * Verifies POST /api/process-mining/retention creates a rule and returns 201 with the created body.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void create_returns201() throws Exception {
        when(service.create(any())).thenReturn(sampleResponse(UUID.randomUUID()));

        String body = "{\"retentionDays\":30,\"enabled\":true}";
        mockMvc.perform(post("/api/process-mining/retention")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.retentionDays").value(30));
    }

    /**
     * Verifies PUT /api/process-mining/retention/{id} updates a rule and returns 200 with the updated body.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void update_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.update(eq(id), any())).thenReturn(sampleResponse(id));

        String body = "{\"retentionDays\":30,\"enabled\":false}";
        mockMvc.perform(put(RETENTION_BY_ID_PATH, id)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    /**
     * Verifies DELETE /api/process-mining/retention/{id} returns 204 and delegates to the service.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void delete_returnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete(RETENTION_BY_ID_PATH, id))
                .andExpect(status().isNoContent());

        verify(service).delete(id);
    }

    /**
     * Verifies POST /api/process-mining/retention/run triggers a manual retention run and returns 200
     * with the aggregate result map (rules processed and total deleted).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void runNow_returnsOk() throws Exception {
        when(service.runRetentionNow())
                .thenReturn(Map.of("rulesProcessed", 2, "totalDeleted", 15, "details", List.of()));

        mockMvc.perform(post("/api/process-mining/retention/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDeleted").value(15))
                .andExpect(jsonPath("$.rulesProcessed").value(2));
    }
}
