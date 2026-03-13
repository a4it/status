package org.automatize.status.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * <p>
 * Entity representing a notification subscriber for a status application.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Store email addresses of users subscribing to incident notifications</li>
 *   <li>Manage email verification status for subscribers</li>
 *   <li>Track active/inactive subscription status</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see StatusApp
 * @see StatusIncident
 */
@Entity
@Table(name = "notification_subscribers")
public class NotificationSubscriber {

    /**
     * Unique identifier for the notification subscriber.
     * Generated automatically using UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * The status application (platform) this subscriber is registered for.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    private StatusApp app;

    /**
     * Email address of the subscriber.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * Display name of the subscriber (optional).
     */
    @Column(name = "name", length = 255)
    private String name;

    /**
     * Flag indicating whether the subscriber is active and should receive notifications.
     * Defaults to true.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Flag indicating whether the subscriber has verified their email address.
     * Defaults to false.
     */
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    /**
     * Verification token used for email verification.
     */
    @Column(name = "verification_token", length = 255)
    private String verificationToken;

    /**
     * Timestamp when the verification token expires.
     */
    @Column(name = "verification_token_expires_at")
    private ZonedDateTime verificationTokenExpiresAt;

    /**
     * Username or identifier of the user who created this subscriber.
     */
    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    /**
     * Timestamp indicating when the subscriber was created.
     */
    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;

    /**
     * Username or identifier of the user who last modified this subscriber.
     */
    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String lastModifiedBy;

    /**
     * Timestamp indicating when the subscriber was last modified.
     */
    @Column(name = "last_modified_date", nullable = false)
    private ZonedDateTime lastModifiedDate;

    /**
     * Technical timestamp in epoch milliseconds for when the subscriber was created.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "created_date_technical", nullable = false)
    private Long createdDateTechnical;

    /**
     * Technical timestamp in epoch milliseconds for when the subscriber was last modified.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "last_modified_date_technical", nullable = false)
    private Long lastModifiedDateTechnical;

    /**
     * JPA lifecycle callback executed before persisting a new subscriber.
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
     * JPA lifecycle callback executed before updating an existing subscriber.
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
    public NotificationSubscriber() {
    }

    /**
     * Gets the unique identifier of the subscriber.
     *
     * @return the UUID of the subscriber
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the subscriber.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the status application this subscriber is registered for.
     *
     * @return the StatusApp
     */
    public StatusApp getApp() {
        return app;
    }

    /**
     * Sets the status application this subscriber is registered for.
     *
     * @param app the StatusApp to set
     */
    public void setApp(StatusApp app) {
        this.app = app;
    }

    /**
     * Gets the email address of the subscriber.
     *
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address of the subscriber.
     *
     * @param email the email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the display name of the subscriber.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name of the subscriber.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Checks if the subscriber is active.
     *
     * @return true if active, false otherwise
     */
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * Sets whether the subscriber is active.
     *
     * @param isActive the active flag to set
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Checks if the subscriber has verified their email.
     *
     * @return true if verified, false otherwise
     */
    public Boolean getIsVerified() {
        return isVerified;
    }

    /**
     * Sets whether the subscriber has verified their email.
     *
     * @param isVerified the verified flag to set
     */
    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    /**
     * Gets the verification token.
     *
     * @return the verification token
     */
    public String getVerificationToken() {
        return verificationToken;
    }

    /**
     * Sets the verification token.
     *
     * @param verificationToken the token to set
     */
    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    /**
     * Gets the verification token expiration timestamp.
     *
     * @return the expiration timestamp
     */
    public ZonedDateTime getVerificationTokenExpiresAt() {
        return verificationTokenExpiresAt;
    }

    /**
     * Sets the verification token expiration timestamp.
     *
     * @param verificationTokenExpiresAt the expiration timestamp to set
     */
    public void setVerificationTokenExpiresAt(ZonedDateTime verificationTokenExpiresAt) {
        this.verificationTokenExpiresAt = verificationTokenExpiresAt;
    }

    /**
     * Gets the username of the user who created this subscriber.
     *
     * @return the creator's username
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the username of the user who created this subscriber.
     *
     * @param createdBy the creator's username to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the creation timestamp of the subscriber.
     *
     * @return the creation date and time
     */
    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the creation timestamp of the subscriber.
     *
     * @param createdDate the creation date and time to set
     */
    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Gets the username of the user who last modified this subscriber.
     *
     * @return the last modifier's username
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Sets the username of the user who last modified this subscriber.
     *
     * @param lastModifiedBy the last modifier's username to set
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * Gets the last modification timestamp of the subscriber.
     *
     * @return the last modification date and time
     */
    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Sets the last modification timestamp of the subscriber.
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
}
