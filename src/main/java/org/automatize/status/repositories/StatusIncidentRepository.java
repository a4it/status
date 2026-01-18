package org.automatize.status.repositories;

import org.automatize.status.models.StatusIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * Repository interface for managing {@link StatusIncident} entities.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for incident data</li>
 *   <li>Support filtering by status, severity, and date ranges</li>
 *   <li>Enable queries for active and resolved incidents</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see StatusIncident
 * @see StatusAppRepository
 * @see StatusIncidentComponentRepository
 * @see StatusIncidentUpdateRepository
 */
@Repository
public interface StatusIncidentRepository extends JpaRepository<StatusIncident, UUID> {

    /**
     * Finds all incidents belonging to a specific status app.
     *
     * @param appId the unique identifier of the status app
     * @return a list of incidents belonging to the specified app
     */
    List<StatusIncident> findByAppId(UUID appId);

    /**
     * Finds all incidents with a specific status.
     *
     * @param status the status to filter by (e.g., "INVESTIGATING", "IDENTIFIED", "RESOLVED")
     * @return a list of incidents matching the specified status
     */
    List<StatusIncident> findByStatus(String status);

    /**
     * Finds all incidents with a specific severity level.
     *
     * @param severity the severity to filter by (e.g., "MINOR", "MAJOR", "CRITICAL")
     * @return a list of incidents matching the specified severity
     */
    List<StatusIncident> findBySeverity(String severity);

    /**
     * Finds all incidents by their public visibility setting.
     *
     * @param isPublic true to find public incidents, false for private ones
     * @return a list of incidents matching the visibility setting
     */
    List<StatusIncident> findByIsPublic(Boolean isPublic);

    /**
     * Finds all incidents belonging to a status app with a specific status.
     *
     * @param appId the unique identifier of the status app
     * @param status the status to filter by
     * @return a list of incidents matching both the app and status criteria
     */
    List<StatusIncident> findByAppIdAndStatus(UUID appId, String status);

    /**
     * Finds all incidents belonging to a status app with a specific visibility setting.
     *
     * @param appId the unique identifier of the status app
     * @param isPublic true to find public incidents, false for private ones
     * @return a list of incidents matching both the app and visibility criteria
     */
    List<StatusIncident> findByAppIdAndIsPublic(UUID appId, Boolean isPublic);

    /**
     * Finds all incidents that started within a specific date range.
     *
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return a list of incidents that started within the specified date range
     */
    List<StatusIncident> findByStartedAtBetween(ZonedDateTime startDate, ZonedDateTime endDate);

    /**
     * Finds all incidents that are currently unresolved (resolvedAt is null).
     *
     * @return a list of all unresolved incidents
     */
    List<StatusIncident> findByResolvedAtIsNull();

    /**
     * Finds all incidents that have been resolved (resolvedAt is not null).
     *
     * @return a list of all resolved incidents
     */
    List<StatusIncident> findByResolvedAtIsNotNull();

    /**
     * Finds all incidents within a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a list of incidents within the specified tenant
     */
    @Query("SELECT i FROM StatusIncident i WHERE i.app.tenant.id = :tenantId")
    List<StatusIncident> findByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Finds all incidents within a specific organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return a list of incidents within the specified organization
     */
    @Query("SELECT i FROM StatusIncident i WHERE i.app.organization.id = :organizationId")
    List<StatusIncident> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Finds all incidents for a status app, ordered by start time in descending order.
     *
     * @param appId the unique identifier of the status app
     * @return a list of incidents ordered by start time (most recent first)
     */
    @Query("SELECT i FROM StatusIncident i WHERE i.app.id = :appId ORDER BY i.startedAt DESC")
    List<StatusIncident> findByAppIdOrderByStartedAtDesc(@Param("appId") UUID appId);

    /**
     * Finds recent incidents for a status app that started on or after the specified date.
     *
     * @param appId the unique identifier of the status app
     * @param startDate the minimum start date for incidents to include
     * @return a list of incidents that started on or after the specified date
     */
    @Query("SELECT i FROM StatusIncident i WHERE i.app.id = :appId AND i.startedAt >= :startDate")
    List<StatusIncident> findRecentIncidentsByAppId(@Param("appId") UUID appId, @Param("startDate") ZonedDateTime startDate);

    /**
     * Searches for incidents globally by title or description containing the search term.
     *
     * @param searchTerm the term to search for in incident title or description
     * @return a list of incidents matching the search criteria
     */
    @Query("SELECT i FROM StatusIncident i WHERE i.title LIKE %:searchTerm% OR i.description LIKE %:searchTerm%")
    List<StatusIncident> search(@Param("searchTerm") String searchTerm);

    /**
     * Counts the total number of incidents belonging to a specific status app.
     *
     * @param appId the unique identifier of the status app
     * @return the count of incidents for the specified app
     */
    @Query("SELECT COUNT(i) FROM StatusIncident i WHERE i.app.id = :appId")
    Long countByAppId(@Param("appId") UUID appId);

    /**
     * Counts the total number of incidents within a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return the count of incidents within the specified tenant
     */
    @Query("SELECT COUNT(i) FROM StatusIncident i WHERE i.app.tenant.id = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Counts the number of active (unresolved) incidents for a specific status app.
     *
     * @param appId the unique identifier of the status app
     * @return the count of active incidents for the specified app
     */
    @Query("SELECT COUNT(i) FROM StatusIncident i WHERE i.app.id = :appId AND i.status != 'RESOLVED'")
    Long countActiveIncidentsByAppId(@Param("appId") UUID appId);

    /**
     * Finds active automated incidents for a specific status app.
     * Automated incidents are created by the system during health check failures.
     *
     * @param appId the unique identifier of the status app
     * @param createdBy the creator identifier (typically "system" for automated incidents)
     * @return a list of active automated incidents
     */
    @Query("SELECT i FROM StatusIncident i WHERE i.app.id = :appId AND i.createdBy = :createdBy AND i.status != 'RESOLVED'")
    List<StatusIncident> findActiveAutomatedIncidents(@Param("appId") UUID appId, @Param("createdBy") String createdBy);
}