package org.automatize.status.security;

import org.automatize.status.models.User;
import org.automatize.status.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Custom implementation of Spring Security's {@link UserDetailsService} interface.
 * <p>
 * This service is responsible for loading user-specific data during authentication.
 * It retrieves user information from the database and converts it into a Spring Security
 * compatible {@link UserDetails} object (specifically {@link UserPrincipal}).
 * </p>
 * <p>
 * The service supports loading users by:
 * <ul>
 *     <li>Username or email address (for standard authentication flows)</li>
 *     <li>User ID (for JWT token-based authentication)</li>
 * </ul>
 * </p>
 *
 * @see UserDetailsService
 * @see UserPrincipal
 * @see UserRepository
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * Repository for accessing user data from the database.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Loads a user by their username or email address.
     * <p>
     * This method is called by Spring Security during the authentication process.
     * It attempts to find a user matching the provided username, treating it as
     * either a username or an email address.
     * </p>
     *
     * @param username the username or email address identifying the user whose data is required
     * @return a fully populated {@link UserDetails} object containing the user's security information
     * @throws UsernameNotFoundException if no user is found with the given username or email
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with username or email : " + username)
                );

        return UserPrincipal.create(user);
    }

    /**
     * Loads a user by their unique identifier (UUID).
     * <p>
     * This method is primarily used during JWT token validation to retrieve
     * the user associated with a token. It fetches the user from the database
     * using their ID and converts it to a {@link UserPrincipal}.
     * </p>
     *
     * @param id the unique identifier (UUID) of the user to load
     * @return a fully populated {@link UserDetails} object containing the user's security information
     * @throws UsernameNotFoundException if no user is found with the given ID
     */
    @Transactional
    public UserDetails loadUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with id : " + id)
                );

        return UserPrincipal.create(user);
    }
}