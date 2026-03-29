package org.automatize.status.repositories;

import org.automatize.status.models.ProcessMiningRetentionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProcessMiningRetentionRuleRepository extends JpaRepository<ProcessMiningRetentionRule, UUID> {

    List<ProcessMiningRetentionRule> findByEnabledTrue();

    @Query("SELECT r FROM ProcessMiningRetentionRule r WHERE r.tenant.id = :tenantId")
    List<ProcessMiningRetentionRule> findByTenantId(@Param("tenantId") UUID tenantId);
}
