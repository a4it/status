package org.automatize.status.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.time.ZonedDateTime;

/**
 * Service responsible for performing various types of health checks on monitored services.
 * <p>
 * This service supports multiple health check types:
 * <ul>
 *   <li><strong>PING</strong> - ICMP ping to verify host reachability</li>
 *   <li><strong>HTTP_GET</strong> - HTTP GET request with expected status code validation</li>
 *   <li><strong>SPRING_BOOT_HEALTH</strong> - Spring Boot Actuator health endpoint check</li>
 *   <li><strong>TCP_PORT</strong> - TCP socket connection check</li>
 * </ul>
 * </p>
 * <p>
 * The service also handles updating app and component status based on check results,
 * including automatic status transitions based on consecutive failures. When status
 * transitions occur, incidents are automatically created and notifications are sent
 * to subscribers.
 * </p>
 *
 * @author Status Monitoring Team
 * @since 1.0
 * @see HealthCheckScheduler
 */
@Service
public class HealthCheckService {

    /**
     * Logger instance for health check operations.
     */
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);

    /**
     * Repository for status app data access and updates.
     */
    private final StatusAppRepository statusAppRepository;

    /**
     * Repository for status component data access and updates.
     */
    private final StatusComponentRepository statusComponentRepository;

    /**
     * JSON object mapper for parsing health check responses.
     */
    private final ObjectMapper objectMapper;

    /**
     * Service for managing automated incidents.
     */
    private final StatusIncidentService statusIncidentService;

    /**
     * Constructs a new HealthCheckService with the required dependencies.
     *
     * @param statusAppRepository repository for status app operations
     * @param statusComponentRepository repository for status component operations
     * @param objectMapper JSON mapper for parsing responses
     * @param statusIncidentService service for automated incident management
     */
    public HealthCheckService(StatusAppRepository statusAppRepository,
                              StatusComponentRepository statusComponentRepository,
                              ObjectMapper objectMapper,
                              StatusIncidentService statusIncidentService) {
        this.statusAppRepository = statusAppRepository;
        this.statusComponentRepository = statusComponentRepository;
        this.objectMapper = objectMapper;
        this.statusIncidentService = statusIncidentService;
    }

    /**
     * Performs a health check based on the specified check type.
     * <p>
     * This method dispatches the check to the appropriate handler based on the check type.
     * </p>
     *
     * @param checkType the type of check to perform (PING, HTTP_GET, SPRING_BOOT_HEALTH, TCP_PORT)
     * @param url the URL or host address to check
     * @param timeoutSeconds the timeout for the check in seconds
     * @param expectedStatus the expected HTTP status code (for HTTP-based checks)
     * @return a HealthCheckResult indicating success or failure with a message
     */
    public HealthCheckResult performCheck(String checkType, String url, int timeoutSeconds, Integer expectedStatus) {
        if (checkType == null || "NONE".equals(checkType)) {
            return new HealthCheckResult(true, "No check configured");
        }

        int timeoutMs = timeoutSeconds * 1000;

        return switch (checkType) {
            case "PING" -> performPing(url, timeoutMs);
            case "HTTP_GET" -> performHttpGet(url, timeoutMs, expectedStatus != null ? expectedStatus : 200);
            case "SPRING_BOOT_HEALTH" -> performSpringBootHealth(url, timeoutMs);
            case "TCP_PORT" -> performTcpCheck(url, timeoutMs);
            default -> new HealthCheckResult(false, "Unknown check type: " + checkType);
        };
    }

    /**
     * Performs an ICMP ping check to verify host reachability.
     *
     * @param host the hostname or IP address to ping
     * @param timeoutMs the timeout in milliseconds
     * @return a HealthCheckResult indicating whether the host is reachable
     */
    private HealthCheckResult performPing(String host, int timeoutMs) {
        try {
            String hostname = extractHostname(host);
            InetAddress address = InetAddress.getByName(hostname);
            long startTime = System.currentTimeMillis();
            boolean reachable = address.isReachable(timeoutMs);
            long responseTime = System.currentTimeMillis() - startTime;

            if (reachable) {
                return new HealthCheckResult(true, "Ping successful (" + responseTime + "ms)");
            } else {
                return new HealthCheckResult(false, "Host unreachable");
            }
        } catch (IOException e) {
            logger.debug("Ping failed for {}: {}", host, e.getMessage());
            return new HealthCheckResult(false, "Ping failed: " + e.getMessage());
        }
    }

    /**
     * Performs an HTTP GET request and validates the response status code.
     * <p>
     * If the expected status is 200, any 2xx status code is considered successful.
     * </p>
     *
     * @param urlString the URL to request
     * @param timeoutMs the connection and read timeout in milliseconds
     * @param expectedStatus the expected HTTP status code
     * @return a HealthCheckResult indicating whether the request was successful
     */
    private HealthCheckResult performHttpGet(String urlString, int timeoutMs, int expectedStatus) {
        HttpURLConnection connection = null;
        try {
            URL url = URI.create(urlString).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setInstanceFollowRedirects(true);

            long startTime = System.currentTimeMillis();
            int responseCode = connection.getResponseCode();
            long responseTime = System.currentTimeMillis() - startTime;

            if (responseCode == expectedStatus || (expectedStatus == 200 && responseCode >= 200 && responseCode < 300)) {
                return new HealthCheckResult(true, "HTTP " + responseCode + " (" + responseTime + "ms)");
            } else {
                return new HealthCheckResult(false, "HTTP " + responseCode + " (expected " + expectedStatus + ")");
            }
        } catch (IOException e) {
            logger.debug("HTTP GET failed for {}: {}", urlString, e.getMessage());
            return new HealthCheckResult(false, "HTTP request failed: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Performs a Spring Boot Actuator health endpoint check.
     * <p>
     * This method requests the /actuator/health endpoint and parses the JSON response
     * to determine if the application status is "UP".
     * </p>
     *
     * @param urlString the base URL of the Spring Boot application
     * @param timeoutMs the connection and read timeout in milliseconds
     * @return a HealthCheckResult indicating whether the application is healthy
     */
    private HealthCheckResult performSpringBootHealth(String urlString, int timeoutMs) {
        HttpURLConnection connection = null;
        try {
            String healthUrl = urlString.endsWith("/") ? urlString + "actuator/health" : urlString + "/actuator/health";
            if (!urlString.contains("/actuator/health")) {
                healthUrl = urlString.endsWith("/") ? urlString + "actuator/health" : urlString + "/actuator/health";
            } else {
                healthUrl = urlString;
            }

            URL url = URI.create(healthUrl).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setRequestProperty("Accept", "application/json");

            long startTime = System.currentTimeMillis();
            int responseCode = connection.getResponseCode();
            long responseTime = System.currentTimeMillis() - startTime;

            if (responseCode >= 200 && responseCode < 300) {
                String responseBody = new String(connection.getInputStream().readAllBytes());
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                String status = jsonNode.has("status") ? jsonNode.get("status").asText() : "UNKNOWN";

                if ("UP".equalsIgnoreCase(status)) {
                    return new HealthCheckResult(true, "Health: UP (" + responseTime + "ms)");
                } else {
                    return new HealthCheckResult(false, "Health: " + status);
                }
            } else {
                return new HealthCheckResult(false, "HTTP " + responseCode);
            }
        } catch (IOException e) {
            logger.debug("Spring Boot health check failed for {}: {}", urlString, e.getMessage());
            return new HealthCheckResult(false, "Health check failed: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Performs a TCP port connection check.
     * <p>
     * This method attempts to establish a TCP socket connection to the specified
     * host and port to verify the service is accepting connections.
     * </p>
     *
     * @param hostPort the host and port in format "host:port" or "tcp://host:port"
     * @param timeoutMs the connection timeout in milliseconds
     * @return a HealthCheckResult indicating whether the connection was successful
     */
    private HealthCheckResult performTcpCheck(String hostPort, int timeoutMs) {
        try {
            String[] parts = hostPort.replace("tcp://", "").split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 80;

            long startTime = System.currentTimeMillis();
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), timeoutMs);
                long responseTime = System.currentTimeMillis() - startTime;
                return new HealthCheckResult(true, "TCP connection successful (" + responseTime + "ms)");
            }
        } catch (IOException e) {
            logger.debug("TCP check failed for {}: {}", hostPort, e.getMessage());
            return new HealthCheckResult(false, "TCP connection failed: " + e.getMessage());
        } catch (NumberFormatException e) {
            return new HealthCheckResult(false, "Invalid port number");
        }
    }

    /**
     * Extracts the hostname from a URL string.
     * <p>
     * This method strips protocol prefixes, port numbers, and path components
     * to return just the hostname.
     * </p>
     *
     * @param url the URL to extract the hostname from
     * @return the extracted hostname, or empty string if url is null
     */
    private String extractHostname(String url) {
        if (url == null) return "";
        String hostname = url.replaceFirst("^(https?://)", "").replaceFirst("^(tcp://)", "");
        int slashIndex = hostname.indexOf('/');
        if (slashIndex > 0) {
            hostname = hostname.substring(0, slashIndex);
        }
        int colonIndex = hostname.indexOf(':');
        if (colonIndex > 0) {
            hostname = hostname.substring(0, colonIndex);
        }
        return hostname;
    }

    /**
     * Updates the health check result for a status app and manages status transitions.
     * <p>
     * On successful checks, the consecutive failure counter is reset and the app
     * may be auto-restored to OPERATIONAL status if it was previously degraded.
     * When restored, any automated incidents are also resolved and notifications sent.
     * </p>
     * <p>
     * On failed checks, the consecutive failure counter is incremented and the app
     * status may be changed to DEGRADED_PERFORMANCE or MAJOR_OUTAGE based on the
     * configured failure threshold. When status degrades, an automated incident is
     * created and subscribers are notified.
     * </p>
     *
     * @param app the status app to update
     * @param result the health check result
     */
    @Transactional
    public void updateAppCheckResult(StatusApp app, HealthCheckResult result) {
        app.setLastCheckAt(ZonedDateTime.now());
        app.setLastCheckSuccess(result.success());
        app.setLastCheckMessage(result.message());

        String previousStatus = app.getStatus();

        if (result.success()) {
            app.setConsecutiveFailures(0);
            // Auto-restore to OPERATIONAL if it was auto-degraded
            if ("DEGRADED_PERFORMANCE".equals(app.getStatus()) || "MAJOR_OUTAGE".equals(app.getStatus())) {
                app.setStatus("OPERATIONAL");
                logger.info("App {} auto-restored from {} to OPERATIONAL", app.getName(), previousStatus);

                // Resolve any automated incidents and notify subscribers
                statusIncidentService.resolveAutomatedIncidents(app);
            }
        } else {
            int failures = (app.getConsecutiveFailures() != null ? app.getConsecutiveFailures() : 0) + 1;
            app.setConsecutiveFailures(failures);

            Integer threshold = app.getCheckFailureThreshold() != null ? app.getCheckFailureThreshold() : 3;
            if (failures >= threshold * 2) {
                if (!"MAJOR_OUTAGE".equals(app.getStatus())) {
                    app.setStatus("MAJOR_OUTAGE");
                    logger.warn("App {} changed to MAJOR_OUTAGE after {} consecutive failures", app.getName(), failures);

                    // Create or upgrade automated incident with CRITICAL severity
                    statusIncidentService.createAutomatedIncident(app, "CRITICAL", result.message());
                }
            } else if (failures >= threshold) {
                if (!"DEGRADED_PERFORMANCE".equals(app.getStatus()) && !"MAJOR_OUTAGE".equals(app.getStatus())) {
                    app.setStatus("DEGRADED_PERFORMANCE");
                    logger.warn("App {} changed to DEGRADED_PERFORMANCE after {} consecutive failures", app.getName(), failures);

                    // Create automated incident with MAJOR severity
                    statusIncidentService.createAutomatedIncident(app, "MAJOR", result.message());
                }
            }
        }

        statusAppRepository.save(app);
    }

    /**
     * Updates the health check result for a status component and manages status transitions.
     * <p>
     * On successful checks, the consecutive failure counter is reset and the component
     * may be auto-restored to OPERATIONAL status if it was previously degraded.
     * On failed checks, the consecutive failure counter is incremented and the component
     * status may be changed to DEGRADED_PERFORMANCE or MAJOR_OUTAGE based on the
     * configured failure threshold.
     * </p>
     *
     * @param component the status component to update
     * @param result the health check result
     */
    @Transactional
    public void updateComponentCheckResult(StatusComponent component, HealthCheckResult result) {
        component.setLastCheckAt(ZonedDateTime.now());
        component.setLastCheckSuccess(result.success());
        component.setLastCheckMessage(result.message());

        if (result.success()) {
            component.setConsecutiveFailures(0);
            // Auto-restore to OPERATIONAL if it was auto-degraded
            if ("DEGRADED_PERFORMANCE".equals(component.getStatus()) || "MAJOR_OUTAGE".equals(component.getStatus())) {
                String previousStatus = component.getStatus();
                component.setStatus("OPERATIONAL");
                logger.info("Component {} auto-restored from {} to OPERATIONAL", component.getName(), previousStatus);
            }
        } else {
            int failures = (component.getConsecutiveFailures() != null ? component.getConsecutiveFailures() : 0) + 1;
            component.setConsecutiveFailures(failures);

            Integer threshold = component.getCheckFailureThreshold() != null ? component.getCheckFailureThreshold() : 3;
            if (failures >= threshold * 2) {
                if (!"MAJOR_OUTAGE".equals(component.getStatus())) {
                    component.setStatus("MAJOR_OUTAGE");
                    logger.warn("Component {} changed to MAJOR_OUTAGE after {} consecutive failures", component.getName(), failures);
                }
            } else if (failures >= threshold) {
                if (!"DEGRADED_PERFORMANCE".equals(component.getStatus()) && !"MAJOR_OUTAGE".equals(component.getStatus())) {
                    component.setStatus("DEGRADED_PERFORMANCE");
                    logger.warn("Component {} changed to DEGRADED_PERFORMANCE after {} consecutive failures", component.getName(), failures);
                }
            }
        }

        statusComponentRepository.save(component);
    }

    /**
     * Record representing the result of a health check operation.
     *
     * @param success true if the health check passed, false otherwise
     * @param message a descriptive message about the check result
     */
    public record HealthCheckResult(boolean success, String message) {}
}
