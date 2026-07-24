package org.automatize.status.controllers.api;

import org.automatize.status.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

/**
 * Base class for scheduler {@code @WebMvcTest} slices whose controllers read the
 * current principal from the {@code SecurityContext}. Installs an authenticated
 * ADMIN {@link UserPrincipal} before each test and clears it afterwards.
 */
abstract class AbstractSchedulerApiControllerTest extends AbstractApiControllerTest {

    /**
     * Installs an authenticated ADMIN {@link UserPrincipal} into the {@link SecurityContextHolder}
     * before each test, since the scheduler controllers read the current principal from the security context.
     */
    @BeforeEach
    void setUpPrincipal() {
        UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(), "admin", "admin@test.local", "pw", "ADMIN",
                UUID.randomUUID(), true, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    /**
     * Clears the {@link SecurityContextHolder} after each test to avoid principal leakage between tests.
     */
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }
}
