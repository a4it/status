package org.automatize.status.api.response;

import org.automatize.status.models.*;
import org.automatize.status.models.scheduler.JobType;
import org.automatize.status.services.scheduler.CronValidationService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response class for a scheduler job.  Sensitive credential fields are masked
 * (replaced with bullet characters) and encrypted storage fields are never exposed.
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
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

        if (job.getJobType() == JobType.PROGRAM && job.getProgramConfig() != null) {
            r.programConfig = mapProgramConfig(job.getProgramConfig());
        }
        if (job.getJobType() == JobType.SQL && job.getSqlConfig() != null) {
            r.sqlConfig = mapSqlConfig(job.getSqlConfig());
        }
        if (job.getJobType() == JobType.REST && job.getRestConfig() != null) {
            r.restConfig = mapRestConfig(job.getRestConfig());
        }
        if (job.getJobType() == JobType.SOAP && job.getSoapConfig() != null) {
            r.soapConfig = mapSoapConfig(job.getSoapConfig());
        }

        return r;
    }

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

    public static class ProgramConfigResponse {
        private UUID id;
        private String command;
        private List<String> arguments;
        private String workingDirectory;
        private Map<String, String> environmentVars;
        private Boolean shellWrap;
        private String shellPath;
        private String runAsUser;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public List<String> getArguments() { return arguments; }
        public void setArguments(List<String> arguments) { this.arguments = arguments; }
        public String getWorkingDirectory() { return workingDirectory; }
        public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }
        public Map<String, String> getEnvironmentVars() { return environmentVars; }
        public void setEnvironmentVars(Map<String, String> environmentVars) { this.environmentVars = environmentVars; }
        public Boolean getShellWrap() { return shellWrap; }
        public void setShellWrap(Boolean shellWrap) { this.shellWrap = shellWrap; }
        public String getShellPath() { return shellPath; }
        public void setShellPath(String shellPath) { this.shellPath = shellPath; }
        public String getRunAsUser() { return runAsUser; }
        public void setRunAsUser(String runAsUser) { this.runAsUser = runAsUser; }
    }

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

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public UUID getDatasourceId() { return datasourceId; }
        public void setDatasourceId(UUID datasourceId) { this.datasourceId = datasourceId; }
        public String getInlineDbType() { return inlineDbType; }
        public void setInlineDbType(String inlineDbType) { this.inlineDbType = inlineDbType; }
        public String getInlineJdbcUrl() { return inlineJdbcUrl; }
        public void setInlineJdbcUrl(String inlineJdbcUrl) { this.inlineJdbcUrl = inlineJdbcUrl; }
        public String getInlineUsername() { return inlineUsername; }
        public void setInlineUsername(String inlineUsername) { this.inlineUsername = inlineUsername; }
        public String getSqlStatement() { return sqlStatement; }
        public void setSqlStatement(String sqlStatement) { this.sqlStatement = sqlStatement; }
        public String getSqlType() { return sqlType; }
        public void setSqlType(String sqlType) { this.sqlType = sqlType; }
        public Boolean getCaptureResultSet() { return captureResultSet; }
        public void setCaptureResultSet(Boolean captureResultSet) { this.captureResultSet = captureResultSet; }
        public Integer getMaxResultRows() { return maxResultRows; }
        public void setMaxResultRows(Integer maxResultRows) { this.maxResultRows = maxResultRows; }
        public Integer getQueryTimeoutSeconds() { return queryTimeoutSeconds; }
        public void setQueryTimeoutSeconds(Integer queryTimeoutSeconds) { this.queryTimeoutSeconds = queryTimeoutSeconds; }
    }

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

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getHttpMethod() { return httpMethod; }
        public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getRequestBody() { return requestBody; }
        public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        public Map<String, String> getQueryParams() { return queryParams; }
        public void setQueryParams(Map<String, String> queryParams) { this.queryParams = queryParams; }
        public String getAuthType() { return authType; }
        public void setAuthType(String authType) { this.authType = authType; }
        public String getAuthUsername() { return authUsername; }
        public void setAuthUsername(String authUsername) { this.authUsername = authUsername; }
        public String getAuthPassword() { return authPassword; }
        public void setAuthPassword(String authPassword) { this.authPassword = authPassword; }
        public String getAuthToken() { return authToken; }
        public void setAuthToken(String authToken) { this.authToken = authToken; }
        public String getAuthApiKeyName() { return authApiKeyName; }
        public void setAuthApiKeyName(String authApiKeyName) { this.authApiKeyName = authApiKeyName; }
        public String getAuthApiKeyValue() { return authApiKeyValue; }
        public void setAuthApiKeyValue(String authApiKeyValue) { this.authApiKeyValue = authApiKeyValue; }
        public String getAuthApiKeyLocation() { return authApiKeyLocation; }
        public void setAuthApiKeyLocation(String authApiKeyLocation) { this.authApiKeyLocation = authApiKeyLocation; }
        public String getAuthOauth2TokenUrl() { return authOauth2TokenUrl; }
        public void setAuthOauth2TokenUrl(String authOauth2TokenUrl) { this.authOauth2TokenUrl = authOauth2TokenUrl; }
        public String getAuthOauth2ClientId() { return authOauth2ClientId; }
        public void setAuthOauth2ClientId(String authOauth2ClientId) { this.authOauth2ClientId = authOauth2ClientId; }
        public String getAuthOauth2ClientSecret() { return authOauth2ClientSecret; }
        public void setAuthOauth2ClientSecret(String authOauth2ClientSecret) { this.authOauth2ClientSecret = authOauth2ClientSecret; }
        public String getAuthOauth2Scope() { return authOauth2Scope; }
        public void setAuthOauth2Scope(String authOauth2Scope) { this.authOauth2Scope = authOauth2Scope; }
        public Boolean getSslVerify() { return sslVerify; }
        public void setSslVerify(Boolean sslVerify) { this.sslVerify = sslVerify; }
        public Integer getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(Integer connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public Integer getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(Integer readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
        public Boolean getFollowRedirects() { return followRedirects; }
        public void setFollowRedirects(Boolean followRedirects) { this.followRedirects = followRedirects; }
        public Integer getMaxResponseBytes() { return maxResponseBytes; }
        public void setMaxResponseBytes(Integer maxResponseBytes) { this.maxResponseBytes = maxResponseBytes; }
        public Integer getAssertStatusCode() { return assertStatusCode; }
        public void setAssertStatusCode(Integer assertStatusCode) { this.assertStatusCode = assertStatusCode; }
        public String getAssertBodyContains() { return assertBodyContains; }
        public void setAssertBodyContains(String assertBodyContains) { this.assertBodyContains = assertBodyContains; }
        public String getAssertJsonPath() { return assertJsonPath; }
        public void setAssertJsonPath(String assertJsonPath) { this.assertJsonPath = assertJsonPath; }
        public String getAssertJsonValue() { return assertJsonValue; }
        public void setAssertJsonValue(String assertJsonValue) { this.assertJsonValue = assertJsonValue; }
    }

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

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getWsdlUrl() { return wsdlUrl; }
        public void setWsdlUrl(String wsdlUrl) { this.wsdlUrl = wsdlUrl; }
        public String getEndpointUrl() { return endpointUrl; }
        public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public String getPortName() { return portName; }
        public void setPortName(String portName) { this.portName = portName; }
        public String getOperationName() { return operationName; }
        public void setOperationName(String operationName) { this.operationName = operationName; }
        public String getSoapAction() { return soapAction; }
        public void setSoapAction(String soapAction) { this.soapAction = soapAction; }
        public String getSoapVersion() { return soapVersion; }
        public void setSoapVersion(String soapVersion) { this.soapVersion = soapVersion; }
        public String getSoapEnvelope() { return soapEnvelope; }
        public void setSoapEnvelope(String soapEnvelope) { this.soapEnvelope = soapEnvelope; }
        public Map<String, String> getExtraHeaders() { return extraHeaders; }
        public void setExtraHeaders(Map<String, String> extraHeaders) { this.extraHeaders = extraHeaders; }
        public String getAuthType() { return authType; }
        public void setAuthType(String authType) { this.authType = authType; }
        public String getAuthUsername() { return authUsername; }
        public void setAuthUsername(String authUsername) { this.authUsername = authUsername; }
        public String getAuthPassword() { return authPassword; }
        public void setAuthPassword(String authPassword) { this.authPassword = authPassword; }
        public String getAuthToken() { return authToken; }
        public void setAuthToken(String authToken) { this.authToken = authToken; }
        public Boolean getSslVerify() { return sslVerify; }
        public void setSslVerify(Boolean sslVerify) { this.sslVerify = sslVerify; }
        public Integer getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(Integer connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public Integer getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(Integer readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
        public Integer getMaxResponseBytes() { return maxResponseBytes; }
        public void setMaxResponseBytes(Integer maxResponseBytes) { this.maxResponseBytes = maxResponseBytes; }
    }

    // -------------------------------------------------------------------------
    // Getters / setters for top-level fields
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    public String getHumanReadableCron() { return humanReadableCron; }
    public void setHumanReadableCron(String humanReadableCron) { this.humanReadableCron = humanReadableCron; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getAllowConcurrent() { return allowConcurrent; }
    public void setAllowConcurrent(Boolean allowConcurrent) { this.allowConcurrent = allowConcurrent; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLastRunStatus() { return lastRunStatus; }
    public void setLastRunStatus(String lastRunStatus) { this.lastRunStatus = lastRunStatus; }
    public ZonedDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(ZonedDateTime lastRunAt) { this.lastRunAt = lastRunAt; }
    public ZonedDateTime getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(ZonedDateTime nextRunAt) { this.nextRunAt = nextRunAt; }
    public Integer getConsecutiveFailures() { return consecutiveFailures; }
    public void setConsecutiveFailures(Integer consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }
    public Integer getMaxRetryAttempts() { return maxRetryAttempts; }
    public void setMaxRetryAttempts(Integer maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }
    public Integer getRetryDelaySeconds() { return retryDelaySeconds; }
    public void setRetryDelaySeconds(Integer retryDelaySeconds) { this.retryDelaySeconds = retryDelaySeconds; }
    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public Integer getMaxOutputBytes() { return maxOutputBytes; }
    public void setMaxOutputBytes(Integer maxOutputBytes) { this.maxOutputBytes = maxOutputBytes; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public ZonedDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(ZonedDateTime createdDate) { this.createdDate = createdDate; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public ProgramConfigResponse getProgramConfig() { return programConfig; }
    public void setProgramConfig(ProgramConfigResponse programConfig) { this.programConfig = programConfig; }
    public SqlConfigResponse getSqlConfig() { return sqlConfig; }
    public void setSqlConfig(SqlConfigResponse sqlConfig) { this.sqlConfig = sqlConfig; }
    public RestConfigResponse getRestConfig() { return restConfig; }
    public void setRestConfig(RestConfigResponse restConfig) { this.restConfig = restConfig; }
    public SoapConfigResponse getSoapConfig() { return soapConfig; }
    public void setSoapConfig(SoapConfigResponse soapConfig) { this.soapConfig = soapConfig; }
}
