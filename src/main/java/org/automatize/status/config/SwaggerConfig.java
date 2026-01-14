package org.automatize.status.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for Swagger UI resource handling.
 * <p>
 * Configures resource handlers for serving Swagger UI static files
 * and webjars with appropriate caching policies.
 * </p>
 */
@Configuration
public class SwaggerConfig implements WebMvcConfigurer {

    /**
     * Configures resource handlers for Swagger UI and webjars.
     *
     * @param registry the resource handler registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui.html")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache());

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS));
    }
}