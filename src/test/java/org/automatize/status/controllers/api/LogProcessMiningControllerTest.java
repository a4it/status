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

    @MockitoBean
    private ProcessMiningService processMiningService;

    @Test
    void getProcessMiningData_valid_returnsOk() throws Exception {
        ProcessMiningResponse.ProcessCase c = new ProcessMiningResponse.ProcessCase("trace-1", List.of());
        when(processMiningService.buildCases(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new ProcessMiningResponse(List.of(c), 1, false));

        mockMvc.perform(get("/api/logs/process-mining")
                        .param("scope", "platform")
                        .param("scopeId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").value(1))
                .andExpect(jsonPath("$.cases[0].caseId").value("trace-1"));
    }

    @Test
    void getProcessMiningData_missingScope_returns400() throws Exception {
        mockMvc.perform(get("/api/logs/process-mining")
                        .param("scopeId", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProcessMiningData_missingScopeId_returns400() throws Exception {
        mockMvc.perform(get("/api/logs/process-mining")
                        .param("scope", "platform"))
                .andExpect(status().isBadRequest());
    }
}
