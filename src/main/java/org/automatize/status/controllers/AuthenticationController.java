package org.automatize.status.controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * <p>
 * MVC controller for authentication-related pages.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Serve login, registration, and password recovery templates</li>
 *   <li>Manage JWT cookie cleanup during logout</li>
 *   <li>Handle authentication-based redirects for authenticated users</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
@Controller
public class AuthenticationController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${server.port}")
    private String serverPort;

    @Value("${app.registration.enabled:true}")
    private boolean registrationEnabled;

    /**
     * Displays the login form page.
     * <p>
     * Redirects authenticated users to the dashboard.
     * </p>
     *
     * @param model the model for template rendering
     * @return the login template view name or redirect to dashboard
     */
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/admin";
        }
        
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("serverPort", serverPort);
        
        return "authentication/login";
    }

    /**
     * Handles user logout by clearing JWT cookies.
     *
     * @param response the HTTP response for cookie manipulation
     * @param redirectAttributes attributes for flash messages
     * @return redirect to login page
     */
    @GetMapping("/logout")
    public String logout(HttpServletResponse response, RedirectAttributes redirectAttributes) {
        Cookie jwtCookie = new Cookie("jwt", null);
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);
        
        Cookie refreshCookie = new Cookie("refresh_token", null);
        refreshCookie.setPath("/");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
        
        SecurityContextHolder.clearContext();
        
        redirectAttributes.addFlashAttribute("logoutMessage", "You have been successfully logged out.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        
        return "redirect:/login";
    }

    /**
     * Handles POST logout requests by delegating to the GET logout handler.
     *
     * @param response the HTTP response for cookie manipulation
     * @param redirectAttributes attributes for flash messages
     * @return redirect to login page
     */
    @PostMapping("/logout")
    public String handleLogout(HttpServletResponse response, RedirectAttributes redirectAttributes) {
        return logout(response, redirectAttributes);
    }

    /**
     * Displays the user registration form.
     * <p>
     * Redirects if registration is disabled or user is already authenticated.
     * </p>
     *
     * @param model the model for template rendering
     * @param redirectAttributes attributes for flash messages
     * @return the register template view name or redirect
     */
    @GetMapping("/register")
    public String showRegisterForm(Model model, RedirectAttributes redirectAttributes) {
        if (!registrationEnabled) {
            redirectAttributes.addFlashAttribute("errorMessage", "Registration is currently disabled.");
            redirectAttributes.addFlashAttribute("messageType", "warning");
            return "redirect:/login";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/admin";
        }

        model.addAttribute("applicationName", applicationName);
        model.addAttribute("serverPort", serverPort);

        return "authentication/register";
    }

    /**
     * Displays the forgot password form.
     * <p>
     * Redirects authenticated users to the dashboard.
     * </p>
     *
     * @param model the model for template rendering
     * @return the forgot password template view name or redirect to dashboard
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/admin";
        }
        
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("serverPort", serverPort);
        
        return "authentication/forgot-password";
    }
    
}