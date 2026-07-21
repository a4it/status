package org.automatize.status.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtUtils}.
 */
@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    private static final String JWT_SECRET =
            "dGhpc2lzYXNlY3JldGtleWZvcmp3dGF1dGhlbnRpY2F0aW9uYW5kYXV0aG9yaXphdGlvbjEyMw==";
    private static final long EXPIRATION_MS = 86_400_000L;
    private static final long REFRESH_EXPIRATION_MS = 604_800_000L;

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtUtils, "refreshTokenExpirationMs", REFRESH_EXPIRATION_MS);
    }

    @Test
    void generateJwtTokenFromUserId_validInput_returnsParsableToken() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        // Act
        String token = jwtUtils.generateJwtTokenFromUserId(userId, "jdoe", "jdoe@x.com", orgId, "USER");

        // Assert
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
        assertThat(jwtUtils.getUserNameFromJwtToken(token)).isEqualTo("jdoe");
        assertThat(jwtUtils.getUserIdFromJwtToken(token)).isEqualTo(userId);
        assertThat(jwtUtils.getOrganizationIdFromJwtToken(token)).isEqualTo(orgId);
    }

    @Test
    void generateRefreshToken_validUsername_returnsTokenWithSubject() {
        // Act
        String token = jwtUtils.generateRefreshToken("jdoe");

        // Assert
        assertThat(token).isNotBlank();
        assertThat(jwtUtils.getUserNameFromJwtToken(token)).isEqualTo("jdoe");
    }

    @Test
    void validateJwtToken_validToken_returnsTrue() {
        // Arrange
        String token = jwtUtils.generateJwtTokenFromUserId(
                UUID.randomUUID(), "jdoe", "jdoe@x.com", null, "USER");

        // Act & Assert
        assertThat(jwtUtils.validateJwtToken(token)).isTrue();
    }

    @Test
    void validateJwtToken_expiredToken_returnsFalse() {
        // Arrange: negative expiry so the token is already expired at creation
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", -10_000L);
        String expired = jwtUtils.generateJwtTokenFromUserId(
                UUID.randomUUID(), "jdoe", "jdoe@x.com", null, "USER");

        // Act & Assert
        assertThat(jwtUtils.validateJwtToken(expired)).isFalse();
    }

    @Test
    void validateJwtToken_tamperedSignature_returnsFalse() {
        // Arrange
        String token = jwtUtils.generateJwtTokenFromUserId(
                UUID.randomUUID(), "jdoe", "jdoe@x.com", null, "USER");
        // Flip the last character of the signature segment
        char last = token.charAt(token.length() - 1);
        char replacement = last == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, token.length() - 1) + replacement;

        // Act & Assert
        assertThat(jwtUtils.validateJwtToken(tampered)).isFalse();
    }

    @Test
    void validateJwtToken_malformedToken_returnsFalse() {
        // Act & Assert
        assertThat(jwtUtils.validateJwtToken("this.is.not-a-jwt")).isFalse();
    }

    @Test
    void validateJwtToken_emptyToken_returnsFalse() {
        // Act & Assert
        assertThat(jwtUtils.validateJwtToken("")).isFalse();
    }

    @Test
    void getAllClaimsFromToken_tokenWithContext_extractsAllClaims() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String token = jwtUtils.generateJwtTokenWithContext(
                userId, "root", "root@x.com", orgId, "SUPERADMIN", tenantId);

        // Act
        Claims claims = jwtUtils.getAllClaimsFromToken(token);

        // Assert
        assertThat(claims.getSubject()).isEqualTo("root");
        assertThat(claims.get("email", String.class)).isEqualTo("root@x.com");
        assertThat(jwtUtils.getTenantIdFromJwtToken(token)).isEqualTo(tenantId);
        assertThat(jwtUtils.getOrganizationIdFromJwtToken(token)).isEqualTo(orgId);
        assertThat(jwtUtils.getUserIdFromJwtToken(token)).isEqualTo(userId);
    }

    @Test
    void requiresContextSelection_superadminToken_returnsTrue() {
        // Arrange
        String token = jwtUtils.generateJwtTokenFromUserId(
                UUID.randomUUID(), "root", "root@x.com", null, "SUPERADMIN");

        // Act & Assert
        assertThat(jwtUtils.requiresContextSelection(token)).isTrue();
    }

    @Test
    void requiresContextSelection_regularUserToken_returnsFalse() {
        // Arrange
        String token = jwtUtils.generateJwtTokenFromUserId(
                UUID.randomUUID(), "jdoe", "jdoe@x.com", null, "USER");

        // Act & Assert
        assertThat(jwtUtils.requiresContextSelection(token)).isFalse();
    }

    @Test
    void getTenantIdFromJwtToken_noTenantClaim_returnsNull() {
        // Arrange
        String token = jwtUtils.generateJwtTokenFromUserId(
                UUID.randomUUID(), "jdoe", "jdoe@x.com", null, "USER");

        // Act & Assert
        assertThat(jwtUtils.getTenantIdFromJwtToken(token)).isNull();
    }
}
