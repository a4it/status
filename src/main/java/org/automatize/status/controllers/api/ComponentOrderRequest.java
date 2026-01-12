package org.automatize.status.controllers.api;

import java.util.UUID;

public class ComponentOrderRequest {
    private UUID componentId;
    private Integer position;

    public ComponentOrderRequest() {
    }

    public UUID getComponentId() {
        return componentId;
    }

    public void setComponentId(UUID componentId) {
        this.componentId = componentId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }
}