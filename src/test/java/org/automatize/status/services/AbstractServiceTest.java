package org.automatize.status.services;

import org.automatize.status.models.StatusApp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

/**
 * Base class for Mockito unit tests of services. Establishes an authenticated
 * security context (principal "tester") before each test — services stamp the
 * current principal onto audited entities — and clears it afterwards so state
 * never leaks between tests. Also provides the shared minimal {@link StatusApp}
 * fixture used across the status-domain service tests.
 */
@ExtendWith(MockitoExtension.class)
abstract class AbstractServiceTest {

    /**
     * Establishes an authenticated security context before each test so that
     * service calls relying on the current principal ("tester") behave as if invoked by a logged-in user.
     */
    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", null, List.of()));
    }

    /**
     * Clears the security context after each test to avoid leaking authentication state between tests.
     */
    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Builds a minimal {@link StatusApp} fixture for use in tests.
     *
     * @param id     the identifier to assign to the app
     * @param status the status value to assign to the app
     * @return a populated {@link StatusApp} instance
     */
    protected StatusApp newApp(UUID id, String status) {
        StatusApp app = new StatusApp();
        app.setId(id);
        app.setName("App");
        app.setStatus(status);
        return app;
    }
}
