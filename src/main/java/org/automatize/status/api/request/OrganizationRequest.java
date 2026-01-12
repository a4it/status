package org.automatize.status.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class OrganizationRequest {
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    @Email(message = "Email must be valid")
    private String email;
    
    private String phone;
    
    private String website;
    
    private String address;
    
    private String logoUrl;
    
    private String status = "ACTIVE";
    
    @NotBlank(message = "Organization type is required")
    private String organizationType;
    
    private String vatNumber;
    
    private String country;
    
    private String postalCode;
    
    private String community;
    
    private String type;
    
    private UUID tenantId;
    
    private Boolean subscriptionExempt = false;
    
    private Boolean throttlingEnabled = true;

    public OrganizationRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrganizationType() {
        return organizationType;
    }

    public void setOrganizationType(String organizationType) {
        this.organizationType = organizationType;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCommunity() {
        return community;
    }

    public void setCommunity(String community) {
        this.community = community;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public Boolean getSubscriptionExempt() {
        return subscriptionExempt;
    }

    public void setSubscriptionExempt(Boolean subscriptionExempt) {
        this.subscriptionExempt = subscriptionExempt;
    }

    public Boolean getThrottlingEnabled() {
        return throttlingEnabled;
    }

    public void setThrottlingEnabled(Boolean throttlingEnabled) {
        this.throttlingEnabled = throttlingEnabled;
    }
}