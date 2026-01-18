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

    Page<PlatformEvent> findByAppId(UUID appId, Pageable pageable);

    Page<PlatformEvent> findByComponentId(UUID componentId, Pageable pageable);

    Page<PlatformEvent> findBySeverity(String severity, Pageable pageable);

    Page<PlatformEvent> findByAppIdAndSeverity(UUID appId, String severity, Pageable pageable);

    Page<PlatformEvent> findByAppIdAndComponentId(UUID appId, UUID componentId, Pageable pageable);

    List<PlatformEvent> findByAppIdOrderByEventTimeDesc(UUID appId);

    @Query("SELECT e FROM PlatformEvent e WHERE e.app.id = :appId AND e.eventTime BETWEEN :startDate AND :endDate ORDER BY e.eventTime DESC")
    Page<PlatformEvent> findByAppIdAndDateRange(
            @Param("appId") UUID appId,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate,
            Pageable pageable);

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

    @Query("SELECT COUNT(e) FROM PlatformEvent e WHERE e.app.id = :appId")
    Long countByAppId(@Param("appId") UUID appId);

    @Query("SELECT COUNT(e) FROM PlatformEvent e WHERE e.app.id = :appId AND e.severity = :severity")
    Long countByAppIdAndSeverity(@Param("appId") UUID appId, @Param("severity") String severity);

    void deleteByAppId(UUID appId);
}
