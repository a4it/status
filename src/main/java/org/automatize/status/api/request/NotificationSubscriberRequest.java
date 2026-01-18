package org.automatize.status.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * <p>
 * Request object for creating or updating a notification subscriber.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate subscriber data for create and update operations</li>
 *   <li>Validate required fields such as app ID and email address</li>
 *   <li>Manage subscriber activation status</li>
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
public class NotificationSubscriberRequest {

    /** The application ID this subscriber is registering for. */
    @NotNull(message = "App ID is required")
    private UUID appId;

    /** The email address of the subscriber. */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /** The display name of the subscriber (optional). */
    private String name;

    /** Whether the subscriber is active. */
    private Boolean isActive = true;

    /**
     * Default constructor.
     */
    public NotificationSubscriberRequest() {
    }

    /**
     * Gets the application ID.
     *
     * @return the application ID
     */
    public UUID getAppId() {
        return appId;
    }

    /**
     * Sets the application ID.
     *
     * @param appId the application ID to set
     */
    public void setAppId(UUID appId) {
        this.appId = appId;
    }

    /**
     * Gets the email address.
     *
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address.
     *
     * @param email the email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the display name.
     *
     * @return the display name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name.
     *
     * @param name the display name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the active status.
     *
     * @return true if active, false otherwise
     */
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * Sets the active status.
     *
     * @param isActive the active status to set
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
