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

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/dashboard";
        }
        
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("serverPort", serverPort);
        
        return "authentication/login";
    }

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

    @PostMapping("/logout")
    public String handleLogout(HttpServletResponse response, RedirectAttributes redirectAttributes) {
        return logout(response, redirectAttributes);
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model, RedirectAttributes redirectAttributes) {
        if (!registrationEnabled) {
            redirectAttributes.addFlashAttribute("errorMessage", "Registration is currently disabled.");
            redirectAttributes.addFlashAttribute("messageType", "warning");
            return "redirect:/login";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/dashboard";
        }

        model.addAttribute("applicationName", applicationName);
        model.addAttribute("serverPort", serverPort);

        return "authentication/register";
    }
    
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/dashboard";
        }
        
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("serverPort", serverPort);
        
        return "authentication/forgot-password";
    }
    
}