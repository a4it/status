package org.automatize.status.repositories;

import org.automatize.status.models.LogApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LogApiKeyRepository extends JpaRepository<LogApiKey, UUID> {

    Optional<LogApiKey> findByApiKeyAndIsActiveTrue(String apiKey);

    List<LogApiKey> findByTenantIdOrderByCreatedDateTechnicalDesc(UUID tenantId);

    List<LogApiKey> findAllByOrderByCreatedDateTechnicalDesc();
}
