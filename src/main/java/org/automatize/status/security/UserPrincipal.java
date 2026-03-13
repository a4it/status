package org.automatize.status.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.automatize.status.models.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * Custom implementation of Spring Security's {@link UserDetails} interface.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Represent authenticated users in the Spring Security context</li>
 *   <li>Encapsulate user information for authentication and authorization</li>
 *   <li>Support multi-tenant architecture with organization context</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see UserDetails
 * @see User
 */
public class UserPrincipal implements UserDetails {

    /**
     * The unique identifier of the user.
     */
    private UUID id;

    /**
     * The username used for authentication.
     */
    private String username;

    /**
     * The user's email address.
     */
    private String email;

    /**
     * The user's encrypted password. Excluded from JSON serialization for security.
     */
    @JsonIgnore
    private String password;

    /**
     * The user's role in the system (e.g., "ADMIN", "USER").
     */
    private String role;

    /**
     * The unique identifier of the organization the user belongs to.
     * Supports multi-tenant architecture.
     */
    private UUID organizationId;

    /**
     * The collection of granted authorities (permissions) for this user.
     */
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * Constructs a new UserPrincipal with all required fields.
     *
     * @param id             the unique identifier of the user
     * @param username       the username for authentication
     * @param email          the user's email address
     * @param password       the user's encrypted password
     * @param role           the user's role in the system
     * @param organizationId the identifier of the user's organization (can be null)
     * @param authorities    the collection of granted authorities for this user
     */
    public UserPrincipal(UUID id, String username, String email, String password,
                        String role, UUID organizationId, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.organizationId = organizationId;
        this.authorities = authorities;
    }

    /**
     * Factory method to create a UserPrincipal from a User entity.
     * <p>
     * This method converts a JPA User entity into a Spring Security compatible
     * UserPrincipal. It automatically generates authorities based on the user's role,
     * prefixing the role with "ROLE_" as per Spring Security conventions.
     * </p>
     * <p>
     * If the user has no role defined, it defaults to "USER".
     * </p>
     *
     * @param user the User entity to convert
     * @return a new UserPrincipal populated with the user's data
     */
    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + (user.getRole() != null ? user.getRole() : "USER"))
        );

        return new UserPrincipal(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getPassword(),
            user.getRole(),
            user.getOrganization() != null ? user.getOrganization().getId() : null,
            authorities
        );
    }

    /**
     * Returns the unique identifier of the user.
     *
     * @return the user's UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Returns the user's email address.
     *
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the user's role in the system.
     *
     * @return the role string (e.g., "ADMIN", "USER")
     */
    public String getRole() {
        return role;
    }

    /**
     * Returns the identifier of the organization the user belongs to.
     *
     * @return the organization's UUID, or null if the user is not associated with an organization
     */
    public UUID getOrganizationId() {
        return organizationId;
    }

    /**
     * Returns the username used for authentication.
     *
     * @return the username
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Returns the user's encrypted password.
     *
     * @return the encrypted password
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Returns the authorities granted to the user.
     * <p>
     * The authorities are derived from the user's role during UserPrincipal creation.
     * </p>
     *
     * @return the collection of granted authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Indicates whether the user's account has expired.
     * <p>
     * Currently always returns true, indicating accounts never expire.
     * Override this behavior if account expiration logic is required.
     * </p>
     *
     * @return true, indicating the account is not expired
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is locked or unlocked.
     * <p>
     * Currently always returns true, indicating accounts are never locked.
     * Override this behavior if account locking logic is required.
     * </p>
     *
     * @return true, indicating the account is not locked
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials (password) have expired.
     * <p>
     * Currently always returns true, indicating credentials never expire.
     * Override this behavior if credential expiration logic is required.
     * </p>
     *
     * @return true, indicating the credentials are not expired
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled or disabled.
     * <p>
     * Currently always returns true, indicating all users are enabled.
     * Override this behavior if user enable/disable logic is required.
     * </p>
     *
     * @return true, indicating the user is enabled
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}