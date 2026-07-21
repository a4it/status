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

    /**
     * Default constructor required by JPA.
     */
    public SchedulerRestConfig() {
    }

    /**
     * Gets the unique identifier of this REST configuration.
     *
     * @return the UUID of the configuration
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of this REST configuration.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the scheduler job that owns this configuration.
     *
     * @return the associated SchedulerJob
     */
    public SchedulerJob getJob() {
        return job;
    }

    /**
     * Sets the scheduler job that owns this configuration.
     *
     * @param job the SchedulerJob to set
     */
    public void setJob(SchedulerJob job) {
        this.job = job;
    }

    /**
     * Gets the HTTP method used for the request.
     *
     * @return the HTTP method
     */
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    /**
     * Sets the HTTP method used for the request.
     *
     * @param httpMethod the HTTP method to set
     */
    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    /**
     * Gets the target URL of the request.
     *
     * @return the request URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the target URL of the request.
     *
     * @param url the request URL to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets the request body payload.
     *
     * @return the request body
     */
    public String getRequestBody() {
        return requestBody;
    }

    /**
     * Sets the request body payload.
     *
     * @param requestBody the request body to set
     */
    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    /**
     * Gets the content type of the request body.
     *
     * @return the content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content type of the request body.
     *
     * @param contentType the content type to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Gets the HTTP request headers.
     *
     * @return a map of header names to values
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Sets the HTTP request headers.
     *
     * @param headers a map of header names to values to set
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * Gets the URL query parameters.
     *
     * @return a map of query parameter names to values
     */
    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    /**
     * Sets the URL query parameters.
     *
     * @param queryParams a map of query parameter names to values to set
     */
    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    /**
     * Gets the authentication type used for the request.
     *
     * @return the authentication type
     */
    public AuthType getAuthType() {
        return authType;
    }

    /**
     * Sets the authentication type used for the request.
     *
     * @param authType the authentication type to set
     */
    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    /**
     * Gets the username for basic authentication.
     *
     * @return the authentication username
     */
    public String getAuthUsername() {
        return authUsername;
    }

    /**
     * Sets the username for basic authentication.
     *
     * @param authUsername the authentication username to set
     */
    public void setAuthUsername(String authUsername) {
        this.authUsername = authUsername;
    }

    /**
     * Gets the encrypted password for basic authentication.
     *
     * @return the encrypted authentication password
     */
    public String getAuthPasswordEnc() {
        return authPasswordEnc;
    }

    /**
     * Sets the encrypted password for basic authentication.
     *
     * @param authPasswordEnc the encrypted authentication password to set
     */
    public void setAuthPasswordEnc(String authPasswordEnc) {
        this.authPasswordEnc = authPasswordEnc;
    }

    /**
     * Gets the encrypted bearer token for token authentication.
     *
     * @return the encrypted authentication token
     */
    public String getAuthTokenEnc() {
        return authTokenEnc;
    }

    /**
     * Sets the encrypted bearer token for token authentication.
     *
     * @param authTokenEnc the encrypted authentication token to set
     */
    public void setAuthTokenEnc(String authTokenEnc) {
        this.authTokenEnc = authTokenEnc;
    }

    /**
     * Gets the name of the API key parameter.
     *
     * @return the API key name
     */
    public String getAuthApiKeyName() {
        return authApiKeyName;
    }

    /**
     * Sets the name of the API key parameter.
     *
     * @param authApiKeyName the API key name to set
     */
    public void setAuthApiKeyName(String authApiKeyName) {
        this.authApiKeyName = authApiKeyName;
    }

    /**
     * Gets the encrypted value of the API key.
     *
     * @return the encrypted API key value
     */
    public String getAuthApiKeyValueEnc() {
        return authApiKeyValueEnc;
    }

    /**
     * Sets the encrypted value of the API key.
     *
     * @param authApiKeyValueEnc the encrypted API key value to set
     */
    public void setAuthApiKeyValueEnc(String authApiKeyValueEnc) {
        this.authApiKeyValueEnc = authApiKeyValueEnc;
    }

    /**
     * Gets the location where the API key is placed (e.g., header or query).
     *
     * @return the API key location
     */
    public ApiKeyLocation getAuthApiKeyLocation() {
        return authApiKeyLocation;
    }

    /**
     * Sets the location where the API key is placed (e.g., header or query).
     *
     * @param authApiKeyLocation the API key location to set
     */
    public void setAuthApiKeyLocation(ApiKeyLocation authApiKeyLocation) {
        this.authApiKeyLocation = authApiKeyLocation;
    }

    /**
     * Gets the OAuth2 token endpoint URL.
     *
     * @return the OAuth2 token URL
     */
    public String getAuthOauth2TokenUrl() {
        return authOauth2TokenUrl;
    }

    /**
     * Sets the OAuth2 token endpoint URL.
     *
     * @param authOauth2TokenUrl the OAuth2 token URL to set
     */
    public void setAuthOauth2TokenUrl(String authOauth2TokenUrl) {
        this.authOauth2TokenUrl = authOauth2TokenUrl;
    }

    /**
     * Gets the OAuth2 client identifier.
     *
     * @return the OAuth2 client id
     */
    public String getAuthOauth2ClientId() {
        return authOauth2ClientId;
    }

    /**
     * Sets the OAuth2 client identifier.
     *
     * @param authOauth2ClientId the OAuth2 client id to set
     */
    public void setAuthOauth2ClientId(String authOauth2ClientId) {
        this.authOauth2ClientId = authOauth2ClientId;
    }

    /**
     * Gets the encrypted OAuth2 client secret.
     *
     * @return the encrypted OAuth2 client secret
     */
    public String getAuthOauth2ClientSecretEnc() {
        return authOauth2ClientSecretEnc;
    }

    /**
     * Sets the encrypted OAuth2 client secret.
     *
     * @param authOauth2ClientSecretEnc the encrypted OAuth2 client secret to set
     */
    public void setAuthOauth2ClientSecretEnc(String authOauth2ClientSecretEnc) {
        this.authOauth2ClientSecretEnc = authOauth2ClientSecretEnc;
    }

    /**
     * Gets the OAuth2 scope requested for the token.
     *
     * @return the OAuth2 scope
     */
    public String getAuthOauth2Scope() {
        return authOauth2Scope;
    }

    /**
     * Sets the OAuth2 scope requested for the token.
     *
     * @param authOauth2Scope the OAuth2 scope to set
     */
    public void setAuthOauth2Scope(String authOauth2Scope) {
        this.authOauth2Scope = authOauth2Scope;
    }

    /**
     * Checks whether SSL certificate verification is enabled.
     *
     * @return true if SSL verification is enabled, false otherwise
     */
    public Boolean getSslVerify() {
        return sslVerify;
    }

    /**
     * Sets whether SSL certificate verification is enabled.
     *
     * @param sslVerify the SSL verification flag to set
     */
    public void setSslVerify(Boolean sslVerify) {
        this.sslVerify = sslVerify;
    }

    /**
     * Gets the path to the custom SSL truststore.
     *
     * @return the truststore path
     */
    public String getSslTruststorePath() {
        return sslTruststorePath;
    }

    /**
     * Sets the path to the custom SSL truststore.
     *
     * @param sslTruststorePath the truststore path to set
     */
    public void setSslTruststorePath(String sslTruststorePath) {
        this.sslTruststorePath = sslTruststorePath;
    }

    /**
     * Gets the encrypted password for the SSL truststore.
     *
     * @return the encrypted truststore password
     */
    public String getSslTruststorePasswordEnc() {
        return sslTruststorePasswordEnc;
    }

    /**
     * Sets the encrypted password for the SSL truststore.
     *
     * @param sslTruststorePasswordEnc the encrypted truststore password to set
     */
    public void setSslTruststorePasswordEnc(String sslTruststorePasswordEnc) {
        this.sslTruststorePasswordEnc = sslTruststorePasswordEnc;
    }

    /**
     * Gets the connection timeout in milliseconds.
     *
     * @return the connect timeout in milliseconds
     */
    public Integer getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    /**
     * Sets the connection timeout in milliseconds.
     *
     * @param connectTimeoutMs the connect timeout in milliseconds to set
     */
    public void setConnectTimeoutMs(Integer connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    /**
     * Gets the read timeout in milliseconds.
     *
     * @return the read timeout in milliseconds
     */
    public Integer getReadTimeoutMs() {
        return readTimeoutMs;
    }

    /**
     * Sets the read timeout in milliseconds.
     *
     * @param readTimeoutMs the read timeout in milliseconds to set
     */
    public void setReadTimeoutMs(Integer readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    /**
     * Checks whether HTTP redirects are automatically followed.
     *
     * @return true if redirects are followed, false otherwise
     */
    public Boolean getFollowRedirects() {
        return followRedirects;
    }

    /**
     * Sets whether HTTP redirects are automatically followed.
     *
     * @param followRedirects the follow-redirects flag to set
     */
    public void setFollowRedirects(Boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    /**
     * Gets the maximum number of response bytes to read.
     *
     * @return the maximum response size in bytes
     */
    public Integer getMaxResponseBytes() {
        return maxResponseBytes;
    }

    /**
     * Sets the maximum number of response bytes to read.
     *
     * @param maxResponseBytes the maximum response size in bytes to set
     */
    public void setMaxResponseBytes(Integer maxResponseBytes) {
        this.maxResponseBytes = maxResponseBytes;
    }

    /**
     * Gets the expected HTTP status code asserted against the response.
     *
     * @return the asserted status code
     */
    public Integer getAssertStatusCode() {
        return assertStatusCode;
    }

    /**
     * Sets the expected HTTP status code asserted against the response.
     *
     * @param assertStatusCode the asserted status code to set
     */
    public void setAssertStatusCode(Integer assertStatusCode) {
        this.assertStatusCode = assertStatusCode;
    }

    /**
     * Gets the substring the response body is asserted to contain.
     *
     * @return the asserted body substring
     */
    public String getAssertBodyContains() {
        return assertBodyContains;
    }

    /**
     * Sets the substring the response body is asserted to contain.
     *
     * @param assertBodyContains the asserted body substring to set
     */
    public void setAssertBodyContains(String assertBodyContains) {
        this.assertBodyContains = assertBodyContains;
    }

    /**
     * Gets the JSON path expression used for response assertion.
     *
     * @return the asserted JSON path
     */
    public String getAssertJsonPath() {
        return assertJsonPath;
    }

    /**
     * Sets the JSON path expression used for response assertion.
     *
     * @param assertJsonPath the asserted JSON path to set
     */
    public void setAssertJsonPath(String assertJsonPath) {
        this.assertJsonPath = assertJsonPath;
    }

    /**
     * Gets the expected value at the asserted JSON path.
     *
     * @return the asserted JSON value
     */
    public String getAssertJsonValue() {
        return assertJsonValue;
    }

    /**
     * Sets the expected value at the asserted JSON path.
     *
     * @param assertJsonValue the asserted JSON value to set
     */
    public void setAssertJsonValue(String assertJsonValue) {
        this.assertJsonValue = assertJsonValue;
    }
}
