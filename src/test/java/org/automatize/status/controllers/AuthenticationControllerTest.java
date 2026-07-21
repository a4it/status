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

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@example.com", "n/a",
                        AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    @Test
    void login_anonymous_returnsLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("authentication/login"))
                .andExpect(model().attributeExists("applicationName", "serverPort"));
    }

    @Test
    void login_authenticated_redirectsToAdmin() throws Exception {
        authenticate();
        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void logout_get_clearsCookiesAndRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void logout_post_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void register_anonymousAndEnabled_returnsRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("authentication/register"))
                .andExpect(model().attributeExists("applicationName", "serverPort"));
    }

    @Test
    void register_authenticated_redirectsToAdmin() throws Exception {
        authenticate();
        mockMvc.perform(get("/register"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void forgotPassword_anonymous_returnsForgotPasswordView() throws Exception {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("authentication/forgot-password"))
                .andExpect(model().attributeExists("applicationName", "serverPort"));
    }

    @Test
    void forgotPassword_authenticated_redirectsToAdmin() throws Exception {
        authenticate();
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }
}
