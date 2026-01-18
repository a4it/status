package org.automatize.status.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Custom authentication entry point for handling unauthorized access attempts.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Handle unauthenticated access to protected resources</li>
 *   <li>Return JSON error responses for REST API authentication</li>
 *   <li>Log unauthorized access attempts for security monitoring</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see AuthenticationEntryPoint
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * Logger instance for recording unauthorized access attempts.
     */
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    /**
     * Handles unauthorized access attempts by returning a JSON error response.
     * <p>
     * This method is called whenever an {@link AuthenticationException} is thrown during
     * the authentication process. It logs the error and sends a structured JSON response
     * to the client with details about the authentication failure.
     * </p>
     *
     * @param request       the HTTP request that resulted in an authentication failure
     * @param response      the HTTP response to be populated with the error information
     * @param authException the exception that was thrown during authentication, containing
     *                      details about why authentication failed
     * @throws IOException      if an input or output error occurs while writing the response
     * @throws ServletException if a servlet-specific error occurs
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        logger.error("Unauthorized error: {}", authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", authException.getMessage());
        body.put("path", request.getServletPath());

        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }
}