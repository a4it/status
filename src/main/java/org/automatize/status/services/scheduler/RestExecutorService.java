package org.automatize.status.services.scheduler;

import org.automatize.status.models.SchedulerJobRun;
import org.automatize.status.models.SchedulerRestConfig;
import org.automatize.status.models.scheduler.AuthType;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Executor service for scheduler jobs of type {@code REST}.
 *
 * <p>Builds and fires an HTTP request using Java's built-in {@link HttpClient},
 * handles authentication (Basic, Bearer, API Key), optional SSL bypass for
 * development/self-signed environments, and evaluates configurable success
 * assertions before writing the outcome to the {@link SchedulerJobRun}.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Service
public class RestExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(RestExecutorService.class);

    @Autowired
    private SchedulerEncryptionService encryptionService;

    /**
     * Executes the REST job defined by {@code config} and writes the result into {@code run}.
     *
     * @param config the REST configuration; if {@code null} the run is marked as FAILURE
     * @param run    the run record to populate with outcome details
     */
    public void execute(SchedulerRestConfig config, SchedulerJobRun run) {
        if (config == null) {
            run.setStatus(JobRunStatus.FAILURE);
            run.setErrorMessage("REST configuration is missing");
            return;
        }

        try {
            String url = buildUrl(config);

            HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(config.getConnectTimeoutMs() != null ? config.getConnectTimeoutMs() : 5000))
                    .followRedirects(Boolean.TRUE.equals(config.getFollowRedirects())
                            ? HttpClient.Redirect.NORMAL
                            : HttpClient.Redirect.NEVER);

            if (!Boolean.TRUE.equals(config.getSslVerify())) {
                clientBuilder.sslContext(buildTrustAllSslContext());
            }

            HttpClient client = clientBuilder.build();
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(URI.create(url));

            if (config.getHeaders() != null) {
                for (Map.Entry<String, String> e : config.getHeaders().entrySet()) {
                    reqBuilder.header(e.getKey(), e.getValue());
                }
            }

            applyAuth(config, reqBuilder);

            String method = config.getHttpMethod() != null ? config.getHttpMethod().name() : "GET";
            String body = config.getRequestBody();

            if (body != null && !body.isBlank()) {
                String contentType = config.getContentType() != null ? config.getContentType() : "application/json";
                reqBuilder.header("Content-Type", contentType);
                reqBuilder.method(method, HttpRequest.BodyPublishers.ofString(body));
            } else {
                reqBuilder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            int readTimeout = config.getReadTimeoutMs() != null ? config.getReadTimeoutMs() : 30000;
            reqBuilder.timeout(Duration.ofMillis(readTimeout));

            HttpResponse<String> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            int httpStatus = response.statusCode();
            String responseBody = response.body();

            int maxBytes = config.getMaxResponseBytes() != null ? config.getMaxResponseBytes() : 102400;
            if (responseBody != null && responseBody.length() > maxBytes) {
                responseBody = responseBody.substring(0, maxBytes) + "\n... [TRUNCATED]";
            }

            run.setHttpStatusCode(httpStatus);
            run.setResponseBody(responseBody);

            boolean success = checkAssertions(config, httpStatus, responseBody);
            run.setStatus(success ? JobRunStatus.SUCCESS : JobRunStatus.FAILURE);
            if (!success) {
                run.setErrorMessage("Assertion failed for HTTP " + httpStatus);
            }

        } catch (java.net.http.HttpTimeoutException e) {
            run.setStatus(JobRunStatus.TIMEOUT);
            run.setErrorMessage("Request timed out: " + e.getMessage());
        } catch (Exception e) {
            logger.error("REST execution failed", e);
            run.setStatus(JobRunStatus.FAILURE);
            run.setErrorMessage(e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String buildUrl(SchedulerRestConfig config) {
        String url = config.getUrl();
        if (config.getQueryParams() == null || config.getQueryParams().isEmpty()) {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);
        sb.append(url.contains("?") ? "&" : "?");
        for (Map.Entry<String, String> e : config.getQueryParams().entrySet()) {
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
              .append("=")
              .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
              .append("&");
        }
        // Remove trailing ampersand
        return sb.toString().replaceAll("&$", "");
    }

    private void applyAuth(SchedulerRestConfig config, HttpRequest.Builder reqBuilder) {
        if (config.getAuthType() == null || config.getAuthType() == AuthType.NONE) return;

        switch (config.getAuthType()) {
            case BASIC -> {
                String user = config.getAuthUsername() != null ? config.getAuthUsername() : "";
                String pass = config.getAuthPasswordEnc() != null
                        ? encryptionService.decrypt(config.getAuthPasswordEnc()) : "";
                String encoded = Base64.getEncoder()
                        .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
                reqBuilder.header("Authorization", "Basic " + encoded);
            }
            case BEARER -> {
                String token = config.getAuthTokenEnc() != null
                        ? encryptionService.decrypt(config.getAuthTokenEnc()) : "";
                reqBuilder.header("Authorization", "Bearer " + token);
            }
            case API_KEY -> {
                String keyName = config.getAuthApiKeyName() != null ? config.getAuthApiKeyName() : "X-API-Key";
                String keyValue = config.getAuthApiKeyValueEnc() != null
                        ? encryptionService.decrypt(config.getAuthApiKeyValueEnc()) : "";
                // Only add as header when location is HEADER (or null — default)
                if (config.getAuthApiKeyLocation() == null
                        || !"QUERY_PARAM".equals(config.getAuthApiKeyLocation().name())) {
                    reqBuilder.header(keyName, keyValue);
                }
            }
            default -> {
                // NONE or unhandled — no auth header
            }
        }
    }

    private boolean checkAssertions(SchedulerRestConfig config, int httpStatus, String body) {
        if (config.getAssertStatusCode() != null) {
            if (httpStatus != config.getAssertStatusCode()) return false;
        } else {
            // Default: any 2xx is success
            if (httpStatus < 200 || httpStatus >= 300) return false;
        }
        if (config.getAssertBodyContains() != null && !config.getAssertBodyContains().isBlank()) {
            if (body == null || !body.contains(config.getAssertBodyContains())) return false;
        }
        return true;
    }

    private SSLContext buildTrustAllSslContext() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
            }}, null);
            return ctx;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build trust-all SSL context", e);
        }
    }
}
