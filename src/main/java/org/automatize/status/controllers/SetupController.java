package org.automatize.status.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/setup")
public class SetupController {

    @Value("${spring.application.name:Status Monitor}")
    private String applicationName;

    @Value("${app.setup.completed:false}")
    private boolean setupCompleted;

    @GetMapping({"", "/"})
    public String wizard(Model model) {
        if (setupCompleted) {
            return "redirect:/login";
        }
        model.addAttribute("applicationName", applicationName);
        return "setup/wizard";
    }
}
