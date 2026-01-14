package org.automatize.status.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for content negotiation settings.
 * <p>
 * Configures how the application handles different content types
 * and media type mappings for requests and responses.
 * </p>
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