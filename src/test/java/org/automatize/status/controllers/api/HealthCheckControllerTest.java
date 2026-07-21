package org.automatize.status.controllers.api;

import org.automatize.status.api.response.HealthCheckTriggerResponse;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.automatize.status.services.HealthCheckScheduler;
import org.automatize.status.services.HealthCheckSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link HealthCheckController}. The controller autowires
 * four beans (settings service, scheduler, two repositories); all are mocked.
 * Focus is mapping, JSON contract, and delegation.
 */
@WebMvcTest(controllers = HealthCheckController.class)
class HealthCheckControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private HealthCheckSettingsService settingsService;

    @MockitoBean
    private HealthCheckScheduler healthCheckScheduler;

    @MockitoBean
    private StatusAppRepository statusAppRepository;

    @MockitoBean
    private StatusComponentRepository statusComponentRepository;

    private void stubSettings() {
        when(settingsService.isEnabled()).thenReturn(true);
        when(settingsService.getSchedulerIntervalMs()).thenReturn(10000L);
        when(settingsService.getThreadPoolSize()).thenReturn(10);
        when(settingsService.getDefaultIntervalSeconds()).thenReturn(60);
        when(settingsService.getDefaultTimeoutSeconds()).thenReturn(10);
    }

    @Test
    void getSettings_returnsOk() throws Exception {
        stubSettings();

        mockMvc.perform(get("/api/health-checks/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.schedulerIntervalMs").value(10000))
                .andExpect(jsonPath("$.threadPoolSize").value(10));
    }

    @Test
    void updateSettings_returnsOk() throws Exception {
        stubSettings();

        String body = "{\"enabled\":true,\"schedulerIntervalMs\":10000,\"threadPoolSize\":10,"
                + "\"defaultIntervalSeconds\":60,\"defaultTimeoutSeconds\":10}";
        mockMvc.perform(put("/api/health-checks/settings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void getHealthCheckStatus_returnsOkArray() throws Exception {
        when(statusAppRepository.findAll()).thenReturn(List.of());
        when(statusComponentRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/health-checks/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getHealthCheckStatus_withFilters_returnsOk() throws Exception {
        when(statusAppRepository.findAll()).thenReturn(List.of());
        when(statusComponentRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/health-checks/status")
                        .param("platformId", UUID.randomUUID().toString())
                        .param("status", "OPERATIONAL")
                        .param("checkEnabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void triggerAllChecks_returnsOk() throws Exception {
        when(healthCheckScheduler.triggerAllChecks()).thenReturn(3);

        mockMvc.perform(post("/api/health-checks/trigger/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Triggered health checks for 3 entities"));
    }

    @Test
    void triggerAppCheck_returnsOk() throws Exception {
        when(healthCheckScheduler.triggerAppCheck(any()))
                .thenReturn(new HealthCheckTriggerResponse(true, "OK", 5L));

        mockMvc.perform(post("/api/health-checks/trigger/app/{id}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.durationMs").value(5));
    }

    @Test
    void triggerComponentCheck_returnsOk() throws Exception {
        when(healthCheckScheduler.triggerComponentCheck(any()))
                .thenReturn(new HealthCheckTriggerResponse(false, "Component not found", 2L));

        mockMvc.perform(post("/api/health-checks/trigger/component/{id}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }
}
