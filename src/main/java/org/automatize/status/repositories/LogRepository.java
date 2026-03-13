package org.automatize.status.repositories;

import org.automatize.status.models.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LogRepository extends JpaRepository<Log, UUID> {

    @Query(value = "SELECT * FROM logs l WHERE " +
           "(CAST(:tenantId AS uuid) IS NULL OR l.tenant_id = :tenantId) AND " +
           "(CAST(:level AS varchar) IS NULL OR l.level = :level) AND " +
           "(CAST(:service AS varchar) IS NULL OR l.service = :service) AND " +
           "(CAST(:startDate AS timestamptz) IS NULL OR l.log_timestamp >= :startDate) AND " +
           "(CAST(:endDate AS timestamptz) IS NULL OR l.log_timestamp <= :endDate) AND " +
           "(:search IS NULL OR LOWER(l.message) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY l.log_timestamp DESC",
           countQuery = "SELECT COUNT(*) FROM logs l WHERE " +
           "(CAST(:tenantId AS uuid) IS NULL OR l.tenant_id = :tenantId) AND " +
           "(CAST(:level AS varchar) IS NULL OR l.level = :level) AND " +
           "(CAST(:service AS varchar) IS NULL OR l.service = :service) AND " +
           "(CAST(:startDate AS timestamptz) IS NULL OR l.log_timestamp >= :startDate) AND " +
           "(CAST(:endDate AS timestamptz) IS NULL OR l.log_timestamp <= :endDate) AND " +
           "(:search IS NULL OR LOWER(l.message) LIKE LOWER(CONCAT('%', :search, '%')))",
           nativeQuery = true)
    Page<Log> searchLogs(
            @Param("tenantId") UUID tenantId,
            @Param("level") String level,
            @Param("service") String service,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT DISTINCT l.service FROM Log l WHERE l.tenant.id = :tenantId ORDER BY l.service")
    List<String> findDistinctServicesByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT DISTINCT l.service FROM Log l ORDER BY l.service")
    List<String> findDistinctServices();

    @Modifying
    @Query("DELETE FROM Log l WHERE l.logTimestamp < :cutoff")
    int deleteOlderThan(@Param("cutoff") ZonedDateTime cutoff);
}
