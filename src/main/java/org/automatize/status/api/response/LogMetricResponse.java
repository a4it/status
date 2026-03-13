package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

public class LogMetricResponse {

    private UUID id;
    private UUID tenantId;
    private String service;
    private String level;
    private ZonedDateTime bucket;
    private String bucketType;
    private Long count;

    public LogMetricResponse() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

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
}
