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
    @Getter
    @Setter
    private String jobType;

    /** The cron expression defining the execution schedule. */
    @NotBlank
    @Getter
    @Setter
    private String cronExpression;

    /** The time zone in which the cron expression is evaluated. */
    @Getter
    @Setter
    private String timeZone = "UTC";

    /** Whether the job is enabled for scheduling. */
    @Getter
    @Setter
    private Boolean enabled = true;

    /** Whether concurrent executions of this job are permitted. */
    @Getter
    @Setter
    private Boolean allowConcurrent = false;

    /** The maximum number of retry attempts on failure. */
    @Getter
    @Setter
    private Integer maxRetryAttempts = 0;

    /** The delay in seconds between retry attempts. */
    @Getter
    @Setter
    private Integer retryDelaySeconds = 60;

    /** The per-execution timeout in seconds. */
    @Getter
    @Setter
    private Integer timeoutSeconds = 300;

    /** The maximum number of captured output bytes to retain. */
    @Getter
    @Setter
    private Integer maxOutputBytes = 102400;

    /** The free-form tags used to categorize the job. */
    @Getter
    @Setter
    private List<String> tags;

    /** The owning organization identifier. */
    @Getter
    @Setter
    private UUID organizationId;

    /** The program-execution configuration (used when jobType is PROGRAM). */
    @Getter
    @Setter
    private ProgramConfigRequest programConfig;

    /** The SQL-execution configuration (used when jobType is SQL). */
    @Getter
    @Setter
    private SqlConfigRequest sqlConfig;

    /** The REST-call configuration (used when jobType is REST). */
    @Getter
    @Setter
    private RestConfigRequest restConfig;

    /** The SOAP-call configuration (used when jobType is SOAP). */
    @Getter
    @Setter
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
        @Getter
        @Setter
        private String command;
        /** The ordered command-line arguments. */
        @Getter
        @Setter
        private List<String> arguments;
        /** The working directory for the process. */
        @Getter
        @Setter
        private String workingDirectory;
        /** The environment variables to inject into the process. */
        @Getter
        @Setter
        private Map<String, String> environmentVars;
        /** Whether to wrap the command in a shell invocation. */
        @Getter
        @Setter
        private Boolean shellWrap = false;
        /** The shell executable path used when shell-wrapping. */
        @Getter
        @Setter
        private String shellPath = "/bin/bash";
        /** The OS user to run the process as. */
        @Getter
        @Setter
        private String runAsUser;
    }

    /**
     * Configuration payload for a SQL-type scheduler job that runs a SQL statement
     * against either a stored datasource or an inline connection as a monitoring check.
     */
    public static class SqlConfigRequest {

        /** The identifier of a stored datasource to run against. */
        @Getter
        @Setter
        private UUID datasourceId;
        /** The database type when supplying an inline connection. */
        @Getter
        @Setter
        private String inlineDbType;
        /** The JDBC URL when supplying an inline connection. */
        @Getter
        @Setter
        private String inlineJdbcUrl;
        /** The username when supplying an inline connection. */
        @Getter
        @Setter
        private String inlineUsername;
        /** The password when supplying an inline connection. */
        @Getter
        @Setter
        private String inlinePassword;
        /** The SQL statement to execute. */
        @Getter
        @Setter
        private String sqlStatement;
        /** The SQL statement category (e.g. DML, DDL, QUERY). */
        @Getter
        @Setter
        private String sqlType = "DML";
        /** Whether to capture the result set of the statement. */
        @Getter
        @Setter
        private Boolean captureResultSet = false;
        /** The maximum number of result rows to capture. */
        @Getter
        @Setter
        private Integer maxResultRows = 100;
        /** The query timeout in seconds. */
        @Getter
        @Setter
        private Integer queryTimeoutSeconds = 60;
    }

    /**
     * Configuration payload for a REST-type scheduler job that issues an HTTP request
     * to a monitored endpoint, supporting multiple authentication schemes and
     * response assertions used to determine check success or failure.
     */
    public static class RestConfigRequest {

        /** The HTTP method to use. */
        @Getter
        @Setter
        private String httpMethod = "GET";
        /** The target request URL. */
        @Getter
        @Setter
        private String url;
        /** The request body payload. */
        @Getter
        @Setter
        private String requestBody;
        /** The request content type. */
        @Getter
        @Setter
        private String contentType = "application/json";
        /** The additional request headers. */
        @Getter
        @Setter
        private Map<String, String> headers;
        /** The additional query string parameters. */
        @Getter
        @Setter
        private Map<String, String> queryParams;
        /** The authentication scheme (e.g. NONE, BASIC, BEARER, API_KEY, OAUTH2). */
        @Getter
        @Setter
        private String authType = "NONE";
        /** The username for BASIC authentication. */
        @Getter
        @Setter
        private String authUsername;
        /** The password for BASIC authentication. */
        @Getter
        @Setter
        private String authPassword;
        /** The bearer token for token authentication. */
        @Getter
        @Setter
        private String authToken;
        /** The API key header/parameter name. */
        @Getter
        @Setter
        private String authApiKeyName;
        /** The API key value. */
        @Getter
        @Setter
        private String authApiKeyValue;
        /** Where to place the API key (e.g. HEADER or QUERY). */
        @Getter
        @Setter
        private String authApiKeyLocation;
        /** The OAuth2 token endpoint URL. */
        @Getter
        @Setter
        private String authOauth2TokenUrl;
        /** The OAuth2 client identifier. */
        @Getter
        @Setter
        private String authOauth2ClientId;
        /** The OAuth2 client secret. */
        @Getter
        @Setter
        private String authOauth2ClientSecret;
        /** The OAuth2 scope. */
        @Getter
        @Setter
        private String authOauth2Scope;
        /** Whether to verify the server's SSL certificate. */
        @Getter
        @Setter
        private Boolean sslVerify = true;
        /** The connection timeout in milliseconds. */
        @Getter
        @Setter
        private Integer connectTimeoutMs = 5000;
        /** The read timeout in milliseconds. */
        @Getter
        @Setter
        private Integer readTimeoutMs = 30000;
        /** Whether to follow HTTP redirects. */
        @Getter
        @Setter
        private Boolean followRedirects = true;
        /** The maximum number of response bytes to read. */
        @Getter
        @Setter
        private Integer maxResponseBytes = 102400;
        /** The expected HTTP status code asserted for success. */
        @Getter
        @Setter
        private Integer assertStatusCode;
        /** A substring asserted to be present in the response body. */
        @Getter
        @Setter
        private String assertBodyContains;
        /** A JSON path expression evaluated against the response body. */
        @Getter
        @Setter
        private String assertJsonPath;
        /** The expected value at the asserted JSON path. */
        @Getter
        @Setter
        private String assertJsonValue;
    }

    /**
     * Configuration payload for a SOAP-type scheduler job that invokes a SOAP web
     * service operation on a monitored endpoint as a monitoring check.
     */
    public static class SoapConfigRequest {

        /** The WSDL document URL describing the service. */
        @Getter
        @Setter
        private String wsdlUrl;
        /** The SOAP service endpoint URL. */
        @Getter
        @Setter
        private String endpointUrl;
        /** The WSDL service name. */
        @Getter
        @Setter
        private String serviceName;
        /** The WSDL port name. */
        @Getter
        @Setter
        private String portName;
        /** The operation name to invoke. */
        @Getter
        @Setter
        private String operationName;
        /** The SOAPAction HTTP header value. */
        @Getter
        @Setter
        private String soapAction;
        /** The SOAP protocol version (e.g. V1_1, V1_2). */
        @Getter
        @Setter
        private String soapVersion = "V1_1";
        /** The raw SOAP envelope to send. */
        @Getter
        @Setter
        private String soapEnvelope;
        /** The additional HTTP headers to send. */
        @Getter
        @Setter
        private Map<String, String> extraHeaders;
        /** The authentication scheme (e.g. NONE, BASIC, BEARER). */
        @Getter
        @Setter
        private String authType = "NONE";
        /** The username for BASIC authentication. */
        @Getter
        @Setter
        private String authUsername;
        /** The password for BASIC authentication. */
        @Getter
        @Setter
        private String authPassword;
        /** The bearer token for token authentication. */
        @Getter
        @Setter
        private String authToken;
        /** Whether to verify the server's SSL certificate. */
        @Getter
        @Setter
        private Boolean sslVerify = true;
        /** The connection timeout in milliseconds. */
        @Getter
        @Setter
        private Integer connectTimeoutMs = 5000;
        /** The read timeout in milliseconds. */
        @Getter
        @Setter
        private Integer readTimeoutMs = 60000;
        /** The maximum number of response bytes to read. */
        @Getter
        @Setter
        private Integer maxResponseBytes = 524288;
    }
}
