package org.automatize.status.repositories;

import org.automatize.status.models.ProcessMiningRetentionRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing {@link ProcessMiningRetentionRule} entities.
 *
 * <p>Provides CRUD operations and finders for retention rules that govern how
 * long process-mining data is kept before being purged.</p>
 */
@Repository
public interface ProcessMiningRetentionRuleRepository extends JpaRepository<ProcessMiningRetentionRule, UUID> {

    /**
     * Finds all enabled retention rules; used by the retention/housekeeping job to
     * determine which rules to apply.
     *
     * @return a list of all enabled retention rules
     */
    @EntityGraph(attributePaths = {"tenant", "platform"})
    List<ProcessMiningRetentionRule> findByEnabledTrue();

    /**
     * Finds all retention rules belonging to a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a list of the tenant's retention rules
     */
    @EntityGraph(attributePaths = {"tenant", "platform"})
    @Query("SELECT r FROM ProcessMiningRetentionRule r WHERE r.tenant.id = :tenantId")
    List<ProcessMiningRetentionRule> findByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Returns a page of retention rules.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of retention rules
     */
    @EntityGraph(attributePaths = {"tenant", "platform"})
    @Override
    Page<ProcessMiningRetentionRule> findAll(Pageable pageable);
}
