package org.automatize.status.services;

import org.automatize.status.api.request.GcScheduleRequest;
import org.automatize.status.api.response.JvmStatsResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JvmService}.
 *
 * <p>{@code JvmService} has no injected dependencies — it reads live JVM
 * management beans — so it is exercised directly with {@code new}.</p>
 */
class JvmServiceTest {

    private final JvmService jvmService = new JvmService();

    /**
     * Verifies that {@link JvmService#getStats()} returns a non-null response whose
     * heap and non-heap memory figures are populated, with {@code heapMax} allowed to
     * be the {@code -1} "undefined" sentinel.
     */
    @Test
    void getStats_always_returnsPopulatedResponse() {
        JvmStatsResponse response = jvmService.getStats();

        assertThat(response).isNotNull();
        // Heap used and committed are always positive for a running JVM.
        assertThat(response.getHeapUsed()).isPositive();
        assertThat(response.getHeapCommitted()).isPositive();
        // heapMax may be -1 (undefined) on some collectors, so allow >= -1.
        assertThat(response.getHeapMax()).isGreaterThanOrEqualTo(-1L);
        assertThat(response.getNonHeapUsed()).isPositive();
        assertThat(response.getNonHeapCommitted()).isPositive();
    }

    /**
     * Verifies that the stats response reports thread counts: a positive live thread
     * count, a non-negative daemon count, and a peak count at least as large as the
     * live count.
     */
    @Test
    void getStats_always_reportsThreadCounts() {
        JvmStatsResponse response = jvmService.getStats();

        assertThat(response.getThreadCount()).isPositive();
        assertThat(response.getDaemonThreadCount()).isGreaterThanOrEqualTo(0);
        assertThat(response.getPeakThreadCount()).isGreaterThanOrEqualTo(response.getThreadCount());
    }

    /**
     * Verifies that the stats response reports a non-negative JVM uptime in milliseconds.
     */
    @Test
    void getStats_always_reportsUptime() {
        JvmStatsResponse response = jvmService.getStats();

        assertThat(response.getUptimeMs()).isGreaterThanOrEqualTo(0L);
    }

    /**
     * Verifies that the reported process CPU load is either a valid fraction in
     * {@code [0,1]} or the {@code -1.0} sentinel used when the value is unavailable.
     */
    @Test
    void getStats_always_reportsProcessCpuLoad() {
        JvmStatsResponse response = jvmService.getStats();

        // Either a valid load in [0,1] or the -1.0 sentinel when unavailable.
        double load = response.getProcessCpuLoad();
        assertThat(load == -1.0 || (load >= 0.0 && load <= 1.0)).isTrue();
    }

    /**
     * Verifies that the stats response exposes a non-empty list of garbage collector
     * entries, each with a non-blank name and non-negative (or {@code -1} sentinel)
     * collection count and time.
     */
    @Test
    void getStats_always_populatesGcCollectors() {
        JvmStatsResponse response = jvmService.getStats();

        assertThat(response.getGcCollectors()).isNotNull();
        // A running JVM always exposes at least one garbage collector bean.
        assertThat(response.getGcCollectors()).isNotEmpty();
        for (JvmStatsResponse.GcCollectorInfo info : response.getGcCollectors()) {
            assertThat(info.getName()).isNotBlank();
            assertThat(info.getCollectionCount()).isGreaterThanOrEqualTo(-1L);
            assertThat(info.getCollectionTimeMs()).isGreaterThanOrEqualTo(-1L);
        }
    }

    /**
     * Verifies that, with scheduled GC disabled by default, the GC schedule info
     * reports {@code enabled=false} and a null cron expression.
     */
    @Test
    void getStats_scheduledGcDisabled_reportsScheduleDisabled() {
        JvmStatsResponse response = jvmService.getStats();

        JvmStatsResponse.GcScheduleInfo schedule = response.getGcSchedule();
        assertThat(schedule).isNotNull();
        assertThat(schedule.isEnabled()).isFalse();
        assertThat(schedule.getCron()).isNull();
    }

    /**
     * Verifies that before any manual GC is triggered, the schedule's last-run
     * timestamp is null.
     */
    @Test
    void getStats_beforeManualGc_lastRunIsNull() {
        JvmStatsResponse response = jvmService.getStats();

        assertThat(response.getGcSchedule().getLastRunAtMs()).isNull();
    }

    /**
     * Verifies that after {@link JvmService#triggerGcNow()} runs, the schedule's
     * last-run timestamp is populated and falls within the window bracketing the call.
     */
    @Test
    void getStats_afterTriggerGcNow_lastRunIsPopulated() {
        long before = System.currentTimeMillis();
        jvmService.triggerGcNow();
        long after = System.currentTimeMillis();

        JvmStatsResponse response = jvmService.getStats();
        Long lastRun = response.getGcSchedule().getLastRunAtMs();

        assertThat(lastRun).isNotNull();
        assertThat(lastRun).isBetween(before, after);
    }

    /**
     * Verifies that {@link JvmService#getScheduleConfig()} returns a non-null config
     * reflecting the disabled default: {@code enabled=false} and a null cron.
     */
    @Test
    void getScheduleConfig_always_returnsDisabledConfig() {
        GcScheduleRequest config = jvmService.getScheduleConfig();

        assertThat(config).isNotNull();
        assertThat(config.isEnabled()).isFalse();
        assertThat(config.getCron()).isNull();
    }

    /**
     * Verifies that {@link JvmService#updateSchedule(boolean, String)} is a no-op:
     * it neither throws nor enables scheduling on subsequent config reads.
     */
    @Test
    void updateSchedule_anyInput_isNoOp() {
        // Scheduled GC is disabled; updateSchedule must not throw and must not
        // enable scheduling on subsequent reads.
        jvmService.updateSchedule(true, "0 0 * * * *");

        GcScheduleRequest config = jvmService.getScheduleConfig();
        assertThat(config.isEnabled()).isFalse();
        assertThat(config.getCron()).isNull();
    }

    /**
     * Verifies that invoking {@link JvmService#triggerGcNow()} twice results in a
     * second last-run timestamp that is at least as recent as the first.
     */
    @Test
    void triggerGcNow_calledTwice_advancesLastRunTimestamp() {
        jvmService.triggerGcNow();
        Long first = jvmService.getStats().getGcSchedule().getLastRunAtMs();

        jvmService.triggerGcNow();
        Long second = jvmService.getStats().getGcSchedule().getLastRunAtMs();

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(second).isGreaterThanOrEqualTo(first);
    }
}
