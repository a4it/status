package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing an API key used to authenticate log ingestion requests.
 */
@Entity
@Table(name = "log_api_keys")
public class LogApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "api_key", nullable = false, length = 255, unique = true)
    private String apiKey;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;

    @Column(name = "created_date_technical", nullable = false)
    private Long createdDateTechnical;

    @PrePersist
    public void prePersist() {
        if (createdDate == null) {
            createdDate = ZonedDateTime.now();
        }
        if (createdDateTechnical == null) {
            createdDateTechnical = System.currentTimeMillis();
        }
    }

    public LogApiKey() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public ZonedDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(ZonedDateTime createdDate) { this.createdDate = createdDate; }

    public Long getCreatedDateTechnical() { return createdDateTechnical; }
    public void setCreatedDateTechnical(Long createdDateTechnical) { this.createdDateTechnical = createdDateTechnical; }
}
