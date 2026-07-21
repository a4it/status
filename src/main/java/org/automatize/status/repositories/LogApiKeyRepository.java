package org.automatize.status.repositories;

import org.automatize.status.models.LogApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link LogApiKey} entities.
 *
 * <p>Provides CRUD operations and finders for API keys used to authenticate
 * external log/event ingestion. Keys are looked up by their SHA-256 hash rather
 * than plaintext for security.</p>
 */
@Repository
public interface LogApiKeyRepository extends JpaRepository<LogApiKey, UUID> {

    // MED-03: lookup by SHA-256 hash of the API key, not plaintext
    /**
     * Finds an active API key by the SHA-256 hash of its plaintext value.
     *
     * @param keyHash the SHA-256 hash of the API key to look up
     * @return an Optional containing the active key if found, or empty otherwise
     */
    Optional<LogApiKey> findByKeyHashAndIsActiveTrue(String keyHash);

    /**
     * Finds all API keys belonging to a specific tenant, ordered by technical creation date descending.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a list of the tenant's API keys, newest first
     */
    List<LogApiKey> findByTenantIdOrderByCreatedDateTechnicalDesc(UUID tenantId);

    /**
     * Finds all API keys, ordered by their technical creation date descending.
     *
     * @return a list of all API keys, newest first
     */
    List<LogApiKey> findAllByOrderByCreatedDateTechnicalDesc();
}
