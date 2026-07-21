package org.automatize.status.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * <p>
 * MVC controller for the first-run setup wizard.
 * </p>
 *
 * <p>
 * Base route: {@code /setup}.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Serve the setup wizard Thymeleaf template on first application run</li>
 *   <li>Redirect to the login page once setup has been completed</li>
 *   <li>Provide configuration values (such as the application name) to the wizard template</li>
 * </ul>
 * </p>
 */
@Controller
@RequestMapping("/setup")
public class SetupController {

    @Value("${spring.application.name:Status Monitor}")
    private String applicationName;

    @Value("${app.setup.completed:false}")
    private boolean setupCompleted;

    /**
     * Displays the first-run setup wizard.
     * <p>
     * Handles {@code GET /setup} (and {@code /setup/}). Redirects to the login
     * page when setup has already been completed.
     * </p>
     *
     * @param model the model for template rendering
     * @return the setup wizard template view name, or a redirect to the login page when setup is complete
     */
    @GetMapping({"", "/"})
    public String wizard(Model model) {
        // Setup already finished: send the user to the login page instead of the wizard
        if (setupCompleted) {
            return "redirect:/login";
        }
        model.addAttribute("applicationName", applicationName);
        return "setup/wizard";
    }
}
