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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

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

    /**
     * Executes the SOAP job defined by {@code config} and writes the result into {@code run}.
     *
     * @param config the SOAP configuration; if {@code null} the run is marked as FAILURE
     * @param run    the run record to populate with outcome details
     */
    public void execute(SchedulerSoapConfig config, SchedulerJobRun run) {
        if (config == null) {
            run.setStatus(JobRunStatus.FAILURE);
            run.setErrorMessage("SOAP configuration is missing");
            return;
        }

        try {
            HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(config.getConnectTimeoutMs() != null ? config.getConnectTimeoutMs() : 5000))
                    .followRedirects(HttpClient.Redirect.NORMAL);

            if (!Boolean.TRUE.equals(config.getSslVerify())) {
                clientBuilder.sslContext(buildTrustAllSslContext());
            }

            HttpClient client = clientBuilder.build();

            // SOAP content type differs between 1.1 and 1.2
            String contentType;
            if (config.getSoapVersion() == SoapVersion.V1_2) {
                contentType = "application/soap+xml; charset=utf-8";
                if (config.getSoapAction() != null) {
                    contentType += "; action=\"" + config.getSoapAction() + "\"";
                }
            } else {
                contentType = "text/xml; charset=utf-8";
            }

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(URI.create(config.getEndpointUrl()))
                    .header("Content-Type", contentType)
                    .timeout(Duration.ofMillis(config.getReadTimeoutMs() != null ? config.getReadTimeoutMs() : 60000));

            // SOAPAction header is only required for SOAP 1.1
            if (config.getSoapVersion() != SoapVersion.V1_2 && config.getSoapAction() != null) {
                reqBuilder.header("SOAPAction", "\"" + config.getSoapAction() + "\"");
            }

            if (config.getExtraHeaders() != null) {
                for (Map.Entry<String, String> e : config.getExtraHeaders().entrySet()) {
                    reqBuilder.header(e.getKey(), e.getValue());
                }
            }

            applyAuth(config, reqBuilder);

            reqBuilder.POST(HttpRequest.BodyPublishers.ofString(config.getSoapEnvelope(), StandardCharsets.UTF_8));

            HttpResponse<String> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            int httpStatus = response.statusCode();
            String responseBody = response.body();

            int maxBytes = config.getMaxResponseBytes() != null ? config.getMaxResponseBytes() : 524288;
            if (responseBody != null && responseBody.length() > maxBytes) {
                responseBody = responseBody.substring(0, maxBytes) + "\n... [TRUNCATED]";
            }

            run.setHttpStatusCode(httpStatus);
            run.setResponseBody(responseBody);

            boolean hasFault = responseBody != null
                    && (responseBody.contains("<soap:Fault")
                        || responseBody.contains("<SOAP-ENV:Fault")
                        || responseBody.contains("<faultcode>"));

            if (httpStatus >= 200 && httpStatus < 300 && !hasFault) {
                run.setStatus(JobRunStatus.SUCCESS);
            } else {
                run.setStatus(JobRunStatus.FAILURE);
                run.setErrorMessage("SOAP call failed with HTTP " + httpStatus
                        + (hasFault ? " (SOAP Fault detected)" : ""));
            }

        } catch (Exception e) {
            logger.error("SOAP execution failed", e);
            run.setStatus(JobRunStatus.FAILURE);
            run.setErrorMessage(e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void applyAuth(SchedulerSoapConfig config, HttpRequest.Builder reqBuilder) {
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
            default -> {
                // Other auth types not applicable to SOAP
            }
        }
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
