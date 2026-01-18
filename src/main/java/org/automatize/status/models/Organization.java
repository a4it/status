package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

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
public class Organization {

    /**
     * Unique identifier for the organization.
     * Generated automatically using UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * The display name of the organization.
     * Must be unique across all organizations in the system.
     */
    @Column(name = "name", nullable = false, length = 255, unique = true)
    private String name;

    /**
     * Detailed description of the organization and its purpose.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Primary contact email address for the organization.
     */
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Primary contact phone number for the organization.
     */
    @Column(name = "phone", length = 50)
    private String phone;

    /**
     * Official website URL of the organization.
     */
    @Column(name = "website", length = 255)
    private String website;

    /**
     * Physical address or mailing address of the organization.
     */
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /**
     * URL to the organization's logo image for branding purposes.
     */
    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    /**
     * Current operational status of the organization.
     * Defaults to "ACTIVE". Common values include ACTIVE, INACTIVE, SUSPENDED.
     */
    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE";

    /**
     * Username or identifier of the user who created this organization.
     */
    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    /**
     * Timestamp indicating when the organization was created.
     */
    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;

    /**
     * Username or identifier of the user who last modified this organization.
     */
    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String lastModifiedBy;

    /**
     * Timestamp indicating when the organization was last modified.
     */
    @Column(name = "last_modified_date", nullable = false)
    private ZonedDateTime lastModifiedDate;

    /**
     * Technical timestamp in epoch milliseconds for when the organization was created.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "created_date_technical", nullable = false)
    private Long createdDateTechnical;

    /**
     * Technical timestamp in epoch milliseconds for when the organization was last modified.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "last_modified_date_technical", nullable = false)
    private Long lastModifiedDateTechnical;

    /**
     * Version field for optimistic locking to prevent concurrent modification conflicts.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Flag indicating whether the organization is exempt from subscription requirements.
     * Defaults to false.
     */
    @Column(name = "subscription_exempt", nullable = false)
    private Boolean subscriptionExempt = false;

    /**
     * Flag indicating whether API throttling is enabled for this organization.
     * Defaults to true for rate limiting protection.
     */
    @Column(name = "throttling_enabled", nullable = false)
    private Boolean throttlingEnabled = true;

    /**
     * Type classification of the organization (e.g., ENTERPRISE, SMALL_BUSINESS, INDIVIDUAL).
     */
    @Column(name = "organization_type", nullable = false, length = 20)
    private String organizationType;

    /**
     * VAT (Value Added Tax) registration number for the organization.
     */
    @Column(name = "vat_number", length = 50)
    private String vatNumber;

    /**
     * Country where the organization is registered or operates.
     */
    @Column(name = "country", length = 100)
    private String country;

    /**
     * Postal code or ZIP code of the organization's primary location.
     */
    @Column(name = "postalcode", columnDefinition = "TEXT")
    private String postalCode;

    /**
     * Community or municipality where the organization is located.
     */
    @Column(name = "community", columnDefinition = "TEXT")
    private String community;

    /**
     * Additional type classification for the organization.
     */
    @Column(name = "type", length = 255)
    private String type;

    /**
     * The tenant to which this organization belongs.
     * Establishes the multi-tenant hierarchy relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    /**
     * JPA lifecycle callback executed before persisting a new organization.
     * Automatically sets creation and modification timestamps if not already set.
     */
    @PrePersist
    public void prePersist() {
        ZonedDateTime now = ZonedDateTime.now();
        if (createdDate == null) {
            createdDate = now;
        }
        if (lastModifiedDate == null) {
            lastModifiedDate = now;
        }
        if (createdDateTechnical == null) {
            createdDateTechnical = System.currentTimeMillis();
        }
        if (lastModifiedDateTechnical == null) {
            lastModifiedDateTechnical = System.currentTimeMillis();
        }
    }

    /**
     * JPA lifecycle callback executed before updating an existing organization.
     * Automatically updates the modification timestamps.
     */
    @PreUpdate
    public void preUpdate() {
        lastModifiedDate = ZonedDateTime.now();
        lastModifiedDateTechnical = System.currentTimeMillis();
    }

    /**
     * Default constructor required by JPA.
     */
    public Organization() {
    }

    /**
     * Gets the unique identifier of the organization.
     *
     * @return the UUID of the organization
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the organization.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the display name of the organization.
     *
     * @return the organization name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name of the organization.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description of the organization.
     *
     * @return the organization description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the organization.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the primary contact email of the organization.
     *
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the primary contact email of the organization.
     *
     * @param email the email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the primary contact phone number of the organization.
     *
     * @return the phone number
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the primary contact phone number of the organization.
     *
     * @param phone the phone number to set
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Gets the website URL of the organization.
     *
     * @return the website URL
     */
    public String getWebsite() {
        return website;
    }

    /**
     * Sets the website URL of the organization.
     *
     * @param website the website URL to set
     */
    public void setWebsite(String website) {
        this.website = website;
    }

    /**
     * Gets the physical address of the organization.
     *
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets the physical address of the organization.
     *
     * @param address the address to set
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Gets the URL to the organization's logo image.
     *
     * @return the logo URL
     */
    public String getLogoUrl() {
        return logoUrl;
    }

    /**
     * Sets the URL to the organization's logo image.
     *
     * @param logoUrl the logo URL to set
     */
    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    /**
     * Gets the current status of the organization.
     *
     * @return the status (e.g., ACTIVE, INACTIVE, SUSPENDED)
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the current status of the organization.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the username of the user who created this organization.
     *
     * @return the creator's username
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the username of the user who created this organization.
     *
     * @param createdBy the creator's username to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the creation timestamp of the organization.
     *
     * @return the creation date and time
     */
    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the creation timestamp of the organization.
     *
     * @param createdDate the creation date and time to set
     */
    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Gets the username of the user who last modified this organization.
     *
     * @return the last modifier's username
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Sets the username of the user who last modified this organization.
     *
     * @param lastModifiedBy the last modifier's username to set
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * Gets the last modification timestamp of the organization.
     *
     * @return the last modification date and time
     */
    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Sets the last modification timestamp of the organization.
     *
     * @param lastModifiedDate the last modification date and time to set
     */
    public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
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

    /**
     * Gets the technical last modification timestamp in epoch milliseconds.
     *
     * @return the last modification timestamp in milliseconds since epoch
     */
    public Long getLastModifiedDateTechnical() {
        return lastModifiedDateTechnical;
    }

    /**
     * Sets the technical last modification timestamp in epoch milliseconds.
     *
     * @param lastModifiedDateTechnical the last modification timestamp in milliseconds to set
     */
    public void setLastModifiedDateTechnical(Long lastModifiedDateTechnical) {
        this.lastModifiedDateTechnical = lastModifiedDateTechnical;
    }

    /**
     * Gets the version number used for optimistic locking.
     *
     * @return the version number
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the version number used for optimistic locking.
     *
     * @param version the version number to set
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Checks if the organization is exempt from subscription requirements.
     *
     * @return true if subscription exempt, false otherwise
     */
    public Boolean getSubscriptionExempt() {
        return subscriptionExempt;
    }

    /**
     * Sets whether the organization is exempt from subscription requirements.
     *
     * @param subscriptionExempt the subscription exempt flag to set
     */
    public void setSubscriptionExempt(Boolean subscriptionExempt) {
        this.subscriptionExempt = subscriptionExempt;
    }

    /**
     * Checks if API throttling is enabled for this organization.
     *
     * @return true if throttling is enabled, false otherwise
     */
    public Boolean getThrottlingEnabled() {
        return throttlingEnabled;
    }

    /**
     * Sets whether API throttling is enabled for this organization.
     *
     * @param throttlingEnabled the throttling enabled flag to set
     */
    public void setThrottlingEnabled(Boolean throttlingEnabled) {
        this.throttlingEnabled = throttlingEnabled;
    }

    /**
     * Gets the type classification of the organization.
     *
     * @return the organization type
     */
    public String getOrganizationType() {
        return organizationType;
    }

    /**
     * Sets the type classification of the organization.
     *
     * @param organizationType the organization type to set
     */
    public void setOrganizationType(String organizationType) {
        this.organizationType = organizationType;
    }

    /**
     * Gets the VAT registration number of the organization.
     *
     * @return the VAT number
     */
    public String getVatNumber() {
        return vatNumber;
    }

    /**
     * Sets the VAT registration number of the organization.
     *
     * @param vatNumber the VAT number to set
     */
    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    /**
     * Gets the country where the organization is registered.
     *
     * @return the country name
     */
    public String getCountry() {
        return country;
    }

    /**
     * Sets the country where the organization is registered.
     *
     * @param country the country name to set
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * Gets the postal code of the organization's primary location.
     *
     * @return the postal code
     */
    public String getPostalCode() {
        return postalCode;
    }

    /**
     * Sets the postal code of the organization's primary location.
     *
     * @param postalCode the postal code to set
     */
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    /**
     * Gets the community or municipality where the organization is located.
     *
     * @return the community name
     */
    public String getCommunity() {
        return community;
    }

    /**
     * Sets the community or municipality where the organization is located.
     *
     * @param community the community name to set
     */
    public void setCommunity(String community) {
        this.community = community;
    }

    /**
     * Gets the additional type classification for the organization.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the additional type classification for the organization.
     *
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the tenant to which this organization belongs.
     *
     * @return the parent tenant
     */
    public Tenant getTenant() {
        return tenant;
    }

    /**
     * Sets the tenant to which this organization belongs.
     *
     * @param tenant the parent tenant to set
     */
    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }
}
