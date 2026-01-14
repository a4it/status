package org.automatize.status.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request object for creating or updating a user.
 * <p>
 * Contains all user account information including credentials, profile data,
 * and organizational associations.
 * </p>
 */
public class UserRequest {

    /** The username (3-50 characters). */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    /** The password (minimum 8 characters, optional for updates). */
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /** The email address. */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /** The full name of the user. */
    @NotBlank(message = "Full name is required")
    private String fullName;

    /** Whether the user account is enabled. */
    private Boolean enabled = true;

    /** The role assigned to the user. */
    private String role = "USER";

    /** The status of the user account. */
    private String status = "ACTIVE";

    /** The organization the user belongs to. */
    private UUID organizationId;

    /** The type classification of the user. */
    private String type;

    /**
     * Default constructor.
     */
    public UserRequest() {
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     *
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the email.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email.
     *
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the full name.
     *
     * @return the full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Sets the full name.
     *
     * @param fullName the full name to set
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Gets the enabled flag.
     *
     * @return true if enabled, false otherwise
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Sets the enabled flag.
     *
     * @param enabled the enabled flag to set
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the role.
     *
     * @return the role
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the role.
     *
     * @param role the role to set
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Gets the status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the organization ID.
     *
     * @return the organization ID
     */
    public UUID getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the organization ID.
     *
     * @param organizationId the organization ID to set
     */
    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
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
}