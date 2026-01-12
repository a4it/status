package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class StatusAppResponse {
    private UUID id;
    private String name;
    private String description;
    private String slug;
    private String status;
    private Boolean isPublic;
    private List<StatusComponentResponse> components;
    private StatusIncidentResponse currentIncident;
    private List<StatusMaintenanceResponse> upcomingMaintenances;
    private ZonedDateTime lastUpdated;

    // Health check configuration
    private Boolean checkEnabled;
    private String checkType;
    private String checkUrl;
    private Integer checkIntervalSeconds;
    private Integer checkTimeoutSeconds;
    private Integer checkExpectedStatus;
    private Integer checkFailureThreshold;
    private ZonedDateTime lastCheckAt;
    private Boolean lastCheckSuccess;
    private String lastCheckMessage;
    private Integer consecutiveFailures;

    public StatusAppResponse() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public List<StatusComponentResponse> getComponents() {
        return components;
    }

    public void setComponents(List<StatusComponentResponse> components) {
        this.components = components;
    }

    public StatusIncidentResponse getCurrentIncident() {
        return currentIncident;
    }

    public void setCurrentIncident(StatusIncidentResponse currentIncident) {
        this.currentIncident = currentIncident;
    }

    public List<StatusMaintenanceResponse> getUpcomingMaintenances() {
        return upcomingMaintenances;
    }

    public void setUpcomingMaintenances(List<StatusMaintenanceResponse> upcomingMaintenances) {
        this.upcomingMaintenances = upcomingMaintenances;
    }

    public ZonedDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(ZonedDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Boolean getCheckEnabled() {
        return checkEnabled;
    }

    public void setCheckEnabled(Boolean checkEnabled) {
        this.checkEnabled = checkEnabled;
    }

    public String getCheckType() {
        return checkType;
    }

    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    public String getCheckUrl() {
        return checkUrl;
    }

    public void setCheckUrl(String checkUrl) {
        this.checkUrl = checkUrl;
    }

    public Integer getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public void setCheckIntervalSeconds(Integer checkIntervalSeconds) {
        this.checkIntervalSeconds = checkIntervalSeconds;
    }

    public Integer getCheckTimeoutSeconds() {
        return checkTimeoutSeconds;
    }

    public void setCheckTimeoutSeconds(Integer checkTimeoutSeconds) {
        this.checkTimeoutSeconds = checkTimeoutSeconds;
    }

    public Integer getCheckExpectedStatus() {
        return checkExpectedStatus;
    }

    public void setCheckExpectedStatus(Integer checkExpectedStatus) {
        this.checkExpectedStatus = checkExpectedStatus;
    }

    public Integer getCheckFailureThreshold() {
        return checkFailureThreshold;
    }

    public void setCheckFailureThreshold(Integer checkFailureThreshold) {
        this.checkFailureThreshold = checkFailureThreshold;
    }

    public ZonedDateTime getLastCheckAt() {
        return lastCheckAt;
    }

    public void setLastCheckAt(ZonedDateTime lastCheckAt) {
        this.lastCheckAt = lastCheckAt;
    }

    public Boolean getLastCheckSuccess() {
        return lastCheckSuccess;
    }

    public void setLastCheckSuccess(Boolean lastCheckSuccess) {
        this.lastCheckSuccess = lastCheckSuccess;
    }

    public String getLastCheckMessage() {
        return lastCheckMessage;
    }

    public void setLastCheckMessage(String lastCheckMessage) {
        this.lastCheckMessage = lastCheckMessage;
    }

    public Integer getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public void setConsecutiveFailures(Integer consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }
}