package org.automatize.status.controllers.api;

import org.automatize.status.services.scheduler.CronValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.ZonedDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link CronWizardApiController}. Cron validation,
 * preview, presets, and timezone endpoints delegate to the mocked
 * {@link CronValidationService}. No security principal is required as the
 * controller does not read the {@code SecurityContext}.
 */
@WebMvcTest(controllers = CronWizardApiController.class)
class CronWizardApiControllerTest extends AbstractApiControllerTest {

    private static final String EVERY_MINUTE_EXPRESSION = "0 * * * * *";
    private static final String EVERY_MINUTE_DESCRIPTION = "Every minute";

    @MockitoBean
    private CronValidationService cronValidationService;

    // -------------------------------------------------------------------------
    // POST /api/scheduler/cron/validate
    // -------------------------------------------------------------------------

    /**
     * Verifies that a valid cron expression yields {@code 200 OK} with
     * {@code valid=true}, a populated {@code humanReadable} description, a
     * non-empty {@code nextRuns} array, and a {@code null} {@code error}.
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void validate_validExpression_returnsOkValidTrue() throws Exception {
        String expr = EVERY_MINUTE_EXPRESSION;
        when(cronValidationService.isValid(expr)).thenReturn(true);
        when(cronValidationService.toHumanReadable(expr)).thenReturn(EVERY_MINUTE_DESCRIPTION);
        when(cronValidationService.getNextExecutions(eq(expr), eq("UTC"), eq(5)))
                .thenReturn(List.of(ZonedDateTime.now()));

        mockMvc.perform(post("/api/scheduler/cron/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"" + expr + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.humanReadable").value(EVERY_MINUTE_DESCRIPTION))
                .andExpect(jsonPath("$.nextRuns").isArray())
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    /**
     * Verifies that an invalid cron expression still returns {@code 200 OK} but
     * with {@code valid=false}, the service-provided {@code error} message, and a
     * {@code null} {@code humanReadable} field.
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void validate_invalidExpression_returnsOkValidFalseWithError() throws Exception {
        String expr = "not-a-cron";
        when(cronValidationService.isValid(expr)).thenReturn(false);
        when(cronValidationService.getValidationError(expr)).thenReturn("Invalid cron");

        mockMvc.perform(post("/api/scheduler/cron/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"" + expr + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.error").value("Invalid cron"))
                .andExpect(jsonPath("$.humanReadable").value(nullValue()));
    }

    // -------------------------------------------------------------------------
    // POST /api/scheduler/cron/preview
    // -------------------------------------------------------------------------

    /**
     * Verifies that the preview endpoint returns {@code 200 OK} with a JSON array
     * of upcoming execution times for a valid expression, timezone, and count.
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void preview_returnsOkListOfExecutions() throws Exception {
        when(cronValidationService.getNextExecutions(eq(EVERY_MINUTE_EXPRESSION), eq("UTC"), anyInt()))
                .thenReturn(List.of(ZonedDateTime.now(), ZonedDateTime.now().plusMinutes(1)));

        mockMvc.perform(post("/api/scheduler/cron/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"0 * * * * *\",\"timezone\":\"UTC\",\"count\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").exists());
    }

    /**
     * Verifies that posting an empty body (missing expression) still returns
     * {@code 200 OK} with an empty JSON array rather than an error.
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void preview_missingExpression_returnsOkEmptyList() throws Exception {
        when(cronValidationService.getNextExecutions(eq(""), eq("UTC"), anyInt()))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/scheduler/cron/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // -------------------------------------------------------------------------
    // GET /api/scheduler/cron/presets
    // -------------------------------------------------------------------------

    /**
     * Verifies that the presets endpoint returns {@code 200 OK} with the static
     * list of named cron presets, asserting the first entry's name and expression.
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void presets_returnsOkStaticList() throws Exception {
        mockMvc.perform(get("/api/scheduler/cron/presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value(EVERY_MINUTE_DESCRIPTION))
                .andExpect(jsonPath("$[0].expression").value(EVERY_MINUTE_EXPRESSION));
    }

    // -------------------------------------------------------------------------
    // GET /api/scheduler/cron/timezones
    // -------------------------------------------------------------------------

    /**
     * Verifies that the timezones endpoint returns {@code 200 OK} with the list
     * of available timezone identifiers supplied by the mocked service.
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void timezones_returnsOkList() throws Exception {
        when(cronValidationService.getAvailableTimezones())
                .thenReturn(List.of("UTC", "Europe/Brussels"));

        mockMvc.perform(get("/api/scheduler/cron/timezones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("UTC"))
                .andExpect(jsonPath("$[1]").value("Europe/Brussels"));
    }
}
