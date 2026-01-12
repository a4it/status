package org.automatize.status.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

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