package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * <p>
 * Response object representing an aggregated log metric count for a given time bucket.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Identify the tenant, service and log level the metric applies to</li>
 *   <li>Describe the time bucket and its granularity type</li>
 *   <li>Carry the aggregated count of log entries for that bucket</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
public class LogMetricResponse {

    /** The unique identifier of the metric record. */
    private UUID id;

    /** The ID of the tenant this metric belongs to. */
    private UUID tenantId;

    /** The service name the metric applies to. */
    private String service;

    /** The log level the metric applies to. */
    private String level;

    /** The start timestamp of the aggregation bucket. */
    private ZonedDateTime bucket;

    /** The granularity type of the bucket (e.g. hour, day). */
    private String bucketType;

    /** The aggregated count of log entries in the bucket. */
    private Long count;

    /**
     * Default constructor.
     */
    public LogMetricResponse() {
    }

    /** Gets the ID. @return the ID */
    public UUID getId() { return id; }
    /** Sets the ID. @param id the ID to set */
    public void setId(UUID id) { this.id = id; }

    /** Gets the tenant ID. @return the tenant ID */
    public UUID getTenantId() { return tenantId; }
    /** Sets the tenant ID. @param tenantId the tenant ID to set */
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    /** Gets the service name. @return the service name */
    public String getService() { return service; }
    /** Sets the service name. @param service the service name to set */
    public void setService(String service) { this.service = service; }

    /** Gets the log level. @return the log level */
    public String getLevel() { return level; }
    /** Sets the log level. @param level the log level to set */
    public void setLevel(String level) { this.level = level; }

    /** Gets the bucket start timestamp. @return the bucket start timestamp */
    public ZonedDateTime getBucket() { return bucket; }
    /** Sets the bucket start timestamp. @param bucket the bucket start timestamp to set */
    public void setBucket(ZonedDateTime bucket) { this.bucket = bucket; }

    /** Gets the bucket granularity type. @return the bucket type */
    public String getBucketType() { return bucketType; }
    /** Sets the bucket granularity type. @param bucketType the bucket type to set */
    public void setBucketType(String bucketType) { this.bucketType = bucketType; }

    /** Gets the aggregated count. @return the count */
    public Long getCount() { return count; }
    /** Sets the aggregated count. @param count the count to set */
    public void setCount(Long count) { this.count = count; }
}
