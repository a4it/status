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

@Service
public class HealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);

    private final StatusAppRepository statusAppRepository;
    private final StatusComponentRepository statusComponentRepository;
    private final ObjectMapper objectMapper;

    public HealthCheckService(StatusAppRepository statusAppRepository,
                              StatusComponentRepository statusComponentRepository,
                              ObjectMapper objectMapper) {
        this.statusAppRepository = statusAppRepository;
        this.statusComponentRepository = statusComponentRepository;
        this.objectMapper = objectMapper;
    }

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

    @Transactional
    public void updateAppCheckResult(StatusApp app, HealthCheckResult result) {
        app.setLastCheckAt(ZonedDateTime.now());
        app.setLastCheckSuccess(result.success());
        app.setLastCheckMessage(result.message());

        if (result.success()) {
            app.setConsecutiveFailures(0);
            // Auto-restore to OPERATIONAL if it was auto-degraded
            if ("DEGRADED_PERFORMANCE".equals(app.getStatus()) || "MAJOR_OUTAGE".equals(app.getStatus())) {
                String previousStatus = app.getStatus();
                app.setStatus("OPERATIONAL");
                logger.info("App {} auto-restored from {} to OPERATIONAL", app.getName(), previousStatus);
            }
        } else {
            int failures = (app.getConsecutiveFailures() != null ? app.getConsecutiveFailures() : 0) + 1;
            app.setConsecutiveFailures(failures);

            Integer threshold = app.getCheckFailureThreshold() != null ? app.getCheckFailureThreshold() : 3;
            if (failures >= threshold * 2) {
                if (!"MAJOR_OUTAGE".equals(app.getStatus())) {
                    app.setStatus("MAJOR_OUTAGE");
                    logger.warn("App {} changed to MAJOR_OUTAGE after {} consecutive failures", app.getName(), failures);
                }
            } else if (failures >= threshold) {
                if (!"DEGRADED_PERFORMANCE".equals(app.getStatus()) && !"MAJOR_OUTAGE".equals(app.getStatus())) {
                    app.setStatus("DEGRADED_PERFORMANCE");
                    logger.warn("App {} changed to DEGRADED_PERFORMANCE after {} consecutive failures", app.getName(), failures);
                }
            }
        }

        statusAppRepository.save(app);
    }

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

    public record HealthCheckResult(boolean success, String message) {}
}
