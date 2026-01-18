package org.automatize.status.repositories;

import org.automatize.status.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <p>
 * Repository interface for managing {@link User} entities.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for user data</li>
 *   <li>Support authentication lookups by username, email, or both</li>
 *   <li>Enable searching and filtering users by organization, role, and status</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see User
 * @see OrganizationRepository
 * @see TenantRepository
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their unique username.
     *
     * @param username the username to search for
     * @return an Optional containing the user if found, or empty if not found
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by their email address.
     *
     * @param email the email address to search for
     * @return an Optional containing the user if found, or empty if not found
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by either username or email address.
     * <p>
     * Useful for login functionality where users can authenticate with either credential.
     * </p>
     *
     * @param username the username to search for
     * @param email the email address to search for
     * @return an Optional containing the user if found by either credential, or empty if not found
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Finds all users belonging to a specific organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return a list of users belonging to the specified organization
     */
    List<User> findByOrganizationId(UUID organizationId);

    /**
     * Finds all users by their enabled status.
     *
     * @param enabled true to find enabled users, false to find disabled ones
     * @return a list of users matching the enabled status
     */
    List<User> findByEnabled(Boolean enabled);

    /**
     * Finds all users with a specific role.
     *
     * @param role the role to filter by (e.g., "ADMIN", "USER", "VIEWER")
     * @return a list of users with the specified role
     */
    List<User> findByRole(String role);

    /**
     * Finds all users with a specific status.
     *
     * @param status the status to filter by (e.g., "ACTIVE", "INACTIVE", "PENDING")
     * @return a list of users matching the specified status
     */
    List<User> findByStatus(String status);

    /**
     * Finds all users belonging to an organization with a specific enabled status.
     *
     * @param organizationId the unique identifier of the organization
     * @param enabled true to find enabled users, false to find disabled ones
     * @return a list of users matching both the organization and enabled criteria
     */
    List<User> findByOrganizationIdAndEnabled(UUID organizationId, Boolean enabled);

    /**
     * Finds all users belonging to an organization with a specific role.
     *
     * @param organizationId the unique identifier of the organization
     * @param role the role to filter by
     * @return a list of users matching both the organization and role criteria
     */
    List<User> findByOrganizationIdAndRole(UUID organizationId, String role);

    /**
     * Finds all users within a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a list of users within the specified tenant
     */
    @Query("SELECT u FROM User u WHERE u.organization.tenant.id = :tenantId")
    List<User> findByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Searches for users within an organization by username, email, or full name containing the search term.
     *
     * @param organizationId the unique identifier of the organization to search within
     * @param searchTerm the term to search for in username, email, or full name
     * @return a list of users matching the search criteria within the specified organization
     */
    @Query("SELECT u FROM User u WHERE u.organization.id = :organizationId AND (u.username LIKE %:searchTerm% OR u.email LIKE %:searchTerm% OR u.fullName LIKE %:searchTerm%)")
    List<User> searchByOrganizationId(@Param("organizationId") UUID organizationId, @Param("searchTerm") String searchTerm);

    /**
     * Searches for users globally by username, email, or full name containing the search term.
     *
     * @param searchTerm the term to search for in username, email, or full name
     * @return a list of users matching the search criteria
     */
    @Query("SELECT u FROM User u WHERE u.username LIKE %:searchTerm% OR u.email LIKE %:searchTerm% OR u.fullName LIKE %:searchTerm%")
    List<User> search(@Param("searchTerm") String searchTerm);

    /**
     * Counts the total number of users belonging to a specific organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return the count of users within the specified organization
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.organization.id = :organizationId")
    Long countByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Counts the total number of users within a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return the count of users within the specified tenant
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.organization.tenant.id = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Checks if a user with the specified username already exists.
     *
     * @param username the username to check for existence
     * @return true if a user with the username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Checks if a user with the specified email already exists.
     *
     * @param email the email to check for existence
     * @return true if a user with the email exists, false otherwise
     */
    boolean existsByEmail(String email);
}