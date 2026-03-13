package org.automatize.status.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenAPI/Swagger documentation.
 * <p>
 * Configures the OpenAPI specification including API information,
 * security schemes, and grouped API endpoints for documentation.
 * APIs are split into two groups: public (no authentication) and
 * authenticated (requires JWT or API key).
 * </p>
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "Bearer Authentication";
    private static final String API_KEY_AUTH = "API Key Authentication";

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
                        .description("A comprehensive Spring Boot application for status monitoring and uptime management. " +
                                "This API is divided into two groups: Public APIs (no authentication required) and " +
                                "Authenticated APIs (requires JWT Bearer token or API Key).")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, createJwtScheme())
                        .addSecuritySchemes(API_KEY_AUTH, createApiKeyScheme()));
    }

    /**
     * Creates a grouped API configuration for public endpoints.
     * <p>
     * Public endpoints include authentication (login, register, token refresh)
     * and public status page endpoints that don't require authentication.
     * </p>
     *
     * @return the grouped API configuration for public endpoints
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("1-public")
                .displayName("Public API")
                .pathsToMatch("/api/auth/**", "/api/public/**")
                .build();
    }

    /**
     * Creates a grouped API configuration for authenticated endpoints.
     * <p>
     * Authenticated endpoints require either JWT Bearer token or API Key.
     * Includes all management endpoints for apps, components, incidents,
     * maintenance, events, users, organizations, tenants, and platforms.
     * </p>
     *
     * @return the grouped API configuration for authenticated endpoints
     */
    @Bean
    public GroupedOpenApi authenticatedApi() {
        return GroupedOpenApi.builder()
                .group("2-authenticated")
                .displayName("Authenticated API")
                .pathsToMatch(
                        "/api/status-apps/**",
                        "/api/components/**",
                        "/api/incidents/**",
                        "/api/maintenance/**",
                        "/api/events/**",
                        "/api/notification-subscribers/**",
                        "/api/users/**",
                        "/api/organizations/**",
                        "/api/tenants/**",
                        "/api/status-platforms/**"
                )
                .addOperationCustomizer((operation, handlerMethod) -> {
                    operation.addSecurityItem(new SecurityRequirement()
                            .addList(BEARER_AUTH)
                            .addList(API_KEY_AUTH));
                    return operation;
                })
                .build();
    }

    /**
     * Creates the JWT bearer authentication security scheme.
     *
     * @return the configured security scheme for JWT authentication
     */
    private SecurityScheme createJwtScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter your JWT token obtained from /api/auth/login");
    }

    /**
     * Creates the API key authentication security scheme.
     *
     * @return the configured security scheme for API key authentication
     */
    private SecurityScheme createApiKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-Key")
                .description("API key for component/app event logging. Obtain from component or app settings.");
    }
}