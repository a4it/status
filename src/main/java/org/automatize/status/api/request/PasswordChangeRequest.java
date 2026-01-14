package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request object for changing a user's password.
 * <p>
 * Contains the current password for verification and the new password to set.
 * </p>
 */
public class PasswordChangeRequest {

    /** The user's current password for verification. */
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    /** The new password to set (minimum 8 characters). */
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    private String newPassword;

    /**
     * Default constructor.
     */
    public PasswordChangeRequest() {
    }

    /**
     * Gets the current password.
     *
     * @return the current password
     */
    public String getCurrentPassword() {
        return currentPassword;
    }

    /**
     * Sets the current password.
     *
     * @param currentPassword the current password to set
     */
    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    /**
     * Gets the new password.
     *
     * @return the new password
     */
    public String getNewPassword() {
        return newPassword;
    }

    /**
     * Sets the new password.
     *
     * @param newPassword the new password to set
     */
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}