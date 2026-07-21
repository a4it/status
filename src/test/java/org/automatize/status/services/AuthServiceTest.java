package org.automatize.status.services;

import org.automatize.status.api.request.LoginRequest;
import org.automatize.status.api.request.RefreshTokenRequest;
import org.automatize.status.api.request.RegisterRequest;
import org.automatize.status.api.response.AuthResponse;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.exceptions.UnauthorizedException;
import org.automatize.status.models.Organization;
import org.automatize.status.models.User;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.UserRepository;
import org.automatize.status.security.JwtUtils;
import org.automatize.status.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}.
 *
 * <p>Testing approach: Mockito unit tests covering authentication, registration,
 * refresh-token rotation, logout and current-user resolution. Collaborators
 * (authentication manager, repositories, password encoder and {@link JwtUtils}) are
 * mocked, and the Spring {@link SecurityContextHolder} is cleared after each test.
 * Refresh-token hashing is reproduced via a local SHA-256 helper so the stored hash
 * can be asserted without invoking the private production method.</p>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    /**
     * Clears the Spring security context after each test to prevent authentication
     * state leaking between test methods.
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Computes the SHA-256 hex digest exactly as AuthService.hashToken does,
     * so the stored refresh-token hash can be reproduced in tests.
     */
    private static String sha256Hex(String token) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    /**
     * Builds an enabled {@link User} fixture with a derived email and encoded password.
     *
     * @param id       the user id to assign
     * @param username the username (also used to derive the email)
     * @param role     the role to assign
     * @return a populated, enabled user fixture
     */
    private User buildUser(UUID id, String username, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setRole(role);
        user.setPassword("encoded");
        user.setEnabled(true);
        return user;
    }

    /**
     * Verifies successful authentication returns a Bearer auth response with access and
     * refresh tokens, and persists the refresh token as a hash (never plaintext).
     * Expects a non-context-selection response for a regular user and a save of the user.
     */
    @Test
    void authenticateUser_validCredentials_returnsAuthResponseAndStoresHashedRefreshToken() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, USERNAME, "USER");
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        LoginRequest request = new LoginRequest();
        request.setUsername(USERNAME);
        request.setPassword(SECRET);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtils.generateJwtToken(auth)).thenReturn(ACCESS_JWT);
        when(jwtUtils.generateRefreshToken(USERNAME)).thenReturn(REFRESH_PLAIN);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.authenticateUser(request);

        assertThat(response.getAccessToken()).isEqualTo(ACCESS_JWT);
        assertThat(response.getRefreshToken()).isEqualTo(REFRESH_PLAIN);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getUsername()).isEqualTo(USERNAME);
        assertThat(response.isRequiresContextSelection()).isFalse();
        // plaintext must never be persisted
        assertThat(user.getRefreshToken()).isNotEqualTo(REFRESH_PLAIN);
        verify(userRepository).save(user);
    }

    /**
     * Verifies that authenticating a SUPERADMIN flags the response for tenant/context
     * selection. Expects {@code requiresContextSelection} to be true.
     */
    @Test
    void authenticateUser_superadmin_setsRequiresContextSelection() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "root", "SUPERADMIN");
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        LoginRequest request = new LoginRequest();
        request.setUsername("root");
        request.setPassword(SECRET);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtils.generateJwtToken(auth)).thenReturn(ACCESS_JWT);
        when(jwtUtils.generateRefreshToken("root")).thenReturn(REFRESH_PLAIN);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.authenticateUser(request);

        assertThat(response.isRequiresContextSelection()).isTrue();
    }

    /**
     * Verifies that if the user cannot be reloaded from the repository after successful
     * authentication, the service throws. Expects a {@link RuntimeException} "User not found".
     */
    @Test
    void authenticateUser_userNotFoundAfterAuth_throwsRuntimeException() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, USERNAME, "USER");
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        LoginRequest request = new LoginRequest();
        request.setUsername(USERNAME);
        request.setPassword(SECRET);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtils.generateJwtToken(auth)).thenReturn(ACCESS_JWT);
        when(jwtUtils.generateRefreshToken(USERNAME)).thenReturn(REFRESH_PLAIN);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticateUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    /**
     * Verifies registration is rejected when the username already exists.
     * Expects an unsuccessful message response and no user save.
     */
    @Test
    void registerUser_usernameTaken_returnsUnsuccessfulResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("taken");
        request.setEmail("a@b.com");
        request.setPassword("pw");

        when(userRepository.existsByUsername("taken")).thenReturn(true);

        MessageResponse response = authService.registerUser(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Username is already taken!");
        verify(userRepository, never()).save(any());
    }

    /**
     * Verifies registration is rejected when the email is already in use.
     * Expects an unsuccessful message response and no user save.
     */
    @Test
    void registerUser_emailInUse_returnsUnsuccessfulResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(NEW_USERNAME);
        request.setEmail("dup@b.com");
        request.setPassword("pw");

        when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(false);
        when(userRepository.existsByEmail("dup@b.com")).thenReturn(true);

        MessageResponse response = authService.registerUser(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Email is already in use!");
        verify(userRepository, never()).save(any());
    }

    /**
     * Verifies a valid registration saves the user with the password encoded, the role
     * forced to USER, and the account enabled. Expects a successful response.
     */
    @Test
    void registerUser_validRequest_savesUserWithForcedUserRole() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(NEW_USERNAME);
        request.setEmail(NEW_EMAIL);
        request.setPassword("pw");
        request.setFullName("New User");

        when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(false);
        when(userRepository.existsByEmail(NEW_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = authService.registerUser(request);

        assertThat(response.isSuccess()).isTrue();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo("USER");
        assertThat(saved.getPassword()).isEqualTo(HASHED_PASSWORD);
        assertThat(saved.getEnabled()).isTrue();
    }

    /**
     * Verifies that when an organization id is supplied, the resolved organization is
     * associated with the new user. Expects a successful response and the org set on the saved user.
     */
    @Test
    void registerUser_withOrganizationId_associatesOrganization() {
        UUID orgId = UUID.randomUUID();
        Organization org = new Organization();
        org.setId(orgId);

        RegisterRequest request = new RegisterRequest();
        request.setUsername(NEW_USERNAME);
        request.setEmail(NEW_EMAIL);
        request.setPassword("pw");
        request.setOrganizationId(orgId);

        when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(false);
        when(userRepository.existsByEmail(NEW_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn(HASHED_PASSWORD);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = authService.registerUser(request);

        assertThat(response.isSuccess()).isTrue();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getOrganization()).isEqualTo(org);
    }

    /**
     * Verifies that a supplied organization id which cannot be resolved causes a failure.
     * Expects a {@link RuntimeException} "Organization not found".
     */
    @Test
    void registerUser_organizationNotFound_throwsRuntimeException() {
        UUID orgId = UUID.randomUUID();

        RegisterRequest request = new RegisterRequest();
        request.setUsername(NEW_USERNAME);
        request.setEmail(NEW_EMAIL);
        request.setPassword("pw");
        request.setOrganizationId(orgId);

        when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(false);
        when(userRepository.existsByEmail(NEW_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn(HASHED_PASSWORD);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Organization not found");
    }

    /**
     * Verifies that refreshing with a token that fails JWT validation is rejected.
     * Expects an {@link UnauthorizedException} "Invalid refresh token".
     */
    @Test
    void refreshToken_invalidToken_throwsUnauthorizedException() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("bad");

        when(jwtUtils.validateJwtToken("bad")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid refresh token");
    }

    /**
     * Verifies that a valid token whose username resolves to no user is rejected.
     * Expects a {@link RuntimeException} "User not found".
     */
    @Test
    void refreshToken_userNotFound_throwsRuntimeException() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("token");

        when(jwtUtils.validateJwtToken("token")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("token")).thenReturn("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    /**
     * Verifies that a valid token whose hash does not match the stored hash is rejected.
     * Expects an {@link UnauthorizedException} "Refresh token mismatch".
     */
    @Test
    void refreshToken_tokenMismatch_throwsUnauthorizedException() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, USERNAME, "USER");
        user.setRefreshToken("some-other-hash");

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("token");

        when(jwtUtils.validateJwtToken("token")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("token")).thenReturn(USERNAME);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token mismatch");
    }

    /**
     * Verifies that a valid, matching refresh token yields new access and refresh tokens
     * and rotates the stored hash to the hash of the new refresh token. Expects a save of the user.
     *
     * @throws Exception if SHA-256 hashing in the test helper fails
     */
    @Test
    void refreshToken_validMatchingToken_returnsNewTokensAndRotatesRefresh() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, USERNAME, "USER");
        user.setRefreshToken(sha256Hex("token"));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("token");

        when(jwtUtils.validateJwtToken("token")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("token")).thenReturn(USERNAME);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(jwtUtils.generateJwtTokenFromUserId(any(), anyString(), anyString(), any(), anyString()))
                .thenReturn("new-access");
        when(jwtUtils.generateRefreshToken(USERNAME)).thenReturn("new-refresh");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        // stored hash rotated to hash of the new refresh token
        assertThat(user.getRefreshToken()).isEqualTo(sha256Hex("new-refresh"));
        verify(userRepository).save(user);
    }

    /**
     * Verifies logout with a null authorization header is rejected.
     * Expects an unsuccessful "Invalid authorization header" response.
     */
    @Test
    void logout_nullToken_returnsUnsuccessfulResponse() {
        MessageResponse response = authService.logout(null);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Invalid authorization header");
    }

    /**
     * Verifies logout with a non-Bearer authorization header is rejected.
     * Expects an unsuccessful "Invalid authorization header" response.
     */
    @Test
    void logout_nonBearerToken_returnsUnsuccessfulResponse() {
        MessageResponse response = authService.logout("Basic xyz");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Invalid authorization header");
    }

    /**
     * Verifies logout with a valid Bearer token clears the stored refresh token and saves.
     * Expects a successful response and a null refresh token on the user.
     */
    @Test
    void logout_validToken_clearsRefreshTokenAndReturnsSuccess() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, USERNAME, "USER");
        user.setRefreshToken("stored");

        when(jwtUtils.getUserIdFromJwtToken("jwt-value")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = authService.logout("Bearer jwt-value");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Logged out successfully");
        assertThat(user.getRefreshToken()).isNull();
        verify(userRepository).save(user);
    }

    /**
     * Verifies logout when the token's user cannot be found returns an error response
     * rather than throwing. Expects an unsuccessful "Error during logout" response.
     */
    @Test
    void logout_userNotFound_returnsErrorResponse() {
        UUID userId = UUID.randomUUID();

        when(jwtUtils.getUserIdFromJwtToken("jwt-value")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        MessageResponse response = authService.logout("Bearer jwt-value");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Error during logout");
    }

    /**
     * Verifies that requesting the current user with a null authorization header is rejected.
     * Expects an {@link UnauthorizedException} "Invalid authorization header".
     */
    @Test
    void getCurrentUser_nullToken_throwsUnauthorizedException() {
        assertThatThrownBy(() -> authService.getCurrentUser(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid authorization header");
    }

    /**
     * Verifies that a valid Bearer token returns the resolved user's details.
     * Expects the user id, username and role to be reflected in the response.
     */
    @Test
    void getCurrentUser_validToken_returnsUserDetails() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, USERNAME, "ADMIN");

        when(jwtUtils.getUserIdFromJwtToken("jwt-value")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        AuthResponse response = authService.getCurrentUser("Bearer jwt-value");

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getUsername()).isEqualTo(USERNAME);
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    /**
     * Verifies that a valid token whose user id resolves to no user throws.
     * Expects a {@link RuntimeException} "User not found".
     */
    @Test
    void getCurrentUser_userNotFound_throwsRuntimeException() {
        UUID userId = UUID.randomUUID();

        when(jwtUtils.getUserIdFromJwtToken("jwt-value")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser("Bearer jwt-value"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }
}
