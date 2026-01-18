package org.automatize.status.services;

import org.automatize.status.api.request.PasswordChangeRequest;
import org.automatize.status.api.request.UserRequest;
import org.automatize.status.models.Organization;
import org.automatize.status.models.User;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.UserRepository;
import org.automatize.status.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 * Service responsible for managing user accounts and profiles.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for user entities</li>
 *   <li>Handle password management and role updates</li>
 *   <li>Enforce permission checks and organization associations</li>
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
@Service
@Transactional
public class UserService {

    /**
     * Repository for user data access operations.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Repository for organization data access operations.
     */
    @Autowired
    private OrganizationRepository organizationRepository;

    /**
     * Password encoder for secure password hashing.
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Retrieves a paginated list of users with optional filtering.
     *
     * @param organizationId optional organization ID to filter users
     * @param role optional role to filter users
     * @param enabled optional enabled status to filter users
     * @param search optional search term for username/email matching
     * @param pageable pagination information
     * @return a page of User entities matching the criteria
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(UUID organizationId, String role, Boolean enabled, String search, Pageable pageable) {
        List<User> users;
        
        if (organizationId != null && role != null) {
            users = userRepository.findByOrganizationIdAndRole(organizationId, role);
        } else if (organizationId != null && enabled != null) {
            users = userRepository.findByOrganizationIdAndEnabled(organizationId, enabled);
        } else if (organizationId != null) {
            users = userRepository.findByOrganizationId(organizationId);
        } else if (role != null) {
            users = userRepository.findByRole(role);
        } else if (enabled != null) {
            users = userRepository.findByEnabled(enabled);
        } else if (search != null && !search.isEmpty()) {
            users = userRepository.search(search);
        } else {
            return userRepository.findAll(pageable);
        }
        
        return new PageImpl<>(users, pageable, users.size());
    }

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param id the UUID of the user
     * @return the User entity
     * @throws RuntimeException if the user is not found
     */
    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    /**
     * Retrieves all users belonging to a specific organization.
     *
     * @param organizationId the UUID of the organization
     * @return a list of User entities in the organization
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByOrganization(UUID organizationId) {
        return userRepository.findByOrganizationId(organizationId);
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @return the User entity for the current user
     * @throws RuntimeException if the current user cannot be determined
     */
    @Transactional(readOnly = true)
    public User getCurrentUserProfile() {
        UserPrincipal principal = getCurrentUserPrincipal();
        return getUserById(principal.getId());
    }

    /**
     * Creates a new user with the provided details.
     * <p>
     * Validates that the username and email are unique before creating.
     * The password is securely encoded before storage.
     * </p>
     *
     * @param request the user creation request
     * @return the newly created User entity
     * @throws RuntimeException if the username or email already exists
     * @throws RuntimeException if the organization is not found
     */
    public User createUser(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        User user = new User();
        mapRequestToUser(request, user);
        
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        if (request.getOrganizationId() != null) {
            Organization organization = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            user.setOrganization(organization);
        }
        
        String currentUser = getCurrentUsername();
        user.setCreatedBy(currentUser);
        user.setLastModifiedBy(currentUser);

        return userRepository.save(user);
    }

    /**
     * Updates an existing user with the provided details.
     * <p>
     * Only the user themselves or an admin can update a user.
     * Password and organization changes require admin privileges.
     * </p>
     *
     * @param id the UUID of the user to update
     * @param request the user update request
     * @return the updated User entity
     * @throws RuntimeException if the user is not found
     * @throws RuntimeException if permission is denied
     * @throws RuntimeException if the username or email conflicts
     */
    public User updateUser(UUID id, UserRequest request) {
        User user = getUserById(id);
        UserPrincipal currentUser = getCurrentUserPrincipal();
        
        // Check if user is updating their own profile or has admin role
        if (!currentUser.getId().equals(id) && !currentUser.getRole().equals("ADMIN")) {
            throw new RuntimeException("Insufficient permissions to update this user");
        }

        if (!user.getUsername().equals(request.getUsername()) && 
            userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        mapRequestToUser(request, user);
        
        // Only admin can change password during update
        if (request.getPassword() != null && currentUser.getRole().equals("ADMIN")) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        // Only admin can change organization
        if (request.getOrganizationId() != null && currentUser.getRole().equals("ADMIN")) {
            Organization organization = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            user.setOrganization(organization);
        }
        
        user.setLastModifiedBy(getCurrentUsername());

        return userRepository.save(user);
    }

    /**
     * Updates the profile of the currently authenticated user.
     * <p>
     * Allows users to update their own basic profile information
     * (username, email, full name, type) without admin privileges.
     * </p>
     *
     * @param request the user update request
     * @return the updated User entity
     * @throws RuntimeException if the username or email conflicts
     */
    public User updateCurrentUserProfile(UserRequest request) {
        UserPrincipal principal = getCurrentUserPrincipal();
        User user = getUserById(principal.getId());

        if (!user.getUsername().equals(request.getUsername()) && 
            userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setType(request.getType());
        user.setLastModifiedBy(getCurrentUsername());

        return userRepository.save(user);
    }

    /**
     * Changes a user's password.
     * <p>
     * Users can change their own password by providing their current password.
     * Admins can change any user's password without providing the current password.
     * </p>
     *
     * @param id the UUID of the user whose password to change
     * @param request the password change request
     * @throws RuntimeException if the user is not found
     * @throws RuntimeException if permission is denied
     * @throws RuntimeException if the current password is incorrect
     */
    public void changePassword(UUID id, PasswordChangeRequest request) {
        User user = getUserById(id);
        UserPrincipal currentUser = getCurrentUserPrincipal();
        
        // Check if user is changing their own password or has admin role
        if (!currentUser.getId().equals(id) && !currentUser.getRole().equals("ADMIN")) {
            throw new RuntimeException("Insufficient permissions to change password for this user");
        }
        
        // Verify current password if user is changing their own password
        if (currentUser.getId().equals(id) && 
            !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setLastModifiedBy(getCurrentUsername());
        userRepository.save(user);
    }

    /**
     * Enables a user account.
     *
     * @param id the UUID of the user to enable
     * @return the updated User entity
     * @throws RuntimeException if the user is not found
     */
    public User enableUser(UUID id) {
        User user = getUserById(id);
        user.setEnabled(true);
        user.setStatus("ACTIVE");
        user.setLastModifiedBy(getCurrentUsername());
        return userRepository.save(user);
    }

    /**
     * Disables a user account.
     *
     * @param id the UUID of the user to disable
     * @return the updated User entity
     * @throws RuntimeException if the user is not found
     */
    public User disableUser(UUID id) {
        User user = getUserById(id);
        user.setEnabled(false);
        user.setStatus("INACTIVE");
        user.setLastModifiedBy(getCurrentUsername());
        return userRepository.save(user);
    }

    /**
     * Updates the role of a user.
     *
     * @param id the UUID of the user
     * @param role the new role to assign
     * @return the updated User entity
     * @throws RuntimeException if the user is not found
     */
    public User updateRole(UUID id, String role) {
        User user = getUserById(id);
        user.setRole(role);
        user.setLastModifiedBy(getCurrentUsername());
        return userRepository.save(user);
    }

    /**
     * Deletes a user by their unique identifier.
     *
     * @param id the UUID of the user to delete
     * @throws RuntimeException if the user is not found
     */
    public void deleteUser(UUID id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    /**
     * Maps fields from a UserRequest to a User entity.
     *
     * @param request the source request containing user data
     * @param user the target User entity to populate
     */
    private void mapRequestToUser(UserRequest request, User user) {
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        user.setRole(request.getRole() != null ? request.getRole() : "USER");
        user.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        user.setType(request.getType());
    }

    /**
     * Retrieves the username of the currently authenticated user.
     *
     * @return the username, or "system" if no user is authenticated
     */
    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            return ((UserPrincipal) principal).getUsername();
        }
        return "system";
    }

    /**
     * Retrieves the UserPrincipal of the currently authenticated user.
     *
     * @return the UserPrincipal for the current user
     * @throws RuntimeException if the current user cannot be determined
     */
    private UserPrincipal getCurrentUserPrincipal() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            return (UserPrincipal) principal;
        }
        throw new RuntimeException("Unable to get current user");
    }
}