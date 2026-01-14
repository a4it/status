package org.automatize.status.repositories;

import org.automatize.status.models.StatusMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing {@link StatusMaintenance} entities.
 * <p>
 * Provides data access operations for scheduled maintenance windows within the system.
 * Maintenance windows represent planned service disruptions or updates that affect
 * one or more components. They have defined start and end times and can be scheduled
 * in advance to notify users.
 * </p>
 *
 * @see StatusMaintenance
 * @see StatusAppRepository
 * @see StatusMaintenanceComponentRepository
 */
@Repository
public interface StatusMaintenanceRepository extends JpaRepository<StatusMaintenance, UUID> {

    /**
     * Finds all maintenance windows belonging to a specific status app.
     *
     * @param appId the unique identifier of the status app
     * @return a list of maintenance windows belonging to the specified app
     */
    List<StatusMaintenance> findByAppId(UUID appId);

    /**
     * Finds all maintenance windows with a specific status.
     *
     * @param status the status to filter by (e.g., "SCHEDULED", "IN_PROGRESS", "COMPLETED")
     * @return a list of maintenance windows matching the specified status
     */
    List<StatusMaintenance> findByStatus(String status);

    /**
     * Finds all maintenance windows by their public visibility setting.
     *
     * @param isPublic true to find public maintenance windows, false for private ones
     * @return a list of maintenance windows matching the visibility setting
     */
    List<StatusMaintenance> findByIsPublic(Boolean isPublic);

    /**
     * Finds all maintenance windows belonging to a status app with a specific status.
     *
     * @param appId the unique identifier of the status app
     * @param status the status to filter by
     * @return a list of maintenance windows matching both the app and status criteria
     */
    List<StatusMaintenance> findByAppIdAndStatus(UUID appId, String status);

    /**
     * Finds all maintenance windows belonging to a status app with a specific visibility setting.
     *
     * @param appId the unique identifier of the status app
     * @param isPublic true to find public maintenance windows, false for private ones
     * @return a list of maintenance windows matching both the app and visibility criteria
     */
    List<StatusMaintenance> findByAppIdAndIsPublic(UUID appId, Boolean isPublic);

    /**
     * Finds all maintenance windows that start within a specific date range.
     *
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return a list of maintenance windows starting within the specified date range
     */
    List<StatusMaintenance> findByStartsAtBetween(ZonedDateTime startDate, ZonedDateTime endDate);

    /**
     * Finds all maintenance windows that end within a specific date range.
     *
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return a list of maintenance windows ending within the specified date range
     */
    List<StatusMaintenance> findByEndsAtBetween(ZonedDateTime startDate, ZonedDateTime endDate);

    /**
     * Finds all maintenance windows within a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a list of maintenance windows within the specified tenant
     */
    @Query("SELECT m FROM StatusMaintenance m WHERE m.app.tenant.id = :tenantId")
    List<StatusMaintenance> findByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Finds all maintenance windows within a specific organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return a list of maintenance windows within the specified organization
     */
    @Query("SELECT m FROM StatusMaintenance m WHERE m.app.organization.id = :organizationId")
    List<StatusMaintenance> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Finds all maintenance windows for a status app, ordered by start time in descending order.
     *
     * @param appId the unique identifier of the status app
     * @return a list of maintenance windows ordered by start time (most recent first)
     */
    @Query("SELECT m FROM StatusMaintenance m WHERE m.app.id = :appId ORDER BY m.startsAt DESC")
    List<StatusMaintenance> findByAppIdOrderByStartsAtDesc(@Param("appId") UUID appId);

    /**
     * Finds all maintenance windows that are currently active (in progress at the specified time).
     *
     * @param currentTime the time to check for active maintenance
     * @return a list of maintenance windows currently in progress
     */
    @Query("SELECT m FROM StatusMaintenance m WHERE m.startsAt <= :currentTime AND m.endsAt >= :currentTime")
    List<StatusMaintenance> findActiveMaintenance(@Param("currentTime") ZonedDateTime currentTime);

    /**
     * Finds upcoming maintenance windows for a status app that start on or after the specified date.
     *
     * @param appId the unique identifier of the status app
     * @param startDate the minimum start date for maintenance windows to include
     * @return a list of upcoming maintenance windows
     */
    @Query("SELECT m FROM StatusMaintenance m WHERE m.app.id = :appId AND m.startsAt >= :startDate")
    List<StatusMaintenance> findUpcomingMaintenanceByAppId(@Param("appId") UUID appId, @Param("startDate") ZonedDateTime startDate);

    /**
     * Searches for maintenance windows globally by title or description containing the search term.
     *
     * @param searchTerm the term to search for in maintenance title or description
     * @return a list of maintenance windows matching the search criteria
     */
    @Query("SELECT m FROM StatusMaintenance m WHERE m.title LIKE %:searchTerm% OR m.description LIKE %:searchTerm%")
    List<StatusMaintenance> search(@Param("searchTerm") String searchTerm);

    /**
     * Counts the total number of maintenance windows belonging to a specific status app.
     *
     * @param appId the unique identifier of the status app
     * @return the count of maintenance windows for the specified app
     */
    @Query("SELECT COUNT(m) FROM StatusMaintenance m WHERE m.app.id = :appId")
    Long countByAppId(@Param("appId") UUID appId);

    /**
     * Counts the total number of maintenance windows within a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return the count of maintenance windows within the specified tenant
     */
    @Query("SELECT COUNT(m) FROM StatusMaintenance m WHERE m.app.tenant.id = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Counts the number of active (scheduled or in progress) maintenance windows for a specific status app.
     *
     * @param appId the unique identifier of the status app
     * @return the count of active maintenance windows for the specified app
     */
    @Query("SELECT COUNT(m) FROM StatusMaintenance m WHERE m.app.id = :appId AND m.status IN ('SCHEDULED', 'IN_PROGRESS')")
    Long countActiveMaintenanceByAppId(@Param("appId") UUID appId);
}