package org.automatize.status.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtAuthenticationEntryPoint}.
 */
class JwtAuthenticationEntryPointTest {

    private JwtAuthenticationEntryPoint entryPoint;

    /**
     * Minimal concrete {@link AuthenticationException} used to drive the entry
     * point, since {@code AuthenticationException} is abstract.
     */
    private static class TestAuthException extends AuthenticationException {
        /**
         * Creates the test exception with the given message.
         *
         * @param msg the authentication failure message
         */
        TestAuthException(String msg) {
            super(msg);
        }
    }

    /**
     * Creates a fresh {@link JwtAuthenticationEntryPoint} (with a real
     * {@link ObjectMapper}) before each test.
     */
    @BeforeEach
    void setUp() {
        entryPoint = new JwtAuthenticationEntryPoint(new ObjectMapper());
    }

    /**
     * Verifies that for an {@code /api/**} request the entry point responds with
     * 401, a JSON content type, and a body containing the status, error, message,
     * and path fields.
     *
     * @throws Exception if writing the response fails
     */
    @Test
    void commence_apiPath_returns401JsonBody() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/secure");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException ex = new TestAuthException("Full authentication is required");

        // Act
        entryPoint.commence(request, response, ex);

        // Assert
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        String body = response.getContentAsString();
        assertThat(body)
                .contains("\"status\":401")
                .contains("\"error\":\"Unauthorized\"")
                .contains("\"message\":\"Full authentication is required\"")
                .contains("\"path\":\"/api/secure\"");
    }

    /**
     * Verifies that for a non-API request the entry point redirects to
     * {@code /login?session_expired=true} and writes no response body.
     *
     * @throws Exception if writing the response fails
     */
    @Test
    void commence_nonApiPath_redirectsToLogin() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException ex = new TestAuthException("no session");

        // Act
        entryPoint.commence(request, response, ex);

        // Assert
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?session_expired=true");
        assertThat(response.getContentAsString()).isEmpty();
    }
}
