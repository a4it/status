package org.automatize.status.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * MVC controller for the main dashboard page.
 * <p>
 * Serves the authenticated user's dashboard view.
 * </p>
 */
@Controller
public class DashboardController {

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Displays the main dashboard page.
     *
     * @param model the model for template rendering
     * @return the dashboard template view name
     */
    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("applicationName", applicationName);
        return "dashboard/index";
    }
}