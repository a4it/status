package org.automatize.status.models;

import jakarta.persistence.*;
import org.automatize.status.models.scheduler.AuthType;
import org.automatize.status.models.scheduler.JsonMapConverter;
import org.automatize.status.models.scheduler.SoapVersion;

import java.util.Map;
import java.util.UUID;

/**
 * Configuration entity for scheduler jobs of type {@code SOAP}.
 *
 * <p>Stores WSDL/endpoint details, the SOAP envelope to send, optional
 * authentication credentials, and SSL settings required to invoke a
 * SOAP web-service operation.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Entity
@Table(name = "scheduler_soap_configs")
public class SchedulerSoapConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private SchedulerJob job;

    @Column(name = "wsdl_url", length = 4096)
    private String wsdlUrl;

    @Column(name = "endpoint_url", length = 4096)
    private String endpointUrl;

    @Column(name = "service_name", length = 512)
    private String serviceName;

    @Column(name = "port_name", length = 512)
    private String portName;

    @Column(name = "operation_name", length = 512)
    private String operationName;

    @Column(name = "soap_action", length = 1024)
    private String soapAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "soap_version", nullable = false, length = 10)
    private SoapVersion soapVersion = SoapVersion.V1_1;

    @Column(name = "soap_envelope", columnDefinition = "TEXT")
    private String soapEnvelope;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "extra_headers", columnDefinition = "TEXT")
    private Map<String, String> extraHeaders;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 50)
    private AuthType authType = AuthType.NONE;

    @Column(name = "auth_username", length = 255)
    private String authUsername;

    @Column(name = "auth_password_enc", length = 2048)
    private String authPasswordEnc;

    @Column(name = "auth_token_enc", length = 2048)
    private String authTokenEnc;

    @Column(name = "ssl_verify", nullable = false)
    private Boolean sslVerify = true;

    @Column(name = "connect_timeout_ms", nullable = false)
    private Integer connectTimeoutMs = 5000;

    @Column(name = "read_timeout_ms", nullable = false)
    private Integer readTimeoutMs = 60000;

    @Column(name = "max_response_bytes", nullable = false)
    private Integer maxResponseBytes = 524288;

    /**
     * Default constructor required by JPA.
     */
    public SchedulerSoapConfig() {
    }

    /**
     * Gets the unique identifier of this SOAP configuration.
     *
     * @return the UUID of the configuration
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of this SOAP configuration.
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
     * Gets the WSDL document URL for the SOAP service.
     *
     * @return the WSDL URL
     */
    public String getWsdlUrl() {
        return wsdlUrl;
    }

    /**
     * Sets the WSDL document URL for the SOAP service.
     *
     * @param wsdlUrl the WSDL URL to set
     */
    public void setWsdlUrl(String wsdlUrl) {
        this.wsdlUrl = wsdlUrl;
    }

    /**
     * Gets the SOAP service endpoint URL.
     *
     * @return the endpoint URL
     */
    public String getEndpointUrl() {
        return endpointUrl;
    }

    /**
     * Sets the SOAP service endpoint URL.
     *
     * @param endpointUrl the endpoint URL to set
     */
    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    /**
     * Gets the SOAP service name.
     *
     * @return the service name
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Sets the SOAP service name.
     *
     * @param serviceName the service name to set
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Gets the SOAP port name.
     *
     * @return the port name
     */
    public String getPortName() {
        return portName;
    }

    /**
     * Sets the SOAP port name.
     *
     * @param portName the port name to set
     */
    public void setPortName(String portName) {
        this.portName = portName;
    }

    /**
     * Gets the SOAP operation name to invoke.
     *
     * @return the operation name
     */
    public String getOperationName() {
        return operationName;
    }

    /**
     * Sets the SOAP operation name to invoke.
     *
     * @param operationName the operation name to set
     */
    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    /**
     * Gets the SOAPAction header value.
     *
     * @return the SOAP action
     */
    public String getSoapAction() {
        return soapAction;
    }

    /**
     * Sets the SOAPAction header value.
     *
     * @param soapAction the SOAP action to set
     */
    public void setSoapAction(String soapAction) {
        this.soapAction = soapAction;
    }

    /**
     * Gets the SOAP protocol version.
     *
     * @return the SOAP version
     */
    public SoapVersion getSoapVersion() {
        return soapVersion;
    }

    /**
     * Sets the SOAP protocol version.
     *
     * @param soapVersion the SOAP version to set
     */
    public void setSoapVersion(SoapVersion soapVersion) {
        this.soapVersion = soapVersion;
    }

    /**
     * Gets the SOAP envelope payload to send.
     *
     * @return the SOAP envelope
     */
    public String getSoapEnvelope() {
        return soapEnvelope;
    }

    /**
     * Sets the SOAP envelope payload to send.
     *
     * @param soapEnvelope the SOAP envelope to set
     */
    public void setSoapEnvelope(String soapEnvelope) {
        this.soapEnvelope = soapEnvelope;
    }

    /**
     * Gets the additional HTTP headers sent with the SOAP request.
     *
     * @return a map of extra header names to values
     */
    public Map<String, String> getExtraHeaders() {
        return extraHeaders;
    }

    /**
     * Sets the additional HTTP headers sent with the SOAP request.
     *
     * @param extraHeaders a map of extra header names to values to set
     */
    public void setExtraHeaders(Map<String, String> extraHeaders) {
        this.extraHeaders = extraHeaders;
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
}
