package org.automatize.status.services;

import org.automatize.status.models.Log;
import org.automatize.status.models.LogMetric;
import org.automatize.status.repositories.LogMetricRepository;
import org.automatize.status.repositories.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for querying log metrics and triggering aggregation.
 * Aggregation reads from the raw logs table and upserts into log_metrics.
 */
@Service
@Transactional
public class LogMetricService {

    private static final Logger logger = LoggerFactory.getLogger(LogMetricService.class);

    @Autowired
    private LogMetricRepository logMetricRepository;

    @Autowired
    private LogRepository logRepository;

    /**
     * Returns all metrics since a given time (for dashboards).
     */
    @Transactional(readOnly = true)
    public List<LogMetric> findSince(ZonedDateTime since) {
        return logMetricRepository.findSince(since);
    }

    /**
     * Returns metrics since a given time for a specific tenant.
     */
    @Transactional(readOnly = true)
    public List<LogMetric> findByTenantSince(UUID tenantId, ZonedDateTime since) {
        return logMetricRepository.findByTenantSince(tenantId, since);
    }

    /**
     * Returns the total log count for the given service/level within the time window.
     * Used by AlertEvaluatorScheduler.
     */
    @Transactional(readOnly = true)
    public long sumCountSince(String service, String level, ZonedDateTime since) {
        Long result = logMetricRepository.sumCountSince(service, level, since);
        return result != null ? result : 0L;
    }

    /**
     * Aggregates raw logs from the last two minutes into log_metrics buckets.
     * Intended to be called by LogMetricScheduler every minute.
     */
    public void aggregateRecentLogs() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime bucketTime = now.truncatedTo(ChronoUnit.MINUTES).minusMinutes(1);

        ZonedDateTime windowStart = bucketTime;
        ZonedDateTime windowEnd = bucketTime.plusMinutes(1);

        // Query raw logs in the previous minute window
        PageRequest all = PageRequest.of(0, Integer.MAX_VALUE);
        logRepository.searchLogs(null, null, null, windowStart, windowEnd, null, all)
                .stream()
                .collect(Collectors.groupingBy(
                        log -> new ServiceLevelKey(
                                log.getTenant() != null ? log.getTenant().getId() : null,
                                log.getService(),
                                log.getLevel()),
                        Collectors.counting()))
                .forEach((key, count) -> upsertMetric(key.tenantId(), key.service(), key.level(),
                        bucketTime, "MINUTE", count));

        logger.debug("Log metrics aggregated for bucket: {}", bucketTime);
    }

    // -------------------------------------------------------------------------

    private void upsertMetric(UUID tenantId, String service, String level,
                               ZonedDateTime bucket, String bucketType, long count) {
        Optional<LogMetric> existing = logMetricRepository
                .findByTenantIdAndServiceAndLevelAndBucketAndBucketType(
                        tenantId, service, level, bucket, bucketType);

        if (existing.isPresent()) {
            LogMetric m = existing.get();
            m.setCount(m.getCount() + count);
            logMetricRepository.save(m);
        } else {
            LogMetric m = new LogMetric();
            if (tenantId != null) {
                // Set tenant lazily via ID — avoids loading entity
                org.automatize.status.models.Tenant t = new org.automatize.status.models.Tenant();
                t.setId(tenantId);
                m.setTenant(t);
            }
            m.setService(service);
            m.setLevel(level);
            m.setBucket(bucket);
            m.setBucketType(bucketType);
            m.setCount(count);
            logMetricRepository.save(m);
        }
    }

    private record ServiceLevelKey(UUID tenantId, String service, String level) {}
}
