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

    @PrePersist
    public void prePersist() {
        if (createdDateTechnical == null) {
            createdDateTechnical = System.currentTimeMillis();
        }
    }

    public LogMetric() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public ZonedDateTime getBucket() { return bucket; }
    public void setBucket(ZonedDateTime bucket) { this.bucket = bucket; }

    public String getBucketType() { return bucketType; }
    public void setBucketType(String bucketType) { this.bucketType = bucketType; }

    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }

    public Long getCreatedDateTechnical() { return createdDateTechnical; }
    public void setCreatedDateTechnical(Long createdDateTechnical) { this.createdDateTechnical = createdDateTechnical; }
}
