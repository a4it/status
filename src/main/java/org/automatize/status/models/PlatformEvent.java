package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * <p>
 * Entity representing an event logged by a platform or component.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Store log entries from monitored platforms and components</li>
 *   <li>Capture event severity, source, and detailed messages</li>
 *   <li>Enable searchable event history in the admin interface</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see StatusApp
 * @see StatusComponent
 */
@Entity
@Table(name = "platform_events")
public class PlatformEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    private StatusApp app;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id")
    private StatusComponent component;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "source", length = 255)
    private String source;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "event_time", nullable = false)
    private ZonedDateTime eventTime;

    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;

    @Column(name = "created_date_technical", nullable = false)
    private Long createdDateTechnical;

    /**
     * JPA lifecycle callback executed before persisting a new platform event.
     * Populates the creation and event timestamps when they have not been set.
     */
    @PrePersist
    public void prePersist() {
        ZonedDateTime now = ZonedDateTime.now();
        // Set the creation timestamp only if it has not already been assigned.
        if (createdDate == null) {
            createdDate = now;
        }
        // Set the technical creation timestamp only if it has not already been assigned.
        if (createdDateTechnical == null) {
            createdDateTechnical = System.currentTimeMillis();
        }
        // Default the event time to now when no explicit event time was provided.
        if (eventTime == null) {
            eventTime = now;
        }
    }

    /**
     * Default constructor required by JPA.
     */
    public PlatformEvent() {
    }

    /**
     * Gets the unique identifier of the platform event.
     *
     * @return the UUID of the event
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the platform event.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the application/platform that produced this event.
     *
     * @return the associated {@link StatusApp}
     */
    public StatusApp getApp() {
        return app;
    }

    /**
     * Sets the application/platform that produced this event.
     *
     * @param app the {@link StatusApp} to associate
     */
    public void setApp(StatusApp app) {
        this.app = app;
    }

    /**
     * Gets the specific component that produced this event, if any.
     *
     * @return the associated {@link StatusComponent}, or {@code null} if not component-specific
     */
    public StatusComponent getComponent() {
        return component;
    }

    /**
     * Sets the specific component that produced this event.
     *
     * @param component the {@link StatusComponent} to associate
     */
    public void setComponent(StatusComponent component) {
        this.component = component;
    }

    /**
     * Gets the severity level of the event.
     *
     * @return the severity (e.g., INFO, WARNING, ERROR)
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * Sets the severity level of the event.
     *
     * @param severity the severity to set
     */
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    /**
     * Gets the source that originated the event.
     *
     * @return the event source
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the source that originated the event.
     *
     * @param source the event source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Gets the human-readable message describing the event.
     *
     * @return the event message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the human-readable message describing the event.
     *
     * @param message the event message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the additional detail text associated with the event.
     *
     * @return the event details
     */
    public String getDetails() {
        return details;
    }

    /**
     * Sets the additional detail text associated with the event.
     *
     * @param details the event details to set
     */
    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * Gets the time at which the event occurred.
     *
     * @return the event time
     */
    public ZonedDateTime getEventTime() {
        return eventTime;
    }

    /**
     * Sets the time at which the event occurred.
     *
     * @param eventTime the event time to set
     */
    public void setEventTime(ZonedDateTime eventTime) {
        this.eventTime = eventTime;
    }

    /**
     * Gets the timestamp when the event record was created.
     *
     * @return the creation date and time
     */
    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the timestamp when the event record was created.
     *
     * @param createdDate the creation date and time to set
     */
    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Gets the technical creation timestamp in epoch milliseconds.
     *
     * @return the creation timestamp in milliseconds since epoch
     */
    public Long getCreatedDateTechnical() {
        return createdDateTechnical;
    }

    /**
     * Sets the technical creation timestamp in epoch milliseconds.
     *
     * @param createdDateTechnical the creation timestamp in milliseconds to set
     */
    public void setCreatedDateTechnical(Long createdDateTechnical) {
        this.createdDateTechnical = createdDateTechnical;
    }
}
