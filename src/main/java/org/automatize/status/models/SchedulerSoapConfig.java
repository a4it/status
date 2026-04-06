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

    public SchedulerSoapConfig() {
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

    public SoapVersion getSoapVersion() {
        return soapVersion;
    }

    public void setSoapVersion(SoapVersion soapVersion) {
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
