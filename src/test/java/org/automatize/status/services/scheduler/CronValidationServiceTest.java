package org.automatize.status.services.scheduler;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CronValidationService}.
 *
 * <p>Pure-logic service with no dependencies — instantiated directly.
 * Uses Spring's 6-field cron format: second minute hour day-of-month month day-of-week.</p>
 */
class CronValidationServiceTest {

    private static final String HOURLY_CRON = "0 0 * * * *";
    private static final String INVALID_CRON = "bogus";

    private final CronValidationService service = new CronValidationService();

    // ---- isValid -------------------------------------------------------

    /**
     * Verifies a well-formed 6-field expression is accepted.
     * Expected outcome: {@code isValid} returns {@code true}.
     */
    @Test
    void isValid_valid6FieldExpression_returnsTrue() {
        assertThat(service.isValid(HOURLY_CRON)).isTrue();
    }

    /**
     * Verifies an all-wildcard "every second" expression is accepted.
     * Expected outcome: {@code isValid} returns {@code true}.
     */
    @Test
    void isValid_everySecondWildcards_returnsTrue() {
        assertThat(service.isValid("* * * * * *")).isTrue();
    }

    /**
     * Verifies an expression using named day-of-week ranges (MON-FRI) is accepted.
     * Expected outcome: {@code isValid} returns {@code true}.
     */
    @Test
    void isValid_weekdayNamedExpression_returnsTrue() {
        assertThat(service.isValid("0 0 9 * * MON-FRI")).isTrue();
    }

    /**
     * Verifies a {@code null} expression is rejected.
     * Expected outcome: {@code isValid} returns {@code false}.
     */
    @Test
    void isValid_null_returnsFalse() {
        assertThat(service.isValid(null)).isFalse();
    }

    /**
     * Verifies a blank (whitespace-only) expression is rejected.
     * Expected outcome: {@code isValid} returns {@code false}.
     */
    @Test
    void isValid_blank_returnsFalse() {
        assertThat(service.isValid("   ")).isFalse();
    }

    /**
     * Verifies an empty expression is rejected.
     * Expected outcome: {@code isValid} returns {@code false}.
     */
    @Test
    void isValid_empty_returnsFalse() {
        assertThat(service.isValid("")).isFalse();
    }

    /**
     * Verifies a classic 5-field crontab expression is rejected (6 fields required).
     * Expected outcome: {@code isValid} returns {@code false}.
     */
    @Test
    void isValid_fiveFieldExpression_returnsFalse() {
        // Spring CronExpression requires 6 fields; a classic 5-field crontab is invalid.
        assertThat(service.isValid("0 * * * *")).isFalse();
    }

    /**
     * Verifies unparseable garbage text is rejected.
     * Expected outcome: {@code isValid} returns {@code false}.
     */
    @Test
    void isValid_garbage_returnsFalse() {
        assertThat(service.isValid("not-a-cron")).isFalse();
    }

    /**
     * Verifies an expression with an out-of-range field value is rejected.
     * Expected outcome: {@code isValid} returns {@code false}.
     */
    @Test
    void isValid_outOfRangeField_returnsFalse() {
        // Minute 99 is out of range.
        assertThat(service.isValid("0 99 * * * *")).isFalse();
    }

    // ---- getValidationError -------------------------------------------

    /**
     * Verifies a valid expression produces no validation error.
     * Expected outcome: {@code getValidationError} returns {@code null}.
     */
    @Test
    void getValidationError_validExpression_returnsNull() {
        assertThat(service.getValidationError(HOURLY_CRON)).isNull();
    }

    /**
     * Verifies an invalid expression yields a non-null error message.
     * Expected outcome: {@code getValidationError} returns a message.
     */
    @Test
    void getValidationError_invalidExpression_returnsMessage() {
        assertThat(service.getValidationError(INVALID_CRON)).isNotNull();
    }

    /**
     * Verifies a {@code null} expression yields a non-null error message.
     * Expected outcome: {@code getValidationError} returns a message.
     */
    @Test
    void getValidationError_null_returnsMessage() {
        // CronExpression.parse(null) throws, so a non-null error message is returned.
        assertThat(service.getValidationError(null)).isNotNull();
    }

    // ---- getNextExecutions --------------------------------------------

    /**
     * Verifies the requested number of upcoming executions is returned, strictly ordered.
     * Expected outcome: a list of the requested size, sorted ascending, starting in the near future.
     */
    @Test
    void getNextExecutions_validExpression_returnsRequestedCount() {
        List<ZonedDateTime> next = service.getNextExecutions(HOURLY_CRON, "UTC", 3);

        assertThat(next).hasSize(3);
        // Results are strictly ordered ascending.
        assertThat(next).isSortedAccordingTo(ZonedDateTime::compareTo);
        assertThat(next.get(0)).isAfter(ZonedDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(1));
    }

    /**
     * Verifies a {@code null} timezone falls back to UTC.
     * Expected outcome: the single returned execution is in the UTC zone.
     */
    @Test
    void getNextExecutions_nullTimezone_defaultsToUtc() {
        List<ZonedDateTime> next = service.getNextExecutions(HOURLY_CRON, null, 1);

        assertThat(next).hasSize(1);
        assertThat(next.get(0).getZone().getId()).isEqualTo("UTC");
    }

    /**
     * Verifies an explicit timezone is honoured.
     * Expected outcome: the returned execution is in the requested zone.
     */
    @Test
    void getNextExecutions_explicitTimezone_usesThatZone() {
        List<ZonedDateTime> next = service.getNextExecutions(HOURLY_CRON, "America/New_York", 1);

        assertThat(next).hasSize(1);
        assertThat(next.get(0).getZone().getId()).isEqualTo("America/New_York");
    }

    /**
     * Verifies an invalid cron expression yields no executions.
     * Expected outcome: an empty list is returned.
     */
    @Test
    void getNextExecutions_invalidExpression_returnsEmptyList() {
        assertThat(service.getNextExecutions(INVALID_CRON, "UTC", 3)).isEmpty();
    }

    /**
     * Verifies an unknown timezone id yields no executions.
     * Expected outcome: an empty list is returned.
     */
    @Test
    void getNextExecutions_invalidTimezone_returnsEmptyList() {
        assertThat(service.getNextExecutions(HOURLY_CRON, "Not/AZone", 3)).isEmpty();
    }

    /**
     * Verifies requesting zero executions yields no executions.
     * Expected outcome: an empty list is returned.
     */
    @Test
    void getNextExecutions_zeroCount_returnsEmptyList() {
        assertThat(service.getNextExecutions(HOURLY_CRON, "UTC", 0)).isEmpty();
    }

    // ---- calculateNextRun ---------------------------------------------

    /**
     * Verifies the next run time for a valid expression lies in the future.
     * Expected outcome: a non-null time in the near future is returned.
     */
    @Test
    void calculateNextRun_validExpression_returnsFutureTime() {
        ZonedDateTime next = service.calculateNextRun(HOURLY_CRON, "UTC");

        assertThat(next).isNotNull();
        assertThat(next).isAfter(ZonedDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(1));
    }

    /**
     * Verifies an invalid expression produces no next run time.
     * Expected outcome: {@code calculateNextRun} returns {@code null}.
     */
    @Test
    void calculateNextRun_invalidExpression_returnsNull() {
        assertThat(service.calculateNextRun(INVALID_CRON, "UTC")).isNull();
    }

    /**
     * Verifies a {@code null} timezone falls back to UTC.
     * Expected outcome: the returned next run time is in the UTC zone.
     */
    @Test
    void calculateNextRun_nullTimezone_returnsUtcTime() {
        ZonedDateTime next = service.calculateNextRun(HOURLY_CRON, null);

        assertThat(next).isNotNull();
        assertThat(next.getZone().getId()).isEqualTo("UTC");
    }

    // ---- toHumanReadable ----------------------------------------------

    /**
     * Verifies a {@code null} expression renders a placeholder.
     * Expected outcome: {@code toHumanReadable} returns "Unknown".
     */
    @Test
    void toHumanReadable_null_returnsUnknown() {
        assertThat(service.toHumanReadable(null)).isEqualTo("Unknown");
    }

    /**
     * Verifies an every-minute expression renders its friendly description.
     * Expected outcome: {@code toHumanReadable} returns "Every minute".
     */
    @Test
    void toHumanReadable_everyMinute_returnsDescription() {
        assertThat(service.toHumanReadable("0 * * * * *")).isEqualTo("Every minute");
    }

    /**
     * Verifies an every-N-minutes expression renders its friendly description.
     * Expected outcome: {@code toHumanReadable} returns "Every 15 minutes".
     */
    @Test
    void toHumanReadable_everyNMinutes_returnsDescription() {
        assertThat(service.toHumanReadable("0 0/15 * * * *")).isEqualTo("Every 15 minutes");
    }

    /**
     * Verifies an hourly expression renders its friendly description.
     * Expected outcome: {@code toHumanReadable} returns "Every hour".
     */
    @Test
    void toHumanReadable_everyHour_returnsDescription() {
        assertThat(service.toHumanReadable(HOURLY_CRON)).isEqualTo("Every hour");
    }

    /**
     * Verifies a daily-at-midnight expression renders its friendly description.
     * Expected outcome: {@code toHumanReadable} returns "Daily at midnight".
     */
    @Test
    void toHumanReadable_dailyMidnight_returnsDescription() {
        assertThat(service.toHumanReadable("0 0 0 * * *")).isEqualTo("Daily at midnight");
    }

    /**
     * Verifies a weekdays-at-hour expression renders its friendly description.
     * Expected outcome: {@code toHumanReadable} returns "Weekdays at 9:00".
     */
    @Test
    void toHumanReadable_weekdaysAtHour_returnsDescription() {
        assertThat(service.toHumanReadable("0 0 9 * * MON-FRI")).isEqualTo("Weekdays at 9:00");
    }

    /**
     * Verifies a daily-at-specific-hour expression renders its friendly description.
     * Expected outcome: {@code toHumanReadable} returns "Daily at 14:00".
     */
    @Test
    void toHumanReadable_dailyAtHour_returnsDescription() {
        assertThat(service.toHumanReadable("0 0 14 * * *")).isEqualTo("Daily at 14:00");
    }

    /**
     * Verifies a valid but unrecognised 6-field pattern is echoed back verbatim.
     * Expected outcome: {@code toHumanReadable} returns the original expression.
     */
    @Test
    void toHumanReadable_unrecognisedSixFields_returnsOriginal() {
        String cron = "30 15 3 5 6 *";
        assertThat(service.toHumanReadable(cron)).isEqualTo(cron);
    }

    /**
     * Verifies an expression with the wrong field count is echoed back verbatim.
     * Expected outcome: {@code toHumanReadable} returns the original expression.
     */
    @Test
    void toHumanReadable_wrongFieldCount_returnsOriginal() {
        String cron = "0 0 *";
        assertThat(service.toHumanReadable(cron)).isEqualTo(cron);
    }

    /**
     * Verifies surrounding whitespace is trimmed before matching.
     * Expected outcome: {@code toHumanReadable} returns "Every hour".
     */
    @Test
    void toHumanReadable_expressionWithSurroundingWhitespace_isTrimmed() {
        assertThat(service.toHumanReadable("  0 0 * * * *  ")).isEqualTo("Every hour");
    }

    // ---- getAvailableTimezones ----------------------------------------

    /**
     * Verifies the available-timezones list is populated, sorted, and includes UTC.
     * Expected outcome: a non-empty, sorted list containing "UTC".
     */
    @Test
    void getAvailableTimezones_always_returnsSortedNonEmptyList() {
        List<String> zones = service.getAvailableTimezones();

        assertThat(zones).isNotEmpty();
        assertThat(zones).isSorted();
        assertThat(zones).contains("UTC");
    }
}
