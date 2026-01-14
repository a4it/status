package org.automatize.status.controllers.api;

import java.util.UUID;

/**
 * Request object for specifying a component's position in a reorder operation.
 * <p>
 * This class is used when reordering status components within a status application.
 * Each request specifies a component ID and its new display position.
 * </p>
 *
 * @see StatusComponentController#reorderComponents
 */
public class ComponentOrderRequest {

    /**
     * The unique identifier of the component to reorder.
     */
    private UUID componentId;

    /**
     * The new position/order for the component (0-based index).
     */
    private Integer position;

    /**
     * Default constructor for JSON deserialization.
     */
    public ComponentOrderRequest() {
    }

    /**
     * Gets the component ID.
     *
     * @return the UUID of the component
     */
    public UUID getComponentId() {
        return componentId;
    }

    /**
     * Sets the component ID.
     *
     * @param componentId the UUID of the component to reorder
     */
    public void setComponentId(UUID componentId) {
        this.componentId = componentId;
    }

    /**
     * Gets the target position.
     *
     * @return the new position for the component
     */
    public Integer getPosition() {
        return position;
    }

    /**
     * Sets the target position.
     *
     * @param position the new position for the component
     */
    public void setPosition(Integer position) {
        this.position = position;
    }
}