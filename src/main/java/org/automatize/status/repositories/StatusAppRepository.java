package org.automatize.status.repositories;

import org.automatize.status.models.StatusApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link StatusApp} entities.
 * <p>
 * Provides data access operations for status applications (status pages) within the system.
 * Status apps represent individual status pages that can contain components, incidents,
 * and maintenance windows. Each status app belongs to a tenant and organization.
 * </p>
 *
 * @see StatusApp
 * @see StatusComponentRepository
 * @see StatusIncidentRepository
 * @see StatusMaintenanceRepository
 */
@Repository
public interface StatusAppRepository extends JpaRepository<StatusApp, UUID> {

    /**
     * Finds a status app by its unique slug.
     *
     * @param slug the URL-friendly identifier of the status app
     * @return an Optional containing the status app if found, or empty if not found
     */
    Optional<StatusApp> findBySlug(String slug);

    /**
     * Finds a status app by tenant ID and slug combination.
     *
     * @param tenantId the unique identifier of the tenant
     * @param slug the URL-friendly identifier of the status app
     * @return an Optional containing the status app if found, or empty if not found
     */
    Optional<StatusApp> findByTenantIdAndSlug(UUID tenantId, String slug);

    /**
     * Finds all status apps belonging to a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a list of status apps belonging to the specified tenant
     */
    List<StatusApp> findByTenantId(UUID tenantId);

    /**
     * Finds all status apps belonging to a specific organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return a list of status apps belonging to the specified organization
     */
    List<StatusApp> findByOrganizationId(UUID organizationId);

    /**
     * Finds all status apps belonging to a specific platform.
     *
     * @param platformId the unique identifier of the platform
     * @return a list of status apps belonging to the specified platform
     */
    List<StatusApp> findByPlatformId(UUID platformId);

    /**
     * Finds all status apps with a specific status.
     *
     * @param status the status to filter by (e.g., "ACTIVE", "INACTIVE")
     * @return a list of status apps matching the specified status
     */
    List<StatusApp> findByStatus(String status);

    /**
     * Finds all status apps by their public visibility setting.
     *
     * @param isPublic true to find public status apps, false for private ones
     * @return a list of status apps matching the visibility setting
     */
    List<StatusApp> findByIsPublic(Boolean isPublic);

    /**
     * Finds all status apps belonging to a tenant with a specific visibility setting.
     *
     * @param tenantId the unique identifier of the tenant
     * @param isPublic true to find public status apps, false for private ones
     * @return a list of status apps matching both the tenant and visibility criteria
     */
    List<StatusApp> findByTenantIdAndIsPublic(UUID tenantId, Boolean isPublic);

    /**
     * Finds all status apps belonging to an organization with a specific status.
     *
     * @param organizationId the unique identifier of the organization
     * @param status the status to filter by
     * @return a list of status apps matching both the organization and status criteria
     */
    List<StatusApp> findByOrganizationIdAndStatus(UUID organizationId, String status);

    /**
     * Searches for status apps within a tenant by name, description, or slug containing the search term.
     *
     * @param tenantId the unique identifier of the tenant to search within
     * @param searchTerm the term to search for in name, description, or slug
     * @return a list of status apps matching the search criteria within the specified tenant
     */
    @Query("SELECT s FROM StatusApp s WHERE s.tenant.id = :tenantId AND (s.name LIKE %:searchTerm% OR s.description LIKE %:searchTerm% OR s.slug LIKE %:searchTerm%)")
    List<StatusApp> searchByTenantId(@Param("tenantId") UUID tenantId, @Param("searchTerm") String searchTerm);

    /**
     * Searches for status apps within an organization by name or description containing the search term.
     *
     * @param organizationId the unique identifier of the organization to search within
     * @param searchTerm the term to search for in name or description
     * @return a list of status apps matching the search criteria within the specified organization
     */
    @Query("SELECT s FROM StatusApp s WHERE s.organization.id = :organizationId AND (s.name LIKE %:searchTerm% OR s.description LIKE %:searchTerm%)")
    List<StatusApp> searchByOrganizationId(@Param("organizationId") UUID organizationId, @Param("searchTerm") String searchTerm);

    /**
     * Searches for status apps globally by name, description, or slug containing the search term.
     *
     * @param searchTerm the term to search for in name, description, or slug
     * @return a list of status apps matching the search criteria
     */
    @Query("SELECT s FROM StatusApp s WHERE s.name LIKE %:searchTerm% OR s.description LIKE %:searchTerm% OR s.slug LIKE %:searchTerm%")
    List<StatusApp> search(@Param("searchTerm") String searchTerm);

    /**
     * Counts the total number of status apps belonging to a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return the count of status apps within the specified tenant
     */
    @Query("SELECT COUNT(s) FROM StatusApp s WHERE s.tenant.id = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Counts the total number of status apps belonging to a specific organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return the count of status apps within the specified organization
     */
    @Query("SELECT COUNT(s) FROM StatusApp s WHERE s.organization.id = :organizationId")
    Long countByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Checks if a status app with the specified slug already exists.
     *
     * @param slug the slug to check for existence
     * @return true if a status app with the slug exists, false otherwise
     */
    boolean existsBySlug(String slug);

    /**
     * Checks if a status app with the specified slug already exists within a tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @param slug the slug to check for existence
     * @return true if a status app with the slug exists in the tenant, false otherwise
     */
    boolean existsByTenantIdAndSlug(UUID tenantId, String slug);

    /**
     * Finds a status app by its API key.
     *
     * @param apiKey the API key for event logging authentication
     * @return an Optional containing the status app if found, or empty if not found
     */
    Optional<StatusApp> findByApiKey(String apiKey);
}