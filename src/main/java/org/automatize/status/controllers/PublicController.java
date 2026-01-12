package org.automatize.status.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

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

    private void addCommonAttributes(Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("serverPort", serverPort);
        model.addAttribute("buildNumber", buildNumber);
        model.addAttribute("buildDate", buildDate);
        model.addAttribute("copyright", copyright);
    }

    @GetMapping("/")
    public String showStatusPage(Model model) {
        addCommonAttributes(model);
        return "public/status";
    }

    @GetMapping("/incidents")
    public String showIncidentsPage(Model model) {
        addCommonAttributes(model);
        return "public/incidents";
    }

    @GetMapping("/incidents/{incidentId}")
    public String showIncidentDetailPage(@PathVariable UUID incidentId, Model model) {
        addCommonAttributes(model);
        model.addAttribute("incidentId", incidentId);
        return "public/incident-detail";
    }

    @GetMapping("/maintenance")
    public String showMaintenancePage(Model model) {
        addCommonAttributes(model);
        return "public/maintenance";
    }

    @GetMapping("/history")
    public String showHistoryPage(Model model) {
        addCommonAttributes(model);
        return "public/history";
    }
}
