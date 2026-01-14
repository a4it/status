package org.automatize.status.api.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response object containing historical status data for a component.
 * <p>
 * Provides daily status history, uptime statistics, and incident counts
 * for a specific component over a given time period.
 * </p>
 */
public class ComponentHistoryResponse {

    /** The unique identifier of the component. */
    private UUID componentId;

    /** The name of the component. */
    private String componentName;

    /** List of daily status records for the component. */
    private List<DailyStatus> history;

    /** The overall uptime percentage for the period. */
    private double uptimePercentage;

    /** The total number of incidents during the period. */
    private int totalIncidents;

    /**
     * Represents the status of a component for a single day.
     */
    public static class DailyStatus {

        /** The date of this status record. */
        private LocalDate date;

        /** The status on this date. */
        private String status;

        /** The number of incidents on this date. */
        private int incidents;

        /** The total minutes of maintenance on this date. */
        private int maintenanceMinutes;

        /**
         * Default constructor.
         */
        public DailyStatus() {
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
         * Gets the incident count.
         *
         * @return the incident count
         */
        public int getIncidents() {
            return incidents;
        }

        /**
         * Sets the incident count.
         *
         * @param incidents the incident count to set
         */
        public void setIncidents(int incidents) {
            this.incidents = incidents;
        }

        /**
         * Gets the maintenance minutes.
         *
         * @return the maintenance minutes
         */
        public int getMaintenanceMinutes() {
            return maintenanceMinutes;
        }

        /**
         * Sets the maintenance minutes.
         *
         * @param maintenanceMinutes the maintenance minutes to set
         */
        public void setMaintenanceMinutes(int maintenanceMinutes) {
            this.maintenanceMinutes = maintenanceMinutes;
        }
    }

    /**
     * Default constructor.
     */
    public ComponentHistoryResponse() {
    }

    /**
     * Gets the component ID.
     *
     * @return the component ID
     */
    public UUID getComponentId() {
        return componentId;
    }

    /**
     * Sets the component ID.
     *
     * @param componentId the component ID to set
     */
    public void setComponentId(UUID componentId) {
        this.componentId = componentId;
    }

    /**
     * Gets the component name.
     *
     * @return the component name
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * Sets the component name.
     *
     * @param componentName the component name to set
     */
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    /**
     * Gets the history list.
     *
     * @return the history list
     */
    public List<DailyStatus> getHistory() {
        return history;
    }

    /**
     * Sets the history list.
     *
     * @param history the history list to set
     */
    public void setHistory(List<DailyStatus> history) {
        this.history = history;
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
     * @param totalIncidents the total incidents count to set
     */
    public void setTotalIncidents(int totalIncidents) {
        this.totalIncidents = totalIncidents;
    }
}