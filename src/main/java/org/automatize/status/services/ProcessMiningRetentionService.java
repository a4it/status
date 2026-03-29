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

    public List<ProcessMiningRetentionResponse> findAll() {
        return retentionRuleRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProcessMiningRetentionResponse findById(UUID id) {
        ProcessMiningRetentionRule rule = retentionRuleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Retention rule not found: " + id));
        return toResponse(rule);
    }

    public ProcessMiningRetentionResponse create(ProcessMiningRetentionRequest req) {
        ProcessMiningRetentionRule rule = new ProcessMiningRetentionRule();
        applyRequest(rule, req);
        rule = retentionRuleRepository.save(rule);
        return toResponse(rule);
    }

    public ProcessMiningRetentionResponse update(UUID id, ProcessMiningRetentionRequest req) {
        ProcessMiningRetentionRule rule = retentionRuleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Retention rule not found: " + id));
        applyRequest(rule, req);
        rule = retentionRuleRepository.save(rule);
        return toResponse(rule);
    }

    public void delete(UUID id) {
        if (!retentionRuleRepository.existsById(id)) {
            throw new NoSuchElementException("Retention rule not found: " + id);
        }
        retentionRuleRepository.deleteById(id);
    }

    // ─── Manual / Scheduled run ───────────────────────────────────────────────

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

    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledRun() {
        logger.info("Starting scheduled process mining retention run...");
        Map<String, Object> result = runRetentionNow();
        logger.info("Scheduled retention run complete: {} rules processed, {} records deleted",
                result.get("rulesProcessed"), result.get("totalDeleted"));
    }

    // ─── Core cleanup logic ───────────────────────────────────────────────────

    private int applyRule(ProcessMiningRetentionRule rule) {
        ZonedDateTime cutoff = ZonedDateTime.now().minusDays(rule.getRetentionDays());

        if (rule.getPlatform() != null) {
            List<String> serviceNames = statusAppRepository.findByPlatformId(rule.getPlatform().getId())
                    .stream()
                    .map(app -> app.getName())
                    .collect(Collectors.toList());
            if (serviceNames.isEmpty()) {
                return 0;
            }
            UUID tenantId = rule.getTenant() != null ? rule.getTenant().getId() : null;
            return deleteByServicesAndCutoff(cutoff, serviceNames, tenantId);
        } else if (rule.getTenant() != null) {
            return deleteByTenantAndCutoff(cutoff, rule.getTenant().getId());
        }

        return 0;
    }

    private int deleteByServicesAndCutoff(ZonedDateTime cutoff, List<String> serviceNames, UUID tenantId) {
        String jpql = "DELETE FROM Log l WHERE l.logTimestamp < :cutoff AND l.service IN :services" +
                (tenantId != null ? " AND l.tenant.id = :tenantId" : "");
        var query = entityManager.createQuery(jpql)
                .setParameter("cutoff", cutoff)
                .setParameter("services", serviceNames);
        if (tenantId != null) {
            query.setParameter("tenantId", tenantId);
        }
        return query.executeUpdate();
    }

    private int deleteByTenantAndCutoff(ZonedDateTime cutoff, UUID tenantId) {
        return entityManager.createQuery(
                "DELETE FROM Log l WHERE l.logTimestamp < :cutoff AND l.tenant.id = :tenantId")
                .setParameter("cutoff", cutoff)
                .setParameter("tenantId", tenantId)
                .executeUpdate();
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private void applyRequest(ProcessMiningRetentionRule rule, ProcessMiningRetentionRequest req) {
        rule.setRetentionDays(req.getRetentionDays());
        rule.setEnabled(req.isEnabled());

        if (req.getTenantId() != null) {
            Tenant tenant = tenantRepository.findById(req.getTenantId())
                    .orElseThrow(() -> new NoSuchElementException("Tenant not found: " + req.getTenantId()));
            rule.setTenant(tenant);
        } else {
            rule.setTenant(null);
        }

        if (req.getPlatformId() != null) {
            StatusPlatform platform = statusPlatformRepository.findById(req.getPlatformId())
                    .orElseThrow(() -> new NoSuchElementException("Platform not found: " + req.getPlatformId()));
            rule.setPlatform(platform);
        } else {
            rule.setPlatform(null);
        }
    }

    private ProcessMiningRetentionResponse toResponse(ProcessMiningRetentionRule rule) {
        ProcessMiningRetentionResponse resp = new ProcessMiningRetentionResponse();
        resp.setId(rule.getId());
        resp.setRetentionDays(rule.getRetentionDays());
        resp.setEnabled(rule.isEnabled());
        resp.setLastRunAt(rule.getLastRunAt());
        resp.setLastRunDeletedCount(rule.getLastRunDeletedCount());
        resp.setCreatedAt(rule.getCreatedAt());

        if (rule.getTenant() != null) {
            resp.setTenantId(rule.getTenant().getId());
            resp.setTenantName(rule.getTenant().getName());
        }

        if (rule.getPlatform() != null) {
            resp.setPlatformId(rule.getPlatform().getId());
            resp.setPlatformName(rule.getPlatform().getName());
        } else {
            resp.setPlatformName("All Platforms");
        }

        return resp;
    }
}
