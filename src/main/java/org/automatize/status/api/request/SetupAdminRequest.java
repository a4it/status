package org.automatize.status.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * <p>
 * Request object carrying the initial administrator account details during the
 * first-run setup wizard of the status monitoring system.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Capture the credentials and profile of the bootstrap admin user</li>
 *   <li>Validate that username, password, email and full name meet the required constraints</li>
 *   <li>Bind the new admin to the organization created earlier in the setup flow</li>
 * </ul>
 * </p>
 *
 * @author Tim De Smedt
 */
public class SetupAdminRequest {

    /** The admin username (3-50 characters). */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    /** The admin password (minimum 8 characters). */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /** The admin email address. */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /** The admin's full name. */
    @NotBlank(message = "Full name is required")
    private String fullName;

    /** The organization the admin account belongs to. */
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    /** @return the admin username */
    public String getUsername() { return username; }
    /** @param username the admin username to set */
    public void setUsername(String username) { this.username = username; }

    /** @return the admin password */
    public String getPassword() { return password; }
    /** @param password the admin password to set */
    public void setPassword(String password) { this.password = password; }

    /** @return the admin email address */
    public String getEmail() { return email; }
    /** @param email the admin email address to set */
    public void setEmail(String email) { this.email = email; }

    /** @return the admin's full name */
    public String getFullName() { return fullName; }
    /** @param fullName the admin's full name to set */
    public void setFullName(String fullName) { this.fullName = fullName; }

    /** @return the owning organization identifier */
    public UUID getOrganizationId() { return organizationId; }
    /** @param organizationId the owning organization identifier to set */
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
}
