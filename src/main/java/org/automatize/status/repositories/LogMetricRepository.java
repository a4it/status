package org.automatize.status.repositories;

import org.automatize.status.models.LogMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link LogMetric} entities.
 *
 * <p>Provides access to pre-aggregated log count metrics bucketed by time
 * window, service, and level. Used to power dashboards and time-series charts
 * without scanning raw log rows.</p>
 */
@Repository
public interface LogMetricRepository extends JpaRepository<LogMetric, UUID> {

    /**
     * Fetches all metric buckets whose timestamp is at or after the given point in time.
     *
     * @param since the lower bound of the bucket time window (inclusive)
     * @return a list of matching metrics ordered by bucket ascending
     */
    @Query("SELECT m FROM LogMetric m WHERE m.bucket >= :since ORDER BY m.bucket ASC")
    List<LogMetric> findSince(@Param("since") ZonedDateTime since);

    /**
     * Fetches all metric buckets of a given bucket type at or after the given point in time.
     *
     * @param since the lower bound of the bucket time window (inclusive)
     * @param type the bucket granularity/type to filter by
     * @return a list of matching metrics ordered by bucket ascending
     */
    @Query("SELECT m FROM LogMetric m WHERE m.bucket >= :since AND m.bucketType = :type ORDER BY m.bucket ASC")
    List<LogMetric> findSinceByType(@Param("since") ZonedDateTime since, @Param("type") String type);

    /**
     * Fetches metric buckets since a point in time, optionally scoped to a tenant.
     * When {@code tenantId} is {@code null} metrics for all tenants are returned.
     *
     * @param tenantId the tenant to scope results to, or {@code null} for all tenants
     * @param since the lower bound of the bucket time window (inclusive)
     * @return a list of matching metrics ordered by bucket ascending
     */
    @Query("SELECT m FROM LogMetric m WHERE " +
           "(:tenantId IS NULL OR m.tenant.id = :tenantId) AND " +
           "m.bucket >= :since " +
           "ORDER BY m.bucket ASC")
    List<LogMetric> findByTenantSince(@Param("tenantId") UUID tenantId, @Param("since") ZonedDateTime since);

    /**
     * Sums the log counts across all buckets since a point in time, optionally filtered
     * by service and/or level. Null filter arguments are ignored (match everything).
     *
     * @param service the service name to filter by, or {@code null} for all services
     * @param level the log level to filter by, or {@code null} for all levels
     * @param since the lower bound of the bucket time window (inclusive)
     * @return the total summed count, or {@code null} if no buckets match
     */
    @Query("SELECT SUM(m.count) FROM LogMetric m WHERE " +
           "(:service IS NULL OR m.service = :service) AND " +
           "(:level IS NULL OR m.level = :level) AND " +
           "m.bucket >= :since")
    Long sumCountSince(
            @Param("service") String service,
            @Param("level") String level,
            @Param("since") ZonedDateTime since);

    /**
     * Finds the single metric bucket matching an exact tenant/service/level/bucket/bucketType
     * combination. Used to locate an existing bucket for incremental upsert during aggregation.
     *
     * @param tenantId the unique identifier of the tenant
     * @param service the service name
     * @param level the log level
     * @param bucket the bucket timestamp
     * @param bucketType the bucket granularity/type
     * @return an Optional containing the matching metric if present
     */
    Optional<LogMetric> findByTenantIdAndServiceAndLevelAndBucketAndBucketType(
            UUID tenantId, String service, String level, ZonedDateTime bucket, String bucketType);
}
