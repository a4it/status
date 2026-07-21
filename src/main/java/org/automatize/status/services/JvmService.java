package org.automatize.status.services;

import org.automatize.status.api.request.GcScheduleRequest;
import org.automatize.status.api.response.JvmStatsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service that exposes live JVM runtime statistics (memory, threads, uptime,
 * CPU load and garbage collector activity) and provides manual garbage
 * collection control. Scheduled GC is intentionally disabled; the JVM manages
 * its own collection heuristics while a manual trigger remains available.
 */
@Service
public class JvmService {

    private static final Logger logger = LoggerFactory.getLogger(JvmService.class);

    private volatile Long lastGcRunAt = null;

    /**
     * Creates a new JvmService instance.
     */
    public JvmService() {
    }

    /**
     * Collects a snapshot of current JVM statistics including heap and non-heap
     * memory usage, thread counts, uptime, process CPU load and garbage
     * collector metrics.
     *
     * @return a populated {@link JvmStatsResponse} describing the JVM state
     */
    public JvmStatsResponse getStats() {
        JvmStatsResponse response = new JvmStatsResponse();

        // Heap / Non-Heap memory
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memBean.getNonHeapMemoryUsage();

        response.setHeapUsed(heapUsage.getUsed());
        response.setHeapMax(heapUsage.getMax());
        response.setHeapCommitted(heapUsage.getCommitted());
        response.setNonHeapUsed(nonHeapUsage.getUsed());
        response.setNonHeapCommitted(nonHeapUsage.getCommitted());

        // Threads
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        response.setThreadCount(threadBean.getThreadCount());
        response.setDaemonThreadCount(threadBean.getDaemonThreadCount());
        response.setPeakThreadCount(threadBean.getPeakThreadCount());

        // Uptime
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        response.setUptimeMs(runtimeBean.getUptime());

        // CPU load
        double cpuLoad = -1.0;
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            // Only the com.sun implementation exposes the process CPU load reading
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                cpuLoad = sunOsBean.getProcessCpuLoad();
            }
        } catch (Exception e) {
            logger.debug("Unable to read process CPU load", e);
        }
        response.setProcessCpuLoad(cpuLoad);

        // GC collectors
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        List<JvmStatsResponse.GcCollectorInfo> collectors = new ArrayList<>();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            JvmStatsResponse.GcCollectorInfo info = new JvmStatsResponse.GcCollectorInfo();
            info.setName(gcBean.getName());
            info.setCollectionCount(gcBean.getCollectionCount());
            info.setCollectionTimeMs(gcBean.getCollectionTime());
            collectors.add(info);
        }
        response.setGcCollectors(collectors);

        // Schedule info — scheduled GC is disabled; manual trigger is still available
        JvmStatsResponse.GcScheduleInfo scheduleInfo = new JvmStatsResponse.GcScheduleInfo();
        scheduleInfo.setEnabled(false);
        scheduleInfo.setCron(null);
        scheduleInfo.setLastRunAtMs(lastGcRunAt);
        response.setGcSchedule(scheduleInfo);

        return response;
    }

    /**
     * Returns the current GC schedule configuration. Scheduled GC is disabled,
     * so this always reports a disabled schedule with no cron expression.
     *
     * @return a {@link GcScheduleRequest} reflecting the (disabled) schedule state
     */
    public GcScheduleRequest getScheduleConfig() {
        GcScheduleRequest req = new GcScheduleRequest();
        req.setEnabled(false);
        req.setCron(null);
        return req;
    }

    /**
     * Updates the GC schedule configuration. This is a no-op because scheduled
     * GC is disabled and the JVM manages its own collection heuristics; the
     * manual trigger via {@link #triggerGcNow()} remains available.
     *
     * @param enabled whether scheduled GC should be enabled (ignored)
     * @param cron    the cron expression for scheduled GC (ignored)
     */
    public void updateSchedule(boolean enabled, String cron) {
        // Scheduled GC is disabled — the JVM manages its own GC heuristics.
        // Manual trigger is still available via triggerGcNow().
    }

    public void triggerGcNow() {
        System.gc();
        lastGcRunAt = System.currentTimeMillis();
    }
}
