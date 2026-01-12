package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

public class StatusComponentResponse {
    private UUID id;
    private UUID appId;
    private String name;
    private String description;
    private String status;
    private String groupName;
    private Integer position;

    // Health check configuration
    private Boolean checkInheritFromApp;
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

    public StatusComponentResponse() {
    }

    public UUID getAppId() {
        return appId;
    }

    public void setAppId(UUID appId) {
        this.appId = appId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
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