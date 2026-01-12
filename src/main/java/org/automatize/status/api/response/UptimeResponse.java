package org.automatize.status.api.response;

import java.util.UUID;

public class UptimeResponse {
    private UUID appId;
    private String appName;
    private double uptimePercentage;
    private int totalIncidents;
    private int totalOutageMinutes;
    private int totalMaintenanceMinutes;
    private int daysCalculated;

    public UptimeResponse() {
    }

    public UUID getAppId() {
        return appId;
    }

    public void setAppId(UUID appId) {
        this.appId = appId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public double getUptimePercentage() {
        return uptimePercentage;
    }

    public void setUptimePercentage(double uptimePercentage) {
        this.uptimePercentage = uptimePercentage;
    }

    public int getTotalIncidents() {
        return totalIncidents;
    }

    public void setTotalIncidents(int totalIncidents) {
        this.totalIncidents = totalIncidents;
    }

    public int getTotalOutageMinutes() {
        return totalOutageMinutes;
    }

    public void setTotalOutageMinutes(int totalOutageMinutes) {
        this.totalOutageMinutes = totalOutageMinutes;
    }

    public int getTotalMaintenanceMinutes() {
        return totalMaintenanceMinutes;
    }

    public void setTotalMaintenanceMinutes(int totalMaintenanceMinutes) {
        this.totalMaintenanceMinutes = totalMaintenanceMinutes;
    }

    public int getDaysCalculated() {
        return daysCalculated;
    }

    public void setDaysCalculated(int daysCalculated) {
        this.daysCalculated = daysCalculated;
    }
}