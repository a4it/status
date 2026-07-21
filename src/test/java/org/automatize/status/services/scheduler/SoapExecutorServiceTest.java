package org.automatize.status.services.scheduler;

import org.automatize.status.models.SchedulerJobRun;
import org.automatize.status.models.SchedulerSoapConfig;
import org.automatize.status.models.scheduler.AuthType;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.automatize.status.models.scheduler.SoapVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SoapExecutorService}.
 *
 * <p>The live HTTP send path uses Java's built-in {@link java.net.http.HttpClient}
 * (created internally, not injectable) and therefore needs integration coverage.
 * These tests cover the null-config guard, content-type resolution, SOAP fault
 * detection, body truncation, authentication header application, and response
 * evaluation via a mocked {@link HttpResponse}.</p>
 */
@ExtendWith(MockitoExtension.class)
class SoapExecutorServiceTest {

    @Mock
    private SchedulerEncryptionService encryptionService;

    @InjectMocks
    private SoapExecutorService soapExecutorService;

    private static final String RESOLVE_CONTENT_TYPE = "resolveContentType";
    private static final String CONTAINS_SOAP_FAULT = "containsSoapFault";
    private static final String HTTP_LOCALHOST = "http://localhost";
    private static final String APPLY_AUTH = "applyAuth";
    private static final String AUTHORIZATION = "Authorization";
    private static final String EVALUATE_RESPONSE = "evaluateResponse";

    // -------------------------------------------------------------------------
    // execute() - null config guard
    // -------------------------------------------------------------------------

    /**
     * Verifies the null-config guard on {@code execute}.
     * Expected outcome: run status is FAILURE with a "SOAP configuration is missing" message.
     */
    @Test
    void execute_nullConfig_setsFailureWithMessage() {
        SchedulerJobRun run = new SchedulerJobRun();

        soapExecutorService.execute(null, run);

        assertThat(run.getStatus()).isEqualTo(JobRunStatus.FAILURE);
        assertThat(run.getErrorMessage()).isEqualTo("SOAP configuration is missing");
    }

    // -------------------------------------------------------------------------
    // resolveContentType()
    // -------------------------------------------------------------------------

    /**
     * Verifies SOAP 1.1 resolves to the {@code text/xml} content type.
     * Expected outcome: content type is "text/xml; charset=utf-8".
     */
    @Test
    void resolveContentType_soap11_returnsTextXml() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        config.setSoapVersion(SoapVersion.V1_1);

        String contentType = ReflectionTestUtils.invokeMethod(
                soapExecutorService, RESOLVE_CONTENT_TYPE, config);

        assertThat(contentType).isEqualTo("text/xml; charset=utf-8");
    }

    /**
     * Verifies SOAP 1.2 without an action resolves to the {@code application/soap+xml} content type.
     * Expected outcome: content type is "application/soap+xml; charset=utf-8".
     */
    @Test
    void resolveContentType_soap12WithoutAction_returnsSoapXml() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        config.setSoapVersion(SoapVersion.V1_2);

        String contentType = ReflectionTestUtils.invokeMethod(
                soapExecutorService, RESOLVE_CONTENT_TYPE, config);

        assertThat(contentType).isEqualTo("application/soap+xml; charset=utf-8");
    }

    /**
     * Verifies SOAP 1.2 with an action appends the action parameter to the content type.
     * Expected outcome: content type includes {@code action="urn:doStuff"}.
     */
    @Test
    void resolveContentType_soap12WithAction_appendsAction() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        config.setSoapVersion(SoapVersion.V1_2);
        config.setSoapAction("urn:doStuff");

        String contentType = ReflectionTestUtils.invokeMethod(
                soapExecutorService, RESOLVE_CONTENT_TYPE, config);

        assertThat(contentType).isEqualTo("application/soap+xml; charset=utf-8; action=\"urn:doStuff\"");
    }

    // -------------------------------------------------------------------------
    // containsSoapFault()
    // -------------------------------------------------------------------------

    /**
     * Verifies a {@code soap:Fault} element is detected as a fault.
     * Expected outcome: fault detection returns {@code true}.
     */
    @Test
    void containsSoapFault_soapPrefixedFault_returnsTrue() {
        Boolean result = ReflectionTestUtils.invokeMethod(
                soapExecutorService, CONTAINS_SOAP_FAULT, "<soap:Fault>error</soap:Fault>");

        assertThat(result).isTrue();
    }

    /**
     * Verifies a {@code SOAP-ENV:Fault} element is detected as a fault.
     * Expected outcome: fault detection returns {@code true}.
     */
    @Test
    void containsSoapFault_soapEnvFault_returnsTrue() {
        Boolean result = ReflectionTestUtils.invokeMethod(
                soapExecutorService, CONTAINS_SOAP_FAULT, "<SOAP-ENV:Fault>error</SOAP-ENV:Fault>");

        assertThat(result).isTrue();
    }

    /**
     * Verifies a bare {@code faultcode} element is detected as a fault.
     * Expected outcome: fault detection returns {@code true}.
     */
    @Test
    void containsSoapFault_faultcodeElement_returnsTrue() {
        Boolean result = ReflectionTestUtils.invokeMethod(
                soapExecutorService, CONTAINS_SOAP_FAULT, "<faultcode>Server</faultcode>");

        assertThat(result).isTrue();
    }

    /**
     * Verifies a fault-free envelope is not flagged as a fault.
     * Expected outcome: fault detection returns {@code false}.
     */
    @Test
    void containsSoapFault_noFault_returnsFalse() {
        Boolean result = ReflectionTestUtils.invokeMethod(
                soapExecutorService, CONTAINS_SOAP_FAULT, "<Envelope><Body>ok</Body></Envelope>");

        assertThat(result).isFalse();
    }

    /**
     * Verifies a {@code null} body is safely treated as no fault.
     * Expected outcome: fault detection returns {@code false}.
     */
    @Test
    void containsSoapFault_nullBody_returnsFalse() {
        Boolean result = ReflectionTestUtils.invokeMethod(
                soapExecutorService, CONTAINS_SOAP_FAULT, new Object[]{null});

        assertThat(result).isFalse();
    }

    // -------------------------------------------------------------------------
    // truncateBody()
    // -------------------------------------------------------------------------

    /**
     * Verifies a body under the byte limit is returned unchanged.
     * Expected outcome: the original body string is returned.
     */
    @Test
    void truncateBody_belowLimit_returnsUnchanged() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        config.setMaxResponseBytes(1000);

        String result = ReflectionTestUtils.invokeMethod(
                soapExecutorService, "truncateBody", "short body", config);

        assertThat(result).isEqualTo("short body");
    }

    /**
     * Verifies a body over the byte limit is truncated with a marker.
     * Expected outcome: the body is cut to the limit followed by the truncation marker.
     */
    @Test
    void truncateBody_aboveLimit_truncates() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        config.setMaxResponseBytes(5);

        String result = ReflectionTestUtils.invokeMethod(
                soapExecutorService, "truncateBody", "0123456789", config);

        assertThat(result).isEqualTo("01234\n... [TRUNCATED]");
    }

    // -------------------------------------------------------------------------
    // applyAuth()
    // -------------------------------------------------------------------------

    /**
     * Verifies {@code AuthType.NONE} adds no Authorization header.
     * Expected outcome: the built request has no Authorization header.
     */
    @Test
    void applyAuth_none_addsNoAuthorizationHeader() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        config.setAuthType(AuthType.NONE);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(HTTP_LOCALHOST)).GET();

        ReflectionTestUtils.invokeMethod(soapExecutorService, APPLY_AUTH, config, builder);

        assertThat(builder.build().headers().firstValue(AUTHORIZATION)).isEmpty();
    }

    /**
     * Verifies BASIC auth decrypts the password and adds a Base64-encoded credentials header.
     * Expected outcome: the Authorization header equals {@code Basic base64(user:secret)}.
     */
    @Test
    void applyAuth_basic_addsBase64EncodedCredentials() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        config.setAuthType(AuthType.BASIC);
        config.setAuthUsername("user");
        config.setAuthPasswordEnc("enc-pass");
        when(encryptionService.decrypt("enc-pass")).thenReturn("secret");
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(HTTP_LOCALHOST)).GET();

        ReflectionTestUtils.invokeMethod(soapExecutorService, APPLY_AUTH, config, builder);

        String expected = "Basic " + Base64.getEncoder()
                .encodeToString("user:secret".getBytes(StandardCharsets.UTF_8));
        assertThat(builder.build().headers().firstValue(AUTHORIZATION)).hasValue(expected);
    }

    /**
     * Verifies BEARER auth decrypts the token and adds a bearer Authorization header.
     * Expected outcome: the Authorization header equals {@code Bearer <token>}.
     */
    @Test
    void applyAuth_bearer_addsBearerToken() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        config.setAuthType(AuthType.BEARER);
        config.setAuthTokenEnc("enc-token");
        when(encryptionService.decrypt("enc-token")).thenReturn("tok123");
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(HTTP_LOCALHOST)).GET();

        ReflectionTestUtils.invokeMethod(soapExecutorService, APPLY_AUTH, config, builder);

        assertThat(builder.build().headers().firstValue(AUTHORIZATION)).hasValue("Bearer tok123");
    }

    // -------------------------------------------------------------------------
    // evaluateResponse() - response-to-run mapping (mocked HttpResponse)
    // -------------------------------------------------------------------------

    /**
     * Verifies a 2xx response with no SOAP fault maps to a successful run.
     * Expected outcome: run is SUCCESS with the status code recorded and no error.
     */
    @Test
    @SuppressWarnings("unchecked")
    void evaluateResponse_2xxNoFault_setsSuccess() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        SchedulerJobRun run = new SchedulerJobRun();
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("<Envelope><Body>ok</Body></Envelope>");

        ReflectionTestUtils.invokeMethod(soapExecutorService, EVALUATE_RESPONSE, config, run, response);

        assertThat(run.getStatus()).isEqualTo(JobRunStatus.SUCCESS);
        assertThat(run.getHttpStatusCode()).isEqualTo(200);
        assertThat(run.getErrorMessage()).isNull();
    }

    /**
     * Verifies a 2xx response containing a SOAP fault maps to a failed run.
     * Expected outcome: run is FAILURE with a "SOAP Fault detected" message.
     */
    @Test
    @SuppressWarnings("unchecked")
    void evaluateResponse_2xxWithFault_setsFailureWithFaultMessage() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        SchedulerJobRun run = new SchedulerJobRun();
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("<soap:Fault>bad</soap:Fault>");

        ReflectionTestUtils.invokeMethod(soapExecutorService, EVALUATE_RESPONSE, config, run, response);

        assertThat(run.getStatus()).isEqualTo(JobRunStatus.FAILURE);
        assertThat(run.getErrorMessage()).isEqualTo("SOAP call failed with HTTP 200 (SOAP Fault detected)");
    }

    /**
     * Verifies a 5xx response maps to a failed run.
     * Expected outcome: run is FAILURE with a "SOAP call failed with HTTP 500" message.
     */
    @Test
    @SuppressWarnings("unchecked")
    void evaluateResponse_serverError_setsFailure() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        SchedulerJobRun run = new SchedulerJobRun();
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);
        when(response.body()).thenReturn("internal error");

        ReflectionTestUtils.invokeMethod(soapExecutorService, EVALUATE_RESPONSE, config, run, response);

        assertThat(run.getStatus()).isEqualTo(JobRunStatus.FAILURE);
        assertThat(run.getErrorMessage()).isEqualTo("SOAP call failed with HTTP 500");
    }

    // -------------------------------------------------------------------------
    // buildRequest() - header assembly for SOAP 1.1
    // -------------------------------------------------------------------------

    /**
     * Verifies SOAP 1.1 request assembly adds the SOAPAction header, content type and POST method.
     * Expected outcome: SOAPAction and Content-Type headers are set and the method is POST.
     */
    @Test
    void buildRequest_soap11WithAction_addsSoapActionHeader() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        config.setSoapVersion(SoapVersion.V1_1);
        config.setEndpointUrl("http://localhost/service");
        config.setSoapAction("urn:doStuff");
        config.setSoapEnvelope("<Envelope/>");

        HttpRequest request = ReflectionTestUtils.invokeMethod(
                soapExecutorService, "buildRequest", config);

        assertThat(request).isNotNull();
        assertThat(request.headers().firstValue("SOAPAction")).hasValue("\"urn:doStuff\"");
        assertThat(request.headers().firstValue("Content-Type")).hasValue("text/xml; charset=utf-8");
        assertThat(request.method()).isEqualTo("POST");
    }
}
