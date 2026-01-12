package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class StatusComponentRequest {
    
    @NotNull(message = "App ID is required")
    private UUID appId;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    private String status = "OPERATIONAL";
    
    private Integer position = 0;
    
    private String groupName;

    // Health check configuration
    private Boolean checkInheritFromApp = true;
    private Boolean checkEnabled = false;
    private String checkType = "NONE";
    private String checkUrl;
    private Integer checkIntervalSeconds = 60;
    private Integer checkTimeoutSeconds = 10;
    private Integer checkExpectedStatus = 200;
    private Integer checkFailureThreshold = 3;

    public StatusComponentRequest() {
    }

    public UUID getAppId() {
        return appId;
    }

    public void setAppId(UUID appId) {
        this.appId = appId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Boolean getCheckInheritFromApp() {
        return checkInheritFromApp;
    }

    public void setCheckInheritFromApp(Boolean checkInheritFromApp) {
        this.checkInheritFromApp = checkInheritFromApp;
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
}