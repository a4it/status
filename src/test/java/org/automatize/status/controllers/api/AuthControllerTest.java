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

    @Test
    void login_missingPassword_returns400() throws Exception {
        String body = "{\"username\":\"tester\"}";
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_rateLimited_returns429() throws Exception {
        when(loginRateLimiter.isAllowed(anyString())).thenReturn(false);

        String body = "{\"username\":\"tester\",\"password\":\"secret123\"}";
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());
    }

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

    @Test
    void register_missingEmail_returns400() throws Exception {
        String body = "{\"username\":\"tester\",\"password\":\"secret123\",\"fullName\":\"Test User\"}";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshToken_valid_returns200() throws Exception {
        when(authService.refreshToken(any())).thenReturn(sampleAuth());

        String body = "{\"refreshToken\":\"some-refresh-token\"}";
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void refreshToken_missingToken_returns400() throws Exception {
        String body = "{}";
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshToken_invalid_returns401() throws Exception {
        when(authService.refreshToken(any()))
                .thenThrow(new UnauthorizedException("Invalid refresh token"));

        String body = "{\"refreshToken\":\"bad-token\"}";
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_withHeader_returns200() throws Exception {
        when(authService.logout(anyString()))
                .thenReturn(new MessageResponse("Logged out successfully", true));

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void logout_missingHeader_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCurrentUser_valid_returns200() throws Exception {
        when(authService.getCurrentUser(anyString())).thenReturn(sampleAuth());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tester"));
    }

    @Test
    void getCurrentUser_invalid_returns401() throws Exception {
        when(authService.getCurrentUser(anyString()))
                .thenThrow(new UnauthorizedException("Invalid authorization header"));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "bad"))
                .andExpect(status().isUnauthorized());
    }
}
