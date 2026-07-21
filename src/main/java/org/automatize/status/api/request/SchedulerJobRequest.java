package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private String name;

    /** The human-readable job description. */
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

    public static class SqlConfigRequest {

        private UUID datasourceId;
        private String inlineDbType;
        private String inlineJdbcUrl;
        private String inlineUsername;
        private String inlinePassword;
        private String sqlStatement;
        private String sqlType = "DML";
        private Boolean captureResultSet = false;
        private Integer maxResultRows = 100;
        private Integer queryTimeoutSeconds = 60;

        public UUID getDatasourceId() {
            return datasourceId;
        }

        public void setDatasourceId(UUID datasourceId) {
            this.datasourceId = datasourceId;
        }

        public String getInlineDbType() {
            return inlineDbType;
        }

        public void setInlineDbType(String inlineDbType) {
            this.inlineDbType = inlineDbType;
        }

        public String getInlineJdbcUrl() {
            return inlineJdbcUrl;
        }

        public void setInlineJdbcUrl(String inlineJdbcUrl) {
            this.inlineJdbcUrl = inlineJdbcUrl;
        }

        public String getInlineUsername() {
            return inlineUsername;
        }

        public void setInlineUsername(String inlineUsername) {
            this.inlineUsername = inlineUsername;
        }

        public String getInlinePassword() {
            return inlinePassword;
        }

        public void setInlinePassword(String inlinePassword) {
            this.inlinePassword = inlinePassword;
        }

        public String getSqlStatement() {
            return sqlStatement;
        }

        public void setSqlStatement(String sqlStatement) {
            this.sqlStatement = sqlStatement;
        }

        public String getSqlType() {
            return sqlType;
        }

        public void setSqlType(String sqlType) {
            this.sqlType = sqlType;
        }

        public Boolean getCaptureResultSet() {
            return captureResultSet;
        }

        public void setCaptureResultSet(Boolean captureResultSet) {
            this.captureResultSet = captureResultSet;
        }

        public Integer getMaxResultRows() {
            return maxResultRows;
        }

        public void setMaxResultRows(Integer maxResultRows) {
            this.maxResultRows = maxResultRows;
        }

        public Integer getQueryTimeoutSeconds() {
            return queryTimeoutSeconds;
        }

        public void setQueryTimeoutSeconds(Integer queryTimeoutSeconds) {
            this.queryTimeoutSeconds = queryTimeoutSeconds;
        }
    }

    public static class RestConfigRequest {

        private String httpMethod = "GET";
        private String url;
        private String requestBody;
        private String contentType = "application/json";
        private Map<String, String> headers;
        private Map<String, String> queryParams;
        private String authType = "NONE";
        private String authUsername;
        private String authPassword;
        private String authToken;
        private String authApiKeyName;
        private String authApiKeyValue;
        private String authApiKeyLocation;
        private String authOauth2TokenUrl;
        private String authOauth2ClientId;
        private String authOauth2ClientSecret;
        private String authOauth2Scope;
        private Boolean sslVerify = true;
        private Integer connectTimeoutMs = 5000;
        private Integer readTimeoutMs = 30000;
        private Boolean followRedirects = true;
        private Integer maxResponseBytes = 102400;
        private Integer assertStatusCode;
        private String assertBodyContains;
        private String assertJsonPath;
        private String assertJsonValue;

        public String getHttpMethod() {
            return httpMethod;
        }

        public void setHttpMethod(String httpMethod) {
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

        public String getAuthType() {
            return authType;
        }

        public void setAuthType(String authType) {
            this.authType = authType;
        }

        public String getAuthUsername() {
            return authUsername;
        }

        public void setAuthUsername(String authUsername) {
            this.authUsername = authUsername;
        }

        public String getAuthPassword() {
            return authPassword;
        }

        public void setAuthPassword(String authPassword) {
            this.authPassword = authPassword;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public String getAuthApiKeyName() {
            return authApiKeyName;
        }

        public void setAuthApiKeyName(String authApiKeyName) {
            this.authApiKeyName = authApiKeyName;
        }

        public String getAuthApiKeyValue() {
            return authApiKeyValue;
        }

        public void setAuthApiKeyValue(String authApiKeyValue) {
            this.authApiKeyValue = authApiKeyValue;
        }

        public String getAuthApiKeyLocation() {
            return authApiKeyLocation;
        }

        public void setAuthApiKeyLocation(String authApiKeyLocation) {
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

        public String getAuthOauth2ClientSecret() {
            return authOauth2ClientSecret;
        }

        public void setAuthOauth2ClientSecret(String authOauth2ClientSecret) {
            this.authOauth2ClientSecret = authOauth2ClientSecret;
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

    public static class SoapConfigRequest {

        private String wsdlUrl;
        private String endpointUrl;
        private String serviceName;
        private String portName;
        private String operationName;
        private String soapAction;
        private String soapVersion = "V1_1";
        private String soapEnvelope;
        private Map<String, String> extraHeaders;
        private String authType = "NONE";
        private String authUsername;
        private String authPassword;
        private String authToken;
        private Boolean sslVerify = true;
        private Integer connectTimeoutMs = 5000;
        private Integer readTimeoutMs = 60000;
        private Integer maxResponseBytes = 524288;

        public String getWsdlUrl() {
            return wsdlUrl;
        }

        public void setWsdlUrl(String wsdlUrl) {
            this.wsdlUrl = wsdlUrl;
        }

        public String getEndpointUrl() {
            return endpointUrl;
        }

        public void setEndpointUrl(String endpointUrl) {
            this.endpointUrl = endpointUrl;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getPortName() {
            return portName;
        }

        public void setPortName(String portName) {
            this.portName = portName;
        }

        public String getOperationName() {
            return operationName;
        }

        public void setOperationName(String operationName) {
            this.operationName = operationName;
        }

        public String getSoapAction() {
            return soapAction;
        }

        public void setSoapAction(String soapAction) {
            this.soapAction = soapAction;
        }

        public String getSoapVersion() {
            return soapVersion;
        }

        public void setSoapVersion(String soapVersion) {
            this.soapVersion = soapVersion;
        }

        public String getSoapEnvelope() {
            return soapEnvelope;
        }

        public void setSoapEnvelope(String soapEnvelope) {
            this.soapEnvelope = soapEnvelope;
        }

        public Map<String, String> getExtraHeaders() {
            return extraHeaders;
        }

        public void setExtraHeaders(Map<String, String> extraHeaders) {
            this.extraHeaders = extraHeaders;
        }

        public String getAuthType() {
            return authType;
        }

        public void setAuthType(String authType) {
            this.authType = authType;
        }

        public String getAuthUsername() {
            return authUsername;
        }

        public void setAuthUsername(String authUsername) {
            this.authUsername = authUsername;
        }

        public String getAuthPassword() {
            return authPassword;
        }

        public void setAuthPassword(String authPassword) {
            this.authPassword = authPassword;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public Boolean getSslVerify() {
            return sslVerify;
        }

        public void setSslVerify(Boolean sslVerify) {
            this.sslVerify = sslVerify;
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

        public Integer getMaxResponseBytes() {
            return maxResponseBytes;
        }

        public void setMaxResponseBytes(Integer maxResponseBytes) {
            this.maxResponseBytes = maxResponseBytes;
        }
    }

    // -------------------------------------------------------------------------
    // Getters / setters for top-level fields
    // -------------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getAllowConcurrent() {
        return allowConcurrent;
    }

    public void setAllowConcurrent(Boolean allowConcurrent) {
        this.allowConcurrent = allowConcurrent;
    }

    public Integer getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(Integer maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public Integer getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    public void setRetryDelaySeconds(Integer retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Integer getMaxOutputBytes() {
        return maxOutputBytes;
    }

    public void setMaxOutputBytes(Integer maxOutputBytes) {
        this.maxOutputBytes = maxOutputBytes;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public ProgramConfigRequest getProgramConfig() {
        return programConfig;
    }

    public void setProgramConfig(ProgramConfigRequest programConfig) {
        this.programConfig = programConfig;
    }

    public SqlConfigRequest getSqlConfig() {
        return sqlConfig;
    }

    public void setSqlConfig(SqlConfigRequest sqlConfig) {
        this.sqlConfig = sqlConfig;
    }

    public RestConfigRequest getRestConfig() {
        return restConfig;
    }

    public void setRestConfig(RestConfigRequest restConfig) {
        this.restConfig = restConfig;
    }

    public SoapConfigRequest getSoapConfig() {
        return soapConfig;
    }

    public void setSoapConfig(SoapConfigRequest soapConfig) {
        this.soapConfig = soapConfig;
    }
}
