package org.automatize.status.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * <p>
 * Request object for creating or updating an organization entity.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate organization data for create and update operations</li>
 *   <li>Validate required organization fields such as name and type</li>
 *   <li>Provide tenant association and configuration options</li>
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
public class OrganizationRequest {

    /** The name of the organization. */
    @NotBlank(message = "Name is required")
    private String name;

    /** A description of the organization. */
    private String description;

    /** The contact email address for the organization. */
    @Email(message = "Email must be valid")
    private String email;

    /** The contact phone number for the organization. */
    private String phone;

    /** The website URL for the organization. */
    private String website;

    /** The physical address of the organization. */
    private String address;

    /** The URL to the organization's logo image. */
    private String logoUrl;

    /** The current status of the organization (e.g., ACTIVE, INACTIVE). */
    private String status = "ACTIVE";

    /** The type classification of the organization. */
    @NotBlank(message = "Organization type is required")
    private String organizationType;

    /** The VAT/tax identification number. */
    private String vatNumber;

    /** The country where the organization is located. */
    private String country;

    /** The postal/ZIP code for the organization's address. */
    private String postalCode;

    /** The community or region identifier. */
    private String community;

    /** Additional type classification. */
    private String type;

    /** The unique identifier of the tenant this organization belongs to. */
    private UUID tenantId;

    /** Whether the organization is exempt from subscription requirements. */
    private Boolean subscriptionExempt = false;

    /** Whether API throttling is enabled for this organization. */
    private Boolean throttlingEnabled = true;

    /**
     * Default constructor.
     */
    public OrganizationRequest() {
    }

    /**
     * Gets the organization name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the organization name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the organization description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the organization description.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the contact email.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the contact email.
     *
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the contact phone number.
     *
     * @return the phone number
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the contact phone number.
     *
     * @param phone the phone number to set
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Gets the website URL.
     *
     * @return the website URL
     */
    public String getWebsite() {
        return website;
    }

    /**
     * Sets the website URL.
     *
     * @param website the website URL to set
     */
    public void setWebsite(String website) {
        this.website = website;
    }

    /**
     * Gets the physical address.
     *
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets the physical address.
     *
     * @param address the address to set
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Gets the logo URL.
     *
     * @return the logo URL
     */
    public String getLogoUrl() {
        return logoUrl;
    }

    /**
     * Sets the logo URL.
     *
     * @param logoUrl the logo URL to set
     */
    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    /**
     * Gets the organization status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the organization status.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the organization type.
     *
     * @return the organization type
     */
    public String getOrganizationType() {
        return organizationType;
    }

    /**
     * Sets the organization type.
     *
     * @param organizationType the organization type to set
     */
    public void setOrganizationType(String organizationType) {
        this.organizationType = organizationType;
    }

    /**
     * Gets the VAT number.
     *
     * @return the VAT number
     */
    public String getVatNumber() {
        return vatNumber;
    }

    /**
     * Sets the VAT number.
     *
     * @param vatNumber the VAT number to set
     */
    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    /**
     * Gets the country.
     *
     * @return the country
     */
    public String getCountry() {
        return country;
    }

    /**
     * Sets the country.
     *
     * @param country the country to set
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * Gets the postal code.
     *
     * @return the postal code
     */
    public String getPostalCode() {
        return postalCode;
    }

    /**
     * Sets the postal code.
     *
     * @param postalCode the postal code to set
     */
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    /**
     * Gets the community.
     *
     * @return the community
     */
    public String getCommunity() {
        return community;
    }

    /**
     * Sets the community.
     *
     * @param community the community to set
     */
    public void setCommunity(String community) {
        this.community = community;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the tenant ID.
     *
     * @return the tenant ID
     */
    public UUID getTenantId() {
        return tenantId;
    }

    /**
     * Sets the tenant ID.
     *
     * @param tenantId the tenant ID to set
     */
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Gets the subscription exempt flag.
     *
     * @return true if subscription exempt, false otherwise
     */
    public Boolean getSubscriptionExempt() {
        return subscriptionExempt;
    }

    /**
     * Sets the subscription exempt flag.
     *
     * @param subscriptionExempt the subscription exempt flag to set
     */
    public void setSubscriptionExempt(Boolean subscriptionExempt) {
        this.subscriptionExempt = subscriptionExempt;
    }

    /**
     * Gets the throttling enabled flag.
     *
     * @return true if throttling is enabled, false otherwise
     */
    public Boolean getThrottlingEnabled() {
        return throttlingEnabled;
    }

    /**
     * Sets the throttling enabled flag.
     *
     * @param throttlingEnabled the throttling enabled flag to set
     */
    public void setThrottlingEnabled(Boolean throttlingEnabled) {
        this.throttlingEnabled = throttlingEnabled;
    }
}