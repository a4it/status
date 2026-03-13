package org.automatize.status.repositories;

import org.automatize.status.models.DropRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DropRuleRepository extends JpaRepository<DropRule, UUID> {

    List<DropRule> findByIsActiveTrueOrderByCreatedDateTechnicalDesc();

    List<DropRule> findAllByOrderByCreatedDateTechnicalDesc();

    List<DropRule> findByTenantIdOrderByCreatedDateTechnicalDesc(UUID tenantId);
}
