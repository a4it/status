package org.automatize.status.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * MVC controller for Swagger UI redirects.
 * <p>
 * Provides various URL paths that redirect to the main Swagger UI page
 * for API documentation convenience.
 * </p>
 */
@Controller
public class SwaggerController {

    /**
     * Redirects /swagger to the Swagger UI.
     *
     * @return forward to swagger-ui.html
     */
    @GetMapping("/swagger")
    public String swaggerRedirect() {
        return "forward:/swagger-ui.html";
    }

    /**
     * Redirects /swagger-ui to the Swagger UI.
     *
     * @return forward to swagger-ui.html
     */
    @GetMapping("/swagger-ui")
    public String swaggerUiRedirect() {
        return "forward:/swagger-ui.html";
    }

    /**
     * Redirects /swagger-ui/ to the Swagger UI.
     *
     * @return forward to swagger-ui.html
     */
    @GetMapping("/swagger-ui/")
    public String swaggerUiSlashRedirect() {
        return "forward:/swagger-ui.html";
    }

    /**
     * Redirects /swagger-ui/index.html to the Swagger UI.
     *
     * @return forward to swagger-ui.html
     */
    @GetMapping("/swagger-ui/index.html")
    public String swaggerUiIndexRedirect() {
        return "forward:/swagger-ui.html";
    }
}