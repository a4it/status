package org.automatize.status.services.scheduler;

import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service providing cron expression validation, next-execution calculation,
 * and human-readable description generation.
 *
 * <p>Uses Spring's built-in {@link CronExpression} (6-field format:
 * second minute hour day-of-month month day-of-week).</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Service
public class CronValidationService {

    /**
     * Returns {@code true} when the expression is a valid 6-field Spring cron expression.
     *
     * @param cronExpression the expression to validate
     * @return {@code true} if valid
     */
    public boolean isValid(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) return false;
        try {
            CronExpression.parse(cronExpression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the parse error message for an invalid expression, or {@code null}
     * when the expression is valid.
     *
     * @param cronExpression the expression to validate
     * @return error message, or {@code null} if valid
     */
    public String getValidationError(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * Calculates the next {@code count} execution times for the expression.
     *
     * @param cronExpression the 6-field Spring cron expression
     * @param timezone       IANA timezone identifier; defaults to {@code UTC} when null
     * @param count          number of upcoming executions to return
     * @return ordered list of upcoming {@link ZonedDateTime} values; empty on error
     */
    public List<ZonedDateTime> getNextExecutions(String cronExpression, String timezone, int count) {
        List<ZonedDateTime> result = new ArrayList<>();
        try {
            CronExpression expr = CronExpression.parse(cronExpression);
            ZoneId zoneId = ZoneId.of(timezone != null ? timezone : "UTC");
            ZonedDateTime current = ZonedDateTime.now(zoneId);
            for (int i = 0; i < count; i++) {
                current = expr.next(current);
                if (current == null) break;
                result.add(current);
            }
        } catch (Exception e) {
            // Return empty list on any parse/compute error
        }
        return result;
    }

    /**
     * Returns the next single execution time for the expression, or {@code null}
     * when the expression is invalid or yields no future time.
     *
     * @param cronExpression the 6-field Spring cron expression
     * @param timezone       IANA timezone identifier; defaults to {@code UTC} when null
     * @return the next execution time, or {@code null}
     */
    public ZonedDateTime calculateNextRun(String cronExpression, String timezone) {
        List<ZonedDateTime> next = getNextExecutions(cronExpression, timezone, 1);
        return next.isEmpty() ? null : next.get(0);
    }

    /**
     * Converts a common 6-field Spring cron expression to a human-readable English description.
     * Falls back to the raw expression string for patterns not explicitly handled.
     *
     * @param cronExpression the expression to describe
     * @return a human-readable string, or the original expression when unrecognised
     */
    public String toHumanReadable(String cronExpression) {
        if (cronExpression == null) return "Unknown";
        String[] parts = cronExpression.trim().split("\\s+");
        if (parts.length != 6) return cronExpression;

        String sec = parts[0], min = parts[1], hr = parts[2], dom = parts[3], mon = parts[4], dow = parts[5];

        if ("0".equals(sec) && "*".equals(min) && "*".equals(hr) && "*".equals(dom) && "*".equals(mon) && "*".equals(dow))
            return "Every minute";
        if ("0".equals(sec) && min.startsWith("0/") && "*".equals(hr) && "*".equals(dom) && "*".equals(mon) && "*".equals(dow))
            return "Every " + min.substring(2) + " minutes";
        if ("0".equals(sec) && "0".equals(min) && "*".equals(hr) && "*".equals(dom) && "*".equals(mon) && "*".equals(dow))
            return "Every hour";
        if ("0".equals(sec) && "0".equals(min) && "0".equals(hr) && "*".equals(dom) && "*".equals(mon) && "*".equals(dow))
            return "Daily at midnight";
        if ("0".equals(sec) && "0".equals(min) && hr.matches("\\d+") && "*".equals(dom) && "*".equals(mon) && "MON-FRI".equals(dow))
            return "Weekdays at " + hr + ":00";
        if ("0".equals(sec) && "0".equals(min) && hr.matches("\\d+") && "*".equals(dom) && "*".equals(mon) && "*".equals(dow))
            return "Daily at " + hr + ":00";

        return cronExpression;
    }

    /**
     * Returns all available Java {@link ZoneId} identifiers, sorted alphabetically.
     *
     * @return sorted list of IANA timezone IDs
     */
    public List<String> getAvailableTimezones() {
        return ZoneId.getAvailableZoneIds().stream().sorted().toList();
    }
}
