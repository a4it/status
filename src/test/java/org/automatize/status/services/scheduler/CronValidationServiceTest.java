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

    private final CronValidationService service = new CronValidationService();

    // ---- isValid -------------------------------------------------------

    @Test
    void isValid_valid6FieldExpression_returnsTrue() {
        assertThat(service.isValid("0 0 * * * *")).isTrue();
    }

    @Test
    void isValid_everySecondWildcards_returnsTrue() {
        assertThat(service.isValid("* * * * * *")).isTrue();
    }

    @Test
    void isValid_weekdayNamedExpression_returnsTrue() {
        assertThat(service.isValid("0 0 9 * * MON-FRI")).isTrue();
    }

    @Test
    void isValid_null_returnsFalse() {
        assertThat(service.isValid(null)).isFalse();
    }

    @Test
    void isValid_blank_returnsFalse() {
        assertThat(service.isValid("   ")).isFalse();
    }

    @Test
    void isValid_empty_returnsFalse() {
        assertThat(service.isValid("")).isFalse();
    }

    @Test
    void isValid_fiveFieldExpression_returnsFalse() {
        // Spring CronExpression requires 6 fields; a classic 5-field crontab is invalid.
        assertThat(service.isValid("0 * * * *")).isFalse();
    }

    @Test
    void isValid_garbage_returnsFalse() {
        assertThat(service.isValid("not-a-cron")).isFalse();
    }

    @Test
    void isValid_outOfRangeField_returnsFalse() {
        // Minute 99 is out of range.
        assertThat(service.isValid("0 99 * * * *")).isFalse();
    }

    // ---- getValidationError -------------------------------------------

    @Test
    void getValidationError_validExpression_returnsNull() {
        assertThat(service.getValidationError("0 0 * * * *")).isNull();
    }

    @Test
    void getValidationError_invalidExpression_returnsMessage() {
        assertThat(service.getValidationError("bogus")).isNotNull();
    }

    @Test
    void getValidationError_null_returnsMessage() {
        // CronExpression.parse(null) throws, so a non-null error message is returned.
        assertThat(service.getValidationError(null)).isNotNull();
    }

    // ---- getNextExecutions --------------------------------------------

    @Test
    void getNextExecutions_validExpression_returnsRequestedCount() {
        List<ZonedDateTime> next = service.getNextExecutions("0 0 * * * *", "UTC", 3);

        assertThat(next).hasSize(3);
        // Results are strictly ordered ascending.
        assertThat(next).isSortedAccordingTo(ZonedDateTime::compareTo);
        assertThat(next.get(0)).isAfter(ZonedDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(1));
    }

    @Test
    void getNextExecutions_nullTimezone_defaultsToUtc() {
        List<ZonedDateTime> next = service.getNextExecutions("0 0 * * * *", null, 1);

        assertThat(next).hasSize(1);
        assertThat(next.get(0).getZone().getId()).isEqualTo("UTC");
    }

    @Test
    void getNextExecutions_explicitTimezone_usesThatZone() {
        List<ZonedDateTime> next = service.getNextExecutions("0 0 * * * *", "America/New_York", 1);

        assertThat(next).hasSize(1);
        assertThat(next.get(0).getZone().getId()).isEqualTo("America/New_York");
    }

    @Test
    void getNextExecutions_invalidExpression_returnsEmptyList() {
        assertThat(service.getNextExecutions("bogus", "UTC", 3)).isEmpty();
    }

    @Test
    void getNextExecutions_invalidTimezone_returnsEmptyList() {
        assertThat(service.getNextExecutions("0 0 * * * *", "Not/AZone", 3)).isEmpty();
    }

    @Test
    void getNextExecutions_zeroCount_returnsEmptyList() {
        assertThat(service.getNextExecutions("0 0 * * * *", "UTC", 0)).isEmpty();
    }

    // ---- calculateNextRun ---------------------------------------------

    @Test
    void calculateNextRun_validExpression_returnsFutureTime() {
        ZonedDateTime next = service.calculateNextRun("0 0 * * * *", "UTC");

        assertThat(next).isNotNull();
        assertThat(next).isAfter(ZonedDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(1));
    }

    @Test
    void calculateNextRun_invalidExpression_returnsNull() {
        assertThat(service.calculateNextRun("bogus", "UTC")).isNull();
    }

    @Test
    void calculateNextRun_nullTimezone_returnsUtcTime() {
        ZonedDateTime next = service.calculateNextRun("0 0 * * * *", null);

        assertThat(next).isNotNull();
        assertThat(next.getZone().getId()).isEqualTo("UTC");
    }

    // ---- toHumanReadable ----------------------------------------------

    @Test
    void toHumanReadable_null_returnsUnknown() {
        assertThat(service.toHumanReadable(null)).isEqualTo("Unknown");
    }

    @Test
    void toHumanReadable_everyMinute_returnsDescription() {
        assertThat(service.toHumanReadable("0 * * * * *")).isEqualTo("Every minute");
    }

    @Test
    void toHumanReadable_everyNMinutes_returnsDescription() {
        assertThat(service.toHumanReadable("0 0/15 * * * *")).isEqualTo("Every 15 minutes");
    }

    @Test
    void toHumanReadable_everyHour_returnsDescription() {
        assertThat(service.toHumanReadable("0 0 * * * *")).isEqualTo("Every hour");
    }

    @Test
    void toHumanReadable_dailyMidnight_returnsDescription() {
        assertThat(service.toHumanReadable("0 0 0 * * *")).isEqualTo("Daily at midnight");
    }

    @Test
    void toHumanReadable_weekdaysAtHour_returnsDescription() {
        assertThat(service.toHumanReadable("0 0 9 * * MON-FRI")).isEqualTo("Weekdays at 9:00");
    }

    @Test
    void toHumanReadable_dailyAtHour_returnsDescription() {
        assertThat(service.toHumanReadable("0 0 14 * * *")).isEqualTo("Daily at 14:00");
    }

    @Test
    void toHumanReadable_unrecognisedSixFields_returnsOriginal() {
        String cron = "30 15 3 5 6 *";
        assertThat(service.toHumanReadable(cron)).isEqualTo(cron);
    }

    @Test
    void toHumanReadable_wrongFieldCount_returnsOriginal() {
        String cron = "0 0 *";
        assertThat(service.toHumanReadable(cron)).isEqualTo(cron);
    }

    @Test
    void toHumanReadable_expressionWithSurroundingWhitespace_isTrimmed() {
        assertThat(service.toHumanReadable("  0 0 * * * *  ")).isEqualTo("Every hour");
    }

    // ---- getAvailableTimezones ----------------------------------------

    @Test
    void getAvailableTimezones_always_returnsSortedNonEmptyList() {
        List<String> zones = service.getAvailableTimezones();

        assertThat(zones).isNotEmpty();
        assertThat(zones).isSorted();
        assertThat(zones).contains("UTC");
    }
}
