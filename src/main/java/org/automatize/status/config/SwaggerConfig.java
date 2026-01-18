package org.automatize.status.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Configuration for Swagger UI resource handling.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Configure resource handlers for webjars</li>
 *   <li>Set appropriate caching policies for API documentation resources</li>
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
@Configuration
public class SwaggerConfig implements WebMvcConfigurer {

    /**
     * Configures resource handlers for webjars.
     *
     * @param registry the resource handler registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS));
    }
}