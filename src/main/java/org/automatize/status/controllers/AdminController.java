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

    /**
     * Displays the health checks configuration and monitoring page.
     *
     * @param model the model for template rendering
     * @return the health-checks template view name
     */
    @GetMapping("/health-checks")
    public String healthChecks(Model model) {
        model.addAttribute("activeNav", "health-checks");
        addCommonAttributes(model);
        return "admin/health-checks";
    }

    /**
     * Displays the tenants management page.
     *
     * @param model the model for template rendering
     * @return the tenants template view name
     */
    @GetMapping("/tenants")
    public String tenants(Model model) {
        model.addAttribute("activeNav", "tenants");
        addCommonAttributes(model);
        return "admin/tenants";
    }

    /**
     * Displays the organizations management page.
     *
     * @param model the model for template rendering
     * @return the organizations template view name
     */
    @GetMapping("/organizations")
    public String organizations(Model model) {
        model.addAttribute("activeNav", "organizations");
        addCommonAttributes(model);
        return "admin/organizations";
    }

    /**
     * Displays the Logs Hub search and explore page.
     *
     * @param model the model for template rendering
     * @return the logs template view name
     */
    @GetMapping("/logs")
    public String logs(Model model) {
        model.addAttribute("activeNav", "logs");
        addCommonAttributes(model);
        return "admin/logs";
    }

    /**
     * Displays the drop rules management page.
     *
     * @param model the model for template rendering
     * @return the drop-rules template view name
     */
    @GetMapping("/drop-rules")
    public String dropRules(Model model) {
        model.addAttribute("activeNav", "drop-rules");
        addCommonAttributes(model);
        return "admin/drop-rules";
    }

    /**
     * Displays the log metrics dashboard page.
     *
     * @param model the model for template rendering
     * @return the log-metrics template view name
     */
    @GetMapping("/log-metrics")
    public String logMetrics(Model model) {
        model.addAttribute("activeNav", "log-metrics");
        addCommonAttributes(model);
        return "admin/log-metrics";
    }

    /**
     * Displays the alert rules management page.
     *
     * @param model the model for template rendering
     * @return the alert-rules template view name
     */
    @GetMapping("/alert-rules")
    public String alertRules(Model model) {
        model.addAttribute("activeNav", "alert-rules");
        addCommonAttributes(model);
        return "admin/alert-rules";
    }

    /**
     * Displays the log API keys management page.
     *
     * @param model the model for template rendering
     * @return the log-api-keys template view name
     */
    @GetMapping("/log-api-keys")
    public String logApiKeys(Model model) {
        model.addAttribute("activeNav", "log-api-keys");
        addCommonAttributes(model);
        return "admin/log-api-keys";
    }
}
