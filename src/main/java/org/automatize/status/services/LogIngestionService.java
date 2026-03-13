package org.automatize.status.services;

import org.automatize.status.models.DropRule;
import org.automatize.status.models.Log;
import org.automatize.status.models.LogApiKey;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.DropRuleRepository;
import org.automatize.status.repositories.LogApiKeyRepository;
import org.automatize.status.repositories.LogRepository;
import org.automatize.status.repositories.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for ingesting logs via the REST API.
 * Validates API keys, evaluates drop rules, and persists log entries.
 */
@Service
@Transactional
public class LogIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(LogIngestionService.class);

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private LogApiKeyRepository logApiKeyRepository;

    @Autowired
    private DropRuleRepository dropRuleRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Value("${logs.retention.days:30}")
    private int retentionDays;

    /**
     * Validates an API key and returns the associated LogApiKey entity.
     *
     * @param apiKey the API key from the X-Log-Api-Key header
     * @return the valid LogApiKey
     * @throws RuntimeException if the key is invalid or inactive
     */
    @Transactional(readOnly = true)
    public LogApiKey validateApiKey(String apiKey) {
        return logApiKeyRepository.findByApiKeyAndIsActiveTrue(apiKey)
                .orElseThrow(() -> new RuntimeException("Invalid or inactive API key"));
    }

    /**
     * Ingests a single log entry, evaluating drop rules before storage.
     *
     * @param tenantId   optional tenant to scope the log
     * @param timestamp  the log event timestamp (defaults to now if null)
     * @param level      log level (DEBUG, INFO, WARNING, ERROR, CRITICAL)
     * @param service    the service that produced the log
     * @param message    the log message
     * @param metadata   optional JSON metadata string
     * @param traceId    optional distributed trace ID
     * @param requestId  optional request ID
     * @return the persisted Log, or null if dropped by a drop rule
     */
    public Log ingest(UUID tenantId, ZonedDateTime timestamp, String level, String service,
                      String message, String metadata, String traceId, String requestId) {
        List<DropRule> activeRules = dropRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc();
        if (isDropped(activeRules, level, service, message)) {
            logger.debug("Log dropped by drop rule: level={}, service={}", level, service);
            return null;
        }

        Log log = new Log();
        if (tenantId != null) {
            tenantRepository.findById(tenantId).ifPresent(log::setTenant);
        }
        log.setLogTimestamp(timestamp != null ? timestamp : ZonedDateTime.now());
        log.setLevel(level.toUpperCase());
        log.setService(service);
        log.setMessage(message);
        log.setMetadata(metadata);
        log.setTraceId(traceId);
        log.setRequestId(requestId);

        return logRepository.save(log);
    }

    /**
     * Ingests a batch of logs in a single transaction.
     * Each entry is independently evaluated against drop rules.
     *
     * @param entries list of log entry parameters
     * @return count of logs that were stored (not dropped)
     */
    public int ingestBatch(List<LogEntry> entries) {
        List<DropRule> activeRules = dropRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc();
        List<Log> toSave = new ArrayList<>();

        for (LogEntry entry : entries) {
            if (!isDropped(activeRules, entry.level(), entry.service(), entry.message())) {
                Log log = new Log();
                if (entry.tenantId() != null) {
                    tenantRepository.findById(entry.tenantId()).ifPresent(log::setTenant);
                }
                log.setLogTimestamp(entry.timestamp() != null ? entry.timestamp() : ZonedDateTime.now());
                log.setLevel(entry.level().toUpperCase());
                log.setService(entry.service());
                log.setMessage(entry.message());
                log.setMetadata(entry.metadata());
                log.setTraceId(entry.traceId());
                log.setRequestId(entry.requestId());
                toSave.add(log);
            }
        }

        logRepository.saveAll(toSave);
        return toSave.size();
    }

    /**
     * Queries logs with optional filters.
     */
    @Transactional(readOnly = true)
    public Page<Log> searchLogs(UUID tenantId, String level, String service,
                                 ZonedDateTime startDate, ZonedDateTime endDate,
                                 String search, Pageable pageable) {
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return logRepository.searchLogs(tenantId, level, service, startDate, endDate, search, unsorted);
    }

    /**
     * Returns a log entry by ID.
     */
    @Transactional(readOnly = true)
    public Log getById(UUID id) {
        return logRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log not found: " + id));
    }

    /**
     * Returns a distinct list of service names across all logs.
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctServices() {
        return logRepository.findDistinctServices();
    }

    /**
     * Deletes log entries older than the configured retention period.
     * Intended to be called by the scheduled retention job.
     */
    @Async
    public void purgeOldLogs() {
        ZonedDateTime cutoff = ZonedDateTime.now().minusDays(retentionDays);
        int deleted = logRepository.deleteOlderThan(cutoff);
        logger.info("Log retention: deleted {} entries older than {} days", deleted, retentionDays);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean isDropped(List<DropRule> rules, String level, String service, String message) {
        for (DropRule rule : rules) {
            if (matchesRule(rule, level, service, message)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesRule(DropRule rule, String level, String service, String message) {
        if (rule.getLevel() != null && !rule.getLevel().equalsIgnoreCase(level)) {
            return false;
        }
        if (rule.getService() != null && !rule.getService().equalsIgnoreCase(service)) {
            return false;
        }
        if (rule.getMessagePattern() != null && message != null &&
                !message.toLowerCase().contains(rule.getMessagePattern().toLowerCase())) {
            return false;
        }
        return true;
    }

    /**
     * Record representing a single log entry for batch ingestion.
     */
    public record LogEntry(
            UUID tenantId,
            ZonedDateTime timestamp,
            String level,
            String service,
            String message,
            String metadata,
            String traceId,
            String requestId
    ) {}
}
