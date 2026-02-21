package org.automatize.status.services;

import org.automatize.status.models.DropRule;
import org.automatize.status.repositories.DropRuleRepository;
import org.automatize.status.repositories.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing drop rules that filter out logs before storage.
 */
@Service
@Transactional
public class DropRuleService {

    @Autowired
    private DropRuleRepository dropRuleRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public List<DropRule> findAll() {
        return dropRuleRepository.findAllByOrderByCreatedDateTechnicalDesc();
    }

    @Transactional(readOnly = true)
    public DropRule findById(UUID id) {
        return dropRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drop rule not found: " + id));
    }

    public DropRule create(UUID tenantId, String name, String level, String service,
                           String messagePattern, boolean active) {
        DropRule rule = new DropRule();
        if (tenantId != null) {
            tenantRepository.findById(tenantId).ifPresent(rule::setTenant);
        }
        rule.setName(name);
        rule.setLevel(level);
        rule.setService(service);
        rule.setMessagePattern(messagePattern);
        rule.setIsActive(active);
        return dropRuleRepository.save(rule);
    }

    public DropRule update(UUID id, String name, String level, String service,
                           String messagePattern, boolean active) {
        DropRule rule = findById(id);
        rule.setName(name);
        rule.setLevel(level);
        rule.setService(service);
        rule.setMessagePattern(messagePattern);
        rule.setIsActive(active);
        return dropRuleRepository.save(rule);
    }

    public void delete(UUID id) {
        DropRule rule = findById(id);
        dropRuleRepository.delete(rule);
    }

    public DropRule toggleActive(UUID id) {
        DropRule rule = findById(id);
        rule.setIsActive(!Boolean.TRUE.equals(rule.getIsActive()));
        return dropRuleRepository.save(rule);
    }
}
