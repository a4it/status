package org.automatize.status;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * <p>
 * Main entry point for the Status Monitoring application.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Bootstrap the Spring Boot application</li>
 *   <li>Enable scheduling for health checks and maintenance tasks</li>
 *   <li>Enable async processing for email notifications</li>
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
