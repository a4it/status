package org.automatize.status.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Response object containing notification subscriber details.
 * <p>
 * Provides information about a notification subscriber including
 * their email, verification status, and associated application.
 * </p>
 */
public class NotificationSubscriberResponse {

    /** The unique identifier of the subscriber. */
    private UUID id;

    /** The application ID this subscriber is monitoring. */
    private UUID appId;

    /** The name of the application. */
    private String appName;

    /** The email address of the subscriber. */
    private String email;

    /** The display name of the subscriber. */
    private String name;

    /** Whether the subscriber is active. */
    private Boolean isActive;

    /** Whether the email has been verified. */
    private Boolean isVerified;

    /** When the subscriber was created. */
    private ZonedDateTime createdDate;

    /** Who created the subscriber. */
    private String createdBy;

    /**
     * Default constructor.
     */
    public NotificationSubscriberResponse() {
    }

    /**
     * Gets the ID.
     *
     * @return the ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the ID.
     *
     * @param id the ID to set
     */
    public void setId(UUID id) {
        this.id = id;
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
     * Gets the application name.
     *
     * @return the application name
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Sets the application name.
     *
     * @param appName the application name to set
     */
    public void setAppName(String appName) {
        this.appName = appName;
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

    /**
     * Gets the verified status.
     *
     * @return true if verified, false otherwise
     */
    public Boolean getIsVerified() {
        return isVerified;
    }

    /**
     * Sets the verified status.
     *
     * @param isVerified the verified status to set
     */
    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    /**
     * Gets the creation date.
     *
     * @return the creation date
     */
    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the creation date.
     *
     * @param createdDate the creation date to set
     */
    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Gets the creator.
     *
     * @return the creator username
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the creator.
     *
     * @param createdBy the creator username to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
