package org.automatize.status.controllers.api;

import org.automatize.status.security.CustomUserDetailsService;
import org.automatize.status.security.JwtAuthenticationEntryPoint;
import org.automatize.status.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for {@code @WebMvcTest} slice tests of REST controllers.
 * <p>
 * The application's {@link org.automatize.status.config.SecurityConfig} is loaded
 * by the WebMvc slice and wires the JWT filter chain; the security collaborators
 * below are mocked so the context can start. HTTP security is disabled at the
 * MockMvc level ({@code addFilters = false}) so tests focus on mapping,
 * validation, JSON contract, and {@code @ResponseStatus} exception mapping rather
 * than authentication/authorization rules.
 * <p>
 * Subclasses add {@code @WebMvcTest(controllers = XController.class)} and a
 * {@code @MockitoBean} for the controller's own service(s).
 */
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public abstract class AbstractApiControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected JwtUtils jwtUtils;

    @MockitoBean
    protected CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    protected JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
}
