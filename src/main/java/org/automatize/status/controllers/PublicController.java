package org.automatize.status.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * <p>
 * MVC controller for public-facing status pages.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Serve publicly accessible Thymeleaf templates for status pages</li>
 *   <li>Provide common attributes to templates such as application name and build info</li>
 *   <li>Handle routing for status overview, incidents, maintenance, and history pages</li>
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
@PropertySource("classpath:build.properties")
public class PublicController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${buildNumber:0}")
    private String buildNumber;

    @Value("${app.build.date:unknown}")
    private String buildDate;

    @Value("${app.copyright:Automatize BV}")
    private String copyright;

    /**
     * Adds common attributes to the model for all public pages.
     *
     * @param model the model to add attributes to
     */
    private void addCommonAttributes(Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("serverPort", serverPort);
        model.addAttribute("buildNumber", buildNumber);
        model.addAttribute("buildDate", buildDate);
        model.addAttribute("copyright", copyright);
    }

    /**
     * Displays the main public status page.
     *
     * @param model the model for template rendering
     * @return the status template view name
     */
    @GetMapping("/")
    public String showStatusPage(Model model) {
        addCommonAttributes(model);
        return "public/status";
    }

    /**
     * Displays the incidents list page.
     *
     * @param model the model for template rendering
     * @return the incidents template view name
     */
    @GetMapping("/incidents")
    public String showIncidentsPage(Model model) {
        addCommonAttributes(model);
        return "public/incidents";
    }

    /**
     * Displays a specific incident's detail page.
     *
     * @param incidentId the UUID of the incident to display
     * @param model the model for template rendering
     * @return the incident detail template view name
     */
    @GetMapping("/incidents/{incidentId}")
    public String showIncidentDetailPage(@PathVariable UUID incidentId, Model model) {
        addCommonAttributes(model);
        model.addAttribute("incidentId", incidentId);
        return "public/incident-detail";
    }

    /**
     * Displays the scheduled maintenance page.
     *
     * @param model the model for template rendering
     * @return the maintenance template view name
     */
    @GetMapping("/maintenance")
    public String showMaintenancePage(Model model) {
        addCommonAttributes(model);
        return "public/maintenance";
    }

    /**
     * Displays the uptime history page.
     *
     * @param model the model for template rendering
     * @return the history template view name
     */
    @GetMapping("/history")
    public String showHistoryPage(Model model) {
        addCommonAttributes(model);
        return "public/history";
    }
}
