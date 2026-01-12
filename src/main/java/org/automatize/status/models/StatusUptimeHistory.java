package org.automatize.status.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "status_uptime_history")
public class StatusUptimeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    private StatusApp app;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id")
    private StatusComponent component;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "OPERATIONAL";

    @Column(name = "uptime_percentage", nullable = false, precision = 6, scale = 3)
    private BigDecimal uptimePercentage = new BigDecimal("100.000");

    @Column(name = "total_minutes", nullable = false)
    private Integer totalMinutes = 1440;

    @Column(name = "operational_minutes", nullable = false)
    private Integer operationalMinutes = 1440;

    @Column(name = "degraded_minutes", nullable = false)
    private Integer degradedMinutes = 0;

    @Column(name = "outage_minutes", nullable = false)
    private Integer outageMinutes = 0;

    @Column(name = "maintenance_minutes", nullable = false)
    private Integer maintenanceMinutes = 0;

    @Column(name = "incident_count", nullable = false)
    private Integer incidentCount = 0;

    @Column(name = "maintenance_count", nullable = false)
    private Integer maintenanceCount = 0;

    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;

    @Column(name = "last_modified_date", nullable = false)
    private ZonedDateTime lastModifiedDate;

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

    @PreUpdate
    public void preUpdate() {
        lastModifiedDate = ZonedDateTime.now();
    }

    public StatusUptimeHistory() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StatusApp getApp() {
        return app;
    }

    public void setApp(StatusApp app) {
        this.app = app;
    }

    public StatusComponent getComponent() {
        return component;
    }

    public void setComponent(StatusComponent component) {
        this.component = component;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getUptimePercentage() {
        return uptimePercentage;
    }

    public void setUptimePercentage(BigDecimal uptimePercentage) {
        this.uptimePercentage = uptimePercentage;
    }

    public Integer getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(Integer totalMinutes) {
        this.totalMinutes = totalMinutes;
    }

    public Integer getOperationalMinutes() {
        return operationalMinutes;
    }

    public void setOperationalMinutes(Integer operationalMinutes) {
        this.operationalMinutes = operationalMinutes;
    }

    public Integer getDegradedMinutes() {
        return degradedMinutes;
    }

    public void setDegradedMinutes(Integer degradedMinutes) {
        this.degradedMinutes = degradedMinutes;
    }

    public Integer getOutageMinutes() {
        return outageMinutes;
    }

    public void setOutageMinutes(Integer outageMinutes) {
        this.outageMinutes = outageMinutes;
    }

    public Integer getMaintenanceMinutes() {
        return maintenanceMinutes;
    }

    public void setMaintenanceMinutes(Integer maintenanceMinutes) {
        this.maintenanceMinutes = maintenanceMinutes;
    }

    public Integer getIncidentCount() {
        return incidentCount;
    }

    public void setIncidentCount(Integer incidentCount) {
        this.incidentCount = incidentCount;
    }

    public Integer getMaintenanceCount() {
        return maintenanceCount;
    }

    public void setMaintenanceCount(Integer maintenanceCount) {
        this.maintenanceCount = maintenanceCount;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
