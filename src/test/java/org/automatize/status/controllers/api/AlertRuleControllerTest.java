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

    private static final String HIGH_ERROR_RATE = "High error rate";
    private static final String ALERT_RULES_PATH = "/api/alert-rules";
    private static final String ALERT_RULE_BY_ID_PATH = "/api/alert-rules/{id}";

    @MockitoBean
    private AlertRuleService alertRuleService;

    /**
     * Builds a fully populated {@link AlertRule} fixture for use as a service stub
     * return value.
     *
     * @param id the identifier to assign to the rule
     * @return a sample alert rule with representative field values
     */
    private AlertRule sampleRule(UUID id) {
        AlertRule rule = new AlertRule();
        rule.setId(id);
        rule.setName(HIGH_ERROR_RATE);
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

    /**
     * Provides a valid JSON request body satisfying all bean-validation
     * constraints for create/update requests.
     *
     * @return a JSON string representing a well-formed alert rule payload
     */
    private String validBody() {
        return "{\"name\":\"High error rate\",\"service\":\"api\",\"level\":\"ERROR\","
                + "\"thresholdCount\":5,\"windowMinutes\":10,\"cooldownMinutes\":15,"
                + "\"notificationType\":\"EMAIL\",\"notificationTarget\":\"ops@example.com\",\"active\":true}";
    }

    /**
     * Verifies that GET {@code /api/alert-rules} returns HTTP 200 with the
     * serialized collection of rules supplied by the service.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void findAll_returnsOk() throws Exception {
        when(alertRuleService.findAll()).thenReturn(List.of(sampleRule(UUID.randomUUID())));

        mockMvc.perform(get(ALERT_RULES_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(HIGH_ERROR_RATE));
    }

    /**
     * Verifies that GET {@code /api/alert-rules/{id}} returns HTTP 200 with the
     * matching rule when the service finds it.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void findById_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(alertRuleService.findById(id)).thenReturn(sampleRule(id));

        mockMvc.perform(get(ALERT_RULE_BY_ID_PATH, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationType").value("EMAIL"));
    }

    /**
     * Verifies that GET {@code /api/alert-rules/{id}} returns HTTP 404 when the
     * service throws {@link ResourceNotFoundException}.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void findById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(alertRuleService.findById(id))
                .thenThrow(new ResourceNotFoundException("Alert rule not found: " + id));

        mockMvc.perform(get(ALERT_RULE_BY_ID_PATH, id))
                .andExpect(status().isNotFound());
    }

    /**
     * Verifies that POST {@code /api/alert-rules} with a valid body returns HTTP
     * 201 and the created rule.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void create_valid_returns201() throws Exception {
        when(alertRuleService.create(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(sampleRule(UUID.randomUUID()));

        mockMvc.perform(post(ALERT_RULES_PATH).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(HIGH_ERROR_RATE));
    }

    /**
     * Verifies that POST {@code /api/alert-rules} with a body missing the required
     * name field fails bean validation with HTTP 400.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void create_missingName_returns400() throws Exception {
        String body = "{\"thresholdCount\":5,\"windowMinutes\":10,\"notificationType\":\"EMAIL\"}";
        mockMvc.perform(post(ALERT_RULES_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that POST {@code /api/alert-rules} with an unsupported notification
     * type fails bean validation with HTTP 400.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void create_invalidNotificationType_returns400() throws Exception {
        String body = "{\"name\":\"x\",\"thresholdCount\":5,\"windowMinutes\":10,\"notificationType\":\"CARRIER_PIGEON\"}";
        mockMvc.perform(post(ALERT_RULES_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that PUT {@code /api/alert-rules/{id}} with a valid body returns
     * HTTP 200 and the updated rule bearing the requested id.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void update_valid_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(alertRuleService.update(eq(id), any(), any(), any(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(sampleRule(id));

        mockMvc.perform(put(ALERT_RULE_BY_ID_PATH, id).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    /**
     * Verifies that POST {@code /api/alert-rules/{id}/toggle} returns HTTP 200 and
     * the rule with its active flag reflected in the response.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void toggle_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(alertRuleService.toggleActive(id)).thenReturn(sampleRule(id));

        mockMvc.perform(post("/api/alert-rules/{id}/toggle", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));
    }

    /**
     * Verifies that DELETE {@code /api/alert-rules/{id}} returns HTTP 200 with a
     * success message and delegates deletion to the service.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void delete_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete(ALERT_RULE_BY_ID_PATH, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(alertRuleService).delete(id);
    }
}
