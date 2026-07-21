package org.automatize.status.repositories;

import org.automatize.status.models.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing {@link AlertRule} entities.
 *
 * <p>Provides CRUD operations and finders for alert rules, which define the
 * conditions under which notifications are raised for monitored platforms.</p>
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {

    /**
     * Finds all active alert rules, ordered by their technical creation date descending.
     *
     * @return a list of active alert rules, newest first
     */
    List<AlertRule> findByIsActiveTrueOrderByCreatedDateTechnicalDesc();

    /**
     * Finds all alert rules regardless of active status, ordered by technical creation date descending.
     *
     * @return a list of all alert rules, newest first
     */
    List<AlertRule> findAllByOrderByCreatedDateTechnicalDesc();

    /**
     * Finds all alert rules belonging to a specific tenant, ordered by technical creation date descending.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a list of the tenant's alert rules, newest first
     */
    List<AlertRule> findByTenantIdOrderByCreatedDateTechnicalDesc(UUID tenantId);
}
