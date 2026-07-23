package org.automatize.status.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

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
    @Getter
    @Setter
    private String name;

    /** A description of the organization. */
    @Getter
    @Setter
    private String description;

    /** The contact email address for the organization. */
    @Email(message = "Email must be valid")
    @Getter
    @Setter
    private String email;

    /** The contact phone number for the organization. */
    @Getter
    @Setter
    private String phone;

    /** The website URL for the organization. */
    @Getter
    @Setter
    private String website;

    /** The physical address of the organization. */
    @Getter
    @Setter
    private String address;

    /** The URL to the organization's logo image. */
    @Getter
    @Setter
    private String logoUrl;

    /** The current status of the organization (e.g., ACTIVE, INACTIVE). */
    @Getter
    @Setter
    private String status = "ACTIVE";

    /** The type classification of the organization. */
    @NotBlank(message = "Organization type is required")
    @Getter
    @Setter
    private String organizationType;

    /** The VAT/tax identification number. */
    @Getter
    @Setter
    private String vatNumber;

    /** The country where the organization is located. */
    @Getter
    @Setter
    private String country;

    /** The postal/ZIP code for the organization's address. */
    @Getter
    @Setter
    private String postalCode;

    /** The community or region identifier. */
    @Getter
    @Setter
    private String community;

    /** Additional type classification. */
    @Getter
    @Setter
    private String type;

    /** The unique identifier of the tenant this organization belongs to. */
    @Getter
    @Setter
    private UUID tenantId;

    /** Whether the organization is exempt from subscription requirements. */
    @Getter
    @Setter
    private Boolean subscriptionExempt = false;

    /** Whether API throttling is enabled for this organization. */
    @Getter
    @Setter
    private Boolean throttlingEnabled = true;

    /**
     * Default constructor.
     */
    public OrganizationRequest() {
    }
}
