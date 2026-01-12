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

@Repository
public interface StatusUptimeHistoryRepository extends JpaRepository<StatusUptimeHistory, UUID> {

    @Query("SELECT h FROM StatusUptimeHistory h WHERE h.app.id = :appId AND h.component IS NULL AND h.recordDate BETWEEN :startDate AND :endDate ORDER BY h.recordDate ASC")
    List<StatusUptimeHistory> findAppUptimeHistory(@Param("appId") UUID appId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT h FROM StatusUptimeHistory h WHERE h.component.id = :componentId AND h.recordDate BETWEEN :startDate AND :endDate ORDER BY h.recordDate ASC")
    List<StatusUptimeHistory> findComponentUptimeHistory(@Param("componentId") UUID componentId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT h FROM StatusUptimeHistory h WHERE h.app.id = :appId AND h.component IS NULL AND h.recordDate = :date")
    Optional<StatusUptimeHistory> findAppUptimeByDate(@Param("appId") UUID appId, @Param("date") LocalDate date);

    @Query("SELECT h FROM StatusUptimeHistory h WHERE h.component.id = :componentId AND h.recordDate = :date")
    Optional<StatusUptimeHistory> findComponentUptimeByDate(@Param("componentId") UUID componentId, @Param("date") LocalDate date);

    @Query("SELECT AVG(h.uptimePercentage) FROM StatusUptimeHistory h WHERE h.app.id = :appId AND h.component IS NULL AND h.recordDate BETWEEN :startDate AND :endDate")
    Double calculateAverageAppUptime(@Param("appId") UUID appId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT AVG(h.uptimePercentage) FROM StatusUptimeHistory h WHERE h.component.id = :componentId AND h.recordDate BETWEEN :startDate AND :endDate")
    Double calculateAverageComponentUptime(@Param("componentId") UUID componentId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(h.incidentCount) FROM StatusUptimeHistory h WHERE h.app.id = :appId AND h.component IS NULL AND h.recordDate BETWEEN :startDate AND :endDate")
    Long countAppIncidents(@Param("appId") UUID appId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(h.incidentCount) FROM StatusUptimeHistory h WHERE h.component.id = :componentId AND h.recordDate BETWEEN :startDate AND :endDate")
    Long countComponentIncidents(@Param("componentId") UUID componentId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<StatusUptimeHistory> findByAppIdAndComponentIsNullOrderByRecordDateAsc(UUID appId);

    List<StatusUptimeHistory> findByComponentIdOrderByRecordDateAsc(UUID componentId);
}
