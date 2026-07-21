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
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}.
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Computes the SHA-256 hex digest exactly as AuthService.hashToken does,
     * so the stored refresh-token hash can be reproduced in tests.
     */
    private static String sha256Hex(String token) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

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

    @Test
    void authenticateUser_validCredentials_returnsAuthResponseAndStoresHashedRefreshToken() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "tester", "USER");
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        LoginRequest request = new LoginRequest();
        request.setUsername("tester");
        request.setPassword("secret");

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtils.generateJwtToken(auth)).thenReturn("access-jwt");
        when(jwtUtils.generateRefreshToken("tester")).thenReturn("refresh-plain");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.authenticateUser(request);

        assertThat(response.getAccessToken()).isEqualTo("access-jwt");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-plain");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getUsername()).isEqualTo("tester");
        assertThat(response.isRequiresContextSelection()).isFalse();
        // plaintext must never be persisted
        assertThat(user.getRefreshToken()).isNotEqualTo("refresh-plain");
        verify(userRepository).save(user);
    }

    @Test
    void authenticateUser_superadmin_setsRequiresContextSelection() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "root", "SUPERADMIN");
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        LoginRequest request = new LoginRequest();
        request.setUsername("root");
        request.setPassword("secret");

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtils.generateJwtToken(auth)).thenReturn("access-jwt");
        when(jwtUtils.generateRefreshToken("root")).thenReturn("refresh-plain");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.authenticateUser(request);

        assertThat(response.isRequiresContextSelection()).isTrue();
    }

    @Test
    void authenticateUser_userNotFoundAfterAuth_throwsRuntimeException() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "tester", "USER");
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        LoginRequest request = new LoginRequest();
        request.setUsername("tester");
        request.setPassword("secret");

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtils.generateJwtToken(auth)).thenReturn("access-jwt");
        when(jwtUtils.generateRefreshToken("tester")).thenReturn("refresh-plain");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticateUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

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

    @Test
    void registerUser_emailInUse_returnsUnsuccessfulResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("dup@b.com");
        request.setPassword("pw");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("dup@b.com")).thenReturn(true);

        MessageResponse response = authService.registerUser(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Email is already in use!");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_validRequest_savesUserWithForcedUserRole() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@b.com");
        request.setPassword("pw");
        request.setFullName("New User");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@b.com")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = authService.registerUser(request);

        assertThat(response.isSuccess()).isTrue();
        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo("USER");
        assertThat(saved.getPassword()).isEqualTo("hashed");
        assertThat(saved.getEnabled()).isTrue();
    }

    @Test
    void registerUser_withOrganizationId_associatesOrganization() {
        UUID orgId = UUID.randomUUID();
        Organization org = new Organization();
        org.setId(orgId);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@b.com");
        request.setPassword("pw");
        request.setOrganizationId(orgId);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@b.com")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = authService.registerUser(request);

        assertThat(response.isSuccess()).isTrue();
        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getOrganization()).isEqualTo(org);
    }

    @Test
    void registerUser_organizationNotFound_throwsRuntimeException() {
        UUID orgId = UUID.randomUUID();

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@b.com");
        request.setPassword("pw");
        request.setOrganizationId(orgId);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@b.com")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Organization not found");
    }

    @Test
    void refreshToken_invalidToken_throwsUnauthorizedException() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("bad");

        when(jwtUtils.validateJwtToken("bad")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid refresh token");
    }

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

    @Test
    void refreshToken_tokenMismatch_throwsUnauthorizedException() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "tester", "USER");
        user.setRefreshToken("some-other-hash");

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("token");

        when(jwtUtils.validateJwtToken("token")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("token")).thenReturn("tester");
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token mismatch");
    }

    @Test
    void refreshToken_validMatchingToken_returnsNewTokensAndRotatesRefresh() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "tester", "USER");
        user.setRefreshToken(sha256Hex("token"));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("token");

        when(jwtUtils.validateJwtToken("token")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("token")).thenReturn("tester");
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(user));
        when(jwtUtils.generateJwtTokenFromUserId(any(), anyString(), anyString(), any(), anyString()))
                .thenReturn("new-access");
        when(jwtUtils.generateRefreshToken("tester")).thenReturn("new-refresh");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        // stored hash rotated to hash of the new refresh token
        assertThat(user.getRefreshToken()).isEqualTo(sha256Hex("new-refresh"));
        verify(userRepository).save(user);
    }

    @Test
    void logout_nullToken_returnsUnsuccessfulResponse() {
        MessageResponse response = authService.logout(null);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Invalid authorization header");
    }

    @Test
    void logout_nonBearerToken_returnsUnsuccessfulResponse() {
        MessageResponse response = authService.logout("Basic xyz");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Invalid authorization header");
    }

    @Test
    void logout_validToken_clearsRefreshTokenAndReturnsSuccess() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "tester", "USER");
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

    @Test
    void logout_userNotFound_returnsErrorResponse() {
        UUID userId = UUID.randomUUID();

        when(jwtUtils.getUserIdFromJwtToken("jwt-value")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        MessageResponse response = authService.logout("Bearer jwt-value");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Error during logout");
    }

    @Test
    void getCurrentUser_nullToken_throwsUnauthorizedException() {
        assertThatThrownBy(() -> authService.getCurrentUser(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid authorization header");
    }

    @Test
    void getCurrentUser_validToken_returnsUserDetails() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "tester", "ADMIN");

        when(jwtUtils.getUserIdFromJwtToken("jwt-value")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        AuthResponse response = authService.getCurrentUser("Bearer jwt-value");

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getUsername()).isEqualTo("tester");
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

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
