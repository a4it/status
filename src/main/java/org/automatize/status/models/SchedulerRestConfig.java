package org.automatize.status.models;

import jakarta.persistence.*;
import org.automatize.status.models.scheduler.ApiKeyLocation;
import org.automatize.status.models.scheduler.AuthType;
import org.automatize.status.models.scheduler.HttpMethod;
import org.automatize.status.models.scheduler.JsonMapConverter;

import java.util.Map;
import java.util.UUID;

/**
 * Configuration entity for scheduler jobs of type {@code REST}.
 *
 * <p>Stores full HTTP request configuration including authentication, SSL
 * settings, timeout settings, and optional assertion rules that are evaluated
 * against the HTTP response to determine job success or failure.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Entity
@Table(name = "scheduler_rest_configs")
public class SchedulerRestConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private SchedulerJob job;

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false, length = 10)
    private HttpMethod httpMethod = HttpMethod.GET;

    @Column(name = "url", length = 4096)
    private String url;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "content_type", length = 255)
    private String contentType = "application/json";

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "headers", columnDefinition = "TEXT")
    private Map<String, String> headers;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "query_params", columnDefinition = "TEXT")
    private Map<String, String> queryParams;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 50)
    private AuthType authType = AuthType.NONE;

    @Column(name = "auth_username", length = 255)
    private String authUsername;

    @Column(name = "auth_password_enc", length = 2048)
    private String authPasswordEnc;

    @Column(name = "auth_token_enc", length = 2048)
    private String authTokenEnc;

    @Column(name = "auth_api_key_name", length = 255)
    private String authApiKeyName;

    @Column(name = "auth_api_key_value_enc", length = 2048)
    private String authApiKeyValueEnc;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_api_key_location", length = 50)
    private ApiKeyLocation authApiKeyLocation;

    @Column(name = "auth_oauth2_token_url", length = 4096)
    private String authOauth2TokenUrl;

    @Column(name = "auth_oauth2_client_id", length = 1024)
    private String authOauth2ClientId;

    @Column(name = "auth_oauth2_client_secret_enc", length = 2048)
    private String authOauth2ClientSecretEnc;

    @Column(name = "auth_oauth2_scope", length = 1024)
    private String authOauth2Scope;

    @Column(name = "ssl_verify", nullable = false)
    private Boolean sslVerify = true;

    @Column(name = "ssl_truststore_path", length = 2048)
    private String sslTruststorePath;

    @Column(name = "ssl_truststore_password_enc", length = 2048)
    private String sslTruststorePasswordEnc;

    @Column(name = "connect_timeout_ms", nullable = false)
    private Integer connectTimeoutMs = 5000;

    @Column(name = "read_timeout_ms", nullable = false)
    private Integer readTimeoutMs = 30000;

    @Column(name = "follow_redirects", nullable = false)
    private Boolean followRedirects = true;

    @Column(name = "max_response_bytes", nullable = false)
    private Integer maxResponseBytes = 102400;

    @Column(name = "assert_status_code")
    private Integer assertStatusCode;

    @Column(name = "assert_body_contains", columnDefinition = "TEXT")
    private String assertBodyContains;

    @Column(name = "assert_json_path", length = 1024)
    private String assertJsonPath;

    @Column(name = "assert_json_value", length = 1024)
    private String assertJsonValue;

    public SchedulerRestConfig() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SchedulerJob getJob() {
        return job;
    }

    public void setJob(SchedulerJob job) {
        this.job = job;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public void setAuthUsername(String authUsername) {
        this.authUsername = authUsername;
    }

    public String getAuthPasswordEnc() {
        return authPasswordEnc;
    }

    public void setAuthPasswordEnc(String authPasswordEnc) {
        this.authPasswordEnc = authPasswordEnc;
    }

    public String getAuthTokenEnc() {
        return authTokenEnc;
    }

    public void setAuthTokenEnc(String authTokenEnc) {
        this.authTokenEnc = authTokenEnc;
    }

    public String getAuthApiKeyName() {
        return authApiKeyName;
    }

    public void setAuthApiKeyName(String authApiKeyName) {
        this.authApiKeyName = authApiKeyName;
    }

    public String getAuthApiKeyValueEnc() {
        return authApiKeyValueEnc;
    }

    public void setAuthApiKeyValueEnc(String authApiKeyValueEnc) {
        this.authApiKeyValueEnc = authApiKeyValueEnc;
    }

    public ApiKeyLocation getAuthApiKeyLocation() {
        return authApiKeyLocation;
    }

    public void setAuthApiKeyLocation(ApiKeyLocation authApiKeyLocation) {
        this.authApiKeyLocation = authApiKeyLocation;
    }

    public String getAuthOauth2TokenUrl() {
        return authOauth2TokenUrl;
    }

    public void setAuthOauth2TokenUrl(String authOauth2TokenUrl) {
        this.authOauth2TokenUrl = authOauth2TokenUrl;
    }

    public String getAuthOauth2ClientId() {
        return authOauth2ClientId;
    }

    public void setAuthOauth2ClientId(String authOauth2ClientId) {
        this.authOauth2ClientId = authOauth2ClientId;
    }

    public String getAuthOauth2ClientSecretEnc() {
        return authOauth2ClientSecretEnc;
    }

    public void setAuthOauth2ClientSecretEnc(String authOauth2ClientSecretEnc) {
        this.authOauth2ClientSecretEnc = authOauth2ClientSecretEnc;
    }

    public String getAuthOauth2Scope() {
        return authOauth2Scope;
    }

    public void setAuthOauth2Scope(String authOauth2Scope) {
        this.authOauth2Scope = authOauth2Scope;
    }

    public Boolean getSslVerify() {
        return sslVerify;
    }

    public void setSslVerify(Boolean sslVerify) {
        this.sslVerify = sslVerify;
    }

    public String getSslTruststorePath() {
        return sslTruststorePath;
    }

    public void setSslTruststorePath(String sslTruststorePath) {
        this.sslTruststorePath = sslTruststorePath;
    }

    public String getSslTruststorePasswordEnc() {
        return sslTruststorePasswordEnc;
    }

    public void setSslTruststorePasswordEnc(String sslTruststorePasswordEnc) {
        this.sslTruststorePasswordEnc = sslTruststorePasswordEnc;
    }

    public Integer getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(Integer connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public Integer getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(Integer readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public Boolean getFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(Boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public Integer getMaxResponseBytes() {
        return maxResponseBytes;
    }

    public void setMaxResponseBytes(Integer maxResponseBytes) {
        this.maxResponseBytes = maxResponseBytes;
    }

    public Integer getAssertStatusCode() {
        return assertStatusCode;
    }

    public void setAssertStatusCode(Integer assertStatusCode) {
        this.assertStatusCode = assertStatusCode;
    }

    public String getAssertBodyContains() {
        return assertBodyContains;
    }

    public void setAssertBodyContains(String assertBodyContains) {
        this.assertBodyContains = assertBodyContains;
    }

    public String getAssertJsonPath() {
        return assertJsonPath;
    }

    public void setAssertJsonPath(String assertJsonPath) {
        this.assertJsonPath = assertJsonPath;
    }

    public String getAssertJsonValue() {
        return assertJsonValue;
    }

    public void setAssertJsonValue(String assertJsonValue) {
        this.assertJsonValue = assertJsonValue;
    }
}
