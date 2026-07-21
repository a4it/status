package org.automatize.status.api.response;

import org.automatize.status.models.*;
import org.automatize.status.models.scheduler.JobType;
import org.automatize.status.services.scheduler.CronValidationService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 * Response class for a scheduler job in the status-monitoring application,
 * exposing a job's schedule, execution state and type-specific configuration
 * over the REST API. Sensitive credential fields are masked (replaced with
 * bullet characters) and encrypted storage fields are never exposed.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Expose scheduler job metadata, cron schedule and human-readable schedule</li>
 *   <li>Report execution state such as status, last/next run and failure counts</li>
 *   <li>Attach the appropriate type-specific configuration (program, SQL, REST or SOAP)</li>
 *   <li>Mask credentials and omit encrypted values so secrets are never leaked</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
public class SchedulerJobResponse {

    private static final String MASKED = "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022";

    private UUID id;
    private String name;
    private String description;
    private String jobType;
    private String cronExpression;
    private String timeZone;
    private String humanReadableCron;
    private Boolean enabled;
    private Boolean allowConcurrent;
    private String status;
    private String lastRunStatus;
    private ZonedDateTime lastRunAt;
    private ZonedDateTime nextRunAt;
    private Integer consecutiveFailures;
    private Integer maxRetryAttempts;
    private Integer retryDelaySeconds;
    private Integer timeoutSeconds;
    private Integer maxOutputBytes;
    private List<String> tags;
    private UUID tenantId;
    private UUID organizationId;
    private ZonedDateTime createdDate;
    private String createdBy;

    private ProgramConfigResponse programConfig;
    private SqlConfigResponse sqlConfig;
    private RestConfigResponse restConfig;
    private SoapConfigResponse soapConfig;

    // -------------------------------------------------------------------------
    // Factory method
    // -------------------------------------------------------------------------

    /**
     * Builds a response from a persisted scheduler job entity, computing the
     * human-readable cron description and attaching only the type-specific
     * configuration matching the job's type. Sensitive fields are masked or
     * omitted during mapping.
     *
     * @param job         the scheduler job entity to convert
     * @param cronService the cron validation service used to render a
     *                    human-readable schedule; when {@code null} the raw
     *                    cron expression is used instead
     * @return the populated response
     */
    public static SchedulerJobResponse fromEntity(SchedulerJob job, CronValidationService cronService) {
        SchedulerJobResponse r = new SchedulerJobResponse();
        r.id = job.getId();
        r.name = job.getName();
        r.description = job.getDescription();
        r.jobType = job.getJobType() != null ? job.getJobType().name() : null;
        r.cronExpression = job.getCronExpression();
        r.timeZone = job.getTimeZone();
        r.humanReadableCron = cronService != null ? cronService.toHumanReadable(job.getCronExpression()) : job.getCronExpression();
        r.enabled = job.getEnabled();
        r.allowConcurrent = job.getAllowConcurrent();
        r.status = job.getStatus() != null ? job.getStatus().name() : null;
        r.lastRunStatus = job.getLastRunStatus() != null ? job.getLastRunStatus().name() : null;
        r.lastRunAt = job.getLastRunAt();
        r.nextRunAt = job.getNextRunAt();
        r.consecutiveFailures = job.getConsecutiveFailures();
        r.maxRetryAttempts = job.getMaxRetryAttempts();
        r.retryDelaySeconds = job.getRetryDelaySeconds();
        r.timeoutSeconds = job.getTimeoutSeconds();
        r.maxOutputBytes = job.getMaxOutputBytes();
        r.tags = job.getTags();
        r.tenantId = job.getTenant() != null ? job.getTenant().getId() : null;
        r.organizationId = job.getOrganization() != null ? job.getOrganization().getId() : null;
        r.createdDate = job.getCreatedDate();
        r.createdBy = job.getCreatedBy();

        // Attach program configuration only for PROGRAM jobs that have one
        if (job.getJobType() == JobType.PROGRAM && job.getProgramConfig() != null) {
            r.programConfig = mapProgramConfig(job.getProgramConfig());
        }
        // Attach SQL configuration only for SQL jobs that have one
        if (job.getJobType() == JobType.SQL && job.getSqlConfig() != null) {
            r.sqlConfig = mapSqlConfig(job.getSqlConfig());
        }
        // Attach REST configuration only for REST jobs that have one
        if (job.getJobType() == JobType.REST && job.getRestConfig() != null) {
            r.restConfig = mapRestConfig(job.getRestConfig());
        }
        // Attach SOAP configuration only for SOAP jobs that have one
        if (job.getJobType() == JobType.SOAP && job.getSoapConfig() != null) {
            r.soapConfig = mapSoapConfig(job.getSoapConfig());
        }

        return r;
    }

    /**
     * Maps a program job configuration entity to its response form.
     *
     * @param cfg the program configuration entity
     * @return the mapped program configuration response
     */
    private static ProgramConfigResponse mapProgramConfig(SchedulerProgramConfig cfg) {
        ProgramConfigResponse r = new ProgramConfigResponse();
        r.id = cfg.getId();
        r.command = cfg.getCommand();
        r.arguments = cfg.getArguments();
        r.workingDirectory = cfg.getWorkingDirectory();
        r.environmentVars = cfg.getEnvironmentVars();
        r.shellWrap = cfg.getShellWrap();
        r.shellPath = cfg.getShellPath();
        r.runAsUser = cfg.getRunAsUser();
        return r;
    }

    /**
     * Maps a SQL job configuration entity to its response form. The encrypted
     * inline password is never copied into the response.
     *
     * @param cfg the SQL configuration entity
     * @return the mapped SQL configuration response
     */
    private static SqlConfigResponse mapSqlConfig(SchedulerSqlConfig cfg) {
        SqlConfigResponse r = new SqlConfigResponse();
        r.id = cfg.getId();
        r.datasourceId = cfg.getDatasource() != null ? cfg.getDatasource().getId() : null;
        r.inlineDbType = cfg.getInlineDbType() != null ? cfg.getInlineDbType().name() : null;
        r.inlineJdbcUrl = cfg.getInlineJdbcUrl();
        r.inlineUsername = cfg.getInlineUsername();
        // inlinePasswordEnc is never returned
        r.sqlStatement = cfg.getSqlStatement();
        r.sqlType = cfg.getSqlType() != null ? cfg.getSqlType().name() : null;
        r.captureResultSet = cfg.getCaptureResultSet();
        r.maxResultRows = cfg.getMaxResultRows();
        r.queryTimeoutSeconds = cfg.getQueryTimeoutSeconds();
        return r;
    }

    /**
     * Maps a REST job configuration entity to its response form. All encrypted
     * credential fields are replaced with a masked placeholder when a value is
     * present and left {@code null} otherwise.
     *
     * @param cfg the REST configuration entity
     * @return the mapped REST configuration response
     */
    private static RestConfigResponse mapRestConfig(SchedulerRestConfig cfg) {
        RestConfigResponse r = new RestConfigResponse();
        r.id = cfg.getId();
        r.httpMethod = cfg.getHttpMethod() != null ? cfg.getHttpMethod().name() : null;
        r.url = cfg.getUrl();
        r.requestBody = cfg.getRequestBody();
        r.contentType = cfg.getContentType();
        r.headers = cfg.getHeaders();
        r.queryParams = cfg.getQueryParams();
        r.authType = cfg.getAuthType() != null ? cfg.getAuthType().name() : null;
        r.authUsername = cfg.getAuthUsername();
        // Mask sensitive fields: show bullet placeholder only when a value is stored
        r.authPassword = cfg.getAuthPasswordEnc() != null && !cfg.getAuthPasswordEnc().isBlank() ? MASKED : null;
        r.authToken = cfg.getAuthTokenEnc() != null && !cfg.getAuthTokenEnc().isBlank() ? MASKED : null;
        r.authApiKeyName = cfg.getAuthApiKeyName();
        r.authApiKeyValue = cfg.getAuthApiKeyValueEnc() != null && !cfg.getAuthApiKeyValueEnc().isBlank() ? MASKED : null;
        r.authApiKeyLocation = cfg.getAuthApiKeyLocation() != null ? cfg.getAuthApiKeyLocation().name() : null;
        r.authOauth2TokenUrl = cfg.getAuthOauth2TokenUrl();
        r.authOauth2ClientId = cfg.getAuthOauth2ClientId();
        r.authOauth2ClientSecret = cfg.getAuthOauth2ClientSecretEnc() != null && !cfg.getAuthOauth2ClientSecretEnc().isBlank() ? MASKED : null;
        r.authOauth2Scope = cfg.getAuthOauth2Scope();
        r.sslVerify = cfg.getSslVerify();
        r.connectTimeoutMs = cfg.getConnectTimeoutMs();
        r.readTimeoutMs = cfg.getReadTimeoutMs();
        r.followRedirects = cfg.getFollowRedirects();
        r.maxResponseBytes = cfg.getMaxResponseBytes();
        r.assertStatusCode = cfg.getAssertStatusCode();
        r.assertBodyContains = cfg.getAssertBodyContains();
        r.assertJsonPath = cfg.getAssertJsonPath();
        r.assertJsonValue = cfg.getAssertJsonValue();
        return r;
    }

    /**
     * Maps a SOAP job configuration entity to its response form. All encrypted
     * credential fields are replaced with a masked placeholder when a value is
     * present and left {@code null} otherwise.
     *
     * @param cfg the SOAP configuration entity
     * @return the mapped SOAP configuration response
     */
    private static SoapConfigResponse mapSoapConfig(SchedulerSoapConfig cfg) {
        SoapConfigResponse r = new SoapConfigResponse();
        r.id = cfg.getId();
        r.wsdlUrl = cfg.getWsdlUrl();
        r.endpointUrl = cfg.getEndpointUrl();
        r.serviceName = cfg.getServiceName();
        r.portName = cfg.getPortName();
        r.operationName = cfg.getOperationName();
        r.soapAction = cfg.getSoapAction();
        r.soapVersion = cfg.getSoapVersion() != null ? cfg.getSoapVersion().name() : null;
        r.soapEnvelope = cfg.getSoapEnvelope();
        r.extraHeaders = cfg.getExtraHeaders();
        r.authType = cfg.getAuthType() != null ? cfg.getAuthType().name() : null;
        r.authUsername = cfg.getAuthUsername();
        r.authPassword = cfg.getAuthPasswordEnc() != null && !cfg.getAuthPasswordEnc().isBlank() ? MASKED : null;
        r.authToken = cfg.getAuthTokenEnc() != null && !cfg.getAuthTokenEnc().isBlank() ? MASKED : null;
        r.sslVerify = cfg.getSslVerify();
        r.connectTimeoutMs = cfg.getConnectTimeoutMs();
        r.readTimeoutMs = cfg.getReadTimeoutMs();
        r.maxResponseBytes = cfg.getMaxResponseBytes();
        return r;
    }

    // -------------------------------------------------------------------------
    // Nested response classes
    // -------------------------------------------------------------------------

    /**
     * <p>
     * Response form of a program (operating-system command) scheduler job
     * configuration, describing the command, arguments and execution
     * environment.
     * </p>
     */
    public static class ProgramConfigResponse {
        private UUID id;
        private String command;
        private List<String> arguments;
        private String workingDirectory;
        private Map<String, String> environmentVars;
        private Boolean shellWrap;
        private String shellPath;
        private String runAsUser;

        /** Gets the configuration id. @return the id */
        public UUID getId() { return id; }
        /** Sets the configuration id. @param id the id to set */
        public void setId(UUID id) { this.id = id; }
        /** Gets the command to execute. @return the command */
        public String getCommand() { return command; }
        /** Sets the command to execute. @param command the command to set */
        public void setCommand(String command) { this.command = command; }
        /** Gets the command arguments. @return the arguments */
        public List<String> getArguments() { return arguments; }
        /** Sets the command arguments. @param arguments the arguments to set */
        public void setArguments(List<String> arguments) { this.arguments = arguments; }
        /** Gets the working directory. @return the working directory */
        public String getWorkingDirectory() { return workingDirectory; }
        /** Sets the working directory. @param workingDirectory the working directory to set */
        public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }
        /** Gets the environment variables. @return the environment variables */
        public Map<String, String> getEnvironmentVars() { return environmentVars; }
        /** Sets the environment variables. @param environmentVars the environment variables to set */
        public void setEnvironmentVars(Map<String, String> environmentVars) { this.environmentVars = environmentVars; }
        /** Gets whether the command is wrapped in a shell. @return the shell-wrap flag */
        public Boolean getShellWrap() { return shellWrap; }
        /** Sets whether the command is wrapped in a shell. @param shellWrap the shell-wrap flag to set */
        public void setShellWrap(Boolean shellWrap) { this.shellWrap = shellWrap; }
        /** Gets the shell path. @return the shell path */
        public String getShellPath() { return shellPath; }
        /** Sets the shell path. @param shellPath the shell path to set */
        public void setShellPath(String shellPath) { this.shellPath = shellPath; }
        /** Gets the OS user to run as. @return the run-as user */
        public String getRunAsUser() { return runAsUser; }
        /** Sets the OS user to run as. @param runAsUser the run-as user to set */
        public void setRunAsUser(String runAsUser) { this.runAsUser = runAsUser; }
    }

    /**
     * <p>
     * Response form of a SQL scheduler job configuration, describing the target
     * datasource (referenced or inline), the statement to run and result
     * handling. The stored password is never exposed.
     * </p>
     */
    public static class SqlConfigResponse {
        private UUID id;
        private UUID datasourceId;
        private String inlineDbType;
        private String inlineJdbcUrl;
        private String inlineUsername;
        // password is never returned
        private String sqlStatement;
        private String sqlType;
        private Boolean captureResultSet;
        private Integer maxResultRows;
        private Integer queryTimeoutSeconds;

        /** Gets the configuration id. @return the id */
        public UUID getId() { return id; }
        /** Sets the configuration id. @param id the id to set */
        public void setId(UUID id) { this.id = id; }
        /** Gets the referenced datasource id. @return the datasource id */
        public UUID getDatasourceId() { return datasourceId; }
        /** Sets the referenced datasource id. @param datasourceId the datasource id to set */
        public void setDatasourceId(UUID datasourceId) { this.datasourceId = datasourceId; }
        /** Gets the inline database type. @return the inline database type */
        public String getInlineDbType() { return inlineDbType; }
        /** Sets the inline database type. @param inlineDbType the inline database type to set */
        public void setInlineDbType(String inlineDbType) { this.inlineDbType = inlineDbType; }
        /** Gets the inline JDBC URL. @return the inline JDBC URL */
        public String getInlineJdbcUrl() { return inlineJdbcUrl; }
        /** Sets the inline JDBC URL. @param inlineJdbcUrl the inline JDBC URL to set */
        public void setInlineJdbcUrl(String inlineJdbcUrl) { this.inlineJdbcUrl = inlineJdbcUrl; }
        /** Gets the inline username. @return the inline username */
        public String getInlineUsername() { return inlineUsername; }
        /** Sets the inline username. @param inlineUsername the inline username to set */
        public void setInlineUsername(String inlineUsername) { this.inlineUsername = inlineUsername; }
        /** Gets the SQL statement. @return the SQL statement */
        public String getSqlStatement() { return sqlStatement; }
        /** Sets the SQL statement. @param sqlStatement the SQL statement to set */
        public void setSqlStatement(String sqlStatement) { this.sqlStatement = sqlStatement; }
        /** Gets the SQL type. @return the SQL type */
        public String getSqlType() { return sqlType; }
        /** Sets the SQL type. @param sqlType the SQL type to set */
        public void setSqlType(String sqlType) { this.sqlType = sqlType; }
        /** Gets whether the result set is captured. @return the capture-result-set flag */
        public Boolean getCaptureResultSet() { return captureResultSet; }
        /** Sets whether the result set is captured. @param captureResultSet the capture-result-set flag to set */
        public void setCaptureResultSet(Boolean captureResultSet) { this.captureResultSet = captureResultSet; }
        /** Gets the maximum number of result rows. @return the maximum result rows */
        public Integer getMaxResultRows() { return maxResultRows; }
        /** Sets the maximum number of result rows. @param maxResultRows the maximum result rows to set */
        public void setMaxResultRows(Integer maxResultRows) { this.maxResultRows = maxResultRows; }
        /** Gets the query timeout in seconds. @return the query timeout in seconds */
        public Integer getQueryTimeoutSeconds() { return queryTimeoutSeconds; }
        /** Sets the query timeout in seconds. @param queryTimeoutSeconds the query timeout in seconds to set */
        public void setQueryTimeoutSeconds(Integer queryTimeoutSeconds) { this.queryTimeoutSeconds = queryTimeoutSeconds; }
    }

    /**
     * <p>
     * Response form of a REST scheduler job configuration, describing the HTTP
     * request, authentication, transport options and response assertions.
     * Credential fields are masked and never carry the actual value.
     * </p>
     */
    public static class RestConfigResponse {
        private UUID id;
        private String httpMethod;
        private String url;
        private String requestBody;
        private String contentType;
        private Map<String, String> headers;
        private Map<String, String> queryParams;
        private String authType;
        private String authUsername;
        /** Masked — never contains actual value. */
        private String authPassword;
        /** Masked — never contains actual value. */
        private String authToken;
        private String authApiKeyName;
        /** Masked — never contains actual value. */
        private String authApiKeyValue;
        private String authApiKeyLocation;
        private String authOauth2TokenUrl;
        private String authOauth2ClientId;
        /** Masked — never contains actual value. */
        private String authOauth2ClientSecret;
        private String authOauth2Scope;
        private Boolean sslVerify;
        private Integer connectTimeoutMs;
        private Integer readTimeoutMs;
        private Boolean followRedirects;
        private Integer maxResponseBytes;
        private Integer assertStatusCode;
        private String assertBodyContains;
        private String assertJsonPath;
        private String assertJsonValue;

        /** Gets the configuration id. @return the id */
        public UUID getId() { return id; }
        /** Sets the configuration id. @param id the id to set */
        public void setId(UUID id) { this.id = id; }
        /** Gets the HTTP method. @return the HTTP method */
        public String getHttpMethod() { return httpMethod; }
        /** Sets the HTTP method. @param httpMethod the HTTP method to set */
        public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
        /** Gets the request URL. @return the URL */
        public String getUrl() { return url; }
        /** Sets the request URL. @param url the URL to set */
        public void setUrl(String url) { this.url = url; }
        /** Gets the request body. @return the request body */
        public String getRequestBody() { return requestBody; }
        /** Sets the request body. @param requestBody the request body to set */
        public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
        /** Gets the content type. @return the content type */
        public String getContentType() { return contentType; }
        /** Sets the content type. @param contentType the content type to set */
        public void setContentType(String contentType) { this.contentType = contentType; }
        /** Gets the request headers. @return the headers */
        public Map<String, String> getHeaders() { return headers; }
        /** Sets the request headers. @param headers the headers to set */
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        /** Gets the query parameters. @return the query parameters */
        public Map<String, String> getQueryParams() { return queryParams; }
        /** Sets the query parameters. @param queryParams the query parameters to set */
        public void setQueryParams(Map<String, String> queryParams) { this.queryParams = queryParams; }
        /** Gets the authentication type. @return the authentication type */
        public String getAuthType() { return authType; }
        /** Sets the authentication type. @param authType the authentication type to set */
        public void setAuthType(String authType) { this.authType = authType; }
        /** Gets the authentication username. @return the authentication username */
        public String getAuthUsername() { return authUsername; }
        /** Sets the authentication username. @param authUsername the authentication username to set */
        public void setAuthUsername(String authUsername) { this.authUsername = authUsername; }
        /** Gets the masked authentication password. @return the masked authentication password */
        public String getAuthPassword() { return authPassword; }
        /** Sets the masked authentication password. @param authPassword the masked authentication password to set */
        public void setAuthPassword(String authPassword) { this.authPassword = authPassword; }
        /** Gets the masked authentication token. @return the masked authentication token */
        public String getAuthToken() { return authToken; }
        /** Sets the masked authentication token. @param authToken the masked authentication token to set */
        public void setAuthToken(String authToken) { this.authToken = authToken; }
        /** Gets the API key name. @return the API key name */
        public String getAuthApiKeyName() { return authApiKeyName; }
        /** Sets the API key name. @param authApiKeyName the API key name to set */
        public void setAuthApiKeyName(String authApiKeyName) { this.authApiKeyName = authApiKeyName; }
        /** Gets the masked API key value. @return the masked API key value */
        public String getAuthApiKeyValue() { return authApiKeyValue; }
        /** Sets the masked API key value. @param authApiKeyValue the masked API key value to set */
        public void setAuthApiKeyValue(String authApiKeyValue) { this.authApiKeyValue = authApiKeyValue; }
        /** Gets the API key location. @return the API key location */
        public String getAuthApiKeyLocation() { return authApiKeyLocation; }
        /** Sets the API key location. @param authApiKeyLocation the API key location to set */
        public void setAuthApiKeyLocation(String authApiKeyLocation) { this.authApiKeyLocation = authApiKeyLocation; }
        /** Gets the OAuth2 token URL. @return the OAuth2 token URL */
        public String getAuthOauth2TokenUrl() { return authOauth2TokenUrl; }
        /** Sets the OAuth2 token URL. @param authOauth2TokenUrl the OAuth2 token URL to set */
        public void setAuthOauth2TokenUrl(String authOauth2TokenUrl) { this.authOauth2TokenUrl = authOauth2TokenUrl; }
        /** Gets the OAuth2 client id. @return the OAuth2 client id */
        public String getAuthOauth2ClientId() { return authOauth2ClientId; }
        /** Sets the OAuth2 client id. @param authOauth2ClientId the OAuth2 client id to set */
        public void setAuthOauth2ClientId(String authOauth2ClientId) { this.authOauth2ClientId = authOauth2ClientId; }
        /** Gets the masked OAuth2 client secret. @return the masked OAuth2 client secret */
        public String getAuthOauth2ClientSecret() { return authOauth2ClientSecret; }
        /** Sets the masked OAuth2 client secret. @param authOauth2ClientSecret the masked OAuth2 client secret to set */
        public void setAuthOauth2ClientSecret(String authOauth2ClientSecret) { this.authOauth2ClientSecret = authOauth2ClientSecret; }
        /** Gets the OAuth2 scope. @return the OAuth2 scope */
        public String getAuthOauth2Scope() { return authOauth2Scope; }
        /** Sets the OAuth2 scope. @param authOauth2Scope the OAuth2 scope to set */
        public void setAuthOauth2Scope(String authOauth2Scope) { this.authOauth2Scope = authOauth2Scope; }
        /** Gets whether SSL verification is enabled. @return the SSL-verify flag */
        public Boolean getSslVerify() { return sslVerify; }
        /** Sets whether SSL verification is enabled. @param sslVerify the SSL-verify flag to set */
        public void setSslVerify(Boolean sslVerify) { this.sslVerify = sslVerify; }
        /** Gets the connect timeout in milliseconds. @return the connect timeout in milliseconds */
        public Integer getConnectTimeoutMs() { return connectTimeoutMs; }
        /** Sets the connect timeout in milliseconds. @param connectTimeoutMs the connect timeout in milliseconds to set */
        public void setConnectTimeoutMs(Integer connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        /** Gets the read timeout in milliseconds. @return the read timeout in milliseconds */
        public Integer getReadTimeoutMs() { return readTimeoutMs; }
        /** Sets the read timeout in milliseconds. @param readTimeoutMs the read timeout in milliseconds to set */
        public void setReadTimeoutMs(Integer readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
        /** Gets whether redirects are followed. @return the follow-redirects flag */
        public Boolean getFollowRedirects() { return followRedirects; }
        /** Sets whether redirects are followed. @param followRedirects the follow-redirects flag to set */
        public void setFollowRedirects(Boolean followRedirects) { this.followRedirects = followRedirects; }
        /** Gets the maximum response size in bytes. @return the maximum response bytes */
        public Integer getMaxResponseBytes() { return maxResponseBytes; }
        /** Sets the maximum response size in bytes. @param maxResponseBytes the maximum response bytes to set */
        public void setMaxResponseBytes(Integer maxResponseBytes) { this.maxResponseBytes = maxResponseBytes; }
        /** Gets the asserted status code. @return the asserted status code */
        public Integer getAssertStatusCode() { return assertStatusCode; }
        /** Sets the asserted status code. @param assertStatusCode the asserted status code to set */
        public void setAssertStatusCode(Integer assertStatusCode) { this.assertStatusCode = assertStatusCode; }
        /** Gets the asserted body substring. @return the asserted body substring */
        public String getAssertBodyContains() { return assertBodyContains; }
        /** Sets the asserted body substring. @param assertBodyContains the asserted body substring to set */
        public void setAssertBodyContains(String assertBodyContains) { this.assertBodyContains = assertBodyContains; }
        /** Gets the asserted JSON path. @return the asserted JSON path */
        public String getAssertJsonPath() { return assertJsonPath; }
        /** Sets the asserted JSON path. @param assertJsonPath the asserted JSON path to set */
        public void setAssertJsonPath(String assertJsonPath) { this.assertJsonPath = assertJsonPath; }
        /** Gets the asserted JSON value. @return the asserted JSON value */
        public String getAssertJsonValue() { return assertJsonValue; }
        /** Sets the asserted JSON value. @param assertJsonValue the asserted JSON value to set */
        public void setAssertJsonValue(String assertJsonValue) { this.assertJsonValue = assertJsonValue; }
    }

    /**
     * <p>
     * Response form of a SOAP scheduler job configuration, describing the web
     * service endpoint, operation, envelope and authentication. Credential
     * fields are masked and never carry the actual value.
     * </p>
     */
    public static class SoapConfigResponse {
        private UUID id;
        private String wsdlUrl;
        private String endpointUrl;
        private String serviceName;
        private String portName;
        private String operationName;
        private String soapAction;
        private String soapVersion;
        private String soapEnvelope;
        private Map<String, String> extraHeaders;
        private String authType;
        private String authUsername;
        /** Masked — never contains actual value. */
        private String authPassword;
        /** Masked — never contains actual value. */
        private String authToken;
        private Boolean sslVerify;
        private Integer connectTimeoutMs;
        private Integer readTimeoutMs;
        private Integer maxResponseBytes;

        /** Gets the configuration id. @return the id */
        public UUID getId() { return id; }
        /** Sets the configuration id. @param id the id to set */
        public void setId(UUID id) { this.id = id; }
        /** Gets the WSDL URL. @return the WSDL URL */
        public String getWsdlUrl() { return wsdlUrl; }
        /** Sets the WSDL URL. @param wsdlUrl the WSDL URL to set */
        public void setWsdlUrl(String wsdlUrl) { this.wsdlUrl = wsdlUrl; }
        /** Gets the endpoint URL. @return the endpoint URL */
        public String getEndpointUrl() { return endpointUrl; }
        /** Sets the endpoint URL. @param endpointUrl the endpoint URL to set */
        public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
        /** Gets the service name. @return the service name */
        public String getServiceName() { return serviceName; }
        /** Sets the service name. @param serviceName the service name to set */
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        /** Gets the port name. @return the port name */
        public String getPortName() { return portName; }
        /** Sets the port name. @param portName the port name to set */
        public void setPortName(String portName) { this.portName = portName; }
        /** Gets the operation name. @return the operation name */
        public String getOperationName() { return operationName; }
        /** Sets the operation name. @param operationName the operation name to set */
        public void setOperationName(String operationName) { this.operationName = operationName; }
        /** Gets the SOAP action. @return the SOAP action */
        public String getSoapAction() { return soapAction; }
        /** Sets the SOAP action. @param soapAction the SOAP action to set */
        public void setSoapAction(String soapAction) { this.soapAction = soapAction; }
        /** Gets the SOAP version. @return the SOAP version */
        public String getSoapVersion() { return soapVersion; }
        /** Sets the SOAP version. @param soapVersion the SOAP version to set */
        public void setSoapVersion(String soapVersion) { this.soapVersion = soapVersion; }
        /** Gets the SOAP envelope. @return the SOAP envelope */
        public String getSoapEnvelope() { return soapEnvelope; }
        /** Sets the SOAP envelope. @param soapEnvelope the SOAP envelope to set */
        public void setSoapEnvelope(String soapEnvelope) { this.soapEnvelope = soapEnvelope; }
        /** Gets the extra headers. @return the extra headers */
        public Map<String, String> getExtraHeaders() { return extraHeaders; }
        /** Sets the extra headers. @param extraHeaders the extra headers to set */
        public void setExtraHeaders(Map<String, String> extraHeaders) { this.extraHeaders = extraHeaders; }
        /** Gets the authentication type. @return the authentication type */
        public String getAuthType() { return authType; }
        /** Sets the authentication type. @param authType the authentication type to set */
        public void setAuthType(String authType) { this.authType = authType; }
        /** Gets the authentication username. @return the authentication username */
        public String getAuthUsername() { return authUsername; }
        /** Sets the authentication username. @param authUsername the authentication username to set */
        public void setAuthUsername(String authUsername) { this.authUsername = authUsername; }
        /** Gets the masked authentication password. @return the masked authentication password */
        public String getAuthPassword() { return authPassword; }
        /** Sets the masked authentication password. @param authPassword the masked authentication password to set */
        public void setAuthPassword(String authPassword) { this.authPassword = authPassword; }
        /** Gets the masked authentication token. @return the masked authentication token */
        public String getAuthToken() { return authToken; }
        /** Sets the masked authentication token. @param authToken the masked authentication token to set */
        public void setAuthToken(String authToken) { this.authToken = authToken; }
        /** Gets whether SSL verification is enabled. @return the SSL-verify flag */
        public Boolean getSslVerify() { return sslVerify; }
        /** Sets whether SSL verification is enabled. @param sslVerify the SSL-verify flag to set */
        public void setSslVerify(Boolean sslVerify) { this.sslVerify = sslVerify; }
        /** Gets the connect timeout in milliseconds. @return the connect timeout in milliseconds */
        public Integer getConnectTimeoutMs() { return connectTimeoutMs; }
        /** Sets the connect timeout in milliseconds. @param connectTimeoutMs the connect timeout in milliseconds to set */
        public void setConnectTimeoutMs(Integer connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        /** Gets the read timeout in milliseconds. @return the read timeout in milliseconds */
        public Integer getReadTimeoutMs() { return readTimeoutMs; }
        /** Sets the read timeout in milliseconds. @param readTimeoutMs the read timeout in milliseconds to set */
        public void setReadTimeoutMs(Integer readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
        /** Gets the maximum response size in bytes. @return the maximum response bytes */
        public Integer getMaxResponseBytes() { return maxResponseBytes; }
        /** Sets the maximum response size in bytes. @param maxResponseBytes the maximum response bytes to set */
        public void setMaxResponseBytes(Integer maxResponseBytes) { this.maxResponseBytes = maxResponseBytes; }
    }

    // -------------------------------------------------------------------------
    // Getters / setters for top-level fields
    // -------------------------------------------------------------------------

    /** Gets the job id. @return the id */
    public UUID getId() { return id; }
    /** Sets the job id. @param id the id to set */
    public void setId(UUID id) { this.id = id; }
    /** Gets the job name. @return the name */
    public String getName() { return name; }
    /** Sets the job name. @param name the name to set */
    public void setName(String name) { this.name = name; }
    /** Gets the job description. @return the description */
    public String getDescription() { return description; }
    /** Sets the job description. @param description the description to set */
    public void setDescription(String description) { this.description = description; }
    /** Gets the job type. @return the job type */
    public String getJobType() { return jobType; }
    /** Sets the job type. @param jobType the job type to set */
    public void setJobType(String jobType) { this.jobType = jobType; }
    /** Gets the cron expression. @return the cron expression */
    public String getCronExpression() { return cronExpression; }
    /** Sets the cron expression. @param cronExpression the cron expression to set */
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    /** Gets the time zone. @return the time zone */
    public String getTimeZone() { return timeZone; }
    /** Sets the time zone. @param timeZone the time zone to set */
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    /** Gets the human-readable cron description. @return the human-readable cron description */
    public String getHumanReadableCron() { return humanReadableCron; }
    /** Sets the human-readable cron description. @param humanReadableCron the human-readable cron description to set */
    public void setHumanReadableCron(String humanReadableCron) { this.humanReadableCron = humanReadableCron; }
    /** Gets whether the job is enabled. @return the enabled flag */
    public Boolean getEnabled() { return enabled; }
    /** Sets whether the job is enabled. @param enabled the enabled flag to set */
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    /** Gets whether concurrent runs are allowed. @return the allow-concurrent flag */
    public Boolean getAllowConcurrent() { return allowConcurrent; }
    /** Sets whether concurrent runs are allowed. @param allowConcurrent the allow-concurrent flag to set */
    public void setAllowConcurrent(Boolean allowConcurrent) { this.allowConcurrent = allowConcurrent; }
    /** Gets the current status. @return the status */
    public String getStatus() { return status; }
    /** Sets the current status. @param status the status to set */
    public void setStatus(String status) { this.status = status; }
    /** Gets the last run status. @return the last run status */
    public String getLastRunStatus() { return lastRunStatus; }
    /** Sets the last run status. @param lastRunStatus the last run status to set */
    public void setLastRunStatus(String lastRunStatus) { this.lastRunStatus = lastRunStatus; }
    /** Gets the time of the last run. @return the last run time */
    public ZonedDateTime getLastRunAt() { return lastRunAt; }
    /** Sets the time of the last run. @param lastRunAt the last run time to set */
    public void setLastRunAt(ZonedDateTime lastRunAt) { this.lastRunAt = lastRunAt; }
    /** Gets the time of the next scheduled run. @return the next run time */
    public ZonedDateTime getNextRunAt() { return nextRunAt; }
    /** Sets the time of the next scheduled run. @param nextRunAt the next run time to set */
    public void setNextRunAt(ZonedDateTime nextRunAt) { this.nextRunAt = nextRunAt; }
    /** Gets the number of consecutive failures. @return the consecutive failure count */
    public Integer getConsecutiveFailures() { return consecutiveFailures; }
    /** Sets the number of consecutive failures. @param consecutiveFailures the consecutive failure count to set */
    public void setConsecutiveFailures(Integer consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }
    /** Gets the maximum retry attempts. @return the maximum retry attempts */
    public Integer getMaxRetryAttempts() { return maxRetryAttempts; }
    /** Sets the maximum retry attempts. @param maxRetryAttempts the maximum retry attempts to set */
    public void setMaxRetryAttempts(Integer maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }
    /** Gets the retry delay in seconds. @return the retry delay in seconds */
    public Integer getRetryDelaySeconds() { return retryDelaySeconds; }
    /** Sets the retry delay in seconds. @param retryDelaySeconds the retry delay in seconds to set */
    public void setRetryDelaySeconds(Integer retryDelaySeconds) { this.retryDelaySeconds = retryDelaySeconds; }
    /** Gets the execution timeout in seconds. @return the timeout in seconds */
    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    /** Sets the execution timeout in seconds. @param timeoutSeconds the timeout in seconds to set */
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    /** Gets the maximum captured output size in bytes. @return the maximum output bytes */
    public Integer getMaxOutputBytes() { return maxOutputBytes; }
    /** Sets the maximum captured output size in bytes. @param maxOutputBytes the maximum output bytes to set */
    public void setMaxOutputBytes(Integer maxOutputBytes) { this.maxOutputBytes = maxOutputBytes; }
    /** Gets the job tags. @return the tags */
    public List<String> getTags() { return tags; }
    /** Sets the job tags. @param tags the tags to set */
    public void setTags(List<String> tags) { this.tags = tags; }
    /** Gets the owning tenant id. @return the tenant id */
    public UUID getTenantId() { return tenantId; }
    /** Sets the owning tenant id. @param tenantId the tenant id to set */
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    /** Gets the owning organization id. @return the organization id */
    public UUID getOrganizationId() { return organizationId; }
    /** Sets the owning organization id. @param organizationId the organization id to set */
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    /** Gets the creation date. @return the created date */
    public ZonedDateTime getCreatedDate() { return createdDate; }
    /** Sets the creation date. @param createdDate the created date to set */
    public void setCreatedDate(ZonedDateTime createdDate) { this.createdDate = createdDate; }
    /** Gets the creator. @return the creator */
    public String getCreatedBy() { return createdBy; }
    /** Sets the creator. @param createdBy the creator to set */
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    /** Gets the program configuration. @return the program configuration */
    public ProgramConfigResponse getProgramConfig() { return programConfig; }
    /** Sets the program configuration. @param programConfig the program configuration to set */
    public void setProgramConfig(ProgramConfigResponse programConfig) { this.programConfig = programConfig; }
    /** Gets the SQL configuration. @return the SQL configuration */
    public SqlConfigResponse getSqlConfig() { return sqlConfig; }
    /** Sets the SQL configuration. @param sqlConfig the SQL configuration to set */
    public void setSqlConfig(SqlConfigResponse sqlConfig) { this.sqlConfig = sqlConfig; }
    /** Gets the REST configuration. @return the REST configuration */
    public RestConfigResponse getRestConfig() { return restConfig; }
    /** Sets the REST configuration. @param restConfig the REST configuration to set */
    public void setRestConfig(RestConfigResponse restConfig) { this.restConfig = restConfig; }
    /** Gets the SOAP configuration. @return the SOAP configuration */
    public SoapConfigResponse getSoapConfig() { return soapConfig; }
    /** Sets the SOAP configuration. @param soapConfig the SOAP configuration to set */
    public void setSoapConfig(SoapConfigResponse soapConfig) { this.soapConfig = soapConfig; }
}
