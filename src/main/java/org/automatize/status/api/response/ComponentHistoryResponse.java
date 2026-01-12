package org.automatize.status.api.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ComponentHistoryResponse {
    private UUID componentId;
    private String componentName;
    private List<DailyStatus> history;
    private double uptimePercentage;
    private int totalIncidents;

    public static class DailyStatus {
        private LocalDate date;
        private String status;
        private int incidents;
        private int maintenanceMinutes;

        public DailyStatus() {
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

        public int getIncidents() {
            return incidents;
        }

        public void setIncidents(int incidents) {
            this.incidents = incidents;
        }

        public int getMaintenanceMinutes() {
            return maintenanceMinutes;
        }

        public void setMaintenanceMinutes(int maintenanceMinutes) {
            this.maintenanceMinutes = maintenanceMinutes;
        }
    }

    public ComponentHistoryResponse() {
    }

    public UUID getComponentId() {
        return componentId;
    }

    public void setComponentId(UUID componentId) {
        this.componentId = componentId;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public List<DailyStatus> getHistory() {
        return history;
    }

    public void setHistory(List<DailyStatus> history) {
        this.history = history;
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
}