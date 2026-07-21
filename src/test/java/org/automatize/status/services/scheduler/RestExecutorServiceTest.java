package org.automatize.status.services.scheduler;

import org.automatize.status.models.SchedulerJobRun;
import org.automatize.status.models.SchedulerRestConfig;
import org.automatize.status.models.scheduler.ApiKeyLocation;
import org.automatize.status.models.scheduler.AuthType;
import org.automatize.status.models.scheduler.HttpMethod;
import org.automatize.status.models.scheduler.JobRunStatus;
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
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RestExecutorService}.
 *
 * <p>The live HTTP send path uses Java's built-in {@link java.net.http.HttpClient},
 * which is created internally and cannot be injected, so it is not exercised here
 * (needs integration coverage). Instead these tests cover the null-config guard,
 * URL building, assertion evaluation, authentication header application, and
 * response-to-run mapping (via a mocked {@link HttpResponse}).</p>
 */
@ExtendWith(MockitoExtension.class)
class RestExecutorServiceTest {

    @Mock
    private SchedulerEncryptionService encryptionService;

    @InjectMocks
    private RestExecutorService restExecutorService;

    // -------------------------------------------------------------------------
    // execute() - null config guard
    // -------------------------------------------------------------------------

    /**
     * Verifies the null-config guard on {@code execute}.
     * Expected outcome: run status is FAILURE with a "REST configuration is missing" message.
     */
    @Test
    void execute_nullConfig_setsFailureWithMessage() {
        SchedulerJobRun run = new SchedulerJobRun();

        restExecutorService.execute(null, run);

        assertThat(run.getStatus()).isEqualTo(JobRunStatus.FAILURE);
        assertThat(run.getErrorMessage()).isEqualTo("REST configuration is missing");
    }

    // -------------------------------------------------------------------------
    // buildUrl()
    // -------------------------------------------------------------------------

    /**
     * Verifies URL building when no query params are configured.
     * Expected outcome: the base URL is returned unchanged.
     */
    @Test
    void buildUrl_noQueryParams_returnsUrlUnchanged() {
        SchedulerRestConfig config = new SchedulerRestConfig();
        config.setUrl("https://api.example.com/status");

        String url = ReflectionTestUtils.invokeMethod(restExecutorService, "buildUrl", config);

        assertThat(url).isEqualTo("https://api.example.com/status");
    }

    /**
     * Verifies query params are URL-encoded and appended with a leading {@code ?}.
     * Expected outcome: the encoded param string is appended to the base URL.
     */
    @Test
    void buildUrl_withQueryParams_appendsEncodedParams() {
        SchedulerRestConfig config = new SchedulerRestConfig();
        config.setUrl("https://api.example.com/status");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", "a b");
        config.setQueryParams(params);

        String url = ReflectionTestUtils.invokeMethod(restExecutorService, "buildUrl", config);

        assertThat(url).isEqualTo("https://api.example.com/status?q=a+b");
    }

    /**
     * Verifies that when the URL already contains a query string, params are joined with {@code &}.
     * Expected outcome: the new param is appended after an ampersand separator.
     */
    @Test
    void buildUrl_urlAlreadyHasQuery_usesAmpersandSeparator() {
        SchedulerRestConfig config = new SchedulerRestConfig();
        config.setUrl("https://api.example.com/status?existing=1");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", "v");
        config.setQueryParams(params);

        String url = ReflectionTestUtils.invokeMethod(restExecutorService, "buildUrl", config);

        assertThat(url).isEqualTo("https://api.example.com/status?existing=1&q=v");
    }

    // -------------------------------------------------------------------------
    // checkAssertions()
    // -------------------------------------------------------------------------

    /**
     * Verifies the default assertion (no explicit asserts) treats a 2xx status as success.
     * Expected outcome: assertion check returns {@code true}.
     */
    @Test
    void checkAssertions_defaultNoAssertions_2xxIsSuccess() {
        SchedulerRestConfig config = new SchedulerRestConfig();

        Boolean result = ReflectionTestUtils.invokeMethod(
                restExecutorService, "checkAssertions", config, 200, "body");

        assertThat(result).isTrue();
    }

    /**
     * Verifies the default assertion treats a non-2xx status as failure.
     * Expected outcome: assertion check returns {@code false}.
     */
    @Test
    void checkAssertions_defaultNoAssertions_non2xxIsFailure() {
        SchedulerRestConfig config = new SchedulerRestConfig();

        Boolean result = ReflectionTestUtils.invokeMethod(
                restExecutorService, "checkAssertions", config, 500, "body");

        assertThat(result).isFalse();
    }

    /**
     * Verifies an explicit expected status code matching the actual code passes.
     * Expected outcome: assertion check returns {@code true}.
     */
    @Test
    void checkAssertions_expectedStatusCodeMatches_returnsTrue() {
        SchedulerRestConfig config = new SchedulerRestConfig();
        config.setAssertStatusCode(404);

        Boolean result = ReflectionTestUtils.invokeMethod(
                restExecutorService, "checkAssertions", config, 404, "not found");

        assertThat(result).isTrue();
    }

    /**
     * Verifies an explicit expected status code not matching the actual code fails.
     * Expected outcome: assertion check returns {@code false}.
     */
    @Test
    void checkAssertions_expectedStatusCodeMismatch_returnsFalse() {
        SchedulerRestConfig config = new SchedulerRestConfig();
        config.setAssertStatusCode(200);

        Boolean result = ReflectionTestUtils.invokeMethod(
                restExecutorService, "checkAssertions", config, 201, "created");

        assertThat(result).isFalse();
    }

    /**
     * Verifies a body-contains assertion passes when the substring is present.
     * Expected outcome: assertion check returns {@code true}.
     */
    @Test
    void checkAssertions_bodyContainsMatches_returnsTrue() {
        SchedulerRestConfig config = new SchedulerRestConfig();
        config.setAssertBodyContains("healthy");

        Boolean result = ReflectionTestUtils.invokeMethod(
                restExecutorService, "checkAssertions", config, 200, "{\"state\":\"healthy\"}");

        assertThat(result).isTrue();
    }

    /**
     * Verifies a body-contains assertion fails when the substring is absent.
     * Expected outcome: assertion check returns {@code false}.
     */
    @Test
    void checkAssertions_bodyDoesNotContainExpected_returnsFalse() {
        SchedulerRestConfig config = new SchedulerRestConfig();
        config.setAssertBodyContains("healthy");

        Boolean result = ReflectionTestUtils.invokeMethod(
                restExecutorService, "checkAssertions", config, 200, "{\"state\":\"down\"}");

        assertThat(result).isFalse();
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
        SchedulerRestConfig config = new SchedulerRestConfig();
        config.setAuthType(AuthType.NONE);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost")).GET();

        ReflectionTestUtils.invokeMethod(restExecutorService, "applyAuth", config, builder);

        HttpRequest request = builder.build();
        assertThat(request.headers().firstValue("Authorization")).isEmpty();
    }

    /**
     * Verifies BASIC auth decrypts the password and adds a Base64-encoded credentials header.
     * Expected outcome: the Authorization header equals {@code Basic base64(user:secret)}.
     */
    @Test
    void applyAuth_basic_addsBase64EncodedCredentials() {
        SchedulerRestConfig config = new SchedulerRestConfig();
        config.setAuthType(AuthType.BASIC);
        config.setAuthUsername("user");
        config.setAuthPasswordEnc("enc-pass");
        when(encryptionService.decrypt("enc-pass")).thenReturn("secret");
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost")).GET();

        ReflectionTestUtils.invokeMethod(restExecutorService, "applyAuth", config, builder);

        String expected = "Basic " + Base64.getEncoder()
                .encodeToString("user:secret".getBytes(StandardCharsets.UTF_8));
        assertThat(builder.build().headers().firstValue("Authorization")).hasValue(expected);
    }

    /**
     * Verifies BEARER auth decrypts the token and adds a bearer Authorization header.
     * Expected outcome: the Authorization header equals {@code Bearer <token>}.
     */
    @Test
    void applyAuth_bearer_addsBearerToken() {
        SchedulerRestConfig config = new SchedulerRestConfig();
        config.setAuthType(AuthType.BEARER);
        config.setAuthTokenEnc("enc-token");
        when(encryptionService.decrypt("enc-token")).thenReturn("tok123");
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost")).GET();

        ReflectionTestUtils.invokeMethod(restExecutorService, "applyAuth", config, builder);

        assertThat(builder.build().headers().firstValue("Authorization")).hasValue("Bearer tok123");
    }

    /**
     * Verifies API-key auth with HEADER location adds the configured custom header.
     * Expected outcome: the custom header carries the decrypted key value.
     */
    @Test
    void applyAuth_apiKeyHeaderLocation_addsCustomHeader() {
        SchedulerRestConfig config = new SchedulerRestConfig();
        config.setAuthType(AuthType.API_KEY);
        config.setAuthApiKeyName("X-Custom-Key");
        config.setAuthApiKeyValueEnc("enc-key");
        config.setAuthApiKeyLocation(ApiKeyLocation.HEADER);
        when(encryptionService.decrypt("enc-key")).thenReturn("keyval");
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost")).GET();

        ReflectionTestUtils.invokeMethod(restExecutorService, "applyAuth", config, builder);

        assertThat(builder.build().headers().firstValue("X-Custom-Key")).hasValue("keyval");
    }

    /**
     * Verifies API-key auth with QUERY_PARAM location does not add a header (key goes in the URL).
     * Expected outcome: the built request has no custom header.
     */
    @Test
    void applyAuth_apiKeyQueryParamLocation_addsNoHeader() {
        SchedulerRestConfig config = new SchedulerRestConfig();
        config.setAuthType(AuthType.API_KEY);
        config.setAuthApiKeyName("X-Custom-Key");
        config.setAuthApiKeyValueEnc("enc-key");
        config.setAuthApiKeyLocation(ApiKeyLocation.QUERY_PARAM);
        // decrypt is still invoked to resolve the value before the location check
        lenient().when(encryptionService.decrypt("enc-key")).thenReturn("keyval");
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost")).GET();

        ReflectionTestUtils.invokeMethod(restExecutorService, "applyAuth", config, builder);

        assertThat(builder.build().headers().firstValue("X-Custom-Key")).isEmpty();
    }

    // -------------------------------------------------------------------------
    // recordResponse() - response-to-run mapping (mocked HttpResponse)
    // -------------------------------------------------------------------------

    /**
     * Verifies a 2xx response under default assertions maps to a successful run.
     * Expected outcome: run is SUCCESS with the status code and body recorded and no error.
     */
    @Test
    @SuppressWarnings("unchecked")
    void recordResponse_2xxDefaultAssertions_setsSuccess() {
        SchedulerRestConfig config = new SchedulerRestConfig();
        SchedulerJobRun run = new SchedulerJobRun();
        HttpResponse<String> response = org.mockito.Mockito.mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("OK");

        ReflectionTestUtils.invokeMethod(restExecutorService, "recordResponse", config, run, response);

        assertThat(run.getStatus()).isEqualTo(JobRunStatus.SUCCESS);
        assertThat(run.getHttpStatusCode()).isEqualTo(200);
        assertThat(run.getResponseBody()).isEqualTo("OK");
        assertThat(run.getErrorMessage()).isNull();
    }

    /**
     * Verifies a non-2xx response under default assertions maps to a failed run.
     * Expected outcome: run is FAILURE with the status code and an assertion-failed message.
     */
    @Test
    @SuppressWarnings("unchecked")
    void recordResponse_non2xx_setsFailureWithAssertionMessage() {
        SchedulerRestConfig config = new SchedulerRestConfig();
        SchedulerJobRun run = new SchedulerJobRun();
        HttpResponse<String> response = org.mockito.Mockito.mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(503);
        when(response.body()).thenReturn("unavailable");

        ReflectionTestUtils.invokeMethod(restExecutorService, "recordResponse", config, run, response);

        assertThat(run.getStatus()).isEqualTo(JobRunStatus.FAILURE);
        assertThat(run.getHttpStatusCode()).isEqualTo(503);
        assertThat(run.getErrorMessage()).isEqualTo("Assertion failed for HTTP 503");
    }

    /**
     * Verifies a response body larger than the configured limit is truncated.
     * Expected outcome: the recorded body is truncated with a marker and the run is still SUCCESS.
     */
    @Test
    @SuppressWarnings("unchecked")
    void recordResponse_bodyTooLarge_truncatesResponseBody() {
        SchedulerRestConfig config = new SchedulerRestConfig();
        config.setMaxResponseBytes(10);
        SchedulerJobRun run = new SchedulerJobRun();
        HttpResponse<String> response = org.mockito.Mockito.mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("0123456789ABCDEF");

        ReflectionTestUtils.invokeMethod(restExecutorService, "recordResponse", config, run, response);

        assertThat(run.getResponseBody()).isEqualTo("0123456789\n... [TRUNCATED]");
        assertThat(run.getStatus()).isEqualTo(JobRunStatus.SUCCESS);
    }

    /**
     * Verifies a 2xx response failing a body-contains assertion maps to a failed run.
     * Expected outcome: run is FAILURE with an assertion-failed message.
     */
    @Test
    @SuppressWarnings("unchecked")
    void recordResponse_bodyContainsAssertionFails_setsFailure() {
        SchedulerRestConfig config = new SchedulerRestConfig();
        config.setAssertBodyContains("healthy");
        SchedulerJobRun run = new SchedulerJobRun();
        HttpResponse<String> response = org.mockito.Mockito.mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("service is down");

        ReflectionTestUtils.invokeMethod(restExecutorService, "recordResponse", config, run, response);

        assertThat(run.getStatus()).isEqualTo(JobRunStatus.FAILURE);
        assertThat(run.getErrorMessage()).isEqualTo("Assertion failed for HTTP 200");
    }

    // -------------------------------------------------------------------------
    // applyMethodAndBody() - method resolution defaults
    // -------------------------------------------------------------------------

    /**
     * Verifies a request with a body sets the configured HTTP method and content type.
     * Expected outcome: the built request uses POST and carries the configured Content-Type header.
     */
    @Test
    void applyMethodAndBody_withBody_setsContentTypeAndMethod() {
        SchedulerRestConfig config = new SchedulerRestConfig();
        config.setHttpMethod(HttpMethod.POST);
        config.setRequestBody("{\"a\":1}");
        config.setContentType("application/json");
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost"));

        ReflectionTestUtils.invokeMethod(restExecutorService, "applyMethodAndBody", config, builder);

        HttpRequest request = builder.build();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.headers().firstValue("Content-Type")).hasValue("application/json");
    }
}
