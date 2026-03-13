package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.LoginRequest;
import org.automatize.status.api.request.RefreshTokenRequest;
import org.automatize.status.api.request.RegisterRequest;
import org.automatize.status.api.response.AuthResponse;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * REST API controller for authentication operations.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Handle user login and JWT token generation</li>
 *   <li>Process user registration and token refresh requests</li>
 *   <li>Manage logout and current user information retrieval</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see AuthService
 * @see AuthResponse
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Value("${app.registration.enabled:true}")
    private boolean registrationEnabled;

    @Autowired
    private AuthService authService;

    /**
     * Authenticates a user with their credentials and returns JWT tokens.
     *
     * @param loginRequest the login request containing username/email and password
     * @return ResponseEntity containing the authentication response with access and refresh tokens
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Registers a new user in the system.
     * <p>
     * Registration can be enabled or disabled via the {@code app.registration.enabled} property.
     * When disabled, this endpoint returns a 403 Forbidden response.
     * </p>
     *
     * @param registerRequest the registration request containing user details
     * @return ResponseEntity containing a message response indicating success or failure
     */
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        if (!registrationEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("Registration is currently disabled.", false));
        }
        MessageResponse response = authService.registerUser(registerRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     *
     * @param refreshTokenRequest the request containing the refresh token
     * @return ResponseEntity containing new authentication tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        AuthResponse response = authService.refreshToken(refreshTokenRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Logs out the current user by invalidating their authentication token.
     *
     * @param token the Authorization header containing the Bearer token to invalidate
     * @return ResponseEntity containing a message response confirming logout
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestHeader("Authorization") String token) {
        MessageResponse response = authService.logout(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the currently authenticated user's information.
     *
     * @param token the Authorization header containing the Bearer token
     * @return ResponseEntity containing the current user's authentication details
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@RequestHeader("Authorization") String token) {
        AuthResponse response = authService.getCurrentUser(token);
        return ResponseEntity.ok(response);
    }
}