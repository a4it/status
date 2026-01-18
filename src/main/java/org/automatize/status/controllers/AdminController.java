package org.automatize.status.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * <p>
 * MVC controller for admin dashboard pages.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Serve Thymeleaf templates for administrative interface</li>
 *   <li>Handle routing for dashboard, platforms, issues, and components management</li>
 *   <li>Provide common attributes and navigation state to templates</li>
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

    /**
     * Adds common attributes to the model for all pages.
     *
     * @param model the model to add attributes to
     */
    private void addCommonAttributes(Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("buildNumber", buildNumber);
        model.addAttribute("buildDate", buildDate);
        model.addAttribute("copyright", copyright);
    }

    /**
     * Displays the admin login page.
     *
     * @return the login template view name
     */
    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    /**
     * Displays the admin dashboard page.
     *
     * @param model the model for template rendering
     * @return the dashboard template view name
     */
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("activeNav", "dashboard");
        addCommonAttributes(model);
        return "admin/dashboard";
    }

    /**
     * Displays the platforms management page.
     *
     * @param model the model for template rendering
     * @return the platforms template view name
     */
    @GetMapping("/platforms")
    public String platforms(Model model) {
        model.addAttribute("activeNav", "platforms");
        addCommonAttributes(model);
        return "admin/platforms";
    }

    /**
     * Displays the issues management page.
     *
     * @param model the model for template rendering
     * @return the issues template view name
     */
    @GetMapping("/issues")
    public String issues(Model model) {
        model.addAttribute("activeNav", "issues");
        addCommonAttributes(model);
        return "admin/issues";
    }

    /**
     * Displays the components management page.
     *
     * @param model the model for template rendering
     * @return the components template view name
     */
    @GetMapping("/components")
    public String components(Model model) {
        model.addAttribute("activeNav", "components");
        addCommonAttributes(model);
        return "admin/components";
    }

    /**
     * Displays the notification subscribers management page.
     *
     * @param model the model for template rendering
     * @return the subscribers template view name
     */
    @GetMapping("/subscribers")
    public String subscribers(Model model) {
        model.addAttribute("activeNav", "subscribers");
        addCommonAttributes(model);
        return "admin/subscribers";
    }

    /**
     * Displays the platform events log page.
     *
     * @param model the model for template rendering
     * @return the events template view name
     */
    @GetMapping("/events")
    public String events(Model model) {
        model.addAttribute("activeNav", "events");
        addCommonAttributes(model);
        return "admin/events";
    }
}
