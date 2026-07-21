package org.automatize.status.controllers.api;

import org.automatize.status.api.response.AuthResponse;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.exceptions.UnauthorizedException;
import org.automatize.status.security.LoginRateLimiter;
import org.automatize.status.services.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link AuthController}. Security filters are disabled
 * ({@code addFilters = false}); focus is request mapping, bean validation (400),
 * JSON contract, {@code @ResponseStatus} exception mapping (401), rate limiting
 * (429), and delegation to the (mocked) service layer.
 */
@WebMvcTest(controllers = AuthController.class)
class AuthControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private LoginRateLimiter loginRateLimiter;

    /**
     * Builds a representative {@link AuthResponse} fixture for stubbing successful
     * authentication, refresh, and current-user responses.
     *
     * @return a sample auth response with tokens and user details populated
     */
    private AuthResponse sampleAuth() {
        AuthResponse r = new AuthResponse();
        r.setAccessToken("access-token");
        r.setRefreshToken("refresh-token");
        r.setTokenType("Bearer");
        r.setUserId(UUID.randomUUID());
        r.setUsername("tester");
        r.setEmail("tester@example.com");
        r.setRole("USER");
        return r;
    }

    /**
     * Verifies that POST {@code /api/auth/login} with valid credentials that pass
     * rate limiting returns HTTP 200 and the issued tokens.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void login_valid_returns200() throws Exception {
        when(loginRateLimiter.isAllowed(anyString())).thenReturn(true);
        when(authService.authenticateUser(any())).thenReturn(sampleAuth());

        String body = "{\"username\":\"tester\",\"password\":\"secret123\"}";
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.username").value("tester"));
    }

    /**
     * Verifies that POST {@code /api/auth/login} without a password fails bean
     * validation with HTTP 400.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void login_missingPassword_returns400() throws Exception {
        String body = "{\"username\":\"tester\"}";
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that POST {@code /api/auth/login} returns HTTP 429 when the rate
     * limiter rejects the request.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void login_rateLimited_returns429() throws Exception {
        when(loginRateLimiter.isAllowed(anyString())).thenReturn(false);

        String body = "{\"username\":\"tester\",\"password\":\"secret123\"}";
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());
    }

    /**
     * Verifies that POST {@code /api/auth/register} with a valid body returns HTTP
     * 200 and a success message.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void register_valid_returns200() throws Exception {
        when(authService.registerUser(any()))
                .thenReturn(new MessageResponse("User registered successfully!", true));

        String body = "{\"username\":\"tester\",\"password\":\"secret123\","
                + "\"email\":\"tester@example.com\",\"fullName\":\"Test User\"}";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * Verifies that POST {@code /api/auth/register} without an email fails bean
     * validation with HTTP 400.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void register_missingEmail_returns400() throws Exception {
        String body = "{\"username\":\"tester\",\"password\":\"secret123\",\"fullName\":\"Test User\"}";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that POST {@code /api/auth/refresh} with a valid refresh token
     * returns HTTP 200 and a new access token.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void refreshToken_valid_returns200() throws Exception {
        when(authService.refreshToken(any())).thenReturn(sampleAuth());

        String body = "{\"refreshToken\":\"some-refresh-token\"}";
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    /**
     * Verifies that POST {@code /api/auth/refresh} without a token fails bean
     * validation with HTTP 400.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void refreshToken_missingToken_returns400() throws Exception {
        String body = "{}";
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that POST {@code /api/auth/refresh} returns HTTP 401 when the
     * service rejects the token with {@link UnauthorizedException}.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void refreshToken_invalid_returns401() throws Exception {
        when(authService.refreshToken(any()))
                .thenThrow(new UnauthorizedException("Invalid refresh token"));

        String body = "{\"refreshToken\":\"bad-token\"}";
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Verifies that POST {@code /api/auth/logout} with an Authorization header
     * returns HTTP 200 and a success message.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void logout_withHeader_returns200() throws Exception {
        when(authService.logout(anyString()))
                .thenReturn(new MessageResponse("Logged out successfully", true));

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * Verifies that POST {@code /api/auth/logout} without an Authorization header
     * returns HTTP 400.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void logout_missingHeader_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that GET {@code /api/auth/me} with a valid Authorization header
     * returns HTTP 200 and the current user's details.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void getCurrentUser_valid_returns200() throws Exception {
        when(authService.getCurrentUser(anyString())).thenReturn(sampleAuth());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tester"));
    }

    /**
     * Verifies that GET {@code /api/auth/me} returns HTTP 401 when the service
     * rejects the header with {@link UnauthorizedException}.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void getCurrentUser_invalid_returns401() throws Exception {
        when(authService.getCurrentUser(anyString()))
                .thenThrow(new UnauthorizedException("Invalid authorization header"));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "bad"))
                .andExpect(status().isUnauthorized());
    }
}
