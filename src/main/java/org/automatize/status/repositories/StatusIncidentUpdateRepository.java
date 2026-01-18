package org.automatize.status.repositories;

import org.automatize.status.models.StatusIncidentUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * Repository interface for managing {@link StatusIncidentUpdate} entities.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for incident update data</li>
 *   <li>Support chronological ordering of updates per incident</li>
 *   <li>Enable filtering by status and date ranges</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see StatusIncidentUpdate
 * @see StatusIncidentRepository
 */
@Repository
public interface StatusIncidentUpdateRepository extends JpaRepository<StatusIncidentUpdate, UUID> {

    /**
     * Finds all updates for a specific incident.
     *
     * @param incidentId the unique identifier of the incident
     * @return a list of updates for the specified incident
     */
    List<StatusIncidentUpdate> findByIncidentId(UUID incidentId);

    /**
     * Finds all updates for an incident, ordered by update time in ascending order.
     *
     * @param incidentId the unique identifier of the incident
     * @return a list of updates ordered chronologically (oldest first)
     */
    List<StatusIncidentUpdate> findByIncidentIdOrderByUpdateTime(UUID incidentId);

    /**
     * Finds all updates for an incident, ordered by update time in descending order.
     *
     * @param incidentId the unique identifier of the incident
     * @return a list of updates ordered chronologically (most recent first)
     */
    List<StatusIncidentUpdate> findByIncidentIdOrderByUpdateTimeDesc(UUID incidentId);

    /**
     * Finds all incident updates with a specific status.
     *
     * @param status the status to filter by (e.g., "INVESTIGATING", "IDENTIFIED", "MONITORING")
     * @return a list of updates matching the specified status
     */
    List<StatusIncidentUpdate> findByStatus(String status);

    /**
     * Finds all incident updates that occurred within a specific date range.
     *
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return a list of updates that occurred within the specified date range
     */
    List<StatusIncidentUpdate> findByUpdateTimeBetween(ZonedDateTime startDate, ZonedDateTime endDate);

    /**
     * Finds all incident updates within a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a list of incident updates within the specified tenant
     */
    @Query("SELECT u FROM StatusIncidentUpdate u WHERE u.incident.app.tenant.id = :tenantId")
    List<StatusIncidentUpdate> findByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Finds all incident updates within a specific organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return a list of incident updates within the specified organization
     */
    @Query("SELECT u FROM StatusIncidentUpdate u WHERE u.incident.app.organization.id = :organizationId")
    List<StatusIncidentUpdate> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Finds all incident updates for a specific status app.
     *
     * @param appId the unique identifier of the status app
     * @return a list of incident updates for the specified app
     */
    @Query("SELECT u FROM StatusIncidentUpdate u WHERE u.incident.app.id = :appId")
    List<StatusIncidentUpdate> findByAppId(@Param("appId") UUID appId);

    /**
     * Searches for incident updates globally by message containing the search term.
     *
     * @param searchTerm the term to search for in update messages
     * @return a list of updates matching the search criteria
     */
    @Query("SELECT u FROM StatusIncidentUpdate u WHERE u.message LIKE %:searchTerm%")
    List<StatusIncidentUpdate> search(@Param("searchTerm") String searchTerm);

    /**
     * Searches for incident updates within an incident by message containing the search term.
     *
     * @param incidentId the unique identifier of the incident to search within
     * @param searchTerm the term to search for in update messages
     * @return a list of updates matching the search criteria within the specified incident
     */
    @Query("SELECT u FROM StatusIncidentUpdate u WHERE u.incident.id = :incidentId AND u.message LIKE %:searchTerm%")
    List<StatusIncidentUpdate> searchByIncidentId(@Param("incidentId") UUID incidentId, @Param("searchTerm") String searchTerm);

    /**
     * Counts the total number of updates for a specific incident.
     *
     * @param incidentId the unique identifier of the incident
     * @return the count of updates for the specified incident
     */
    @Query("SELECT COUNT(u) FROM StatusIncidentUpdate u WHERE u.incident.id = :incidentId")
    Long countByIncidentId(@Param("incidentId") UUID incidentId);

    /**
     * Finds the most recent update for a specific incident.
     *
     * @param incidentId the unique identifier of the incident
     * @return the latest incident update, or null if no updates exist
     */
    @Query("SELECT u FROM StatusIncidentUpdate u WHERE u.incident.id = :incidentId ORDER BY u.updateTime DESC LIMIT 1")
    StatusIncidentUpdate findLatestByIncidentId(@Param("incidentId") UUID incidentId);
}