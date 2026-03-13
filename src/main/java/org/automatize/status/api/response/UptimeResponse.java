package org.automatize.status.api.response;

import java.util.UUID;

/**
 * <p>
 * Response object containing uptime statistics for an application.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide aggregate uptime percentage for an application</li>
 *   <li>Track incident counts and outage duration</li>
 *   <li>Include maintenance time and calculation period</li>
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
public class UptimeResponse {

    /** The unique identifier of the application. */
    private UUID appId;

    /** The name of the application. */
    private String appName;

    /** The uptime percentage for the period. */
    private double uptimePercentage;

    /** The total number of incidents during the period. */
    private int totalIncidents;

    /** The total minutes of outage during the period. */
    private int totalOutageMinutes;

    /** The total minutes of scheduled maintenance during the period. */
    private int totalMaintenanceMinutes;

    /** The number of days included in the calculation. */
    private int daysCalculated;

    /**
     * Default constructor.
     */
    public UptimeResponse() {
    }

    /**
     * Gets the app ID.
     *
     * @return the app ID
     */
    public UUID getAppId() {
        return appId;
    }

    /**
     * Sets the app ID.
     *
     * @param appId the app ID to set
     */
    public void setAppId(UUID appId) {
        this.appId = appId;
    }

    /**
     * Gets the app name.
     *
     * @return the app name
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Sets the app name.
     *
     * @param appName the app name to set
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

    /**
     * Gets the uptime percentage.
     *
     * @return the uptime percentage
     */
    public double getUptimePercentage() {
        return uptimePercentage;
    }

    /**
     * Sets the uptime percentage.
     *
     * @param uptimePercentage the uptime percentage to set
     */
    public void setUptimePercentage(double uptimePercentage) {
        this.uptimePercentage = uptimePercentage;
    }

    /**
     * Gets the total incidents count.
     *
     * @return the total incidents count
     */
    public int getTotalIncidents() {
        return totalIncidents;
    }

    /**
     * Sets the total incidents count.
     *
     * @param totalIncidents the count to set
     */
    public void setTotalIncidents(int totalIncidents) {
        this.totalIncidents = totalIncidents;
    }

    /**
     * Gets the total outage minutes.
     *
     * @return the total outage minutes
     */
    public int getTotalOutageMinutes() {
        return totalOutageMinutes;
    }

    /**
     * Sets the total outage minutes.
     *
     * @param totalOutageMinutes the minutes to set
     */
    public void setTotalOutageMinutes(int totalOutageMinutes) {
        this.totalOutageMinutes = totalOutageMinutes;
    }

    /**
     * Gets the total maintenance minutes.
     *
     * @return the total maintenance minutes
     */
    public int getTotalMaintenanceMinutes() {
        return totalMaintenanceMinutes;
    }

    /**
     * Sets the total maintenance minutes.
     *
     * @param totalMaintenanceMinutes the minutes to set
     */
    public void setTotalMaintenanceMinutes(int totalMaintenanceMinutes) {
        this.totalMaintenanceMinutes = totalMaintenanceMinutes;
    }

    /**
     * Gets the days calculated.
     *
     * @return the days calculated
     */
    public int getDaysCalculated() {
        return daysCalculated;
    }

    /**
     * Sets the days calculated.
     *
     * @param daysCalculated the days to set
     */
    public void setDaysCalculated(int daysCalculated) {
        this.daysCalculated = daysCalculated;
    }
}