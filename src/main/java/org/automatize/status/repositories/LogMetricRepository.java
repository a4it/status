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

@Repository
public interface LogMetricRepository extends JpaRepository<LogMetric, UUID> {

    @Query("SELECT m FROM LogMetric m WHERE m.bucket >= :since ORDER BY m.bucket ASC")
    List<LogMetric> findSince(@Param("since") ZonedDateTime since);

    @Query("SELECT m FROM LogMetric m WHERE m.bucket >= :since AND m.bucketType = :type ORDER BY m.bucket ASC")
    List<LogMetric> findSinceByType(@Param("since") ZonedDateTime since, @Param("type") String type);

    @Query("SELECT m FROM LogMetric m WHERE " +
           "(:tenantId IS NULL OR m.tenant.id = :tenantId) AND " +
           "m.bucket >= :since " +
           "ORDER BY m.bucket ASC")
    List<LogMetric> findByTenantSince(@Param("tenantId") UUID tenantId, @Param("since") ZonedDateTime since);

    @Query("SELECT SUM(m.count) FROM LogMetric m WHERE " +
           "(:service IS NULL OR m.service = :service) AND " +
           "(:level IS NULL OR m.level = :level) AND " +
           "m.bucket >= :since")
    Long sumCountSince(
            @Param("service") String service,
            @Param("level") String level,
            @Param("since") ZonedDateTime since);

    Optional<LogMetric> findByTenantIdAndServiceAndLevelAndBucketAndBucketType(
            UUID tenantId, String service, String level, ZonedDateTime bucket, String bucketType);
}
