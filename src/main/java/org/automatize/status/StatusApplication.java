package org.automatize.status;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Status Monitoring application.
 * <p>
 * This Spring Boot application provides comprehensive status monitoring capabilities
 * including uptime tracking, incident management, and maintenance scheduling for
 * multiple platforms in a multi-tenant environment.
 * </p>
 *
 * @author Status Team
 * @version 1.0
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class StatusApplication {

    /**
     * Application entry point.
     *
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(StatusApplication.class, args);
    }

}
