package org.automatize.status.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * <p>
 * JWT authentication filter that intercepts incoming HTTP requests to validate JWT tokens.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Extract JWT tokens from Authorization headers</li>
 *   <li>Validate tokens and establish Spring Security context</li>
 *   <li>Skip processing for static resources</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see OncePerRequestFilter
 * @see JwtUtils
 * @see CustomUserDetailsService
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Utility class for JWT token operations including generation and validation.
     */
    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Service for loading user details from the database.
     */
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    /**
     * Logger instance for recording authentication events and errors.
     */
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /**
     * Processes each incoming HTTP request to validate JWT tokens and establish authentication.
     * <p>
     * This method first checks if the request is for a static resource and skips JWT processing
     * if so. For other requests, it extracts the JWT token from the Authorization header,
     * validates it, retrieves the associated user, and sets up the Spring Security context.
     * </p>
     * <p>
     * If any exception occurs during processing, it is logged and the request continues
     * without authentication (allowing the security configuration to handle unauthorized access).
     * </p>
     *
     * @param request     the HTTP servlet request being processed
     * @param response    the HTTP servlet response
     * @param filterChain the filter chain for passing the request to the next filter
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs during request processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip JWT processing for static resources
        String path = request.getRequestURI();
        if (path.contains("/webjars/") || path.endsWith(".css") || path.endsWith(".js") ||
            path.endsWith(".html") || path.endsWith(".png") || path.endsWith(".ico")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the HTTP request's Authorization header.
     * <p>
     * This method looks for an Authorization header with the "Bearer " prefix
     * and extracts the token portion. If no valid Bearer token is found,
     * it returns null.
     * </p>
     *
     * @param request the HTTP servlet request containing the Authorization header
     * @return the JWT token string without the "Bearer " prefix, or null if not found
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}