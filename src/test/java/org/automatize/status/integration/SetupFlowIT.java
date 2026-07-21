package org.automatize.status.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Setup API behaviour when setup is ALREADY complete.
 *
 * <p>The {@code test} profile sets {@code app.setup.completed=true}, so the setup
 * write endpoints are gated off. The {@code /api/setup/**} paths are
 * {@code permitAll} in security, so gating is enforced by the controller/service,
 * not the filter chain.</p>
 */
class SetupFlowIT extends AbstractIntegrationIT {

    /** The status endpoint is public and reports setup as completed under the test profile. */
    @Test
    void setupStatus_withoutAuth_reportsCompleted() throws Exception {
        mockMvc.perform(get("/api/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupCompleted").value(true));
    }

    /**
     * A setup write endpoint is refused with 403 once setup is complete
     * ({@code SetupApiController} returns 403 "Setup is already complete.").
     */
    @Test
    void setupTenant_whenAlreadyComplete_isForbidden() throws Exception {
        mockMvc.perform(post("/api/setup/tenant")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Should Not Be Created\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
