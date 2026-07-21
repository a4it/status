package org.automatize.status.services;

import jakarta.persistence.EntityManager;
import org.automatize.status.api.request.ProcessMiningRetentionRequest;
import org.automatize.status.api.response.ProcessMiningRetentionResponse;
import org.automatize.status.models.ProcessMiningRetentionRule;
import org.automatize.status.models.StatusPlatform;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.ProcessMiningRetentionRuleRepository;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusPlatformRepository;
import org.automatize.status.repositories.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * Service responsible for managing process mining log retention rules and enforcing them.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for retention rules scoped by platform and/or tenant</li>
 *   <li>Run retention cleanup (manually or on a daily schedule) to purge expired log data</li>
 *   <li>Track the last run time and deleted record count per rule</li>
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
@Service
@Transactional
public class ProcessMiningRetentionService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessMiningRetentionService.class);

    @Autowired
    private ProcessMiningRetentionRuleRepository retentionRuleRepository;

    @Autowired
    private StatusAppRepository statusAppRepository;

    @Autowired
    private StatusPlatformRepository statusPlatformRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private EntityManager entityManager;

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    /**
     * Retrieves all configured retention rules.
     *
     * @return a list of ProcessMiningRetentionResponse objects for all rules
     */
    public List<ProcessMiningRetentionResponse> findAll() {
        return retentionRuleRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single retention rule by its unique identifier.
     *
     * @param id the UUID of the retention rule
     * @return the ProcessMiningRetentionResponse for the requested rule
     * @throws NoSuchElementException if the rule is not found
     */
    public ProcessMiningRetentionResponse findById(UUID id) {
        ProcessMiningRetentionRule rule = retentionRuleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Retention rule not found: " + id));
        return toResponse(rule);
    }

    /**
     * Creates a new retention rule from the provided request.
     *
     * @param req the retention rule creation request
     * @return the newly created ProcessMiningRetentionResponse
     * @throws NoSuchElementException if the referenced tenant or platform is not found
     */
    public ProcessMiningRetentionResponse create(ProcessMiningRetentionRequest req) {
        ProcessMiningRetentionRule rule = new ProcessMiningRetentionRule();
        applyRequest(rule, req);
        rule = retentionRuleRepository.save(rule);
        return toResponse(rule);
    }

    /**
     * Updates an existing retention rule with the provided request.
     *
     * @param id the UUID of the retention rule to update
     * @param req the retention rule update request
     * @return the updated ProcessMiningRetentionResponse
     * @throws NoSuchElementException if the rule, referenced tenant, or platform is not found
     */
    public ProcessMiningRetentionResponse update(UUID id, ProcessMiningRetentionRequest req) {
        ProcessMiningRetentionRule rule = retentionRuleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Retention rule not found: " + id));
        applyRequest(rule, req);
        rule = retentionRuleRepository.save(rule);
        return toResponse(rule);
    }

    /**
     * Deletes a retention rule by its unique identifier.
     *
     * @param id the UUID of the retention rule to delete
     * @throws NoSuchElementException if the rule is not found
     */
    public void delete(UUID id) {
        // Guard against deleting a non-existent rule
        if (!retentionRuleRepository.existsById(id)) {
            throw new NoSuchElementException("Retention rule not found: " + id);
        }
        retentionRuleRepository.deleteById(id);
    }

    // ─── Manual / Scheduled run ───────────────────────────────────────────────

    /**
     * Runs all enabled retention rules immediately, purging log data past each rule's cutoff.
     * <p>
     * Updates each rule's last-run timestamp and deleted count, and returns a summary
     * detailing how many records were removed per rule.
     * </p>
     *
     * @return a map summarising the run: rules processed, total deleted, run timestamp, and per-rule details
     */
    public Map<String, Object> runRetentionNow() {
        List<ProcessMiningRetentionRule> rules = retentionRuleRepository.findByEnabledTrue();
        List<Map<String, Object>> details = new ArrayList<>();
        int totalDeleted = 0;

        for (ProcessMiningRetentionRule rule : rules) {
            int deleted = applyRule(rule);
            rule.setLastRunAt(ZonedDateTime.now());
            rule.setLastRunDeletedCount(deleted);
            retentionRuleRepository.save(rule);
            totalDeleted += deleted;

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("ruleId", rule.getId().toString());
            detail.put("platform", rule.getPlatform() != null ? rule.getPlatform().getName() : "All Platforms");
            detail.put("tenant", rule.getTenant() != null ? rule.getTenant().getName() : "All Tenants");
            detail.put("retentionDays", rule.getRetentionDays());
            detail.put("deletedCount", deleted);
            details.add(detail);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rulesProcessed", rules.size());
        result.put("totalDeleted", totalDeleted);
        result.put("runAt", ZonedDateTime.now().toString());
        result.put("details", details);
        return result;
    }

    /**
     * Scheduled job that runs the retention cleanup daily at 03:00.
     * Delegates to {@link #runRetentionNow()} and logs the outcome.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledRun() {
        logger.info("Starting scheduled process mining retention run...");
        Map<String, Object> result = runRetentionNow();
        logger.info("Scheduled retention run complete: {} rules processed, {} records deleted",
                result.get("rulesProcessed"), result.get("totalDeleted"));
    }

    // ─── Core cleanup logic ───────────────────────────────────────────────────

    /**
     * Applies a single retention rule, deleting log records older than the rule's cutoff.
     * <p>
     * Scoping depends on the rule: a platform-scoped rule deletes by the platform's service
     * names (optionally further narrowed to a tenant), whereas a tenant-only rule deletes by tenant.
     * </p>
     *
     * @param rule the retention rule to apply
     * @return the number of log records deleted
     */
    private int applyRule(ProcessMiningRetentionRule rule) {
        ZonedDateTime cutoff = ZonedDateTime.now().minusDays(rule.getRetentionDays());

        // Platform-scoped rule: delete by the service names belonging to the platform
        if (rule.getPlatform() != null) {
            List<String> serviceNames = statusAppRepository.findByPlatformId(rule.getPlatform().getId())
                    .stream()
                    .map(app -> app.getName())
                    .collect(Collectors.toList());
            // No services under the platform means there is nothing to delete
            if (serviceNames.isEmpty()) {
                return 0;
            }
            UUID tenantId = rule.getTenant() != null ? rule.getTenant().getId() : null;
            return deleteByServicesAndCutoff(cutoff, serviceNames, tenantId);
        // Tenant-only rule: delete all of the tenant's logs past the cutoff
        } else if (rule.getTenant() != null) {
            return deleteByTenantAndCutoff(cutoff, rule.getTenant().getId());
        }

        return 0;
    }

    /**
     * Deletes log records older than the cutoff for the given service names, optionally scoped to a tenant.
     *
     * @param cutoff the timestamp before which logs are deleted
     * @param serviceNames the service names whose logs should be considered
     * @param tenantId optional tenant id to further scope the deletion; may be null
     * @return the number of log records deleted
     */
    private int deleteByServicesAndCutoff(ZonedDateTime cutoff, List<String> serviceNames, UUID tenantId) {
        String jpql = "DELETE FROM Log l WHERE l.logTimestamp < :cutoff AND l.service IN :services" +
                (tenantId != null ? " AND l.tenant.id = :tenantId" : "");
        var query = entityManager.createQuery(jpql)
                .setParameter("cutoff", cutoff)
                .setParameter("services", serviceNames);
        // Bind the tenant filter only when the deletion is tenant-scoped
        if (tenantId != null) {
            query.setParameter("tenantId", tenantId);
        }
        return query.executeUpdate();
    }

    /**
     * Deletes all log records for a tenant that are older than the cutoff.
     *
     * @param cutoff the timestamp before which logs are deleted
     * @param tenantId the tenant whose logs should be deleted
     * @return the number of log records deleted
     */
    private int deleteByTenantAndCutoff(ZonedDateTime cutoff, UUID tenantId) {
        return entityManager.createQuery(
                "DELETE FROM Log l WHERE l.logTimestamp < :cutoff AND l.tenant.id = :tenantId")
                .setParameter("cutoff", cutoff)
                .setParameter("tenantId", tenantId)
                .executeUpdate();
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    /**
     * Applies fields from a request onto a retention rule entity, resolving optional tenant and platform references.
     *
     * @param rule the target retention rule entity to populate
     * @param req the source request containing rule data
     * @throws NoSuchElementException if the referenced tenant or platform is not found
     */
    private void applyRequest(ProcessMiningRetentionRule rule, ProcessMiningRetentionRequest req) {
        rule.setRetentionDays(req.getRetentionDays());
        rule.setEnabled(req.isEnabled());

        // Resolve the tenant reference when provided, otherwise clear it (applies to all tenants)
        if (req.getTenantId() != null) {
            Tenant tenant = tenantRepository.findById(req.getTenantId())
                    .orElseThrow(() -> new NoSuchElementException("Tenant not found: " + req.getTenantId()));
            rule.setTenant(tenant);
        } else {
            rule.setTenant(null);
        }

        // Resolve the platform reference when provided, otherwise clear it (applies to all platforms)
        if (req.getPlatformId() != null) {
            StatusPlatform platform = statusPlatformRepository.findById(req.getPlatformId())
                    .orElseThrow(() -> new NoSuchElementException("Platform not found: " + req.getPlatformId()));
            rule.setPlatform(platform);
        } else {
            rule.setPlatform(null);
        }
    }

    /**
     * Maps a retention rule entity to its response representation.
     *
     * @param rule the retention rule entity to map
     * @return the mapped ProcessMiningRetentionResponse
     */
    private ProcessMiningRetentionResponse toResponse(ProcessMiningRetentionRule rule) {
        ProcessMiningRetentionResponse resp = new ProcessMiningRetentionResponse();
        resp.setId(rule.getId());
        resp.setRetentionDays(rule.getRetentionDays());
        resp.setEnabled(rule.isEnabled());
        resp.setLastRunAt(rule.getLastRunAt());
        resp.setLastRunDeletedCount(rule.getLastRunDeletedCount());
        resp.setCreatedAt(rule.getCreatedAt());

        // Expose tenant identity only when the rule is tenant-scoped
        if (rule.getTenant() != null) {
            resp.setTenantId(rule.getTenant().getId());
            resp.setTenantName(rule.getTenant().getName());
        }

        // Expose platform identity when scoped, otherwise label it as applying to all platforms
        if (rule.getPlatform() != null) {
            resp.setPlatformId(rule.getPlatform().getId());
            resp.setPlatformName(rule.getPlatform().getName());
        } else {
            resp.setPlatformName("All Platforms");
        }

        return resp;
    }
}
