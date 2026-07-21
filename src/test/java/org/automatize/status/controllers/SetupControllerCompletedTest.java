package org.automatize.status.controllers;

import org.automatize.status.controllers.api.AbstractApiControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} slice test for {@link SetupController} with setup already
 * completed ({@code app.setup.completed=true}): the wizard redirects to login.
 * A separate context is required because the gating flag is bound at startup.
 */
@WebMvcTest(controllers = SetupController.class,
        excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@TestPropertySource(properties = {"app.setup.completed=true"})
class SetupControllerCompletedTest extends AbstractApiControllerTest {

    @Test
    void wizard_setupCompleted_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/setup"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
