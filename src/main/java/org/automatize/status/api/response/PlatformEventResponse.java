package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * <p>
 * Response object containing platform event details.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide platform event log entry information</li>
 *   <li>Track event severity, source, and timing</li>
 *   <li>Associate events with applications and components</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
public class PlatformEventResponse {

    private UUID id;

    private UUID appId;

    private String appName;

    private UUID componentId;

    private String componentName;

    private String severity;

    private String source;

    private String message;

    private String details;

    private ZonedDateTime eventTime;

    private ZonedDateTime createdDate;

    /**
     * Default constructor.
     */
    public PlatformEventResponse() {
    }

    /**
     * Gets the event id.
     *
     * @return the event id
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the event id.
     *
     * @param id the event id to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the associated application id.
     *
     * @return the application id
     */
    public UUID getAppId() {
        return appId;
    }

    /**
     * Sets the associated application id.
     *
     * @param appId the application id to set
     */
    public void setAppId(UUID appId) {
        this.appId = appId;
    }

    /**
     * Gets the associated application name.
     *
     * @return the application name
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Sets the associated application name.
     *
     * @param appName the application name to set
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

    /**
     * Gets the associated component id.
     *
     * @return the component id
     */
    public UUID getComponentId() {
        return componentId;
    }

    /**
     * Sets the associated component id.
     *
     * @param componentId the component id to set
     */
    public void setComponentId(UUID componentId) {
        this.componentId = componentId;
    }

    /**
     * Gets the associated component name.
     *
     * @return the component name
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * Sets the associated component name.
     *
     * @param componentName the component name to set
     */
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    /**
     * Gets the event severity.
     *
     * @return the severity
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * Sets the event severity.
     *
     * @param severity the severity to set
     */
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    /**
     * Gets the event source.
     *
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the event source.
     *
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Gets the event message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the event message.
     *
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the event details.
     *
     * @return the details
     */
    public String getDetails() {
        return details;
    }

    /**
     * Sets the event details.
     *
     * @param details the details to set
     */
    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * Gets the time the event occurred.
     *
     * @return the event time
     */
    public ZonedDateTime getEventTime() {
        return eventTime;
    }

    /**
     * Sets the time the event occurred.
     *
     * @param eventTime the event time to set
     */
    public void setEventTime(ZonedDateTime eventTime) {
        this.eventTime = eventTime;
    }

    /**
     * Gets the date the event record was created.
     *
     * @return the created date
     */
    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the date the event record was created.
     *
     * @param createdDate the created date to set
     */
    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
