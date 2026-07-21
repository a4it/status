package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing a drop rule that rejects matching logs before storage.
 * All fields except name are optional — null means "match any".
 */
@Entity
@Table(name = "drop_rules")
public class DropRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "level", length = 20)
    private String level;

    @Column(name = "service", length = 255)
    private String service;

    @Column(name = "message_pattern", length = 500)
    private String messagePattern;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;

    @Column(name = "created_date_technical", nullable = false)
    private Long createdDateTechnical;

    /**
     * JPA lifecycle callback executed before persisting a new drop rule.
     * Populates the creation timestamps if they have not been set explicitly.
     */
    @PrePersist
    public void prePersist() {
        // Default the human-readable creation timestamp when not already set
        if (createdDate == null) {
            createdDate = ZonedDateTime.now();
        }
        // Default the technical (epoch millis) creation timestamp when not already set
        if (createdDateTechnical == null) {
            createdDateTechnical = System.currentTimeMillis();
        }
    }

    /**
     * Default constructor required by JPA.
     */
    public DropRule() {
    }

    /** @return the unique identifier of the drop rule */
    public UUID getId() { return id; }
    /** @param id the unique identifier to set */
    public void setId(UUID id) { this.id = id; }

    /** @return the tenant that owns this drop rule */
    public Tenant getTenant() { return tenant; }
    /** @param tenant the owning tenant to set */
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    /** @return the display name of the drop rule */
    public String getName() { return name; }
    /** @param name the display name to set */
    public void setName(String name) { this.name = name; }

    /** @return the log level to match, or null to match any level */
    public String getLevel() { return level; }
    /** @param level the log level to match to set */
    public void setLevel(String level) { this.level = level; }

    /** @return the service to match, or null to match any service */
    public String getService() { return service; }
    /** @param service the service to match to set */
    public void setService(String service) { this.service = service; }

    /** @return the message pattern to match, or null to match any message */
    public String getMessagePattern() { return messagePattern; }
    /** @param messagePattern the message pattern to match to set */
    public void setMessagePattern(String messagePattern) { this.messagePattern = messagePattern; }

    /** @return true if the rule is active, false otherwise */
    public Boolean getIsActive() { return isActive; }
    /** @param isActive the active flag to set */
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    /** @return the creation timestamp */
    public ZonedDateTime getCreatedDate() { return createdDate; }
    /** @param createdDate the creation timestamp to set */
    public void setCreatedDate(ZonedDateTime createdDate) { this.createdDate = createdDate; }

    /** @return the technical creation timestamp in epoch milliseconds */
    public Long getCreatedDateTechnical() { return createdDateTechnical; }
    /** @param createdDateTechnical the technical creation timestamp to set */
    public void setCreatedDateTechnical(Long createdDateTechnical) { this.createdDateTechnical = createdDateTechnical; }
}
