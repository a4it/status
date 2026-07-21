package org.automatize.status.controllers.api;

import org.automatize.status.api.response.ProcessMiningResponse;
import org.automatize.status.services.ProcessMiningService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link LogProcessMiningController} (trace-based process
 * mining dataset builder). {@code scope} and {@code scopeId} are required params.
 */
@WebMvcTest(controllers = LogProcessMiningController.class)
class LogProcessMiningControllerTest extends AbstractApiControllerTest {

    private static final String PROCESS_MINING_URL = "/api/logs/process-mining";

    @MockitoBean
    private ProcessMiningService processMiningService;

    /**
     * Verifies that {@code GET /api/logs/process-mining} with valid {@code scope} and
     * {@code scopeId} params returns {@code 200 OK} with the built case dataset in the body.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void getProcessMiningData_valid_returnsOk() throws Exception {
        ProcessMiningResponse.ProcessCase c = new ProcessMiningResponse.ProcessCase("trace-1", List.of());
        when(processMiningService.buildCases(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new ProcessMiningResponse(List.of(c), 1, false));

        mockMvc.perform(get(PROCESS_MINING_URL)
                        .param("scope", "platform")
                        .param("scopeId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").value(1))
                .andExpect(jsonPath("$.cases[0].caseId").value("trace-1"));
    }

    /**
     * Verifies that omitting the required {@code scope} param returns {@code 400 Bad Request}.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void getProcessMiningData_missingScope_returns400() throws Exception {
        mockMvc.perform(get(PROCESS_MINING_URL)
                        .param("scopeId", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that omitting the required {@code scopeId} param returns {@code 400 Bad Request}.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void getProcessMiningData_missingScopeId_returns400() throws Exception {
        mockMvc.perform(get(PROCESS_MINING_URL)
                        .param("scope", "platform"))
                .andExpect(status().isBadRequest());
    }
}
