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

    /**
     * Retrieves all drop rules ordered by creation date (newest first).
     *
     * @return the list of all persisted drop rules
     */
    @Transactional(readOnly = true)
    public List<DropRule> findAll() {
        return dropRuleRepository.findAllByOrderByCreatedDateTechnicalDesc();
    }

    /**
     * Retrieves a single drop rule by its identifier.
     *
     * @param id the unique identifier of the drop rule
     * @return the matching drop rule
     * @throws RuntimeException if no drop rule exists for the given id
     */
    @Transactional(readOnly = true)
    public DropRule findById(UUID id) {
        return dropRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drop rule not found: " + id));
    }

    /**
     * Creates and persists a new drop rule used to filter out logs before storage.
     *
     * @param tenantId the tenant this rule belongs to, or {@code null} for no tenant association
     * @param name the human-readable name of the rule
     * @param level the log level this rule matches
     * @param service the service this rule matches
     * @param messagePattern the message pattern this rule matches
     * @param active whether the rule is active upon creation
     * @return the persisted drop rule
     */
    public DropRule create(UUID tenantId, String name, String level, String service,
                           String messagePattern, boolean active) {
        DropRule rule = new DropRule();
        // Associate the rule with a tenant only when a tenant id was supplied
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

    /**
     * Updates an existing drop rule with new field values and persists the change.
     *
     * @param id the unique identifier of the drop rule to update
     * @param name the new human-readable name of the rule
     * @param level the new log level this rule matches
     * @param service the new service this rule matches
     * @param messagePattern the new message pattern this rule matches
     * @param active whether the rule should be active
     * @return the updated and persisted drop rule
     * @throws RuntimeException if no drop rule exists for the given id
     */
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

    /**
     * Deletes the drop rule identified by the given id.
     *
     * @param id the unique identifier of the drop rule to delete
     * @throws RuntimeException if no drop rule exists for the given id
     */
    public void delete(UUID id) {
        DropRule rule = findById(id);
        dropRuleRepository.delete(rule);
    }

    /**
     * Toggles the active state of the drop rule identified by the given id.
     *
     * @param id the unique identifier of the drop rule to toggle
     * @return the updated and persisted drop rule with its active state flipped
     * @throws RuntimeException if no drop rule exists for the given id
     */
    public DropRule toggleActive(UUID id) {
        DropRule rule = findById(id);
        rule.setIsActive(!Boolean.TRUE.equals(rule.getIsActive()));
        return dropRuleRepository.save(rule);
    }
}
