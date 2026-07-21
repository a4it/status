package org.automatize.status.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JwtAuthenticationFilter}.
 *
 * <p>Note: the filter extracts the token only from the {@code Authorization: Bearer ...}
 * header (see {@code getJwtFromRequest}); it does not read cookies, so no cookie-based
 * extraction test exists.</p>
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String API_DATA_URI = API_DATA_URI;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String VALID_TOKEN = VALID_TOKEN;
    private static final String CTX_TOKEN = CTX_TOKEN;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    /**
     * Clears the {@link SecurityContextHolder} after each test so authentication
     * set by the filter in one test does not leak into the next.
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Verifies that a valid {@code Authorization: Bearer} token results in an
     * authenticated {@link SecurityContextHolder} whose principal is the loaded
     * {@code UserDetails}, and that the filter chain proceeds.
     *
     * @throws Exception if filter processing fails
     */
    @Test
    void doFilterInternal_validBearerToken_setsAuthenticationAndContinuesChain() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(API_DATA_URI);
        request.addHeader(AUTHORIZATION_HEADER, "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtils.validateJwtToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtils.getUserIdFromJwtToken(VALID_TOKEN)).thenReturn(userId);
        when(jwtUtils.getTenantIdFromJwtToken(VALID_TOKEN)).thenReturn(null);
        when(jwtUtils.getOrganizationIdFromJwtToken(VALID_TOKEN)).thenReturn(null);
        when(customUserDetailsService.loadUserById(userId)).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(userDetails);
        verify(filterChain).doFilter(request, response);
    }

    /**
     * Verifies that when the token carries tenant and organization claims, the
     * filter uses the context-aware {@code loadUserByIdWithContext} lookup,
     * authenticates, and continues the chain.
     *
     * @throws Exception if filter processing fails
     */
    @Test
    void doFilterInternal_tokenWithContext_usesContextAwareLookup() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(API_DATA_URI);
        request.addHeader(AUTHORIZATION_HEADER, "Bearer ctx-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtils.validateJwtToken(CTX_TOKEN)).thenReturn(true);
        when(jwtUtils.getUserIdFromJwtToken(CTX_TOKEN)).thenReturn(userId);
        when(jwtUtils.getTenantIdFromJwtToken(CTX_TOKEN)).thenReturn(tenantId);
        when(jwtUtils.getOrganizationIdFromJwtToken(CTX_TOKEN)).thenReturn(orgId);
        when(customUserDetailsService.loadUserByIdWithContext(userId, tenantId, orgId))
                .thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(customUserDetailsService).loadUserByIdWithContext(userId, tenantId, orgId);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    /**
     * Verifies that when no {@code Authorization} header is present the context
     * stays unauthenticated, no user lookup occurs, and the chain still proceeds.
     *
     * @throws Exception if filter processing fails
     */
    @Test
    void doFilterInternal_missingToken_doesNotAuthenticateButContinuesChain() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(API_DATA_URI);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(customUserDetailsService, never()).loadUserById(any());
        verify(filterChain).doFilter(request, response);
    }

    /**
     * Verifies that when the token fails validation the context stays
     * unauthenticated and the chain still proceeds.
     *
     * @throws Exception if filter processing fails
     */
    @Test
    void doFilterInternal_invalidToken_doesNotAuthenticateButContinuesChain() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(API_DATA_URI);
        request.addHeader(AUTHORIZATION_HEADER, "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtils.validateJwtToken("bad-token")).thenReturn(false);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    /**
     * Verifies that for a static-resource request the filter skips JWT
     * processing entirely (no token validation), leaves the context
     * unauthenticated, and forwards the request down the chain.
     *
     * @throws Exception if filter processing fails
     */
    @Test
    void doFilterInternal_staticResource_skipsJwtProcessing() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/static/app.css");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtUtils, never()).validateJwtToken(any());
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    /**
     * Verifies that when the user lookup throws, the filter swallows the
     * exception (logging it), leaves the context unauthenticated, and still
     * continues the chain.
     *
     * @throws Exception if filter processing fails
     */
    @Test
    void doFilterInternal_userLookupThrows_swallowsExceptionAndContinuesChain() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(API_DATA_URI);
        request.addHeader(AUTHORIZATION_HEADER, "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtils.validateJwtToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtils.getUserIdFromJwtToken(VALID_TOKEN)).thenReturn(userId);
        when(jwtUtils.getTenantIdFromJwtToken(VALID_TOKEN)).thenReturn(null);
        when(jwtUtils.getOrganizationIdFromJwtToken(VALID_TOKEN)).thenReturn(null);
        when(customUserDetailsService.loadUserById(userId))
                .thenThrow(new RuntimeException("db down"));

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert: exception is logged and swallowed, chain still proceeds
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
