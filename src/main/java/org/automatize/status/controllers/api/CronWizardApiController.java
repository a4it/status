package org.automatize.status.controllers.api;

import org.automatize.status.services.scheduler.CronValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.*;

/**
 * REST API controller providing cron expression validation, preview, presets, and timezone utilities.
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@RestController
@RequestMapping("/api/scheduler/cron")
@PreAuthorize("isAuthenticated()")
public class CronWizardApiController {

    private static final String EXPRESSION = "expression";

    @Autowired
    private CronValidationService cronValidationService;

    // -------------------------------------------------------------------------
    // Validate a cron expression
    // -------------------------------------------------------------------------

    /**
     * Validates a cron expression and, when valid, returns a human-readable description
     * and a preview of upcoming executions.
     * <p>
     * Handles {@code POST /api/scheduler/cron/validate}.
     * </p>
     *
     * @param request request body containing the {@code expression} to validate
     * @return ResponseEntity containing validity, a human-readable form, the next runs, and any error
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody Map<String, String> request) {
        String expression = request.get(EXPRESSION);
        boolean valid = cronValidationService.isValid(expression);
        String error = valid ? null : cronValidationService.getValidationError(expression);

        List<String> nextRuns = Collections.emptyList();
        String humanReadable = null;
        // Only compute human-readable text and next runs when the expression is valid
        if (valid) {
            humanReadable = cronValidationService.toHumanReadable(expression);
            nextRuns = cronValidationService.getNextExecutions(expression, "UTC", 5)
                    .stream().map(ZonedDateTime::toString).toList();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", valid);
        result.put("humanReadable", humanReadable);
        result.put("nextRuns", nextRuns);
        result.put("error", error);
        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------------------------
    // Preview next N executions with timezone
    // -------------------------------------------------------------------------

    /**
     * Previews the next executions of a cron expression in a given timezone.
     * <p>
     * Handles {@code POST /api/scheduler/cron/preview}.
     * </p>
     *
     * @param request request body containing {@code expression}, optional {@code timezone}
     *                (default {@code UTC}) and optional {@code count} (default 5)
     * @return ResponseEntity containing the formatted list of upcoming execution times
     */
    @PostMapping("/preview")
    public ResponseEntity<List<String>> preview(@RequestBody Map<String, Object> request) {
        String expression = (String) request.getOrDefault(EXPRESSION, "");
        String timezone = (String) request.getOrDefault("timezone", "UTC");
        int count = 5;
        Object countObj = request.get("count");
        // Use the supplied count only when it is a numeric value
        if (countObj instanceof Number n) {
            count = n.intValue();
        }

        List<String> formatted = cronValidationService
                .getNextExecutions(expression, timezone, count)
                .stream()
                .map(ZonedDateTime::toString)
                .toList();
        return ResponseEntity.ok(formatted);
    }

    // -------------------------------------------------------------------------
    // Common cron presets
    // -------------------------------------------------------------------------

    @GetMapping("/presets")
    public ResponseEntity<List<Map<String, String>>> presets() {
        List<Map<String, String>> presets = new ArrayList<>();
        presets.add(preset("Every minute",         "0 * * * * *",       "Every minute"));
        presets.add(preset("Every 5 minutes",      "0 0/5 * * * *",     "Every 5 minutes"));
        presets.add(preset("Every 15 minutes",     "0 0/15 * * * *",    "Every 15 minutes"));
        presets.add(preset("Every 30 minutes",     "0 0/30 * * * *",    "Every 30 minutes"));
        presets.add(preset("Every hour",           "0 0 * * * *",       "Every hour"));
        presets.add(preset("Every 6 hours",        "0 0 0/6 * * *",     "Every 6 hours"));
        presets.add(preset("Every 12 hours",       "0 0 0/12 * * *",    "Every 12 hours"));
        presets.add(preset("Daily at midnight",    "0 0 0 * * *",       "Daily at midnight"));
        presets.add(preset("Daily at 6am",         "0 0 6 * * *",       "Daily at 06:00"));
        presets.add(preset("Daily at noon",        "0 0 12 * * *",      "Daily at 12:00"));
        presets.add(preset("Weekdays at 8am",      "0 0 8 * * MON-FRI", "Weekdays at 08:00"));
        presets.add(preset("Weekdays at midnight", "0 0 0 * * MON-FRI", "Weekdays at midnight"));
        presets.add(preset("Weekly on Monday",     "0 0 0 * * MON",     "Weekly on Monday at midnight"));
        presets.add(preset("Monthly on 1st",       "0 0 0 1 * *",       "Monthly on the 1st at midnight"));
        return ResponseEntity.ok(presets);
    }

    // -------------------------------------------------------------------------
    // Available timezones
    // -------------------------------------------------------------------------

    @GetMapping("/timezones")
    public ResponseEntity<List<String>> timezones() {
        return ResponseEntity.ok(cronValidationService.getAvailableTimezones());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<String, String> preset(String name, String expression, String humanReadable) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put(EXPRESSION, expression);
        m.put("humanReadable", humanReadable);
        return m;
    }
}
