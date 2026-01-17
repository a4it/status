package org.automatize.status.repositories;

import org.automatize.status.models.StatusPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link StatusPlatform} entities.
 * <p>
 * Provides data access operations for status platforms within the system.
 * Status platforms represent higher-level groupings that can contain
 * multiple status applications.
 * </p>
 *
 * @see StatusPlatform
 * @see StatusAppRepository
 */
@Repository
public interface StatusPlatformRepository extends JpaRepository<StatusPlatform, UUID> {

    /**
     * Finds a platform by its unique slug.
     *
     * @param slug the URL-friendly identifier of the platform
     * @return an Optional containing the platform if found, or empty if not found
     */
    Optional<StatusPlatform> findBySlug(String slug);

    /**
     * Finds a platform by tenant ID and slug combination.
     *
     * @param tenantId the unique identifier of the tenant
     * @param slug the URL-friendly identifier of the platform
     * @return an Optional containing the platform if found, or empty if not found
     */
    Optional<StatusPlatform> findByTenantIdAndSlug(UUID tenantId, String slug);

    /**
     * Finds all platforms belonging to a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a list of platforms belonging to the specified tenant
     */
    List<StatusPlatform> findByTenantId(UUID tenantId);

    /**
     * Finds all platforms belonging to a specific tenant, ordered by position.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a list of platforms ordered by position
     */
    List<StatusPlatform> findByTenantIdOrderByPosition(UUID tenantId);

    /**
     * Finds all platforms belonging to a specific organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return a list of platforms belonging to the specified organization
     */
    List<StatusPlatform> findByOrganizationId(UUID organizationId);

    /**
     * Finds all platforms with a specific status.
     *
     * @param status the status to filter by (e.g., "OPERATIONAL", "DEGRADED")
     * @return a list of platforms matching the specified status
     */
    List<StatusPlatform> findByStatus(String status);

    /**
     * Finds all platforms by their public visibility setting.
     *
     * @param isPublic true to find public platforms, false for private ones
     * @return a list of platforms matching the visibility setting
     */
    List<StatusPlatform> findByIsPublic(Boolean isPublic);

    /**
     * Finds all platforms belonging to a tenant with a specific visibility setting.
     *
     * @param tenantId the unique identifier of the tenant
     * @param isPublic true to find public platforms, false for private ones
     * @return a list of platforms matching both the tenant and visibility criteria
     */
    List<StatusPlatform> findByTenantIdAndIsPublic(UUID tenantId, Boolean isPublic);

    /**
     * Finds all public platforms ordered by position.
     *
     * @return a list of public platforms ordered by position
     */
    List<StatusPlatform> findByIsPublicTrueOrderByPosition();

    /**
     * Finds all platforms ordered by position.
     *
     * @return a list of all platforms ordered by position
     */
    List<StatusPlatform> findAllByOrderByPosition();

    /**
     * Searches for platforms within a tenant by name, description, or slug containing the search term.
     *
     * @param tenantId the unique identifier of the tenant to search within
     * @param searchTerm the term to search for in name, description, or slug
     * @return a list of platforms matching the search criteria within the specified tenant
     */
    @Query("SELECT p FROM StatusPlatform p WHERE p.tenant.id = :tenantId AND (p.name LIKE %:searchTerm% OR p.description LIKE %:searchTerm% OR p.slug LIKE %:searchTerm%)")
    List<StatusPlatform> searchByTenantId(@Param("tenantId") UUID tenantId, @Param("searchTerm") String searchTerm);

    /**
     * Searches for platforms globally by name, description, or slug containing the search term.
     *
     * @param searchTerm the term to search for in name, description, or slug
     * @return a list of platforms matching the search criteria
     */
    @Query("SELECT p FROM StatusPlatform p WHERE p.name LIKE %:searchTerm% OR p.description LIKE %:searchTerm% OR p.slug LIKE %:searchTerm%")
    List<StatusPlatform> search(@Param("searchTerm") String searchTerm);

    /**
     * Counts the total number of platforms belonging to a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return the count of platforms within the specified tenant
     */
    @Query("SELECT COUNT(p) FROM StatusPlatform p WHERE p.tenant.id = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Checks if a platform with the specified slug already exists.
     *
     * @param slug the slug to check for existence
     * @return true if a platform with the slug exists, false otherwise
     */
    boolean existsBySlug(String slug);

    /**
     * Checks if a platform with the specified slug already exists within a tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @param slug the slug to check for existence
     * @return true if a platform with the slug exists in the tenant, false otherwise
     */
    boolean existsByTenantIdAndSlug(UUID tenantId, String slug);
}
