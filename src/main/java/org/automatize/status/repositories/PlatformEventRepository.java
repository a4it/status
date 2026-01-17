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
 * Repository interface for managing {@link PlatformEvent} entities.
 * <p>
 * Provides data access operations for platform events within the system.
 * Events are log entries from monitored platforms and their components.
 * </p>
 *
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

    @Query("SELECT e FROM PlatformEvent e WHERE " +
           "(:appId IS NULL OR e.app.id = :appId) AND " +
           "(:componentId IS NULL OR e.component.id = :componentId) AND " +
           "(:severity IS NULL OR e.severity = :severity) AND " +
           "(:startDate IS NULL OR e.eventTime >= :startDate) AND " +
           "(:endDate IS NULL OR e.eventTime <= :endDate) " +
           "ORDER BY e.eventTime DESC")
    Page<PlatformEvent> findWithFilters(
            @Param("appId") UUID appId,
            @Param("componentId") UUID componentId,
            @Param("severity") String severity,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate,
            Pageable pageable);

    @Query("SELECT e FROM PlatformEvent e WHERE " +
           "(:appId IS NULL OR e.app.id = :appId) AND " +
           "(:componentId IS NULL OR e.component.id = :componentId) AND " +
           "(:severity IS NULL OR e.severity = :severity) AND " +
           "(:startDate IS NULL OR e.eventTime >= :startDate) AND " +
           "(:endDate IS NULL OR e.eventTime <= :endDate) AND " +
           "(LOWER(e.message) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
           " LOWER(e.details) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
           " LOWER(e.source) LIKE LOWER(CONCAT('%', :searchText, '%'))) " +
           "ORDER BY e.eventTime DESC")
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
