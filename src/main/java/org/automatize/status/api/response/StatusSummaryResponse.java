package org.automatize.status.api.response;

import java.util.List;

/**
 * <p>
 * Response object containing an overall status summary.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide aggregate statistics across all monitored applications</li>
 *   <li>Track operational vs issue counts for applications</li>
 *   <li>Include active incidents and upcoming maintenance counts</li>
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
public class StatusSummaryResponse {

    /** The overall system status across all apps. */
    private String overallStatus;

    /** Total number of monitored applications. */
    private int totalApps;

    /** Number of applications that are fully operational. */
    private int operationalApps;

    /** Number of applications with current issues. */
    private int appsWithIssues;

    /** Number of currently active incidents. */
    private int activeIncidents;

    /** Number of scheduled upcoming maintenances. */
    private int upcomingMaintenances;

    /** List of all status applications. */
    private List<StatusAppResponse> apps;

    /**
     * Default constructor.
     */
    public StatusSummaryResponse() {
    }

    /**
     * Gets the overall status.
     *
     * @return the overall status
     */
    public String getOverallStatus() {
        return overallStatus;
    }

    /**
     * Sets the overall status.
     *
     * @param overallStatus the overall status to set
     */
    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    /**
     * Gets the total apps count.
     *
     * @return the total apps count
     */
    public int getTotalApps() {
        return totalApps;
    }

    /**
     * Sets the total apps count.
     *
     * @param totalApps the total apps count to set
     */
    public void setTotalApps(int totalApps) {
        this.totalApps = totalApps;
    }

    /**
     * Gets the operational apps count.
     *
     * @return the operational apps count
     */
    public int getOperationalApps() {
        return operationalApps;
    }

    /**
     * Sets the operational apps count.
     *
     * @param operationalApps the count to set
     */
    public void setOperationalApps(int operationalApps) {
        this.operationalApps = operationalApps;
    }

    /**
     * Gets the apps with issues count.
     *
     * @return the apps with issues count
     */
    public int getAppsWithIssues() {
        return appsWithIssues;
    }

    /**
     * Sets the apps with issues count.
     *
     * @param appsWithIssues the count to set
     */
    public void setAppsWithIssues(int appsWithIssues) {
        this.appsWithIssues = appsWithIssues;
    }

    /**
     * Gets the active incidents count.
     *
     * @return the active incidents count
     */
    public int getActiveIncidents() {
        return activeIncidents;
    }

    /**
     * Sets the active incidents count.
     *
     * @param activeIncidents the count to set
     */
    public void setActiveIncidents(int activeIncidents) {
        this.activeIncidents = activeIncidents;
    }

    /**
     * Gets the upcoming maintenances count.
     *
     * @return the upcoming maintenances count
     */
    public int getUpcomingMaintenances() {
        return upcomingMaintenances;
    }

    /**
     * Sets the upcoming maintenances count.
     *
     * @param upcomingMaintenances the count to set
     */
    public void setUpcomingMaintenances(int upcomingMaintenances) {
        this.upcomingMaintenances = upcomingMaintenances;
    }

    /**
     * Gets the list of apps.
     *
     * @return the list of apps
     */
    public List<StatusAppResponse> getApps() {
        return apps;
    }

    /**
     * Sets the list of apps.
     *
     * @param apps the list of apps to set
     */
    public void setApps(List<StatusAppResponse> apps) {
        this.apps = apps;
    }
}