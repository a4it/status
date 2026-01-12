package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class StatusMaintenanceResponse {
    private UUID id;
    private String title;
    private String description;
    private String status;
    private ZonedDateTime startsAt;
    private ZonedDateTime endsAt;
    private List<StatusComponentResponse> affectedComponents;

    public StatusMaintenanceResponse() {
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

    public List<StatusComponentResponse> getAffectedComponents() {
        return affectedComponents;
    }

    public void setAffectedComponents(List<StatusComponentResponse> affectedComponents) {
        this.affectedComponents = affectedComponents;
    }
}