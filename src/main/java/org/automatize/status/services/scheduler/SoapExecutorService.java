package org.automatize.status.services.scheduler;

import org.automatize.status.models.SchedulerJobRun;
import org.automatize.status.models.SchedulerSoapConfig;
import org.automatize.status.models.scheduler.AuthType;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.automatize.status.models.scheduler.SoapVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Executor service for scheduler jobs of type {@code SOAP}.
 *
 * <p>Sends a raw SOAP envelope as an HTTP POST using Java's built-in
 * {@link HttpClient}. Handles SOAP 1.1 and 1.2 content types, the
 * {@code SOAPAction} header, Basic/Bearer authentication, optional SSL
 * bypass, and SOAP fault detection in the response body.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Service
public class SoapExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(SoapExecutorService.class);

    @Autowired
    private SchedulerEncryptionService encryptionService;

    @Autowired
    private SchedulerHttpClientFactory httpClientFactory;

    /**
     * Executes the SOAP job defined by {@code config} and writes the result into {@code run}.
     *
     * @param config the SOAP configuration; if {@code null} the run is marked as FAILURE
     * @param run    the run record to populate with outcome details
     */
    public void execute(SchedulerSoapConfig config, SchedulerJobRun run) {
        // Missing configuration cannot be executed
        if (config == null) {
            run.setStatus(JobRunStatus.FAILURE);
            run.setErrorMessage("SOAP configuration is missing");
            return;
        }

        try {
            HttpClient client = buildHttpClient(config);
            HttpRequest request = buildRequest(config);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            evaluateResponse(config, run, response);
        } catch (Exception e) {
            logger.error("SOAP execution failed", e);
            run.setStatus(JobRunStatus.FAILURE);
            run.setErrorMessage(e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the shared {@link HttpClient} matching this job's connect timeout and
     * certificate-verification setting. Clients are cached by the factory rather than built
     * per execution, which would leak a selector thread each time.
     *
     * @param config the SOAP configuration
     * @return a reusable HTTP client for this configuration
     */
    private HttpClient buildHttpClient(SchedulerSoapConfig config) {
        return httpClientFactory.getClient(
                config.getConnectTimeoutMs() != null ? config.getConnectTimeoutMs() : 5000,
                true,
                Boolean.TRUE.equals(config.getSslVerify()));
    }

    /** Resolves the SOAP {@code Content-Type} header, which differs between SOAP 1.1 and 1.2. */
    private String resolveContentType(SchedulerSoapConfig config) {
        // SOAP 1.1 uses text/xml
        if (config.getSoapVersion() != SoapVersion.V1_2) {
            return "text/xml; charset=utf-8";
        }
        String contentType = "application/soap+xml; charset=utf-8";
        // SOAP 1.2 folds the action into the content type
        if (config.getSoapAction() != null) {
            contentType += "; action=\"" + config.getSoapAction() + "\"";
        }
        return contentType;
    }

    /**
     * Builds the SOAP HTTP POST request: content type, read timeout, the SOAPAction header
     * (SOAP 1.1 only), any extra headers, authentication, and the SOAP envelope body.
     *
     * @param config the SOAP configuration
     * @return the assembled HTTP request
     */
    private HttpRequest buildRequest(SchedulerSoapConfig config) {
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(URI.create(config.getEndpointUrl()))
                .header("Content-Type", resolveContentType(config))
                .timeout(Duration.ofMillis(config.getReadTimeoutMs() != null ? config.getReadTimeoutMs() : 60000));

        // SOAPAction header is only required for SOAP 1.1
        if (config.getSoapVersion() != SoapVersion.V1_2 && config.getSoapAction() != null) {
            reqBuilder.header("SOAPAction", "\"" + config.getSoapAction() + "\"");
        }
        // Apply any extra headers when configured
        if (config.getExtraHeaders() != null) {
            config.getExtraHeaders().forEach(reqBuilder::header);
        }
        applyAuth(config, reqBuilder);
        reqBuilder.POST(HttpRequest.BodyPublishers.ofString(config.getSoapEnvelope(), StandardCharsets.UTF_8));
        return reqBuilder.build();
    }

    /** Populates {@code run} from the SOAP response, applying truncation and fault detection. */
    private void evaluateResponse(SchedulerSoapConfig config, SchedulerJobRun run, HttpResponse<String> response) {
        int httpStatus = response.statusCode();
        String responseBody = truncateBody(response.body(), config);

        run.setHttpStatusCode(httpStatus);
        run.setResponseBody(responseBody);

        boolean hasFault = containsSoapFault(responseBody);
        // Success requires a 2xx status and no SOAP fault in the body
        if (httpStatus >= 200 && httpStatus < 300 && !hasFault) {
            run.setStatus(JobRunStatus.SUCCESS);
        // Non-2xx status or a detected SOAP fault marks the run as failed
        } else {
            run.setStatus(JobRunStatus.FAILURE);
            run.setErrorMessage("SOAP call failed with HTTP " + httpStatus
                    + (hasFault ? " (SOAP Fault detected)" : ""));
        }
    }

    /**
     * Truncates the response body to the configured maximum length, appending a marker.
     *
     * @param responseBody the raw response body
     * @param config       the SOAP configuration supplying the byte cap
     * @return the (possibly truncated) body
     */
    private String truncateBody(String responseBody, SchedulerSoapConfig config) {
        int maxBytes = config.getMaxResponseBytes() != null ? config.getMaxResponseBytes() : 524288;
        // Truncate only when the body exceeds the configured maximum
        if (responseBody != null && responseBody.length() > maxBytes) {
            return responseBody.substring(0, maxBytes) + "\n... [TRUNCATED]";
        }
        return responseBody;
    }

    /**
     * Detects whether the response body contains a SOAP fault element (1.1 or 1.2 style).
     *
     * @param responseBody the response body to inspect
     * @return {@code true} when a SOAP fault marker is present
     */
    private boolean containsSoapFault(String responseBody) {
        return responseBody != null
                && (responseBody.contains("<soap:Fault")
                    || responseBody.contains("<SOAP-ENV:Fault")
                    || responseBody.contains("<faultcode>"));
    }

    /**
     * Applies Basic or Bearer authentication to the request; other auth types are ignored
     * for SOAP.
     *
     * @param config     the SOAP configuration
     * @param reqBuilder the request builder to mutate
     */
    private void applyAuth(SchedulerSoapConfig config, HttpRequest.Builder reqBuilder) {
        // No authentication configured — nothing to apply
        if (config.getAuthType() == null || config.getAuthType() == AuthType.NONE) return;
        // Dispatch to the handler for the configured auth type
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
            default -> {
                // Other auth types not applicable to SOAP
            }
        }
    }
}
