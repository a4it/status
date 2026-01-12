package org.automatize.status.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @Value("${spring.application.name}")
    private String applicationName;

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("applicationName", applicationName);
        return "dashboard/index";
    }
}