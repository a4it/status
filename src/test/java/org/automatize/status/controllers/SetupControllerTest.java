package org.automatize.status.controllers;

import org.automatize.status.controllers.api.AbstractApiControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * {@code @WebMvcTest} slice test for {@link SetupController} with setup NOT yet
 * completed ({@code app.setup.completed=false}): the wizard is served.
 */
@WebMvcTest(controllers = SetupController.class,
        excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@TestPropertySource(properties = {"app.setup.completed=false"})
class SetupControllerTest extends AbstractApiControllerTest {

    @Test
    void wizard_setupNotCompleted_returnsWizardView() throws Exception {
        mockMvc.perform(get("/setup"))
                .andExpect(status().isOk())
                .andExpect(view().name("setup/wizard"))
                .andExpect(model().attributeExists("applicationName"));
    }

    @Test
    void wizard_setupNotCompleted_trailingSlash_returnsWizardView() throws Exception {
        mockMvc.perform(get("/setup/"))
                .andExpect(status().isOk())
                .andExpect(view().name("setup/wizard"));
    }
}
