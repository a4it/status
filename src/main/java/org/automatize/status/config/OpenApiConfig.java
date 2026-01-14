package org.automatize.status.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenAPI/Swagger documentation.
 * <p>
 * Configures the OpenAPI specification including API information,
 * security schemes, and grouped API endpoints for documentation.
 * </p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates the main OpenAPI configuration bean.
     *
     * @return the configured OpenAPI instance with API info and security schemes
     */
    @Bean
    public OpenAPI statusOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Status Monitoring API")
                        .description("A comprehensive Spring Boot application for status monitoring and uptime management")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }

    /**
     * Creates a grouped API configuration for public endpoints.
     *
     * @return the grouped API configuration for authentication and public status endpoints
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .displayName("Public API (Auth & Status)")
                .pathsToMatch("/api/auth/**", "/api/public/**")
                .build();
    }

    /**
     * Creates a grouped API configuration for authenticated endpoints.
     *
     * @return the grouped API configuration for endpoints requiring authentication
     */
    @Bean
    public GroupedOpenApi authenticatedApi() {
        return GroupedOpenApi.builder()
                .group("authenticated")
                .displayName("Authenticated API")
                .pathsToExclude("/api/auth/**", "/api/public/**")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    operation.addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"));
                    return operation;
                })
                .build();
    }

    /**
     * Creates the JWT bearer authentication security scheme.
     *
     * @return the configured security scheme for JWT authentication
     */
    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme().type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }
}