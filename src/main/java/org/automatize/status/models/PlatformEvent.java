package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing an event logged by a platform or component.
 * <p>
 * PlatformEvent stores log entries from monitored platforms and their components.
 * Events can be logged via API and are searchable in the admin interface.
 * </p>
 *
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

    @PrePersist
    public void prePersist() {
        ZonedDateTime now = ZonedDateTime.now();
        if (createdDate == null) {
            createdDate = now;
        }
        if (createdDateTechnical == null) {
            createdDateTechnical = System.currentTimeMillis();
        }
        if (eventTime == null) {
            eventTime = now;
        }
    }

    public PlatformEvent() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StatusApp getApp() {
        return app;
    }

    public void setApp(StatusApp app) {
        this.app = app;
    }

    public StatusComponent getComponent() {
        return component;
    }

    public void setComponent(StatusComponent component) {
        this.component = component;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public ZonedDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(ZonedDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Long getCreatedDateTechnical() {
        return createdDateTechnical;
    }

    public void setCreatedDateTechnical(Long createdDateTechnical) {
        this.createdDateTechnical = createdDateTechnical;
    }
}
