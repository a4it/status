package org.automatize.status.controllers;

import org.automatize.status.controllers.api.AbstractApiControllerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * {@code @WebMvcTest} slice tests for {@link AuthenticationController}.
 * <p>
 * Verifies view names, auth-based redirects, and model attributes. Security
 * filters are disabled ({@code addFilters = false}); authenticated state is
 * simulated by seeding the {@link SecurityContextHolder} directly, which is what
 * the controller inspects.
 */
@WebMvcTest(controllers = AuthenticationController.class,
        excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@TestPropertySource(properties = {"app.registration.enabled=true"})
class AuthenticationControllerTest extends AbstractApiControllerTest {

    /**
     * Clears the {@link SecurityContextHolder} after each test so authenticated
     * state seeded by one test does not leak into the next.
     */
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Test helper that seeds the {@link SecurityContextHolder} with an
     * authenticated admin principal, simulating a logged-in user for tests that
     * exercise the authenticated redirect branches.
     */
    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@example.com", "n/a",
                        AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    /**
     * Verifies that an anonymous {@code GET /login} responds 200 OK, renders the
     * {@code authentication/login} view, and exposes the application config
     * attributes.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void login_anonymous_returnsLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("authentication/login"))
                .andExpect(model().attributeExists("applicationName", "serverPort"));
    }

    /**
     * Verifies that an already-authenticated {@code GET /login} redirects to
     * {@code /admin} rather than showing the login form.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void login_authenticated_redirectsToAdmin() throws Exception {
        authenticate();
        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    /**
     * Verifies that {@code GET /logout} clears auth cookies and redirects to
     * {@code /login}.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void logout_get_clearsCookiesAndRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    /**
     * Verifies that {@code POST /logout} redirects to {@code /login}.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void logout_post_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    /**
     * Verifies that when registration is enabled, an anonymous
     * {@code GET /register} responds 200 OK, renders the
     * {@code authentication/register} view, and exposes the application config
     * attributes.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void register_anonymousAndEnabled_returnsRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("authentication/register"))
                .andExpect(model().attributeExists("applicationName", "serverPort"));
    }

    /**
     * Verifies that an already-authenticated {@code GET /register} redirects to
     * {@code /admin} rather than showing the registration form.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void register_authenticated_redirectsToAdmin() throws Exception {
        authenticate();
        mockMvc.perform(get("/register"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    /**
     * Verifies that an anonymous {@code GET /forgot-password} responds 200 OK,
     * renders the {@code authentication/forgot-password} view, and exposes the
     * application config attributes.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void forgotPassword_anonymous_returnsForgotPasswordView() throws Exception {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("authentication/forgot-password"))
                .andExpect(model().attributeExists("applicationName", "serverPort"));
    }

    /**
     * Verifies that an already-authenticated {@code GET /forgot-password}
     * redirects to {@code /admin} rather than showing the forgot-password form.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void forgotPassword_authenticated_redirectsToAdmin() throws Exception {
        authenticate();
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }
}
