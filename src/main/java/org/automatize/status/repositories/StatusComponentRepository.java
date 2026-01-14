package org.automatize.status.repositories;

import org.automatize.status.models.StatusComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link StatusComponent} entities.
 * <p>
 * Provides data access operations for status components within status applications.
 * Components represent individual services or subsystems that can be monitored
 * and have their status tracked. Components can be grouped and positioned for
 * display purposes on the status page.
 * </p>
 *
 * @see StatusComponent
 * @see StatusAppRepository
 * @see StatusIncidentComponentRepository
 * @see StatusMaintenanceComponentRepository
 */
@Repository
public interface StatusComponentRepository extends JpaRepository<StatusComponent, UUID> {

    /**
     * Finds all components belonging to a specific status app.
     *
     * @param appId the unique identifier of the status app
     * @return a list of components belonging to the specified app
     */
    List<StatusComponent> findByAppId(UUID appId);

    /**
     * Finds a component by app ID and component name combination.
     *
     * @param appId the unique identifier of the status app
     * @param name the name of the component
     * @return an Optional containing the component if found, or empty if not found
     */
    Optional<StatusComponent> findByAppIdAndName(UUID appId, String name);

    /**
     * Finds all components belonging to a status app, ordered by their display position.
     *
     * @param appId the unique identifier of the status app
     * @return a list of components ordered by position for display purposes
     */
    List<StatusComponent> findByAppIdOrderByPosition(UUID appId);

    /**
     * Finds all components with a specific status.
     *
     * @param status the status to filter by (e.g., "OPERATIONAL", "DEGRADED", "OUTAGE")
     * @return a list of components matching the specified status
     */
    List<StatusComponent> findByStatus(String status);

    /**
     * Finds all components belonging to a status app with a specific status.
     *
     * @param appId the unique identifier of the status app
     * @param status the status to filter by
     * @return a list of components matching both the app and status criteria
     */
    List<StatusComponent> findByAppIdAndStatus(UUID appId, String status);

    /**
     * Finds all components belonging to a specific group.
     *
     * @param groupName the name of the group to filter by
     * @return a list of components belonging to the specified group
     */
    List<StatusComponent> findByGroupName(String groupName);

    /**
     * Finds all components belonging to a specific group within a status app.
     *
     * @param appId the unique identifier of the status app
     * @param groupName the name of the group to filter by
     * @return a list of components matching both the app and group criteria
     */
    List<StatusComponent> findByAppIdAndGroupName(UUID appId, String groupName);

    /**
     * Finds all components belonging to status apps within a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a list of components within the specified tenant
     */
    @Query("SELECT c FROM StatusComponent c WHERE c.app.tenant.id = :tenantId")
    List<StatusComponent> findByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Finds all components belonging to status apps within a specific organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return a list of components within the specified organization
     */
    @Query("SELECT c FROM StatusComponent c WHERE c.app.organization.id = :organizationId")
    List<StatusComponent> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Searches for components within a status app by name or description containing the search term.
     *
     * @param appId the unique identifier of the status app to search within
     * @param searchTerm the term to search for in component name or description
     * @return a list of components matching the search criteria within the specified app
     */
    @Query("SELECT c FROM StatusComponent c WHERE c.app.id = :appId AND (c.name LIKE %:searchTerm% OR c.description LIKE %:searchTerm%)")
    List<StatusComponent> searchByAppId(@Param("appId") UUID appId, @Param("searchTerm") String searchTerm);

    /**
     * Searches for components globally by name or description containing the search term.
     *
     * @param searchTerm the term to search for in component name or description
     * @return a list of components matching the search criteria
     */
    @Query("SELECT c FROM StatusComponent c WHERE c.name LIKE %:searchTerm% OR c.description LIKE %:searchTerm%")
    List<StatusComponent> search(@Param("searchTerm") String searchTerm);

    /**
     * Counts the total number of components belonging to a specific status app.
     *
     * @param appId the unique identifier of the status app
     * @return the count of components within the specified app
     */
    @Query("SELECT COUNT(c) FROM StatusComponent c WHERE c.app.id = :appId")
    Long countByAppId(@Param("appId") UUID appId);

    /**
     * Counts the total number of components within a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return the count of components within the specified tenant
     */
    @Query("SELECT COUNT(c) FROM StatusComponent c WHERE c.app.tenant.id = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Checks if a component with the specified name already exists within a status app.
     *
     * @param appId the unique identifier of the status app
     * @param name the component name to check for existence
     * @return true if a component with the name exists in the app, false otherwise
     */
    boolean existsByAppIdAndName(UUID appId, String name);
}