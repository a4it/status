package org.automatize.status.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request object for creating or updating a notification subscriber.
 * <p>
 * A notification subscriber represents a user who wants to be notified
 * about incidents for a specific status application.
 * </p>
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
