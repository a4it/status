package org.automatize.status.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing daily uptime statistics for applications and components.
 * <p>
 * StatusUptimeHistory tracks historical uptime data on a per-day basis for both
 * applications and their components. This enables display of uptime percentages,
 * historical graphs, and SLA reporting.
 * </p>
 * <p>
 * Each record captures detailed metrics including operational time, degraded time,
 * outage time, and maintenance time, as well as counts of incidents and maintenance
 * windows for the recorded date.
 * </p>
 *
 * @see StatusApp
 * @see StatusComponent
 */
@Entity
@Table(name = "status_uptime_history")
public class StatusUptimeHistory {

    /**
     * Unique identifier for the uptime history record.
     * Generated automatically using UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * The application this uptime record applies to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    private StatusApp app;

    /**
     * The specific component this uptime record applies to.
     * Null if this record represents application-level or platform-level uptime.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id")
    private StatusComponent component;

    /**
     * The platform this uptime record applies to.
     * Null if this record represents application-level or component-level uptime.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id")
    private StatusPlatform platform;

    /**
     * The date this uptime record represents.
     */
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    /**
     * Overall status for the day (worst status encountered).
     * Defaults to "OPERATIONAL". Values include OPERATIONAL, DEGRADED, PARTIAL_OUTAGE, MAJOR_OUTAGE.
     */
    @Column(name = "status", nullable = false, length = 50)
    private String status = "OPERATIONAL";

    /**
     * Calculated uptime percentage for the day.
     * Stored with 3 decimal precision (e.g., 99.950 for 99.95%).
     * Defaults to 100.000%.
     */
    @Column(name = "uptime_percentage", nullable = false, precision = 6, scale = 3)
    private BigDecimal uptimePercentage = new BigDecimal("100.000");

    /**
     * Total minutes in the day being tracked.
     * Typically 1440 (24 hours x 60 minutes).
     */
    @Column(name = "total_minutes", nullable = false)
    private Integer totalMinutes = 1440;

    /**
     * Minutes the service was fully operational.
     * Defaults to 1440 (full day).
     */
    @Column(name = "operational_minutes", nullable = false)
    private Integer operationalMinutes = 1440;

    /**
     * Minutes the service was in a degraded state.
     * Defaults to 0.
     */
    @Column(name = "degraded_minutes", nullable = false)
    private Integer degradedMinutes = 0;

    /**
     * Minutes the service was in an outage state.
     * Defaults to 0.
     */
    @Column(name = "outage_minutes", nullable = false)
    private Integer outageMinutes = 0;

    /**
     * Minutes the service was under scheduled maintenance.
     * Defaults to 0.
     */
    @Column(name = "maintenance_minutes", nullable = false)
    private Integer maintenanceMinutes = 0;

    /**
     * Number of incidents that occurred on this date.
     * Defaults to 0.
     */
    @Column(name = "incident_count", nullable = false)
    private Integer incidentCount = 0;

    /**
     * Number of maintenance windows that occurred on this date.
     * Defaults to 0.
     */
    @Column(name = "maintenance_count", nullable = false)
    private Integer maintenanceCount = 0;

    /**
     * Timestamp indicating when the uptime record was created.
     */
    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;

    /**
     * Timestamp indicating when the uptime record was last modified.
     */
    @Column(name = "last_modified_date", nullable = false)
    private ZonedDateTime lastModifiedDate;

    /**
     * JPA lifecycle callback executed before persisting a new uptime record.
     * Automatically sets creation and modification timestamps if not already set.
     */
    @PrePersist
    public void prePersist() {
        ZonedDateTime now = ZonedDateTime.now();
        if (createdDate == null) {
            createdDate = now;
        }
        if (lastModifiedDate == null) {
            lastModifiedDate = now;
        }
    }

    /**
     * JPA lifecycle callback executed before updating an existing uptime record.
     * Automatically updates the modification timestamp.
     */
    @PreUpdate
    public void preUpdate() {
        lastModifiedDate = ZonedDateTime.now();
    }

    /**
     * Default constructor required by JPA.
     */
    public StatusUptimeHistory() {
    }

    /**
     * Gets the unique identifier of the uptime record.
     *
     * @return the UUID of the uptime record
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the uptime record.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the application this uptime record applies to.
     *
     * @return the associated StatusApp
     */
    public StatusApp getApp() {
        return app;
    }

    /**
     * Sets the application this uptime record applies to.
     *
     * @param app the StatusApp to set
     */
    public void setApp(StatusApp app) {
        this.app = app;
    }

    /**
     * Gets the component this uptime record applies to.
     *
     * @return the associated StatusComponent, or null for app-level records
     */
    public StatusComponent getComponent() {
        return component;
    }

    /**
     * Sets the component this uptime record applies to.
     *
     * @param component the StatusComponent to set, or null for app-level records
     */
    public void setComponent(StatusComponent component) {
        this.component = component;
    }

    /**
     * Gets the date this uptime record represents.
     *
     * @return the record date
     */
    public LocalDate getRecordDate() {
        return recordDate;
    }

    /**
     * Sets the date this uptime record represents.
     *
     * @param recordDate the date to set
     */
    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    /**
     * Gets the overall status for the day.
     *
     * @return the status (e.g., OPERATIONAL, DEGRADED, MAJOR_OUTAGE)
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the overall status for the day.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the uptime percentage for the day.
     *
     * @return the uptime percentage (0-100 with 3 decimal precision)
     */
    public BigDecimal getUptimePercentage() {
        return uptimePercentage;
    }

    /**
     * Sets the uptime percentage for the day.
     *
     * @param uptimePercentage the uptime percentage to set
     */
    public void setUptimePercentage(BigDecimal uptimePercentage) {
        this.uptimePercentage = uptimePercentage;
    }

    /**
     * Gets the total minutes tracked for the day.
     *
     * @return the total minutes (typically 1440)
     */
    public Integer getTotalMinutes() {
        return totalMinutes;
    }

    /**
     * Sets the total minutes tracked for the day.
     *
     * @param totalMinutes the total minutes to set
     */
    public void setTotalMinutes(Integer totalMinutes) {
        this.totalMinutes = totalMinutes;
    }

    /**
     * Gets the minutes the service was operational.
     *
     * @return the operational minutes
     */
    public Integer getOperationalMinutes() {
        return operationalMinutes;
    }

    /**
     * Sets the minutes the service was operational.
     *
     * @param operationalMinutes the operational minutes to set
     */
    public void setOperationalMinutes(Integer operationalMinutes) {
        this.operationalMinutes = operationalMinutes;
    }

    /**
     * Gets the minutes the service was in a degraded state.
     *
     * @return the degraded minutes
     */
    public Integer getDegradedMinutes() {
        return degradedMinutes;
    }

    /**
     * Sets the minutes the service was in a degraded state.
     *
     * @param degradedMinutes the degraded minutes to set
     */
    public void setDegradedMinutes(Integer degradedMinutes) {
        this.degradedMinutes = degradedMinutes;
    }

    /**
     * Gets the minutes the service was in an outage state.
     *
     * @return the outage minutes
     */
    public Integer getOutageMinutes() {
        return outageMinutes;
    }

    /**
     * Sets the minutes the service was in an outage state.
     *
     * @param outageMinutes the outage minutes to set
     */
    public void setOutageMinutes(Integer outageMinutes) {
        this.outageMinutes = outageMinutes;
    }

    /**
     * Gets the minutes the service was under scheduled maintenance.
     *
     * @return the maintenance minutes
     */
    public Integer getMaintenanceMinutes() {
        return maintenanceMinutes;
    }

    /**
     * Sets the minutes the service was under scheduled maintenance.
     *
     * @param maintenanceMinutes the maintenance minutes to set
     */
    public void setMaintenanceMinutes(Integer maintenanceMinutes) {
        this.maintenanceMinutes = maintenanceMinutes;
    }

    /**
     * Gets the number of incidents on this date.
     *
     * @return the incident count
     */
    public Integer getIncidentCount() {
        return incidentCount;
    }

    /**
     * Sets the number of incidents on this date.
     *
     * @param incidentCount the incident count to set
     */
    public void setIncidentCount(Integer incidentCount) {
        this.incidentCount = incidentCount;
    }

    /**
     * Gets the number of maintenance windows on this date.
     *
     * @return the maintenance count
     */
    public Integer getMaintenanceCount() {
        return maintenanceCount;
    }

    /**
     * Sets the number of maintenance windows on this date.
     *
     * @param maintenanceCount the maintenance count to set
     */
    public void setMaintenanceCount(Integer maintenanceCount) {
        this.maintenanceCount = maintenanceCount;
    }

    /**
     * Gets the creation timestamp of the uptime record.
     *
     * @return the creation date and time
     */
    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the creation timestamp of the uptime record.
     *
     * @param createdDate the creation date and time to set
     */
    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Gets the last modification timestamp of the uptime record.
     *
     * @return the last modification date and time
     */
    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Sets the last modification timestamp of the uptime record.
     *
     * @param lastModifiedDate the last modification date and time to set
     */
    public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
