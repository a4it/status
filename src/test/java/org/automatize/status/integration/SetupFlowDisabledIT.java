package org.automatize.status.integration;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Setup API behaviour when setup is NOT yet complete.
 *
 * <p>Overrides {@code app.setup.completed=false} so the setup gate is open. This
 * uses a dedicated class because the flag is read once into the singleton
 * {@code SetupService} bean, so it must be set before the context starts.</p>
 */
@TestPropertySource(properties = "app.setup.completed=false")
class SetupFlowDisabledIT extends AbstractIntegrationIT {

    /**
     * With setup incomplete, the setup write path is reachable WITHOUT auth: the
     * {@code /api/setup/**} paths are {@code permitAll} and the "already complete"
     * 403 gate is not triggered. We assert the request is neither 401 (security
     * would block it) nor 403 (the setup gate), i.e. the setup path is reachable.
     */
    @Test
    void setupTenant_whenSetupIncomplete_isReachable() throws Exception {
        int statusCode = mockMvc.perform(post("/api/setup/tenant")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Bootstrap Tenant\"}"))
                .andReturn().getResponse().getStatus();

        assertNotEquals(401, statusCode, "Setup path must not require authentication");
        assertNotEquals(403, statusCode, "Setup gate must be open when app.setup.completed=false");
    }
}
