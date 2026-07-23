package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Entity representing an organization within the status monitoring system.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Store organization data including name, contact information, and settings</li>
 *   <li>Maintain multi-tenant hierarchy relationships with parent tenant</li>
 *   <li>Track audit information with creation and modification timestamps</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see Tenant
 * @see User
 * @see StatusApp
 */
@Entity
@Table(name = "organizations")
@EntityListeners(AuditTimestampListener.class)
public class Organization implements Auditable {

    /**
     * Unique identifier for the organization.
     * Generated automatically using UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    @Getter
    @Setter
    private UUID id;

    /**
     * The display name of the organization.
     * Must be unique across all organizations in the system.
     */
    @Column(name = "name", nullable = false, length = 255, unique = true)
    @Getter
    @Setter
    private String name;

    /**
     * Detailed description of the organization and its purpose.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    @Getter
    @Setter
    private String description;

    /**
     * Primary contact email address for the organization.
     */
    @Column(name = "email", length = 255)
    @Getter
    @Setter
    private String email;

    /**
     * Primary contact phone number for the organization.
     */
    @Column(name = "phone", length = 50)
    @Getter
    @Setter
    private String phone;

    /**
     * Official website URL of the organization.
     */
    @Column(name = "website", length = 255)
    @Getter
    @Setter
    private String website;

    /**
     * Physical address or mailing address of the organization.
     */
    @Column(name = "address", columnDefinition = "TEXT")
    @Getter
    @Setter
    private String address;

    /**
     * URL to the organization's logo image for branding purposes.
     */
    @Column(name = "logo_url", length = 255)
    @Getter
    @Setter
    private String logoUrl;

    /**
     * Current operational status of the organization.
     * Defaults to "ACTIVE". Common values include ACTIVE, INACTIVE, SUSPENDED.
     */
    @Column(name = "status", nullable = false, length = 50)
    @Getter
    @Setter
    private String status = "ACTIVE";

    /**
     * Username or identifier of the user who created this organization.
     */
    @Column(name = "created_by", nullable = false, length = 255)
    @Getter
    @Setter
    private String createdBy;

    /**
     * Timestamp indicating when the organization was created.
     */
    @Column(name = "created_date", nullable = false)
    @Getter
    @Setter
    private ZonedDateTime createdDate;

    /**
     * Username or identifier of the user who last modified this organization.
     */
    @Column(name = "last_modified_by", nullable = false, length = 255)
    @Getter
    @Setter
    private String lastModifiedBy;

    /**
     * Timestamp indicating when the organization was last modified.
     */
    @Column(name = "last_modified_date", nullable = false)
    @Getter
    @Setter
    private ZonedDateTime lastModifiedDate;

    /**
     * Technical timestamp in epoch milliseconds for when the organization was created.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "created_date_technical", nullable = false)
    @Getter
    @Setter
    private Long createdDateTechnical;

    /**
     * Technical timestamp in epoch milliseconds for when the organization was last modified.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "last_modified_date_technical", nullable = false)
    @Getter
    @Setter
    private Long lastModifiedDateTechnical;

    /**
     * Version field for optimistic locking to prevent concurrent modification conflicts.
     */
    @Version
    @Column(name = "version")
    @Getter
    @Setter
    private Long version;

    /**
     * Flag indicating whether the organization is exempt from subscription requirements.
     * Defaults to false.
     */
    @Column(name = "subscription_exempt", nullable = false)
    @Getter
    @Setter
    private Boolean subscriptionExempt = false;

    /**
     * Flag indicating whether API throttling is enabled for this organization.
     * Defaults to true for rate limiting protection.
     */
    @Column(name = "throttling_enabled", nullable = false)
    @Getter
    @Setter
    private Boolean throttlingEnabled = true;

    /**
     * Type classification of the organization (e.g., ENTERPRISE, SMALL_BUSINESS, INDIVIDUAL).
     */
    @Column(name = "organization_type", nullable = false, length = 20)
    @Getter
    @Setter
    private String organizationType;

    /**
     * VAT (Value Added Tax) registration number for the organization.
     */
    @Column(name = "vat_number", length = 50)
    @Getter
    @Setter
    private String vatNumber;

    /**
     * Country where the organization is registered or operates.
     */
    @Column(name = "country", length = 100)
    @Getter
    @Setter
    private String country;

    /**
     * Postal code or ZIP code of the organization's primary location.
     */
    @Column(name = "postalcode", columnDefinition = "TEXT")
    @Getter
    @Setter
    private String postalCode;

    /**
     * Community or municipality where the organization is located.
     */
    @Column(name = "community", columnDefinition = "TEXT")
    @Getter
    @Setter
    private String community;

    /**
     * Additional type classification for the organization.
     */
    @Column(name = "type", length = 255)
    @Getter
    @Setter
    private String type;

    /**
     * The tenant to which this organization belongs.
     * Establishes the multi-tenant hierarchy relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "organizations"})
    @Getter
    @Setter
    private Tenant tenant;

    /**
     * Default constructor required by JPA.
     */
    public Organization() {
    }
}
