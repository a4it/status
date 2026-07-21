package org.automatize.status.controllers;

import org.automatize.status.controllers.api.AbstractApiControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * {@code @WebMvcTest} slice tests for {@link PublicController}. Asserts view
 * names and model attributes for the public-facing status pages.
 */
@WebMvcTest(controllers = PublicController.class,
        excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
class PublicControllerTest extends AbstractApiControllerTest {

    @Test
    void status_returnsStatusViewWithCommonAttributes() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/status"))
                .andExpect(model().attributeExists("applicationName", "serverPort",
                        "buildNumber", "buildDate", "copyright"));
    }

    @Test
    void incidents_returnsIncidentsView() throws Exception {
        mockMvc.perform(get("/incidents"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/incidents"))
                .andExpect(model().attributeExists("applicationName"));
    }

    @Test
    void incidentDetail_returnsDetailViewWithIncidentId() throws Exception {
        UUID incidentId = UUID.randomUUID();
        mockMvc.perform(get("/incidents/{incidentId}", incidentId))
                .andExpect(status().isOk())
                .andExpect(view().name("public/incident-detail"))
                .andExpect(model().attribute("incidentId", incidentId));
    }

    @Test
    void maintenance_returnsMaintenanceView() throws Exception {
        mockMvc.perform(get("/maintenance"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/maintenance"));
    }

    @Test
    void history_returnsHistoryView() throws Exception {
        mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/history"));
    }
}
