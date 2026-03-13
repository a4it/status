package org.automatize.status.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p>
 * Web configuration for content negotiation settings.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Configure content negotiation for requests and responses</li>
 *   <li>Set up media type mappings for CSS, JavaScript, and HTML files</li>
 *   <li>Define default content type for API responses</li>
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
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configures content negotiation settings for the application.
     * <p>
     * Sets up media type mappings for CSS, JavaScript, and HTML files,
     * with JSON as the default content type.
     * </p>
     *
     * @param configurer the content negotiation configurer
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .favorParameter(false)
            .ignoreAcceptHeader(false)
            .defaultContentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .mediaType("css", org.springframework.http.MediaType.valueOf("text/css"))
            .mediaType("js", org.springframework.http.MediaType.valueOf("application/javascript"))
            .mediaType("html", org.springframework.http.MediaType.TEXT_HTML);
    }
}