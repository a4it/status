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

    // -------------------------------------------------------------------------
    // execute() - null config guard
    // -------------------------------------------------------------------------

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

    @Test
    void resolveContentType_soap11_returnsTextXml() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        config.setSoapVersion(SoapVersion.V1_1);

        String contentType = ReflectionTestUtils.invokeMethod(
                soapExecutorService, "resolveContentType", config);

        assertThat(contentType).isEqualTo("text/xml; charset=utf-8");
    }

    @Test
    void resolveContentType_soap12WithoutAction_returnsSoapXml() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        config.setSoapVersion(SoapVersion.V1_2);

        String contentType = ReflectionTestUtils.invokeMethod(
                soapExecutorService, "resolveContentType", config);

        assertThat(contentType).isEqualTo("application/soap+xml; charset=utf-8");
    }

    @Test
    void resolveContentType_soap12WithAction_appendsAction() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        config.setSoapVersion(SoapVersion.V1_2);
        config.setSoapAction("urn:doStuff");

        String contentType = ReflectionTestUtils.invokeMethod(
                soapExecutorService, "resolveContentType", config);

        assertThat(contentType).isEqualTo("application/soap+xml; charset=utf-8; action=\"urn:doStuff\"");
    }

    // -------------------------------------------------------------------------
    // containsSoapFault()
    // -------------------------------------------------------------------------

    @Test
    void containsSoapFault_soapPrefixedFault_returnsTrue() {
        Boolean result = ReflectionTestUtils.invokeMethod(
                soapExecutorService, "containsSoapFault", "<soap:Fault>error</soap:Fault>");

        assertThat(result).isTrue();
    }

    @Test
    void containsSoapFault_soapEnvFault_returnsTrue() {
        Boolean result = ReflectionTestUtils.invokeMethod(
                soapExecutorService, "containsSoapFault", "<SOAP-ENV:Fault>error</SOAP-ENV:Fault>");

        assertThat(result).isTrue();
    }

    @Test
    void containsSoapFault_faultcodeElement_returnsTrue() {
        Boolean result = ReflectionTestUtils.invokeMethod(
                soapExecutorService, "containsSoapFault", "<faultcode>Server</faultcode>");

        assertThat(result).isTrue();
    }

    @Test
    void containsSoapFault_noFault_returnsFalse() {
        Boolean result = ReflectionTestUtils.invokeMethod(
                soapExecutorService, "containsSoapFault", "<Envelope><Body>ok</Body></Envelope>");

        assertThat(result).isFalse();
    }

    @Test
    void containsSoapFault_nullBody_returnsFalse() {
        Boolean result = ReflectionTestUtils.invokeMethod(
                soapExecutorService, "containsSoapFault", new Object[]{null});

        assertThat(result).isFalse();
    }

    // -------------------------------------------------------------------------
    // truncateBody()
    // -------------------------------------------------------------------------

    @Test
    void truncateBody_belowLimit_returnsUnchanged() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        config.setMaxResponseBytes(1000);

        String result = ReflectionTestUtils.invokeMethod(
                soapExecutorService, "truncateBody", "short body", config);

        assertThat(result).isEqualTo("short body");
    }

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

    @Test
    void applyAuth_none_addsNoAuthorizationHeader() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        config.setAuthType(AuthType.NONE);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost")).GET();

        ReflectionTestUtils.invokeMethod(soapExecutorService, "applyAuth", config, builder);

        assertThat(builder.build().headers().firstValue("Authorization")).isEmpty();
    }

    @Test
    void applyAuth_basic_addsBase64EncodedCredentials() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        config.setAuthType(AuthType.BASIC);
        config.setAuthUsername("user");
        config.setAuthPasswordEnc("enc-pass");
        when(encryptionService.decrypt("enc-pass")).thenReturn("secret");
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost")).GET();

        ReflectionTestUtils.invokeMethod(soapExecutorService, "applyAuth", config, builder);

        String expected = "Basic " + Base64.getEncoder()
                .encodeToString("user:secret".getBytes(StandardCharsets.UTF_8));
        assertThat(builder.build().headers().firstValue("Authorization")).hasValue(expected);
    }

    @Test
    void applyAuth_bearer_addsBearerToken() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        config.setAuthType(AuthType.BEARER);
        config.setAuthTokenEnc("enc-token");
        when(encryptionService.decrypt("enc-token")).thenReturn("tok123");
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost")).GET();

        ReflectionTestUtils.invokeMethod(soapExecutorService, "applyAuth", config, builder);

        assertThat(builder.build().headers().firstValue("Authorization")).hasValue("Bearer tok123");
    }

    // -------------------------------------------------------------------------
    // evaluateResponse() - response-to-run mapping (mocked HttpResponse)
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void evaluateResponse_2xxNoFault_setsSuccess() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        SchedulerJobRun run = new SchedulerJobRun();
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("<Envelope><Body>ok</Body></Envelope>");

        ReflectionTestUtils.invokeMethod(soapExecutorService, "evaluateResponse", config, run, response);

        assertThat(run.getStatus()).isEqualTo(JobRunStatus.SUCCESS);
        assertThat(run.getHttpStatusCode()).isEqualTo(200);
        assertThat(run.getErrorMessage()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void evaluateResponse_2xxWithFault_setsFailureWithFaultMessage() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        SchedulerJobRun run = new SchedulerJobRun();
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("<soap:Fault>bad</soap:Fault>");

        ReflectionTestUtils.invokeMethod(soapExecutorService, "evaluateResponse", config, run, response);

        assertThat(run.getStatus()).isEqualTo(JobRunStatus.FAILURE);
        assertThat(run.getErrorMessage()).isEqualTo("SOAP call failed with HTTP 200 (SOAP Fault detected)");
    }

    @Test
    @SuppressWarnings("unchecked")
    void evaluateResponse_serverError_setsFailure() {
        SchedulerSoapConfig config = new SchedulerSoapConfig();
        SchedulerJobRun run = new SchedulerJobRun();
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);
        when(response.body()).thenReturn("internal error");

        ReflectionTestUtils.invokeMethod(soapExecutorService, "evaluateResponse", config, run, response);

        assertThat(run.getStatus()).isEqualTo(JobRunStatus.FAILURE);
        assertThat(run.getErrorMessage()).isEqualTo("SOAP call failed with HTTP 500");
    }

    // -------------------------------------------------------------------------
    // buildRequest() - header assembly for SOAP 1.1
    // -------------------------------------------------------------------------

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
