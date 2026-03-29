package org.automatize.status.services;

import org.automatize.status.api.request.GcScheduleRequest;
import org.automatize.status.api.response.JvmStatsResponse;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Service
public class JvmService {

    private final ThreadPoolTaskScheduler taskScheduler;
    private ScheduledFuture<?> gcFuture;
    private volatile boolean gcEnabled = false;
    private volatile String gcCron = "0 0 * * * *";
    private volatile Long lastGcRunAt = null;

    public JvmService() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("gc-scheduler-");
        taskScheduler.initialize();
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

        // Schedule info
        JvmStatsResponse.GcScheduleInfo scheduleInfo = new JvmStatsResponse.GcScheduleInfo();
        scheduleInfo.setEnabled(gcEnabled);
        scheduleInfo.setCron(gcCron);
        scheduleInfo.setLastRunAtMs(lastGcRunAt);
        response.setGcSchedule(scheduleInfo);

        return response;
    }

    public GcScheduleRequest getScheduleConfig() {
        GcScheduleRequest req = new GcScheduleRequest();
        req.setEnabled(gcEnabled);
        req.setCron(gcCron);
        return req;
    }

    public void updateSchedule(boolean enabled, String cron) {
        // Cancel any existing scheduled task
        if (gcFuture != null && !gcFuture.isCancelled()) {
            gcFuture.cancel(false);
            gcFuture = null;
        }

        gcEnabled = enabled;
        if (cron != null && !cron.isBlank()) {
            gcCron = cron;
        }

        if (enabled && gcCron != null && !gcCron.isBlank()) {
            Runnable gcTask = () -> {
                System.gc();
                lastGcRunAt = System.currentTimeMillis();
            };
            gcFuture = taskScheduler.schedule(gcTask, new CronTrigger(gcCron));
        }
    }

    public void triggerGcNow() {
        System.gc();
        lastGcRunAt = System.currentTimeMillis();
    }
}
