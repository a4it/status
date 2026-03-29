package org.automatize.status.services;

import org.automatize.status.api.request.GcScheduleRequest;
import org.automatize.status.api.response.JvmStatsResponse;
import org.springframework.stereotype.Service;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class JvmService {

    private volatile Long lastGcRunAt = null;

    public JvmService() {
    }

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
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                cpuLoad = sunOsBean.getProcessCpuLoad();
            }
        } catch (Exception ignored) {
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

    public GcScheduleRequest getScheduleConfig() {
        GcScheduleRequest req = new GcScheduleRequest();
        req.setEnabled(false);
        req.setCron(null);
        return req;
    }

    public void updateSchedule(boolean enabled, String cron) {
        // Scheduled GC is disabled — the JVM manages its own GC heuristics.
        // Manual trigger is still available via triggerGcNow().
    }

    public void triggerGcNow() {
        System.gc();
        lastGcRunAt = System.currentTimeMillis();
    }
}
