package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class StatusMaintenanceRequest {
    
    @NotNull(message = "App ID is required")
    private UUID appId;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @NotBlank(message = "Status is required")
    private String status;
    
    @NotNull(message = "Start time is required")
    private ZonedDateTime startsAt;
    
    @NotNull(message = "End time is required")
    private ZonedDateTime endsAt;
    
    private Boolean isPublic = true;
    
    private List<UUID> affectedComponentIds;

    public StatusMaintenanceRequest() {
    }

    public UUID getAppId() {
        return appId;
    }

    public void setAppId(UUID appId) {
        this.appId = appId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public ZonedDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(ZonedDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public ZonedDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(ZonedDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public List<UUID> getAffectedComponentIds() {
        return affectedComponentIds;
    }

    public void setAffectedComponentIds(List<UUID> affectedComponentIds) {
        this.affectedComponentIds = affectedComponentIds;
    }
}