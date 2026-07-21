package org.automatize.status.controllers;

import org.automatize.status.controllers.api.AbstractApiControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} slice test covering the registration-disabled branch of
 * {@link AuthenticationController#showRegisterForm}. A separate class is used
 * because the {@code app.registration.enabled} property is bound at context
 * startup and cannot vary per test method within one context.
 */
@WebMvcTest(controllers = AuthenticationController.class,
        excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@TestPropertySource(properties = {"app.registration.enabled=false"})
class AuthenticationControllerRegistrationDisabledTest extends AbstractApiControllerTest {

    /**
     * Verifies that when {@code app.registration.enabled=false},
     * {@code GET /register} issues a 3xx redirect to {@code /login} instead of
     * rendering the registration form.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void register_disabled_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
