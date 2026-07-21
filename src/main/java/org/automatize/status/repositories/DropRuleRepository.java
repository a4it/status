package org.automatize.status.repositories;

import org.automatize.status.models.DropRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing {@link DropRule} entities.
 *
 * <p>Provides CRUD operations and finders for drop rules, which define the
 * conditions under which incoming log or event data is discarded before storage.</p>
 */
@Repository
public interface DropRuleRepository extends JpaRepository<DropRule, UUID> {

    /**
     * Finds all active drop rules, ordered by their technical creation date descending.
     *
     * @return a list of active drop rules, newest first
     */
    List<DropRule> findByIsActiveTrueOrderByCreatedDateTechnicalDesc();

    /**
     * Finds all drop rules regardless of active status, ordered by technical creation date descending.
     *
     * @return a list of all drop rules, newest first
     */
    List<DropRule> findAllByOrderByCreatedDateTechnicalDesc();

    /**
     * Finds all drop rules belonging to a specific tenant, ordered by technical creation date descending.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a list of the tenant's drop rules, newest first
     */
    List<DropRule> findByTenantIdOrderByCreatedDateTechnicalDesc(UUID tenantId);
}
