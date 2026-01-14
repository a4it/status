package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.UserRequest;
import org.automatize.status.api.request.PasswordChangeRequest;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.models.User;
import org.automatize.status.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API controller for user management operations.
 * <p>
 * This controller provides CRUD operations for users within the multi-tenant
 * system. Users belong to organizations and have roles (ADMIN, MANAGER, USER)
 * that determine their access levels. Access to endpoints is controlled based
 * on the user's role and whether they are accessing their own data.
 * </p>
 *
 * @see UserService
 * @see User
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("isAuthenticated()")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Retrieves a paginated list of all users with optional filtering.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param organizationId optional filter by organization ID
     * @param role optional filter by user role
     * @param enabled optional filter by account enabled status
     * @param search optional search term for filtering by name or email
     * @param pageable pagination parameters (page, size, sort)
     * @return ResponseEntity containing a page of users
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<User> users = userService.getAllUsers(organizationId, role, enabled, search, pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Retrieves a user by their unique identifier.
     * <p>
     * This endpoint is accessible to users with ADMIN role or to the user
     * accessing their own data.
     * </p>
     *
     * @param id the UUID of the user
     * @return ResponseEntity containing the user details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Creates a new user.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param request the user creation request containing user details
     * @return ResponseEntity containing the created user with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<User> createUser(@Valid @RequestBody UserRequest request) {
        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Updates an existing user.
     * <p>
     * This endpoint is accessible to users with ADMIN role or to regular users
     * updating their own profile.
     * </p>
     *
     * @param id the UUID of the user to update
     * @param request the user update request containing new details
     * @return ResponseEntity containing the updated user
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (#id == authentication.principal.id and hasRole('USER'))")
    public ResponseEntity<User> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserRequest request) {
        User user = userService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }

    /**
     * Deletes a user by their unique identifier.
     * <p>
     * This endpoint is restricted to users with ADMIN role.
     * </p>
     *
     * @param id the UUID of the user to delete
     * @return ResponseEntity containing a success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new MessageResponse("User deleted successfully", true));
    }

    /**
     * Changes a user's password.
     * <p>
     * This endpoint is accessible to users with ADMIN role or to the user
     * changing their own password.
     * </p>
     *
     * @param id the UUID of the user whose password will be changed
     * @param request the password change request containing old and new passwords
     * @return ResponseEntity containing a success message
     */
    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<MessageResponse> changePassword(
            @PathVariable UUID id,
            @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.ok(new MessageResponse("Password changed successfully", true));
    }

    /**
     * Enables a disabled user account.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the user to enable
     * @return ResponseEntity containing the updated user
     */
    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<User> enableUser(@PathVariable UUID id) {
        User user = userService.enableUser(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Disables an active user account.
     * <p>
     * Disabled users cannot log in to the system. This endpoint is restricted
     * to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the user to disable
     * @return ResponseEntity containing the updated user
     */
    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<User> disableUser(@PathVariable UUID id) {
        User user = userService.disableUser(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Updates a user's role.
     * <p>
     * This endpoint is restricted to users with ADMIN role.
     * </p>
     *
     * @param id the UUID of the user whose role will be updated
     * @param role the new role to assign to the user
     * @return ResponseEntity containing the updated user
     */
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> updateUserRole(
            @PathVariable UUID id,
            @RequestParam String role) {
        User user = userService.updateRole(id, role);
        return ResponseEntity.ok(user);
    }

    /**
     * Retrieves all users belonging to a specific organization.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param organizationId the UUID of the organization
     * @return ResponseEntity containing a list of users
     */
    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<User>> getUsersByOrganization(@PathVariable UUID organizationId) {
        List<User> users = userService.getUsersByOrganization(organizationId);
        return ResponseEntity.ok(users);
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @return ResponseEntity containing the current user's profile
     */
    @GetMapping("/profile")
    public ResponseEntity<User> getCurrentUserProfile() {
        User user = userService.getCurrentUserProfile();
        return ResponseEntity.ok(user);
    }

    /**
     * Updates the profile of the currently authenticated user.
     *
     * @param request the profile update request containing new details
     * @return ResponseEntity containing the updated user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<User> updateCurrentUserProfile(@Valid @RequestBody UserRequest request) {
        User user = userService.updateCurrentUserProfile(request);
        return ResponseEntity.ok(user);
    }
}