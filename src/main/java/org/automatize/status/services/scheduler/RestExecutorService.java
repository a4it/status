package org.automatize.status.services.scheduler;

import org.automatize.status.exceptions.SchedulerExecutionException;
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
        // Missing configuration cannot be executed
        if (config == null) {
            run.setStatus(JobRunStatus.FAILURE);
            run.setErrorMessage("REST configuration is missing");
            return;
        }

        try {
            HttpClient client = buildClient(config);
            HttpRequest request = buildRequest(config);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            recordResponse(config, run, response);
        } catch (java.net.http.HttpTimeoutException e) {
            logger.warn("REST execution timed out for {}", config.getUrl(), e);
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

    /**
     * Builds the {@link HttpClient} with configured connect timeout and redirect policy,
     * installing a trust-all SSL context when certificate verification is disabled.
     *
     * @param config the REST configuration
     * @return a configured HTTP client
     */
    private HttpClient buildClient(SchedulerRestConfig config) {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getConnectTimeoutMs() != null ? config.getConnectTimeoutMs() : 5000))
                .followRedirects(Boolean.TRUE.equals(config.getFollowRedirects())
                        ? HttpClient.Redirect.NORMAL
                        : HttpClient.Redirect.NEVER);
        // SSL verification disabled — install a trust-all context
        if (!Boolean.TRUE.equals(config.getSslVerify())) {
            clientBuilder.sslContext(buildTrustAllSslContext());
        }
        return clientBuilder.build();
    }

    /**
     * Builds the {@link HttpRequest} including URL, headers, authentication, method,
     * body, and read timeout.
     *
     * @param config the REST configuration
     * @return the assembled HTTP request
     */
    private HttpRequest buildRequest(SchedulerRestConfig config) {
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(URI.create(buildUrl(config)));
        // Apply any custom headers when configured
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(reqBuilder::header);
        }
        applyAuth(config, reqBuilder);
        applyMethodAndBody(config, reqBuilder);
        int readTimeout = config.getReadTimeoutMs() != null ? config.getReadTimeoutMs() : 30000;
        reqBuilder.timeout(Duration.ofMillis(readTimeout));
        return reqBuilder.build();
    }

    /**
     * Applies the HTTP method and request body to the builder. When a non-blank body is
     * present the Content-Type header is set; otherwise an empty body is used.
     *
     * @param config     the REST configuration
     * @param reqBuilder the request builder to mutate
     */
    private void applyMethodAndBody(SchedulerRestConfig config, HttpRequest.Builder reqBuilder) {
        String method = config.getHttpMethod() != null ? config.getHttpMethod().name() : "GET";
        String body = config.getRequestBody();
        // A non-blank body is sent with its content type
        if (body != null && !body.isBlank()) {
            String contentType = config.getContentType() != null ? config.getContentType() : "application/json";
            reqBuilder.header("Content-Type", contentType);
            reqBuilder.method(method, HttpRequest.BodyPublishers.ofString(body));
        // No body — send the method with an empty body publisher
        } else {
            reqBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        }
    }

    /**
     * Records the HTTP response onto the run, truncating the body to the configured cap
     * and evaluating success assertions to determine the final run status.
     *
     * @param config   the REST configuration
     * @param run      the run record to populate
     * @param response the HTTP response received
     */
    private void recordResponse(SchedulerRestConfig config, SchedulerJobRun run, HttpResponse<String> response) {
        int httpStatus = response.statusCode();
        String responseBody = response.body();

        int maxBytes = config.getMaxResponseBytes() != null ? config.getMaxResponseBytes() : 102400;
        // Truncate an oversized response body to the configured maximum
        if (responseBody != null && responseBody.length() > maxBytes) {
            responseBody = responseBody.substring(0, maxBytes) + "\n... [TRUNCATED]";
        }

        run.setHttpStatusCode(httpStatus);
        run.setResponseBody(responseBody);

        boolean success = checkAssertions(config, httpStatus, responseBody);
        run.setStatus(success ? JobRunStatus.SUCCESS : JobRunStatus.FAILURE);
        // Record an error message when assertions did not pass
        if (!success) {
            run.setErrorMessage("Assertion failed for HTTP " + httpStatus);
        }
    }

    /**
     * Builds the request URL, appending URL-encoded query parameters when present.
     *
     * @param config the REST configuration
     * @return the final request URL
     */
    private String buildUrl(SchedulerRestConfig config) {
        String url = config.getUrl();
        // No query parameters — use the base URL unchanged
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

    /**
     * Applies the configured authentication scheme (Basic, Bearer, or API Key) to the request.
     *
     * @param config     the REST configuration
     * @param reqBuilder the request builder to mutate
     */
    private void applyAuth(SchedulerRestConfig config, HttpRequest.Builder reqBuilder) {
        // No authentication configured — nothing to apply
        if (config.getAuthType() == null || config.getAuthType() == AuthType.NONE) return;

        // Dispatch to the handler for the configured auth type
        switch (config.getAuthType()) {
            case BASIC -> applyBasicAuth(config, reqBuilder);
            case BEARER -> applyBearerAuth(config, reqBuilder);
            case API_KEY -> applyApiKeyAuth(config, reqBuilder);
            default -> {
                // NONE or unhandled — no auth header
            }
        }
    }

    /**
     * Applies HTTP Basic authentication, decrypting the stored password and Base64-encoding
     * the {@code user:pass} credentials into an Authorization header.
     *
     * @param config     the REST configuration
     * @param reqBuilder the request builder to mutate
     */
    private void applyBasicAuth(SchedulerRestConfig config, HttpRequest.Builder reqBuilder) {
        String user = config.getAuthUsername() != null ? config.getAuthUsername() : "";
        String pass = config.getAuthPasswordEnc() != null
                ? encryptionService.decrypt(config.getAuthPasswordEnc()) : "";
        String encoded = Base64.getEncoder()
                .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
        reqBuilder.header("Authorization", "Basic " + encoded);
    }

    /**
     * Applies Bearer token authentication, decrypting the stored token into an
     * {@code Authorization: Bearer} header.
     *
     * @param config     the REST configuration
     * @param reqBuilder the request builder to mutate
     */
    private void applyBearerAuth(SchedulerRestConfig config, HttpRequest.Builder reqBuilder) {
        String token = config.getAuthTokenEnc() != null
                ? encryptionService.decrypt(config.getAuthTokenEnc()) : "";
        reqBuilder.header("Authorization", "Bearer " + token);
    }

    /**
     * Applies API-key authentication, decrypting the stored key value and adding it as a
     * header (query-param placement is not applied here).
     *
     * @param config     the REST configuration
     * @param reqBuilder the request builder to mutate
     */
    private void applyApiKeyAuth(SchedulerRestConfig config, HttpRequest.Builder reqBuilder) {
        String keyName = config.getAuthApiKeyName() != null ? config.getAuthApiKeyName() : "X-API-Key";
        String keyValue = config.getAuthApiKeyValueEnc() != null
                ? encryptionService.decrypt(config.getAuthApiKeyValueEnc()) : "";
        // Only add as header when location is HEADER (or null — default)
        if (config.getAuthApiKeyLocation() == null
                || !"QUERY_PARAM".equals(config.getAuthApiKeyLocation().name())) {
            reqBuilder.header(keyName, keyValue);
        }
    }

    /**
     * Evaluates success assertions against the response: an exact status-code match (or 2xx
     * by default) and an optional body-contains check.
     *
     * @param config     the REST configuration
     * @param httpStatus the received HTTP status code
     * @param body       the received response body
     * @return {@code true} when all configured assertions pass
     */
    private boolean checkAssertions(SchedulerRestConfig config, int httpStatus, String body) {
        // An explicit expected status code is configured
        if (config.getAssertStatusCode() != null) {
            // Status must match the expected code exactly
            if (httpStatus != config.getAssertStatusCode()) return false;
        // No explicit code — fall back to 2xx success
        } else {
            // Default: any 2xx is success
            if (httpStatus < 200 || httpStatus >= 300) return false;
        }
        // A required response-body substring is configured
        if (config.getAssertBodyContains() != null && !config.getAssertBodyContains().isBlank()) {
            // Body must be present and contain the required substring
            if (body == null || !body.contains(config.getAssertBodyContains())) return false;
        }
        return true;
    }

    /**
     * Builds an {@link SSLContext} whose trust manager accepts all certificates.
     * Intended only for development/self-signed environments.
     *
     * @return a trust-all SSL context
     * @throws SchedulerExecutionException when the context cannot be constructed
     */
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
            throw new SchedulerExecutionException("Failed to build trust-all SSL context", e);
        }
    }
}
