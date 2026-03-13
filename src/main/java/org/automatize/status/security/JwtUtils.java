package org.automatize.status.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * <p>
 * Utility class for JSON Web Token (JWT) operations.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Generate access and refresh tokens with user claims</li>
 *   <li>Validate JWT tokens for authenticity and expiration</li>
 *   <li>Extract claims and user information from tokens</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see io.jsonwebtoken.Jwts
 */
@Component
public class JwtUtils {

    /**
     * Logger instance for recording JWT-related events and errors.
     */
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    /**
     * Base64-encoded secret key used for signing and verifying JWT tokens.
     * Configured via the {@code jwt.secret} application property.
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Access token expiration time in milliseconds.
     * Configured via the {@code jwt.expiration} application property.
     */
    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    /**
     * Refresh token expiration time in milliseconds.
     * Configured via the {@code jwt.refresh.expiration} application property.
     */
    @Value("${jwt.refresh.expiration}")
    private int refreshTokenExpirationMs;

    /**
     * Creates the cryptographic signing key from the configured secret.
     * <p>
     * The secret is expected to be Base64-encoded and is decoded to create
     * an HMAC-SHA key suitable for JWT signing operations.
     * </p>
     *
     * @return the signing key derived from the configured JWT secret
     */
    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    /**
     * Generates a JWT access token from a Spring Security Authentication object.
     * <p>
     * This method extracts user details from the {@link UserPrincipal} contained
     * in the authentication object and creates a token with all relevant user claims.
     * </p>
     *
     * @param authentication the Spring Security authentication object containing the user principal
     * @return a signed JWT access token string
     */
    public String generateJwtToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername()))
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .claim("userId", userPrincipal.getId())
                .claim("email", userPrincipal.getEmail())
                .claim("organizationId", userPrincipal.getOrganizationId())
                .claim("role", userPrincipal.getRole())
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates a JWT access token from explicit user attributes.
     * <p>
     * This method is useful when creating tokens outside of the normal authentication
     * flow, such as during token refresh or programmatic authentication.
     * </p>
     *
     * @param userId         the unique identifier of the user
     * @param username       the username to set as the token subject
     * @param email          the user's email address
     * @param organizationId the user's organization identifier (can be null)
     * @param role           the user's role in the system
     * @return a signed JWT access token string
     */
    public String generateJwtTokenFromUserId(UUID userId, String username, String email, UUID organizationId, String role) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .claim("userId", userId.toString())
                .claim("email", email)
                .claim("organizationId", organizationId != null ? organizationId.toString() : null)
                .claim("role", role)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates a refresh token for the specified username.
     * <p>
     * Refresh tokens have a longer expiration time than access tokens and contain
     * only the username as the subject. They are used to obtain new access tokens
     * without requiring the user to re-authenticate.
     * </p>
     *
     * @param username the username to include in the refresh token
     * @return a signed JWT refresh token string
     */
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + refreshTokenExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the username (subject) from a JWT token.
     *
     * @param token the JWT token string to parse
     * @return the username stored in the token's subject claim
     * @throws JwtException if the token is invalid or cannot be parsed
     */
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser().verifyWith((SecretKey)key()).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    /**
     * Extracts the user ID from a JWT token.
     *
     * @param token the JWT token string to parse
     * @return the user's UUID from the token's userId claim, or null if not present
     * @throws JwtException if the token is invalid or cannot be parsed
     */
    public UUID getUserIdFromJwtToken(String token) {
        Claims claims = Jwts.parser().verifyWith((SecretKey)key()).build()
                .parseSignedClaims(token).getPayload();
        String userId = claims.get("userId", String.class);
        return userId != null ? UUID.fromString(userId) : null;
    }

    /**
     * Validates a JWT token for authenticity and expiration.
     * <p>
     * This method attempts to parse and verify the token signature. It handles
     * various JWT-related exceptions and logs appropriate error messages for each case.
     * </p>
     * <p>
     * The following validation checks are performed:
     * <ul>
     *     <li>Token format and structure (MalformedJwtException)</li>
     *     <li>Token expiration (ExpiredJwtException)</li>
     *     <li>Token type support (UnsupportedJwtException)</li>
     *     <li>Token content presence (IllegalArgumentException)</li>
     * </ul>
     * </p>
     *
     * @param authToken the JWT token string to validate
     * @return true if the token is valid and not expired, false otherwise
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith((SecretKey)key()).build().parse(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Extracts all claims from a JWT token.
     * <p>
     * This method provides access to the complete claims set contained in the token,
     * allowing retrieval of any custom claims beyond the standard ones.
     * </p>
     *
     * @param token the JWT token string to parse
     * @return a {@link Claims} object containing all claims from the token
     * @throws JwtException if the token is invalid or cannot be parsed
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().verifyWith((SecretKey)key()).build()
                .parseSignedClaims(token).getPayload();
    }
}