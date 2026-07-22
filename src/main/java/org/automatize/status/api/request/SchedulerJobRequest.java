package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Request object for creating or updating a scheduler job in the status monitoring system.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate the scheduling parameters (cron expression, time zone, concurrency,
 *       retry and timeout policy) of a monitoring job</li>
 *   <li>Carry exactly one job-type-specific configuration payload (program, SQL, REST or SOAP)
 *       depending on the selected {@code jobType}</li>
 *   <li>Associate the job with an owning organization in the multi-tenant hierarchy</li>
 * </ul>
 * </p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
public class SchedulerJobRequest {

    /** The job display name. */
    @NotBlank
    @Getter
    @Setter
    private String name;

    /** The human-readable job description. */
    @Getter
    @Setter
    private String description;

    /** The job type discriminator (e.g. PROGRAM, SQL, REST, SOAP). */
    @NotBlank
    private String jobType;

    /** The cron expression defining the execution schedule. */
    @NotBlank
    private String cronExpression;

    /** The time zone in which the cron expression is evaluated. */
    private String timeZone = "UTC";

    /** Whether the job is enabled for scheduling. */
    private Boolean enabled = true;

    /** Whether concurrent executions of this job are permitted. */
    private Boolean allowConcurrent = false;

    /** The maximum number of retry attempts on failure. */
    private Integer maxRetryAttempts = 0;

    /** The delay in seconds between retry attempts. */
    private Integer retryDelaySeconds = 60;

    /** The per-execution timeout in seconds. */
    private Integer timeoutSeconds = 300;

    /** The maximum number of captured output bytes to retain. */
    private Integer maxOutputBytes = 102400;

    /** The free-form tags used to categorize the job. */
    private List<String> tags;

    /** The owning organization identifier. */
    private UUID organizationId;

    /** The program-execution configuration (used when jobType is PROGRAM). */
    private ProgramConfigRequest programConfig;

    /** The SQL-execution configuration (used when jobType is SQL). */
    private SqlConfigRequest sqlConfig;

    /** The REST-call configuration (used when jobType is REST). */
    private RestConfigRequest restConfig;

    /** The SOAP-call configuration (used when jobType is SOAP). */
    private SoapConfigRequest soapConfig;

    // -------------------------------------------------------------------------
    // Nested request classes
    // -------------------------------------------------------------------------

    /**
     * Configuration payload for a PROGRAM-type scheduler job that executes an
     * external command or shell script as part of a monitoring check.
     */
    public static class ProgramConfigRequest {

        /** The executable or command to run. */
        private String command;
        /** The ordered command-line arguments. */
        private List<String> arguments;
        /** The working directory for the process. */
        private String workingDirectory;
        /** The environment variables to inject into the process. */
        private Map<String, String> environmentVars;
        /** Whether to wrap the command in a shell invocation. */
        private Boolean shellWrap = false;
        /** The shell executable path used when shell-wrapping. */
        private String shellPath = "/bin/bash";
        /** The OS user to run the process as. */
        private String runAsUser;

        /** @return the executable or command to run */
        public String getCommand() {
            return command;
        }

        /** @param command the executable or command to run */
        public void setCommand(String command) {
            this.command = command;
        }

        /** @return the ordered command-line arguments */
        public List<String> getArguments() {
            return arguments;
        }

        /** @param arguments the ordered command-line arguments to set */
        public void setArguments(List<String> arguments) {
            this.arguments = arguments;
        }

        /** @return the working directory for the process */
        public String getWorkingDirectory() {
            return workingDirectory;
        }

        /** @param workingDirectory the working directory for the process to set */
        public void setWorkingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        /** @return the environment variables to inject into the process */
        public Map<String, String> getEnvironmentVars() {
            return environmentVars;
        }

        /** @param environmentVars the environment variables to inject into the process */
        public void setEnvironmentVars(Map<String, String> environmentVars) {
            this.environmentVars = environmentVars;
        }

        /** @return whether to wrap the command in a shell invocation */
        public Boolean getShellWrap() {
            return shellWrap;
        }

        /** @param shellWrap whether to wrap the command in a shell invocation */
        public void setShellWrap(Boolean shellWrap) {
            this.shellWrap = shellWrap;
        }

        /** @return the shell executable path used when shell-wrapping */
        public String getShellPath() {
            return shellPath;
        }

        /** @param shellPath the shell executable path to set */
        public void setShellPath(String shellPath) {
            this.shellPath = shellPath;
        }

        /** @return the OS user to run the process as */
        public String getRunAsUser() {
            return runAsUser;
        }

        /** @param runAsUser the OS user to run the process as */
        public void setRunAsUser(String runAsUser) {
            this.runAsUser = runAsUser;
        }
    }

    /**
     * Configuration payload for a SQL-type scheduler job that runs a SQL statement
     * against either a stored datasource or an inline connection as a monitoring check.
     */
    public static class SqlConfigRequest {

        /** The identifier of a stored datasource to run against. */
        private UUID datasourceId;
        /** The database type when supplying an inline connection. */
        private String inlineDbType;
        /** The JDBC URL when supplying an inline connection. */
        private String inlineJdbcUrl;
        /** The username when supplying an inline connection. */
        private String inlineUsername;
        /** The password when supplying an inline connection. */
        private String inlinePassword;
        /** The SQL statement to execute. */
        private String sqlStatement;
        /** The SQL statement category (e.g. DML, DDL, QUERY). */
        private String sqlType = "DML";
        /** Whether to capture the result set of the statement. */
        private Boolean captureResultSet = false;
        /** The maximum number of result rows to capture. */
        private Integer maxResultRows = 100;
        /** The query timeout in seconds. */
        private Integer queryTimeoutSeconds = 60;

        /** @return the stored datasource identifier */
        public UUID getDatasourceId() {
            return datasourceId;
        }

        /** @param datasourceId the stored datasource identifier to set */
        public void setDatasourceId(UUID datasourceId) {
            this.datasourceId = datasourceId;
        }

        /** @return the inline database type */
        public String getInlineDbType() {
            return inlineDbType;
        }

        /** @param inlineDbType the inline database type to set */
        public void setInlineDbType(String inlineDbType) {
            this.inlineDbType = inlineDbType;
        }

        /** @return the inline JDBC URL */
        public String getInlineJdbcUrl() {
            return inlineJdbcUrl;
        }

        /** @param inlineJdbcUrl the inline JDBC URL to set */
        public void setInlineJdbcUrl(String inlineJdbcUrl) {
            this.inlineJdbcUrl = inlineJdbcUrl;
        }

        /** @return the inline connection username */
        public String getInlineUsername() {
            return inlineUsername;
        }

        /** @param inlineUsername the inline connection username to set */
        public void setInlineUsername(String inlineUsername) {
            this.inlineUsername = inlineUsername;
        }

        /** @return the inline connection password */
        public String getInlinePassword() {
            return inlinePassword;
        }

        /** @param inlinePassword the inline connection password to set */
        public void setInlinePassword(String inlinePassword) {
            this.inlinePassword = inlinePassword;
        }

        /** @return the SQL statement to execute */
        public String getSqlStatement() {
            return sqlStatement;
        }

        /** @param sqlStatement the SQL statement to execute */
        public void setSqlStatement(String sqlStatement) {
            this.sqlStatement = sqlStatement;
        }

        /** @return the SQL statement category */
        public String getSqlType() {
            return sqlType;
        }

        /** @param sqlType the SQL statement category to set */
        public void setSqlType(String sqlType) {
            this.sqlType = sqlType;
        }

        /** @return whether to capture the result set */
        public Boolean getCaptureResultSet() {
            return captureResultSet;
        }

        /** @param captureResultSet whether to capture the result set */
        public void setCaptureResultSet(Boolean captureResultSet) {
            this.captureResultSet = captureResultSet;
        }

        /** @return the maximum number of result rows to capture */
        public Integer getMaxResultRows() {
            return maxResultRows;
        }

        /** @param maxResultRows the maximum number of result rows to capture */
        public void setMaxResultRows(Integer maxResultRows) {
            this.maxResultRows = maxResultRows;
        }

        /** @return the query timeout in seconds */
        public Integer getQueryTimeoutSeconds() {
            return queryTimeoutSeconds;
        }

        /** @param queryTimeoutSeconds the query timeout in seconds to set */
        public void setQueryTimeoutSeconds(Integer queryTimeoutSeconds) {
            this.queryTimeoutSeconds = queryTimeoutSeconds;
        }
    }

    /**
     * Configuration payload for a REST-type scheduler job that issues an HTTP request
     * to a monitored endpoint, supporting multiple authentication schemes and
     * response assertions used to determine check success or failure.
     */
    public static class RestConfigRequest {

        /** The HTTP method to use. */
        private String httpMethod = "GET";
        /** The target request URL. */
        private String url;
        /** The request body payload. */
        private String requestBody;
        /** The request content type. */
        private String contentType = "application/json";
        /** The additional request headers. */
        private Map<String, String> headers;
        /** The additional query string parameters. */
        private Map<String, String> queryParams;
        /** The authentication scheme (e.g. NONE, BASIC, BEARER, API_KEY, OAUTH2). */
        private String authType = "NONE";
        /** The username for BASIC authentication. */
        private String authUsername;
        /** The password for BASIC authentication. */
        private String authPassword;
        /** The bearer token for token authentication. */
        private String authToken;
        /** The API key header/parameter name. */
        private String authApiKeyName;
        /** The API key value. */
        private String authApiKeyValue;
        /** Where to place the API key (e.g. HEADER or QUERY). */
        private String authApiKeyLocation;
        /** The OAuth2 token endpoint URL. */
        private String authOauth2TokenUrl;
        /** The OAuth2 client identifier. */
        private String authOauth2ClientId;
        /** The OAuth2 client secret. */
        private String authOauth2ClientSecret;
        /** The OAuth2 scope. */
        private String authOauth2Scope;
        /** Whether to verify the server's SSL certificate. */
        private Boolean sslVerify = true;
        /** The connection timeout in milliseconds. */
        private Integer connectTimeoutMs = 5000;
        /** The read timeout in milliseconds. */
        private Integer readTimeoutMs = 30000;
        /** Whether to follow HTTP redirects. */
        private Boolean followRedirects = true;
        /** The maximum number of response bytes to read. */
        private Integer maxResponseBytes = 102400;
        /** The expected HTTP status code asserted for success. */
        private Integer assertStatusCode;
        /** A substring asserted to be present in the response body. */
        private String assertBodyContains;
        /** A JSON path expression evaluated against the response body. */
        private String assertJsonPath;
        /** The expected value at the asserted JSON path. */
        private String assertJsonValue;

        /** @return the HTTP method to use */
        public String getHttpMethod() {
            return httpMethod;
        }

        /** @param httpMethod the HTTP method to use */
        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }

        /** @return the target request URL */
        public String getUrl() {
            return url;
        }

        /** @param url the target request URL to set */
        public void setUrl(String url) {
            this.url = url;
        }

        /** @return the request body payload */
        public String getRequestBody() {
            return requestBody;
        }

        /** @param requestBody the request body payload to set */
        public void setRequestBody(String requestBody) {
            this.requestBody = requestBody;
        }

        /** @return the request content type */
        public String getContentType() {
            return contentType;
        }

        /** @param contentType the request content type to set */
        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        /** @return the additional request headers */
        public Map<String, String> getHeaders() {
            return headers;
        }

        /** @param headers the additional request headers to set */
        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        /** @return the additional query string parameters */
        public Map<String, String> getQueryParams() {
            return queryParams;
        }

        /** @param queryParams the additional query string parameters to set */
        public void setQueryParams(Map<String, String> queryParams) {
            this.queryParams = queryParams;
        }

        /** @return the authentication scheme */
        public String getAuthType() {
            return authType;
        }

        /** @param authType the authentication scheme to set */
        public void setAuthType(String authType) {
            this.authType = authType;
        }

        /** @return the BASIC authentication username */
        public String getAuthUsername() {
            return authUsername;
        }

        /** @param authUsername the BASIC authentication username to set */
        public void setAuthUsername(String authUsername) {
            this.authUsername = authUsername;
        }

        /** @return the BASIC authentication password */
        public String getAuthPassword() {
            return authPassword;
        }

        /** @param authPassword the BASIC authentication password to set */
        public void setAuthPassword(String authPassword) {
            this.authPassword = authPassword;
        }

        /** @return the bearer token */
        public String getAuthToken() {
            return authToken;
        }

        /** @param authToken the bearer token to set */
        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        /** @return the API key header/parameter name */
        public String getAuthApiKeyName() {
            return authApiKeyName;
        }

        /** @param authApiKeyName the API key header/parameter name to set */
        public void setAuthApiKeyName(String authApiKeyName) {
            this.authApiKeyName = authApiKeyName;
        }

        /** @return the API key value */
        public String getAuthApiKeyValue() {
            return authApiKeyValue;
        }

        /** @param authApiKeyValue the API key value to set */
        public void setAuthApiKeyValue(String authApiKeyValue) {
            this.authApiKeyValue = authApiKeyValue;
        }

        /** @return where the API key is placed */
        public String getAuthApiKeyLocation() {
            return authApiKeyLocation;
        }

        /** @param authApiKeyLocation where the API key is placed */
        public void setAuthApiKeyLocation(String authApiKeyLocation) {
            this.authApiKeyLocation = authApiKeyLocation;
        }

        /** @return the OAuth2 token endpoint URL */
        public String getAuthOauth2TokenUrl() {
            return authOauth2TokenUrl;
        }

        /** @param authOauth2TokenUrl the OAuth2 token endpoint URL to set */
        public void setAuthOauth2TokenUrl(String authOauth2TokenUrl) {
            this.authOauth2TokenUrl = authOauth2TokenUrl;
        }

        /** @return the OAuth2 client identifier */
        public String getAuthOauth2ClientId() {
            return authOauth2ClientId;
        }

        /** @param authOauth2ClientId the OAuth2 client identifier to set */
        public void setAuthOauth2ClientId(String authOauth2ClientId) {
            this.authOauth2ClientId = authOauth2ClientId;
        }

        /** @return the OAuth2 client secret */
        public String getAuthOauth2ClientSecret() {
            return authOauth2ClientSecret;
        }

        /** @param authOauth2ClientSecret the OAuth2 client secret to set */
        public void setAuthOauth2ClientSecret(String authOauth2ClientSecret) {
            this.authOauth2ClientSecret = authOauth2ClientSecret;
        }

        /** @return the OAuth2 scope */
        public String getAuthOauth2Scope() {
            return authOauth2Scope;
        }

        /** @param authOauth2Scope the OAuth2 scope to set */
        public void setAuthOauth2Scope(String authOauth2Scope) {
            this.authOauth2Scope = authOauth2Scope;
        }

        /** @return whether the SSL certificate is verified */
        public Boolean getSslVerify() {
            return sslVerify;
        }

        /** @param sslVerify whether the SSL certificate is verified */
        public void setSslVerify(Boolean sslVerify) {
            this.sslVerify = sslVerify;
        }

        /** @return the connection timeout in milliseconds */
        public Integer getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        /** @param connectTimeoutMs the connection timeout in milliseconds to set */
        public void setConnectTimeoutMs(Integer connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        /** @return the read timeout in milliseconds */
        public Integer getReadTimeoutMs() {
            return readTimeoutMs;
        }

        /** @param readTimeoutMs the read timeout in milliseconds to set */
        public void setReadTimeoutMs(Integer readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        /** @return whether HTTP redirects are followed */
        public Boolean getFollowRedirects() {
            return followRedirects;
        }

        /** @param followRedirects whether HTTP redirects are followed */
        public void setFollowRedirects(Boolean followRedirects) {
            this.followRedirects = followRedirects;
        }

        /** @return the maximum number of response bytes to read */
        public Integer getMaxResponseBytes() {
            return maxResponseBytes;
        }

        /** @param maxResponseBytes the maximum number of response bytes to read */
        public void setMaxResponseBytes(Integer maxResponseBytes) {
            this.maxResponseBytes = maxResponseBytes;
        }

        /** @return the expected HTTP status code asserted for success */
        public Integer getAssertStatusCode() {
            return assertStatusCode;
        }

        /** @param assertStatusCode the expected HTTP status code asserted for success */
        public void setAssertStatusCode(Integer assertStatusCode) {
            this.assertStatusCode = assertStatusCode;
        }

        /** @return the substring asserted to be present in the response body */
        public String getAssertBodyContains() {
            return assertBodyContains;
        }

        /** @param assertBodyContains the substring asserted to be present in the response body */
        public void setAssertBodyContains(String assertBodyContains) {
            this.assertBodyContains = assertBodyContains;
        }

        /** @return the JSON path expression evaluated against the response body */
        public String getAssertJsonPath() {
            return assertJsonPath;
        }

        /** @param assertJsonPath the JSON path expression evaluated against the response body */
        public void setAssertJsonPath(String assertJsonPath) {
            this.assertJsonPath = assertJsonPath;
        }

        /** @return the expected value at the asserted JSON path */
        public String getAssertJsonValue() {
            return assertJsonValue;
        }

        /** @param assertJsonValue the expected value at the asserted JSON path */
        public void setAssertJsonValue(String assertJsonValue) {
            this.assertJsonValue = assertJsonValue;
        }
    }

    /**
     * Configuration payload for a SOAP-type scheduler job that invokes a SOAP web
     * service operation on a monitored endpoint as a monitoring check.
     */
    public static class SoapConfigRequest {

        /** The WSDL document URL describing the service. */
        private String wsdlUrl;
        /** The SOAP service endpoint URL. */
        private String endpointUrl;
        /** The WSDL service name. */
        private String serviceName;
        /** The WSDL port name. */
        private String portName;
        /** The operation name to invoke. */
        private String operationName;
        /** The SOAPAction HTTP header value. */
        private String soapAction;
        /** The SOAP protocol version (e.g. V1_1, V1_2). */
        private String soapVersion = "V1_1";
        /** The raw SOAP envelope to send. */
        private String soapEnvelope;
        /** The additional HTTP headers to send. */
        private Map<String, String> extraHeaders;
        /** The authentication scheme (e.g. NONE, BASIC, BEARER). */
        private String authType = "NONE";
        /** The username for BASIC authentication. */
        private String authUsername;
        /** The password for BASIC authentication. */
        private String authPassword;
        /** The bearer token for token authentication. */
        private String authToken;
        /** Whether to verify the server's SSL certificate. */
        private Boolean sslVerify = true;
        /** The connection timeout in milliseconds. */
        private Integer connectTimeoutMs = 5000;
        /** The read timeout in milliseconds. */
        private Integer readTimeoutMs = 60000;
        /** The maximum number of response bytes to read. */
        private Integer maxResponseBytes = 524288;

        /** @return the WSDL document URL */
        public String getWsdlUrl() {
            return wsdlUrl;
        }

        /** @param wsdlUrl the WSDL document URL to set */
        public void setWsdlUrl(String wsdlUrl) {
            this.wsdlUrl = wsdlUrl;
        }

        /** @return the SOAP service endpoint URL */
        public String getEndpointUrl() {
            return endpointUrl;
        }

        /** @param endpointUrl the SOAP service endpoint URL to set */
        public void setEndpointUrl(String endpointUrl) {
            this.endpointUrl = endpointUrl;
        }

        /** @return the WSDL service name */
        public String getServiceName() {
            return serviceName;
        }

        /** @param serviceName the WSDL service name to set */
        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        /** @return the WSDL port name */
        public String getPortName() {
            return portName;
        }

        /** @param portName the WSDL port name to set */
        public void setPortName(String portName) {
            this.portName = portName;
        }

        /** @return the operation name to invoke */
        public String getOperationName() {
            return operationName;
        }

        /** @param operationName the operation name to invoke */
        public void setOperationName(String operationName) {
            this.operationName = operationName;
        }

        /** @return the SOAPAction HTTP header value */
        public String getSoapAction() {
            return soapAction;
        }

        /** @param soapAction the SOAPAction HTTP header value to set */
        public void setSoapAction(String soapAction) {
            this.soapAction = soapAction;
        }

        /** @return the SOAP protocol version */
        public String getSoapVersion() {
            return soapVersion;
        }

        /** @param soapVersion the SOAP protocol version to set */
        public void setSoapVersion(String soapVersion) {
            this.soapVersion = soapVersion;
        }

        /** @return the raw SOAP envelope to send */
        public String getSoapEnvelope() {
            return soapEnvelope;
        }

        /** @param soapEnvelope the raw SOAP envelope to send */
        public void setSoapEnvelope(String soapEnvelope) {
            this.soapEnvelope = soapEnvelope;
        }

        /** @return the additional HTTP headers to send */
        public Map<String, String> getExtraHeaders() {
            return extraHeaders;
        }

        /** @param extraHeaders the additional HTTP headers to send */
        public void setExtraHeaders(Map<String, String> extraHeaders) {
            this.extraHeaders = extraHeaders;
        }

        /** @return the authentication scheme */
        public String getAuthType() {
            return authType;
        }

        /** @param authType the authentication scheme to set */
        public void setAuthType(String authType) {
            this.authType = authType;
        }

        /** @return the BASIC authentication username */
        public String getAuthUsername() {
            return authUsername;
        }

        /** @param authUsername the BASIC authentication username to set */
        public void setAuthUsername(String authUsername) {
            this.authUsername = authUsername;
        }

        /** @return the BASIC authentication password */
        public String getAuthPassword() {
            return authPassword;
        }

        /** @param authPassword the BASIC authentication password to set */
        public void setAuthPassword(String authPassword) {
            this.authPassword = authPassword;
        }

        /** @return the bearer token */
        public String getAuthToken() {
            return authToken;
        }

        /** @param authToken the bearer token to set */
        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        /** @return whether the SSL certificate is verified */
        public Boolean getSslVerify() {
            return sslVerify;
        }

        /** @param sslVerify whether the SSL certificate is verified */
        public void setSslVerify(Boolean sslVerify) {
            this.sslVerify = sslVerify;
        }

        /** @return the connection timeout in milliseconds */
        public Integer getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        /** @param connectTimeoutMs the connection timeout in milliseconds to set */
        public void setConnectTimeoutMs(Integer connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        /** @return the read timeout in milliseconds */
        public Integer getReadTimeoutMs() {
            return readTimeoutMs;
        }

        /** @param readTimeoutMs the read timeout in milliseconds to set */
        public void setReadTimeoutMs(Integer readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        /** @return the maximum number of response bytes to read */
        public Integer getMaxResponseBytes() {
            return maxResponseBytes;
        }

        /** @param maxResponseBytes the maximum number of response bytes to read */
        public void setMaxResponseBytes(Integer maxResponseBytes) {
            this.maxResponseBytes = maxResponseBytes;
        }
    }

    // -------------------------------------------------------------------------
    // Getters / setters for top-level fields
    // -------------------------------------------------------------------------

    /** @return the job type discriminator */
    public String getJobType() {
        return jobType;
    }

    /** @param jobType the job type discriminator to set */
    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    /** @return the cron expression defining the execution schedule */
    public String getCronExpression() {
        return cronExpression;
    }

    /** @param cronExpression the cron expression to set */
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    /** @return the time zone in which the cron expression is evaluated */
    public String getTimeZone() {
        return timeZone;
    }

    /** @param timeZone the time zone to set */
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    /** @return whether the job is enabled for scheduling */
    public Boolean getEnabled() {
        return enabled;
    }

    /** @param enabled whether the job is enabled for scheduling */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /** @return whether concurrent executions are permitted */
    public Boolean getAllowConcurrent() {
        return allowConcurrent;
    }

    /** @param allowConcurrent whether concurrent executions are permitted */
    public void setAllowConcurrent(Boolean allowConcurrent) {
        this.allowConcurrent = allowConcurrent;
    }

    /** @return the maximum number of retry attempts on failure */
    public Integer getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    /** @param maxRetryAttempts the maximum number of retry attempts on failure */
    public void setMaxRetryAttempts(Integer maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    /** @return the delay in seconds between retry attempts */
    public Integer getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    /** @param retryDelaySeconds the delay in seconds between retry attempts */
    public void setRetryDelaySeconds(Integer retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }

    /** @return the per-execution timeout in seconds */
    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /** @param timeoutSeconds the per-execution timeout in seconds */
    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /** @return the maximum number of captured output bytes to retain */
    public Integer getMaxOutputBytes() {
        return maxOutputBytes;
    }

    /** @param maxOutputBytes the maximum number of captured output bytes to retain */
    public void setMaxOutputBytes(Integer maxOutputBytes) {
        this.maxOutputBytes = maxOutputBytes;
    }

    /** @return the free-form tags used to categorize the job */
    public List<String> getTags() {
        return tags;
    }

    /** @param tags the free-form tags used to categorize the job */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /** @return the owning organization identifier */
    public UUID getOrganizationId() {
        return organizationId;
    }

    /** @param organizationId the owning organization identifier to set */
    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    /** @return the program-execution configuration */
    public ProgramConfigRequest getProgramConfig() {
        return programConfig;
    }

    /** @param programConfig the program-execution configuration to set */
    public void setProgramConfig(ProgramConfigRequest programConfig) {
        this.programConfig = programConfig;
    }

    /** @return the SQL-execution configuration */
    public SqlConfigRequest getSqlConfig() {
        return sqlConfig;
    }

    /** @param sqlConfig the SQL-execution configuration to set */
    public void setSqlConfig(SqlConfigRequest sqlConfig) {
        this.sqlConfig = sqlConfig;
    }

    /** @return the REST-call configuration */
    public RestConfigRequest getRestConfig() {
        return restConfig;
    }

    /** @param restConfig the REST-call configuration to set */
    public void setRestConfig(RestConfigRequest restConfig) {
        this.restConfig = restConfig;
    }

    /** @return the SOAP-call configuration */
    public SoapConfigRequest getSoapConfig() {
        return soapConfig;
    }

    /** @param soapConfig the SOAP-call configuration to set */
    public void setSoapConfig(SoapConfigRequest soapConfig) {
        this.soapConfig = soapConfig;
    }
}
