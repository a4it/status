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

/**
 * Repository interface for managing {@link Log} entities.
 *
 * <p>Provides paginated full-text search over ingested log records, distinct
 * value lookups for filter dropdowns (services, trace IDs), trace correlation,
 * retention-based bulk deletes, and metric aggregation used to build
 * {@link org.automatize.status.models.LogMetric} rollups.</p>
 */
@Repository
public interface LogRepository extends JpaRepository<Log, UUID> {

    /**
     * Searches log records with optional filters on tenant, level, service, date range,
     * and free-text message search, returning a paginated result ordered by timestamp descending.
     * Null filter arguments are ignored (match everything).
     *
     * @param tenantId the tenant to scope results to, or {@code null} for all tenants
     * @param level the log level to filter by, or {@code null} for all levels
     * @param service the service name to filter by, or {@code null} for all services
     * @param startDate the lower bound of the timestamp range (inclusive), or {@code null}
     * @param endDate the upper bound of the timestamp range (inclusive), or {@code null}
     * @param search a case-insensitive substring to match against the message, or {@code null}
     * @param pageable pagination and sorting parameters
     * @return a page of matching log records, newest first
     */
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

    /**
     * Returns the distinct set of service names that have logged for a specific tenant.
     * Used to populate service filter dropdowns.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a sorted list of distinct service names for the tenant
     */
    @Query("SELECT DISTINCT l.service FROM Log l WHERE l.tenant.id = :tenantId ORDER BY l.service")
    List<String> findDistinctServicesByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Returns the distinct set of all service names that have logged, across all tenants.
     * Used to populate service filter dropdowns.
     *
     * @return a sorted list of all distinct service names
     */
    @Query("SELECT DISTINCT l.service FROM Log l ORDER BY l.service")
    List<String> findDistinctServices();

    /**
     * Returns distinct non-empty trace IDs for the given services within a time window,
     * optionally scoped to a tenant. Used to page through traces for correlation views.
     *
     * @param tenantId the tenant to scope results to, or {@code null} for all tenants
     * @param from the lower bound of the timestamp range (inclusive)
     * @param to the upper bound of the timestamp range (inclusive)
     * @param serviceNames the services whose trace IDs should be returned
     * @param pageable pagination parameters limiting the number of trace IDs
     * @return a list of distinct trace IDs matching the criteria
     */
    @Query("SELECT DISTINCT l.traceId FROM Log l WHERE l.traceId IS NOT NULL AND l.traceId <> '' " +
           "AND (:tenantId IS NULL OR l.tenant.id = :tenantId) " +
           "AND l.logTimestamp BETWEEN :from AND :to " +
           "AND l.service IN :serviceNames")
    List<String> findDistinctTraceIdsForServices(
            @Param("tenantId") UUID tenantId,
            @Param("from") ZonedDateTime from,
            @Param("to") ZonedDateTime to,
            @Param("serviceNames") List<String> serviceNames,
            Pageable pageable);

    /**
     * Fetches all log records belonging to the given trace IDs, ordered by trace then timestamp.
     * Used to reconstruct the full sequence of logs for one or more traces.
     *
     * @param traceIds the trace IDs whose logs should be fetched
     * @return a list of log records grouped by trace ID and ordered chronologically within each trace
     */
    @Query("SELECT l FROM Log l WHERE l.traceId IN :traceIds ORDER BY l.traceId, l.logTimestamp")
    List<Log> findByTraceIdIn(@Param("traceIds") List<String> traceIds);

    /**
     * Deletes all log records older than the given cutoff timestamp; used for retention/housekeeping.
     *
     * @param cutoff logs with a timestamp strictly before this value are deleted
     * @return the number of log records deleted
     */
    @Modifying
    @Query("DELETE FROM Log l WHERE l.logTimestamp < :cutoff")
    int deleteOlderThan(@Param("cutoff") ZonedDateTime cutoff);

    /**
     * Deletes all log records belonging to any of the given services.
     *
     * @param serviceNames the services whose logs should be removed
     * @return the number of log records deleted
     */
    @Modifying
    @Query("DELETE FROM Log l WHERE l.service IN :serviceNames")
    int deleteByServiceIn(@Param("serviceNames") List<String> serviceNames);

    /**
     * Aggregates log counts by (tenant_id, service, level) for a time window.
     * Returns one row per unique combination — never materializes individual log rows.
     * Each row is Object[]{tenant_id (UUID), service (String), level (String), count (Long)}.
     */
    @Query(value = "SELECT l.tenant_id, l.service, l.level, COUNT(*) as cnt " +
                   "FROM logs l WHERE l.log_timestamp >= :start AND l.log_timestamp < :end " +
                   "GROUP BY l.tenant_id, l.service, l.level",
           nativeQuery = true)
    List<Object[]> aggregateByServiceLevel(@Param("start") ZonedDateTime start,
                                            @Param("end") ZonedDateTime end);
}
