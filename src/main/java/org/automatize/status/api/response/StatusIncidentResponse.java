package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class StatusIncidentResponse {
    private UUID id;
    private UUID appId;
    private String title;
    private String description;
    private String status;
    private String severity;
    private String impact;
    private ZonedDateTime startedAt;
    private ZonedDateTime resolvedAt;
    private Boolean isPublic;
    private List<StatusIncidentUpdateResponse> updates;
    private List<StatusComponentResponse> affectedComponents;

    public StatusIncidentResponse() {
    }

    public UUID getAppId() {
        return appId;
    }

    public void setAppId(UUID appId) {
        this.appId = appId;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public ZonedDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(ZonedDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public ZonedDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(ZonedDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public List<StatusIncidentUpdateResponse> getUpdates() {
        return updates;
    }

    public void setUpdates(List<StatusIncidentUpdateResponse> updates) {
        this.updates = updates;
    }

    public List<StatusComponentResponse> getAffectedComponents() {
        return affectedComponents;
    }

    public void setAffectedComponents(List<StatusComponentResponse> affectedComponents) {
        this.affectedComponents = affectedComponents;
    }
}