package org.automatize.status.repositories;

import org.automatize.status.models.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link Organization} entities.
 * <p>
 * Provides data access operations for organizations within the multi-tenant hierarchy.
 * Organizations belong to tenants and contain users, forming the middle layer of the
 * tenant-organization-user hierarchy.
 * </p>
 *
 * @see Organization
 * @see TenantRepository
 * @see UserRepository
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    /**
     * Finds an organization by its unique name.
     *
     * @param name the name of the organization to find
     * @return an Optional containing the organization if found, or empty if not found
     */
    Optional<Organization> findByName(String name);

    /**
     * Finds all organizations belonging to a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a list of organizations belonging to the specified tenant
     */
    List<Organization> findByTenantId(UUID tenantId);

    /**
     * Finds all organizations with a specific status.
     *
     * @param status the status to filter by (e.g., "ACTIVE", "INACTIVE")
     * @return a list of organizations matching the specified status
     */
    List<Organization> findByStatus(String status);

    /**
     * Finds all organizations belonging to a specific tenant with a specific status.
     *
     * @param tenantId the unique identifier of the tenant
     * @param status the status to filter by
     * @return a list of organizations matching both the tenant and status criteria
     */
    List<Organization> findByTenantIdAndStatus(UUID tenantId, String status);

    /**
     * Finds all organizations of a specific type.
     *
     * @param organizationType the type of organization to filter by
     * @return a list of organizations matching the specified type
     */
    List<Organization> findByOrganizationType(String organizationType);

    /**
     * Finds all organizations located in a specific country.
     *
     * @param country the country to filter by
     * @return a list of organizations located in the specified country
     */
    List<Organization> findByCountry(String country);

    /**
     * Searches for organizations within a tenant by name or email containing the search term.
     *
     * @param tenantId the unique identifier of the tenant to search within
     * @param searchTerm the term to search for in organization name or email
     * @return a list of organizations matching the search criteria within the specified tenant
     */
    @Query("SELECT o FROM Organization o WHERE o.tenant.id = :tenantId AND (o.name LIKE %:searchTerm% OR o.email LIKE %:searchTerm%)")
    List<Organization> searchByTenantId(@Param("tenantId") UUID tenantId, @Param("searchTerm") String searchTerm);

    /**
     * Searches for organizations globally by name, email, or description containing the search term.
     *
     * @param searchTerm the term to search for in organization name, email, or description
     * @return a list of organizations matching the search criteria
     */
    @Query("SELECT o FROM Organization o WHERE o.name LIKE %:searchTerm% OR o.email LIKE %:searchTerm% OR o.description LIKE %:searchTerm%")
    List<Organization> search(@Param("searchTerm") String searchTerm);

    /**
     * Finds all organizations belonging to a tenant, ordered by creation date in descending order.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a list of organizations ordered by creation date (newest first)
     */
    @Query("SELECT o FROM Organization o WHERE o.tenant.id = :tenantId ORDER BY o.createdDate DESC")
    List<Organization> findByTenantIdOrderByCreatedDateDesc(@Param("tenantId") UUID tenantId);

    /**
     * Counts the total number of organizations belonging to a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return the count of organizations within the specified tenant
     */
    @Query("SELECT COUNT(o) FROM Organization o WHERE o.tenant.id = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Checks if an organization with the specified name already exists.
     *
     * @param name the name to check for existence
     * @return true if an organization with the name exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Checks if an organization with the specified email already exists.
     *
     * @param email the email to check for existence
     * @return true if an organization with the email exists, false otherwise
     */
    boolean existsByEmail(String email);
}