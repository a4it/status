package org.automatize.status.controllers.api;

import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.AlertRule;
import org.automatize.status.services.AlertRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link AlertRuleController}: mapping, bean validation
 * (400), JSON contract, {@code @ResponseStatus} exception mapping, and delegation
 * to the mocked {@link AlertRuleService}.
 */
@WebMvcTest(controllers = AlertRuleController.class)
class AlertRuleControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private AlertRuleService alertRuleService;

    private AlertRule sampleRule(UUID id) {
        AlertRule rule = new AlertRule();
        rule.setId(id);
        rule.setName("High error rate");
        rule.setService("api");
        rule.setLevel("ERROR");
        rule.setThresholdCount(5L);
        rule.setWindowMinutes(10);
        rule.setCooldownMinutes(15);
        rule.setNotificationType("EMAIL");
        rule.setNotificationTarget("ops@example.com");
        rule.setIsActive(true);
        return rule;
    }

    private String validBody() {
        return "{\"name\":\"High error rate\",\"service\":\"api\",\"level\":\"ERROR\","
                + "\"thresholdCount\":5,\"windowMinutes\":10,\"cooldownMinutes\":15,"
                + "\"notificationType\":\"EMAIL\",\"notificationTarget\":\"ops@example.com\",\"active\":true}";
    }

    @Test
    void findAll_returnsOk() throws Exception {
        when(alertRuleService.findAll()).thenReturn(List.of(sampleRule(UUID.randomUUID())));

        mockMvc.perform(get("/api/alert-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("High error rate"));
    }

    @Test
    void findById_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(alertRuleService.findById(id)).thenReturn(sampleRule(id));

        mockMvc.perform(get("/api/alert-rules/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationType").value("EMAIL"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(alertRuleService.findById(id))
                .thenThrow(new ResourceNotFoundException("Alert rule not found: " + id));

        mockMvc.perform(get("/api/alert-rules/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_valid_returns201() throws Exception {
        when(alertRuleService.create(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(sampleRule(UUID.randomUUID()));

        mockMvc.perform(post("/api/alert-rules").contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("High error rate"));
    }

    @Test
    void create_missingName_returns400() throws Exception {
        String body = "{\"thresholdCount\":5,\"windowMinutes\":10,\"notificationType\":\"EMAIL\"}";
        mockMvc.perform(post("/api/alert-rules").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_invalidNotificationType_returns400() throws Exception {
        String body = "{\"name\":\"x\",\"thresholdCount\":5,\"windowMinutes\":10,\"notificationType\":\"CARRIER_PIGEON\"}";
        mockMvc.perform(post("/api/alert-rules").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_valid_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(alertRuleService.update(eq(id), any(), any(), any(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(sampleRule(id));

        mockMvc.perform(put("/api/alert-rules/{id}", id).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void toggle_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(alertRuleService.toggleActive(id)).thenReturn(sampleRule(id));

        mockMvc.perform(post("/api/alert-rules/{id}/toggle", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void delete_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/alert-rules/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(alertRuleService).delete(id);
    }
}
