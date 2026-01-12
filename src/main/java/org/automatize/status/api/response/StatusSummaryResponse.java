package org.automatize.status.api.response;

import java.util.List;

public class StatusSummaryResponse {
    private String overallStatus;
    private int totalApps;
    private int operationalApps;
    private int appsWithIssues;
    private int activeIncidents;
    private int upcomingMaintenances;
    private List<StatusAppResponse> apps;

    public StatusSummaryResponse() {
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public int getTotalApps() {
        return totalApps;
    }

    public void setTotalApps(int totalApps) {
        this.totalApps = totalApps;
    }

    public int getOperationalApps() {
        return operationalApps;
    }

    public void setOperationalApps(int operationalApps) {
        this.operationalApps = operationalApps;
    }

    public int getAppsWithIssues() {
        return appsWithIssues;
    }

    public void setAppsWithIssues(int appsWithIssues) {
        this.appsWithIssues = appsWithIssues;
    }

    public int getActiveIncidents() {
        return activeIncidents;
    }

    public void setActiveIncidents(int activeIncidents) {
        this.activeIncidents = activeIncidents;
    }

    public int getUpcomingMaintenances() {
        return upcomingMaintenances;
    }

    public void setUpcomingMaintenances(int upcomingMaintenances) {
        this.upcomingMaintenances = upcomingMaintenances;
    }

    public List<StatusAppResponse> getApps() {
        return apps;
    }

    public void setApps(List<StatusAppResponse> apps) {
        this.apps = apps;
    }
}