package org.automatize.status.api.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * Response object containing uptime statistics for a single day.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide uptime percentage for a specific date</li>
 *   <li>Track incident and maintenance counts for the day</li>
 *   <li>Reference incident IDs occurring on the date</li>
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
public class DailyUptimeResponse {

    /** The date for this uptime record. */
    private LocalDate date;

    /** The overall status for this date. */
    private String status;

    /** The uptime percentage for this date. */
    private BigDecimal uptimePercentage;

    /** The number of incidents on this date. */
    private int incidentCount;

    /** The number of maintenance windows on this date. */
    private int maintenanceCount;

    /** List of incident IDs that occurred on this date. */
    private List<UUID> incidentIds;

    /**
     * Default constructor.
     */
    public DailyUptimeResponse() {
    }

    /**
     * Gets the date.
     *
     * @return the date
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Sets the date.
     *
     * @param date the date to set
     */
    public void setDate(LocalDate date) {
        this.date = date;
    }

    /**
     * Gets the status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the uptime percentage.
     *
     * @return the uptime percentage
     */
    public BigDecimal getUptimePercentage() {
        return uptimePercentage;
    }

    /**
     * Sets the uptime percentage.
     *
     * @param uptimePercentage the uptime percentage to set
     */
    public void setUptimePercentage(BigDecimal uptimePercentage) {
        this.uptimePercentage = uptimePercentage;
    }

    /**
     * Gets the incident count.
     *
     * @return the incident count
     */
    public int getIncidentCount() {
        return incidentCount;
    }

    /**
     * Sets the incident count.
     *
     * @param incidentCount the incident count to set
     */
    public void setIncidentCount(int incidentCount) {
        this.incidentCount = incidentCount;
    }

    /**
     * Gets the maintenance count.
     *
     * @return the maintenance count
     */
    public int getMaintenanceCount() {
        return maintenanceCount;
    }

    /**
     * Sets the maintenance count.
     *
     * @param maintenanceCount the maintenance count to set
     */
    public void setMaintenanceCount(int maintenanceCount) {
        this.maintenanceCount = maintenanceCount;
    }

    /**
     * Gets the list of incident IDs.
     *
     * @return the list of incident IDs
     */
    public List<UUID> getIncidentIds() {
        return incidentIds;
    }

    /**
     * Sets the list of incident IDs.
     *
     * @param incidentIds the list of incident IDs to set
     */
    public void setIncidentIds(List<UUID> incidentIds) {
        this.incidentIds = incidentIds;
    }
}
