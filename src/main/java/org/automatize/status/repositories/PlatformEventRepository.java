package org.automatize.status.repositories;

import org.automatize.status.models.PlatformEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * Repository interface for managing {@link PlatformEvent} entities.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for platform event data</li>
 *   <li>Support filtering by severity, date ranges, and search text</li>
 *   <li>Enable paginated queries for event listings</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see PlatformEvent
 * @see StatusAppRepository
 * @see StatusComponentRepository
 */
@Repository
public interface PlatformEventRepository extends JpaRepository<PlatformEvent, UUID> {

    /**
     * Finds a page of platform events belonging to a specific status app.
     *
     * @param appId the unique identifier of the status app
     * @param pageable pagination and sorting parameters
     * @return a page of events for the specified app
     */
    Page<PlatformEvent> findByAppId(UUID appId, Pageable pageable);

    /**
     * Finds a page of platform events belonging to a specific component.
     *
     * @param componentId the unique identifier of the component
     * @param pageable pagination and sorting parameters
     * @return a page of events for the specified component
     */
    Page<PlatformEvent> findByComponentId(UUID componentId, Pageable pageable);

    /**
     * Finds a page of platform events with a specific severity.
     *
     * @param severity the severity to filter by (e.g., "INFO", "WARNING", "CRITICAL")
     * @param pageable pagination and sorting parameters
     * @return a page of events matching the specified severity
     */
    Page<PlatformEvent> findBySeverity(String severity, Pageable pageable);

    /**
     * Finds a page of platform events for a status app with a specific severity.
     *
     * @param appId the unique identifier of the status app
     * @param severity the severity to filter by
     * @param pageable pagination and sorting parameters
     * @return a page of events matching both the app and severity criteria
     */
    Page<PlatformEvent> findByAppIdAndSeverity(UUID appId, String severity, Pageable pageable);

    /**
     * Finds a page of platform events for a status app affecting a specific component.
     *
     * @param appId the unique identifier of the status app
     * @param componentId the unique identifier of the component
     * @param pageable pagination and sorting parameters
     * @return a page of events matching both the app and component criteria
     */
    Page<PlatformEvent> findByAppIdAndComponentId(UUID appId, UUID componentId, Pageable pageable);

    /**
     * Finds all platform events for a status app, ordered by event time descending.
     *
     * @param appId the unique identifier of the status app
     * @return a list of the app's events, most recent first
     */
    List<PlatformEvent> findByAppIdOrderByEventTimeDesc(UUID appId);

    /**
     * Finds a page of platform events for a status app within a date range, most recent first.
     *
     * @param appId the unique identifier of the status app
     * @param startDate the start of the event-time range (inclusive)
     * @param endDate the end of the event-time range (inclusive)
     * @param pageable pagination and sorting parameters
     * @return a page of events for the app within the specified range
     */
    @Query("SELECT e FROM PlatformEvent e WHERE e.app.id = :appId AND e.eventTime BETWEEN :startDate AND :endDate ORDER BY e.eventTime DESC")
    Page<PlatformEvent> findByAppIdAndDateRange(
            @Param("appId") UUID appId,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate,
            Pageable pageable);

    /**
     * Finds a page of platform events applying optional filters on app, component, severity,
     * and event-time range, ordered by event time descending. Null filter arguments are ignored.
     *
     * @param appId the app to filter by, or {@code null} for all apps
     * @param componentId the component to filter by, or {@code null} for all components
     * @param severity the severity to filter by, or {@code null} for all severities
     * @param startDate the lower bound of the event-time range (inclusive), or {@code null}
     * @param endDate the upper bound of the event-time range (inclusive), or {@code null}
     * @param pageable pagination and sorting parameters
     * @return a page of events matching the supplied filters, most recent first
     */
    @Query(value = "SELECT * FROM platform_events e WHERE " +
           "(CAST(:appId AS uuid) IS NULL OR e.app_id = :appId) AND " +
           "(CAST(:componentId AS uuid) IS NULL OR e.component_id = :componentId) AND " +
           "(CAST(:severity AS varchar) IS NULL OR e.severity = :severity) AND " +
           "(CAST(:startDate AS timestamptz) IS NULL OR e.event_time >= :startDate) AND " +
           "(CAST(:endDate AS timestamptz) IS NULL OR e.event_time <= :endDate) " +
           "ORDER BY e.event_time DESC",
           countQuery = "SELECT COUNT(*) FROM platform_events e WHERE " +
           "(CAST(:appId AS uuid) IS NULL OR e.app_id = :appId) AND " +
           "(CAST(:componentId AS uuid) IS NULL OR e.component_id = :componentId) AND " +
           "(CAST(:severity AS varchar) IS NULL OR e.severity = :severity) AND " +
           "(CAST(:startDate AS timestamptz) IS NULL OR e.event_time >= :startDate) AND " +
           "(CAST(:endDate AS timestamptz) IS NULL OR e.event_time <= :endDate)",
           nativeQuery = true)
    Page<PlatformEvent> findWithFilters(
            @Param("appId") UUID appId,
            @Param("componentId") UUID componentId,
            @Param("severity") String severity,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate,
            Pageable pageable);

    /**
     * Finds a page of platform events applying the same optional filters as
     * {@link #findWithFilters}, additionally restricted to events whose message,
     * details, or source contain the given search text (case-insensitive).
     *
     * @param appId the app to filter by, or {@code null} for all apps
     * @param componentId the component to filter by, or {@code null} for all components
     * @param severity the severity to filter by, or {@code null} for all severities
     * @param startDate the lower bound of the event-time range (inclusive), or {@code null}
     * @param endDate the upper bound of the event-time range (inclusive), or {@code null}
     * @param searchText the case-insensitive substring matched against message, details, and source
     * @param pageable pagination and sorting parameters
     * @return a page of events matching the filters and search text, most recent first
     */
    @Query(value = "SELECT * FROM platform_events e WHERE " +
           "(CAST(:appId AS uuid) IS NULL OR e.app_id = :appId) AND " +
           "(CAST(:componentId AS uuid) IS NULL OR e.component_id = :componentId) AND " +
           "(CAST(:severity AS varchar) IS NULL OR e.severity = :severity) AND " +
           "(CAST(:startDate AS timestamptz) IS NULL OR e.event_time >= :startDate) AND " +
           "(CAST(:endDate AS timestamptz) IS NULL OR e.event_time <= :endDate) AND " +
           "(LOWER(e.message) LIKE LOWER(CONCAT('%', CAST(:searchText AS varchar), '%')) OR " +
           " LOWER(COALESCE(e.details, '')) LIKE LOWER(CONCAT('%', CAST(:searchText AS varchar), '%')) OR " +
           " LOWER(COALESCE(e.source, '')) LIKE LOWER(CONCAT('%', CAST(:searchText AS varchar), '%'))) " +
           "ORDER BY e.event_time DESC",
           countQuery = "SELECT COUNT(*) FROM platform_events e WHERE " +
           "(CAST(:appId AS uuid) IS NULL OR e.app_id = :appId) AND " +
           "(CAST(:componentId AS uuid) IS NULL OR e.component_id = :componentId) AND " +
           "(CAST(:severity AS varchar) IS NULL OR e.severity = :severity) AND " +
           "(CAST(:startDate AS timestamptz) IS NULL OR e.event_time >= :startDate) AND " +
           "(CAST(:endDate AS timestamptz) IS NULL OR e.event_time <= :endDate) AND " +
           "(LOWER(e.message) LIKE LOWER(CONCAT('%', CAST(:searchText AS varchar), '%')) OR " +
           " LOWER(COALESCE(e.details, '')) LIKE LOWER(CONCAT('%', CAST(:searchText AS varchar), '%')) OR " +
           " LOWER(COALESCE(e.source, '')) LIKE LOWER(CONCAT('%', CAST(:searchText AS varchar), '%')))",
           nativeQuery = true)
    Page<PlatformEvent> searchWithFilters(
            @Param("appId") UUID appId,
            @Param("componentId") UUID componentId,
            @Param("severity") String severity,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate,
            @Param("searchText") String searchText,
            Pageable pageable);

    /**
     * Counts the total number of platform events belonging to a specific status app.
     *
     * @param appId the unique identifier of the status app
     * @return the count of events for the specified app
     */
    @Query("SELECT COUNT(e) FROM PlatformEvent e WHERE e.app.id = :appId")
    Long countByAppId(@Param("appId") UUID appId);

    /**
     * Counts the number of platform events for a status app with a specific severity.
     *
     * @param appId the unique identifier of the status app
     * @param severity the severity to count
     * @return the count of matching events
     */
    @Query("SELECT COUNT(e) FROM PlatformEvent e WHERE e.app.id = :appId AND e.severity = :severity")
    Long countByAppIdAndSeverity(@Param("appId") UUID appId, @Param("severity") String severity);

    /**
     * Deletes all platform events belonging to a specific status app.
     *
     * @param appId the unique identifier of the status app whose events should be removed
     */
    void deleteByAppId(UUID appId);
}
