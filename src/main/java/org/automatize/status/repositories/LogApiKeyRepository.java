package org.automatize.status.repositories;

import org.automatize.status.models.LogApiKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
    @EntityGraph(attributePaths = {"tenant"})
    List<LogApiKey> findByTenantIdOrderByCreatedDateTechnicalDesc(UUID tenantId);

    /**
     * Finds all API keys, ordered by their technical creation date descending.
     *
     * @return a list of all API keys, newest first
     */
    @EntityGraph(attributePaths = {"tenant"})
    List<LogApiKey> findAllByOrderByCreatedDateTechnicalDesc();

    /**
     * Returns a page of API keys, newest first.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of API keys
     */
    @EntityGraph(attributePaths = {"tenant"})
    Page<LogApiKey> findAllByOrderByCreatedDateTechnicalDesc(Pageable pageable);
}
