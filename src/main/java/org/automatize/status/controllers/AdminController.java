package org.automatize.status.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@PropertySource("classpath:build.properties")
public class AdminController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${buildNumber:0}")
    private String buildNumber;

    @Value("${app.build.date:unknown}")
    private String buildDate;

    @Value("${app.copyright:Automatize BV}")
    private String copyright;

    private void addCommonAttributes(Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("buildNumber", buildNumber);
        model.addAttribute("buildDate", buildDate);
        model.addAttribute("copyright", copyright);
    }

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("activeNav", "dashboard");
        addCommonAttributes(model);
        return "admin/dashboard";
    }

    @GetMapping("/platforms")
    public String platforms(Model model) {
        model.addAttribute("activeNav", "platforms");
        addCommonAttributes(model);
        return "admin/platforms";
    }

    @GetMapping("/issues")
    public String issues(Model model) {
        model.addAttribute("activeNav", "issues");
        addCommonAttributes(model);
        return "admin/issues";
    }

    @GetMapping("/components")
    public String components(Model model) {
        model.addAttribute("activeNav", "components");
        addCommonAttributes(model);
        return "admin/components";
    }
}
