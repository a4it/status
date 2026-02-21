package org.automatize.status.repositories;

import org.automatize.status.models.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {

    List<AlertRule> findByIsActiveTrueOrderByCreatedDateTechnicalDesc();

    List<AlertRule> findAllByOrderByCreatedDateTechnicalDesc();

    List<AlertRule> findByTenantIdOrderByCreatedDateTechnicalDesc(UUID tenantId);
}
