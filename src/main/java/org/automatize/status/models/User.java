package org.automatize.status.models;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * <p>
 * Entity representing a user in the status monitoring system.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Store user authentication credentials and profile information</li>
 *   <li>Support JWT-based authentication with refresh token storage</li>
 *   <li>Maintain role-based access control permissions within the system</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see Organization
 * @see Tenant
 */
@Entity
@Table(name = "users")
public class User {

    /**
     * Unique identifier for the user.
     * Generated automatically using UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * Username or identifier of the user who created this user account.
     */
    @Column(name = "created_by", length = 255)
    private String createdBy;

    /**
     * Timestamp indicating when the user account was created.
     */
    @Column(name = "created_date")
    private ZonedDateTime createdDate;

    /**
     * Technical timestamp in epoch milliseconds for when the user was created.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "created_date_technical")
    private Long createdDateTechnical;

    /**
     * Username or identifier of the user who last modified this user account.
     */
    @Column(name = "last_modified_by", length = 255)
    private String lastModifiedBy;

    /**
     * Timestamp indicating when the user account was last modified.
     */
    @Column(name = "last_modified_date")
    private ZonedDateTime lastModifiedDate;

    /**
     * Technical timestamp in epoch milliseconds for when the user was last modified.
     * Used for efficient sorting and querying operations.
     */
    @Column(name = "last_modified_date_technical")
    private Long lastModifiedDateTechnical;

    /**
     * Version field for optimistic locking to prevent concurrent modification conflicts.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Flag indicating whether the user account is enabled for authentication.
     * Disabled users cannot log in. Defaults to true.
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * Full display name of the user.
     */
    @Column(name = "full_name", length = 255)
    private String fullName;

    /**
     * Encrypted password for user authentication.
     * Should be stored using a secure hashing algorithm.
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * JWT refresh token for maintaining user sessions.
     * Used for obtaining new access tokens without re-authentication.
     */
    @Column(name = "refresh_token", length = 255)
    private String refreshToken;

    /**
     * Unique username for authentication and identification.
     */
    @Column(name = "username", nullable = false, length = 255, unique = true)
    private String username;

    /**
     * Email address of the user.
     * Must be unique across all users.
     */
    @Column(name = "email", length = 255, unique = true)
    private String email;

    /**
     * Role assigned to the user for access control.
     * Common values include ADMIN, USER, VIEWER.
     */
    @Column(name = "role", length = 20)
    private String role;

    /**
     * Current status of the user account.
     * Common values include ACTIVE, INACTIVE, SUSPENDED, PENDING.
     */
    @Column(name = "status", length = 30)
    private String status;

    /**
     * The organization to which this user belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    /**
     * Type classification for the user account.
     */
    @Column(name = "type", length = 255)
    private String type;

    /**
     * JPA lifecycle callback executed before persisting a new user.
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
     * JPA lifecycle callback executed before updating an existing user.
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
    public User() {
    }

    /**
     * Gets the unique identifier of the user.
     *
     * @return the UUID of the user
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the user.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the username of the user who created this account.
     *
     * @return the creator's username
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the username of the user who created this account.
     *
     * @param createdBy the creator's username to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the creation timestamp of the user account.
     *
     * @return the creation date and time
     */
    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the creation timestamp of the user account.
     *
     * @param createdDate the creation date and time to set
     */
    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
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
     * Gets the username of the user who last modified this account.
     *
     * @return the last modifier's username
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Sets the username of the user who last modified this account.
     *
     * @param lastModifiedBy the last modifier's username to set
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * Gets the last modification timestamp of the user account.
     *
     * @return the last modification date and time
     */
    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Sets the last modification timestamp of the user account.
     *
     * @param lastModifiedDate the last modification date and time to set
     */
    public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
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
     * Checks if the user account is enabled for authentication.
     *
     * @return true if enabled, false otherwise
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Sets whether the user account is enabled for authentication.
     *
     * @param enabled the enabled flag to set
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the full display name of the user.
     *
     * @return the full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Sets the full display name of the user.
     *
     * @param fullName the full name to set
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Gets the encrypted password of the user.
     *
     * @return the encrypted password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password of the user.
     * The password should be encrypted before setting.
     *
     * @param password the encrypted password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the JWT refresh token for the user.
     *
     * @return the refresh token
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Sets the JWT refresh token for the user.
     *
     * @param refreshToken the refresh token to set
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * Gets the username used for authentication.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username used for authentication.
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the email address of the user.
     *
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address of the user.
     *
     * @param email the email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the role assigned to the user.
     *
     * @return the role (e.g., ADMIN, USER, VIEWER)
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the role assigned to the user.
     *
     * @param role the role to set
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Gets the current status of the user account.
     *
     * @return the status (e.g., ACTIVE, INACTIVE, SUSPENDED)
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the current status of the user account.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the organization to which this user belongs.
     *
     * @return the parent organization
     */
    public Organization getOrganization() {
        return organization;
    }

    /**
     * Sets the organization to which this user belongs.
     *
     * @param organization the parent organization to set
     */
    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    /**
     * Gets the type classification of the user account.
     *
     * @return the user type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type classification of the user account.
     *
     * @param type the user type to set
     */
    public void setType(String type) {
        this.type = type;
    }
}
