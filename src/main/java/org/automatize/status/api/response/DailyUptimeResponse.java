package org.automatize.status.api.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class DailyUptimeResponse {
    private LocalDate date;
    private String status;
    private BigDecimal uptimePercentage;
    private int incidentCount;
    private int maintenanceCount;
    private List<UUID> incidentIds;

    public DailyUptimeResponse() {
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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

    public int getIncidentCount() {
        return incidentCount;
    }

    public void setIncidentCount(int incidentCount) {
        this.incidentCount = incidentCount;
    }

    public int getMaintenanceCount() {
        return maintenanceCount;
    }

    public void setMaintenanceCount(int maintenanceCount) {
        this.maintenanceCount = maintenanceCount;
    }

    public List<UUID> getIncidentIds() {
        return incidentIds;
    }

    public void setIncidentIds(List<UUID> incidentIds) {
        this.incidentIds = incidentIds;
    }
}
