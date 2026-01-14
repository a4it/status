package org.automatize.status.repositories;

import org.automatize.status.models.StatusUptimeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link StatusUptimeHistory} entities.
 * <p>
 * Provides data access operations for uptime history records within the system.
 * Uptime history tracks daily uptime statistics for both status apps and individual
 * components, including uptime percentages and incident counts. This data is used
 * for generating uptime reports and historical analytics.
 * </p>
 *
 * @see StatusUptimeHistory
 * @see StatusAppRepository
 * @see StatusComponentRepository
 */
@Repository
public interface StatusUptimeHistoryRepository extends JpaRepository<StatusUptimeHistory, UUID> {

    /**
     * Finds uptime history records for a status app within a date range.
     * <p>
     * Returns app-level records only (where component is null).
     * </p>
     *
     * @param appId the unique identifier of the status app
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return a list of uptime history records ordered by date ascending
     */
    @Query("SELECT h FROM StatusUptimeHistory h WHERE h.app.id = :appId AND h.component IS NULL AND h.recordDate BETWEEN :startDate AND :endDate ORDER BY h.recordDate ASC")
    List<StatusUptimeHistory> findAppUptimeHistory(@Param("appId") UUID appId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Finds uptime history records for a specific component within a date range.
     *
     * @param componentId the unique identifier of the component
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return a list of uptime history records ordered by date ascending
     */
    @Query("SELECT h FROM StatusUptimeHistory h WHERE h.component.id = :componentId AND h.recordDate BETWEEN :startDate AND :endDate ORDER BY h.recordDate ASC")
    List<StatusUptimeHistory> findComponentUptimeHistory(@Param("componentId") UUID componentId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Finds the uptime record for a status app on a specific date.
     * <p>
     * Returns app-level record only (where component is null).
     * </p>
     *
     * @param appId the unique identifier of the status app
     * @param date the specific date to retrieve uptime for
     * @return an Optional containing the uptime record if found, or empty if not found
     */
    @Query("SELECT h FROM StatusUptimeHistory h WHERE h.app.id = :appId AND h.component IS NULL AND h.recordDate = :date")
    Optional<StatusUptimeHistory> findAppUptimeByDate(@Param("appId") UUID appId, @Param("date") LocalDate date);

    /**
     * Finds the uptime record for a component on a specific date.
     *
     * @param componentId the unique identifier of the component
     * @param date the specific date to retrieve uptime for
     * @return an Optional containing the uptime record if found, or empty if not found
     */
    @Query("SELECT h FROM StatusUptimeHistory h WHERE h.component.id = :componentId AND h.recordDate = :date")
    Optional<StatusUptimeHistory> findComponentUptimeByDate(@Param("componentId") UUID componentId, @Param("date") LocalDate date);

    /**
     * Calculates the average uptime percentage for a status app within a date range.
     * <p>
     * Uses app-level records only (where component is null).
     * </p>
     *
     * @param appId the unique identifier of the status app
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return the average uptime percentage, or null if no records exist
     */
    @Query("SELECT AVG(h.uptimePercentage) FROM StatusUptimeHistory h WHERE h.app.id = :appId AND h.component IS NULL AND h.recordDate BETWEEN :startDate AND :endDate")
    Double calculateAverageAppUptime(@Param("appId") UUID appId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Calculates the average uptime percentage for a component within a date range.
     *
     * @param componentId the unique identifier of the component
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return the average uptime percentage, or null if no records exist
     */
    @Query("SELECT AVG(h.uptimePercentage) FROM StatusUptimeHistory h WHERE h.component.id = :componentId AND h.recordDate BETWEEN :startDate AND :endDate")
    Double calculateAverageComponentUptime(@Param("componentId") UUID componentId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Counts the total number of incidents for a status app within a date range.
     * <p>
     * Uses app-level records only (where component is null).
     * </p>
     *
     * @param appId the unique identifier of the status app
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return the total incident count, or null if no records exist
     */
    @Query("SELECT SUM(h.incidentCount) FROM StatusUptimeHistory h WHERE h.app.id = :appId AND h.component IS NULL AND h.recordDate BETWEEN :startDate AND :endDate")
    Long countAppIncidents(@Param("appId") UUID appId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Counts the total number of incidents for a component within a date range.
     *
     * @param componentId the unique identifier of the component
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return the total incident count, or null if no records exist
     */
    @Query("SELECT SUM(h.incidentCount) FROM StatusUptimeHistory h WHERE h.component.id = :componentId AND h.recordDate BETWEEN :startDate AND :endDate")
    Long countComponentIncidents(@Param("componentId") UUID componentId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Finds all uptime history records for a status app (app-level only).
     * <p>
     * Returns records where component is null, ordered by date ascending.
     * </p>
     *
     * @param appId the unique identifier of the status app
     * @return a list of uptime history records ordered by date ascending
     */
    List<StatusUptimeHistory> findByAppIdAndComponentIsNullOrderByRecordDateAsc(UUID appId);

    /**
     * Finds all uptime history records for a specific component.
     *
     * @param componentId the unique identifier of the component
     * @return a list of uptime history records ordered by date ascending
     */
    List<StatusUptimeHistory> findByComponentIdOrderByRecordDateAsc(UUID componentId);
}
