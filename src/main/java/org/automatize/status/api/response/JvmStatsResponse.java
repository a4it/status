package org.automatize.status.api.response;

import java.util.List;

/**
 * <p>
 * Response object exposing runtime JVM statistics for the status-monitoring
 * application's self-monitoring dashboard.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Report heap and non-heap memory usage of the running instance</li>
 *   <li>Report thread counts and process CPU load</li>
 *   <li>Expose garbage collector statistics and GC scheduling configuration</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
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

    /**
     * Default constructor.
     */
    public JvmStatsResponse() {
    }

    /** Gets the used heap memory in bytes. @return the used heap memory */
    public long getHeapUsed() { return heapUsed; }
    /** Sets the used heap memory in bytes. @param heapUsed the used heap memory to set */
    public void setHeapUsed(long heapUsed) { this.heapUsed = heapUsed; }

    /** Gets the maximum heap memory in bytes. @return the maximum heap memory */
    public long getHeapMax() { return heapMax; }
    /** Sets the maximum heap memory in bytes. @param heapMax the maximum heap memory to set */
    public void setHeapMax(long heapMax) { this.heapMax = heapMax; }

    /** Gets the committed heap memory in bytes. @return the committed heap memory */
    public long getHeapCommitted() { return heapCommitted; }
    /** Sets the committed heap memory in bytes. @param heapCommitted the committed heap memory to set */
    public void setHeapCommitted(long heapCommitted) { this.heapCommitted = heapCommitted; }

    /** Gets the used non-heap memory in bytes. @return the used non-heap memory */
    public long getNonHeapUsed() { return nonHeapUsed; }
    /** Sets the used non-heap memory in bytes. @param nonHeapUsed the used non-heap memory to set */
    public void setNonHeapUsed(long nonHeapUsed) { this.nonHeapUsed = nonHeapUsed; }

    /** Gets the committed non-heap memory in bytes. @return the committed non-heap memory */
    public long getNonHeapCommitted() { return nonHeapCommitted; }
    /** Sets the committed non-heap memory in bytes. @param nonHeapCommitted the committed non-heap memory to set */
    public void setNonHeapCommitted(long nonHeapCommitted) { this.nonHeapCommitted = nonHeapCommitted; }

    /** Gets the current live thread count. @return the thread count */
    public int getThreadCount() { return threadCount; }
    /** Sets the current live thread count. @param threadCount the thread count to set */
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

    /** Gets the current daemon thread count. @return the daemon thread count */
    public int getDaemonThreadCount() { return daemonThreadCount; }
    /** Sets the current daemon thread count. @param daemonThreadCount the daemon thread count to set */
    public void setDaemonThreadCount(int daemonThreadCount) { this.daemonThreadCount = daemonThreadCount; }

    /** Gets the peak thread count. @return the peak thread count */
    public int getPeakThreadCount() { return peakThreadCount; }
    /** Sets the peak thread count. @param peakThreadCount the peak thread count to set */
    public void setPeakThreadCount(int peakThreadCount) { this.peakThreadCount = peakThreadCount; }

    /** Gets the JVM uptime in milliseconds. @return the uptime in milliseconds */
    public long getUptimeMs() { return uptimeMs; }
    /** Sets the JVM uptime in milliseconds. @param uptimeMs the uptime to set */
    public void setUptimeMs(long uptimeMs) { this.uptimeMs = uptimeMs; }

    /** Gets the process CPU load (0.0 to 1.0). @return the process CPU load */
    public double getProcessCpuLoad() { return processCpuLoad; }
    /** Sets the process CPU load (0.0 to 1.0). @param processCpuLoad the process CPU load to set */
    public void setProcessCpuLoad(double processCpuLoad) { this.processCpuLoad = processCpuLoad; }

    /** Gets the per-collector GC statistics. @return the GC collector list */
    public List<GcCollectorInfo> getGcCollectors() { return gcCollectors; }
    /** Sets the per-collector GC statistics. @param gcCollectors the GC collector list to set */
    public void setGcCollectors(List<GcCollectorInfo> gcCollectors) { this.gcCollectors = gcCollectors; }

    /** Gets the GC scheduling configuration. @return the GC schedule info */
    public GcScheduleInfo getGcSchedule() { return gcSchedule; }
    /** Sets the GC scheduling configuration. @param gcSchedule the GC schedule info to set */
    public void setGcSchedule(GcScheduleInfo gcSchedule) { this.gcSchedule = gcSchedule; }

    /**
     * Statistics for a single JVM garbage collector.
     */
    public static class GcCollectorInfo {
        private String name;
        private long collectionCount;
        private long collectionTimeMs;

        /**
         * Default constructor.
         */
        public GcCollectorInfo() {
        }

        /** Gets the collector name. @return the collector name */
        public String getName() { return name; }
        /** Sets the collector name. @param name the collector name to set */
        public void setName(String name) { this.name = name; }

        /** Gets the total number of collections performed. @return the collection count */
        public long getCollectionCount() { return collectionCount; }
        /** Sets the total number of collections performed. @param collectionCount the collection count to set */
        public void setCollectionCount(long collectionCount) { this.collectionCount = collectionCount; }

        /** Gets the total collection time in milliseconds. @return the collection time in milliseconds */
        public long getCollectionTimeMs() { return collectionTimeMs; }
        /** Sets the total collection time in milliseconds. @param collectionTimeMs the collection time to set */
        public void setCollectionTimeMs(long collectionTimeMs) { this.collectionTimeMs = collectionTimeMs; }
    }

    /**
     * Configuration describing the scheduled garbage collection routine.
     */
    public static class GcScheduleInfo {
        private boolean enabled;
        private String cron;
        private Long lastRunAtMs;

        /**
         * Default constructor.
         */
        public GcScheduleInfo() {
        }

        /** Gets whether scheduled GC is enabled. @return true if enabled */
        public boolean isEnabled() { return enabled; }
        /** Sets whether scheduled GC is enabled. @param enabled the enabled flag to set */
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        /** Gets the cron expression for scheduled GC. @return the cron expression */
        public String getCron() { return cron; }
        /** Sets the cron expression for scheduled GC. @param cron the cron expression to set */
        public void setCron(String cron) { this.cron = cron; }

        /** Gets the timestamp of the last scheduled GC run in epoch milliseconds. @return the last run timestamp */
        public Long getLastRunAtMs() { return lastRunAtMs; }
        /** Sets the timestamp of the last scheduled GC run in epoch milliseconds. @param lastRunAtMs the last run timestamp to set */
        public void setLastRunAtMs(Long lastRunAtMs) { this.lastRunAtMs = lastRunAtMs; }
    }
}
