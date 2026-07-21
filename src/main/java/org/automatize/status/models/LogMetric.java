package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing an aggregated log count for a service+level in a time bucket.
 * Used for dashboards and alert rule evaluation without scanning raw logs.
 */
@Entity
@Table(name = "log_metrics")
public class LogMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "service", nullable = false, length = 255)
    private String service;

    @Column(name = "level", nullable = false, length = 20)
    private String level;

    @Column(name = "bucket", nullable = false)
    private ZonedDateTime bucket;

    @Column(name = "bucket_type", nullable = false, length = 10)
    private String bucketType = "MINUTE";

    @Column(name = "count", nullable = false)
    private Long count = 0L;

    @Column(name = "created_date_technical", nullable = false)
    private Long createdDateTechnical;

    /**
     * JPA lifecycle callback executed before persisting a new metric row.
     * Populates the technical creation timestamp if it has not been set explicitly.
     */
    @PrePersist
    public void prePersist() {
        // Default the technical (epoch millis) creation timestamp when not already set
        if (createdDateTechnical == null) {
            createdDateTechnical = System.currentTimeMillis();
        }
    }

    /**
     * Default constructor required by JPA.
     */
    public LogMetric() {
    }

    /** @return the unique identifier of the metric row */
    public UUID getId() { return id; }
    /** @param id the unique identifier to set */
    public void setId(UUID id) { this.id = id; }

    /** @return the tenant that owns this metric */
    public Tenant getTenant() { return tenant; }
    /** @param tenant the owning tenant to set */
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    /** @return the service the metric aggregates */
    public String getService() { return service; }
    /** @param service the service to set */
    public void setService(String service) { this.service = service; }

    /** @return the log level the metric aggregates */
    public String getLevel() { return level; }
    /** @param level the log level to set */
    public void setLevel(String level) { this.level = level; }

    /** @return the start of the time bucket */
    public ZonedDateTime getBucket() { return bucket; }
    /** @param bucket the time bucket start to set */
    public void setBucket(ZonedDateTime bucket) { this.bucket = bucket; }

    /** @return the bucket granularity type (e.g. MINUTE) */
    public String getBucketType() { return bucketType; }
    /** @param bucketType the bucket granularity type to set */
    public void setBucketType(String bucketType) { this.bucketType = bucketType; }

    /** @return the aggregated log count for the bucket */
    public Long getCount() { return count; }
    /** @param count the aggregated log count to set */
    public void setCount(Long count) { this.count = count; }

    /** @return the technical creation timestamp in epoch milliseconds */
    public Long getCreatedDateTechnical() { return createdDateTechnical; }
    /** @param createdDateTechnical the technical creation timestamp to set */
    public void setCreatedDateTechnical(Long createdDateTechnical) { this.createdDateTechnical = createdDateTechnical; }
}
