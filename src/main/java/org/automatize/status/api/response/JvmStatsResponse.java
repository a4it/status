package org.automatize.status.api.response;

import java.util.List;

public class JvmStatsResponse {

    private long heapUsed;
    private long heapMax;
    private long heapCommitted;
    private long nonHeapUsed;
    private long nonHeapCommitted;
    private int threadCount;
    private int daemonThreadCount;
    private int peakThreadCount;
    private long uptimeMs;
    private double processCpuLoad;
    private List<GcCollectorInfo> gcCollectors;
    private GcScheduleInfo gcSchedule;

    public JvmStatsResponse() {
    }

    public long getHeapUsed() { return heapUsed; }
    public void setHeapUsed(long heapUsed) { this.heapUsed = heapUsed; }

    public long getHeapMax() { return heapMax; }
    public void setHeapMax(long heapMax) { this.heapMax = heapMax; }

    public long getHeapCommitted() { return heapCommitted; }
    public void setHeapCommitted(long heapCommitted) { this.heapCommitted = heapCommitted; }

    public long getNonHeapUsed() { return nonHeapUsed; }
    public void setNonHeapUsed(long nonHeapUsed) { this.nonHeapUsed = nonHeapUsed; }

    public long getNonHeapCommitted() { return nonHeapCommitted; }
    public void setNonHeapCommitted(long nonHeapCommitted) { this.nonHeapCommitted = nonHeapCommitted; }

    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

    public int getDaemonThreadCount() { return daemonThreadCount; }
    public void setDaemonThreadCount(int daemonThreadCount) { this.daemonThreadCount = daemonThreadCount; }

    public int getPeakThreadCount() { return peakThreadCount; }
    public void setPeakThreadCount(int peakThreadCount) { this.peakThreadCount = peakThreadCount; }

    public long getUptimeMs() { return uptimeMs; }
    public void setUptimeMs(long uptimeMs) { this.uptimeMs = uptimeMs; }

    public double getProcessCpuLoad() { return processCpuLoad; }
    public void setProcessCpuLoad(double processCpuLoad) { this.processCpuLoad = processCpuLoad; }

    public List<GcCollectorInfo> getGcCollectors() { return gcCollectors; }
    public void setGcCollectors(List<GcCollectorInfo> gcCollectors) { this.gcCollectors = gcCollectors; }

    public GcScheduleInfo getGcSchedule() { return gcSchedule; }
    public void setGcSchedule(GcScheduleInfo gcSchedule) { this.gcSchedule = gcSchedule; }

    public static class GcCollectorInfo {
        private String name;
        private long collectionCount;
        private long collectionTimeMs;

        public GcCollectorInfo() {
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public long getCollectionCount() { return collectionCount; }
        public void setCollectionCount(long collectionCount) { this.collectionCount = collectionCount; }

        public long getCollectionTimeMs() { return collectionTimeMs; }
        public void setCollectionTimeMs(long collectionTimeMs) { this.collectionTimeMs = collectionTimeMs; }
    }

    public static class GcScheduleInfo {
        private boolean enabled;
        private String cron;
        private Long lastRunAtMs;

        public GcScheduleInfo() {
        }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }

        public Long getLastRunAtMs() { return lastRunAtMs; }
        public void setLastRunAtMs(Long lastRunAtMs) { this.lastRunAtMs = lastRunAtMs; }
    }
}
