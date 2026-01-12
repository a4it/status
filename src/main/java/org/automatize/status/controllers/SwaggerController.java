package org.automatize.status.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SwaggerController {

    @GetMapping("/swagger")
    public String swaggerRedirect() {
        return "forward:/swagger-ui.html";
    }

    @GetMapping("/swagger-ui")
    public String swaggerUiRedirect() {
        return "forward:/swagger-ui.html";
    }

    @GetMapping("/swagger-ui/")
    public String swaggerUiSlashRedirect() {
        return "forward:/swagger-ui.html";
    }

    @GetMapping("/swagger-ui/index.html")
    public String swaggerUiIndexRedirect() {
        return "forward:/swagger-ui.html";
    }
}