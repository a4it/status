package org.automatize.status.api.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class UptimeHistoryResponse {
    private UUID id;
    private String name;
    private String type;
    private BigDecimal overallUptimePercentage;
    private int daysInRange;
    private int totalIncidents;
    private List<DailyUptimeResponse> dailyHistory;

    public UptimeHistoryResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getOverallUptimePercentage() {
        return overallUptimePercentage;
    }

    public void setOverallUptimePercentage(BigDecimal overallUptimePercentage) {
        this.overallUptimePercentage = overallUptimePercentage;
    }

    public int getDaysInRange() {
        return daysInRange;
    }

    public void setDaysInRange(int daysInRange) {
        this.daysInRange = daysInRange;
    }

    public int getTotalIncidents() {
        return totalIncidents;
    }

    public void setTotalIncidents(int totalIncidents) {
        this.totalIncidents = totalIncidents;
    }

    public List<DailyUptimeResponse> getDailyHistory() {
        return dailyHistory;
    }

    public void setDailyHistory(List<DailyUptimeResponse> dailyHistory) {
        this.dailyHistory = dailyHistory;
    }
}
