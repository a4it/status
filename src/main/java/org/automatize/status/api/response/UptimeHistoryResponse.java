package org.automatize.status.api.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * Response object containing uptime history for an application or component.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide overall uptime statistics for a date range</li>
 *   <li>Include daily breakdown of uptime records</li>
 *   <li>Track total incidents during the reporting period</li>
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
public class UptimeHistoryResponse {

    /** The unique identifier of the entity. */
    private UUID id;

    /** The name of the entity. */
    private String name;

    /** The type of entity (APP or COMPONENT). */
    private String type;

    /** The overall uptime percentage for the period. */
    private BigDecimal overallUptimePercentage;

    /** The number of days in the reporting range. */
    private int daysInRange;

    /** The total number of incidents during the period. */
    private int totalIncidents;

    /** Daily uptime records for the period. */
    private List<DailyUptimeResponse> dailyHistory;

    /**
     * Default constructor.
     */
    public UptimeHistoryResponse() {
    }

    /**
     * Gets the ID.
     *
     * @return the ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the ID.
     *
     * @param id the ID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the overall uptime percentage.
     *
     * @return the overall uptime percentage
     */
    public BigDecimal getOverallUptimePercentage() {
        return overallUptimePercentage;
    }

    /**
     * Sets the overall uptime percentage.
     *
     * @param overallUptimePercentage the percentage to set
     */
    public void setOverallUptimePercentage(BigDecimal overallUptimePercentage) {
        this.overallUptimePercentage = overallUptimePercentage;
    }

    /**
     * Gets the number of days in range.
     *
     * @return the days in range
     */
    public int getDaysInRange() {
        return daysInRange;
    }

    /**
     * Sets the number of days in range.
     *
     * @param daysInRange the days in range to set
     */
    public void setDaysInRange(int daysInRange) {
        this.daysInRange = daysInRange;
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
     * Gets the daily history list.
     *
     * @return the daily history list
     */
    public List<DailyUptimeResponse> getDailyHistory() {
        return dailyHistory;
    }

    /**
     * Sets the daily history list.
     *
     * @param dailyHistory the daily history list to set
     */
    public void setDailyHistory(List<DailyUptimeResponse> dailyHistory) {
        this.dailyHistory = dailyHistory;
    }
}
