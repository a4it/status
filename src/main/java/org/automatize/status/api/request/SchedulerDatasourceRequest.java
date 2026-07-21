package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * <p>
 * Request object for creating or updating a scheduler JDBC datasource in the
 * status monitoring system.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Encapsulate the connection details of a reusable JDBC datasource that
 *       scheduler jobs can reference to run SQL checks against monitored databases</li>
 *   <li>Carry connection pool sizing and timeout tuning parameters</li>
 *   <li>Associate the datasource with an owning organization in the multi-tenant hierarchy</li>
 * </ul>
 * </p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
public class SchedulerDatasourceRequest {

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String dbType;

    private String host;

    private Integer port;

    private String databaseName;

    private String schemaName;

    private String jdbcUrlOverride;

    private String username;

    /** Plaintext password; will be encrypted by the service layer. */
    private String password;

    private Integer minPoolSize = 1;

    private Integer maxPoolSize = 5;

    private Integer connectionTimeoutMs = 5000;

    private Boolean enabled = true;

    private UUID organizationId;

    /** @return the datasource display name */
    public String getName() {
        return name;
    }

    /** @param name the datasource display name to set */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the human-readable description */
    public String getDescription() {
        return description;
    }

    /** @param description the human-readable description to set */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return the database type (e.g. POSTGRES, MYSQL, ORACLE) */
    public String getDbType() {
        return dbType;
    }

    /** @param dbType the database type to set */
    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    /** @return the database host name or IP address */
    public String getHost() {
        return host;
    }

    /** @param host the database host name or IP address to set */
    public void setHost(String host) {
        this.host = host;
    }

    /** @return the database port */
    public Integer getPort() {
        return port;
    }

    /** @param port the database port to set */
    public void setPort(Integer port) {
        this.port = port;
    }

    /** @return the database (schema catalog) name */
    public String getDatabaseName() {
        return databaseName;
    }

    /** @param databaseName the database name to set */
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    /** @return the default schema name */
    public String getSchemaName() {
        return schemaName;
    }

    /** @param schemaName the default schema name to set */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /** @return an explicit JDBC URL that overrides the host/port/database composition */
    public String getJdbcUrlOverride() {
        return jdbcUrlOverride;
    }

    /** @param jdbcUrlOverride the explicit JDBC URL override to set */
    public void setJdbcUrlOverride(String jdbcUrlOverride) {
        this.jdbcUrlOverride = jdbcUrlOverride;
    }

    /** @return the connection username */
    public String getUsername() {
        return username;
    }

    /** @param username the connection username to set */
    public void setUsername(String username) {
        this.username = username;
    }

    /** @return the plaintext connection password (encrypted by the service layer) */
    public String getPassword() {
        return password;
    }

    /** @param password the plaintext connection password to set */
    public void setPassword(String password) {
        this.password = password;
    }

    /** @return the minimum connection pool size */
    public Integer getMinPoolSize() {
        return minPoolSize;
    }

    /** @param minPoolSize the minimum connection pool size to set */
    public void setMinPoolSize(Integer minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    /** @return the maximum connection pool size */
    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    /** @param maxPoolSize the maximum connection pool size to set */
    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    /** @return the connection acquisition timeout in milliseconds */
    public Integer getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    /** @param connectionTimeoutMs the connection acquisition timeout in milliseconds to set */
    public void setConnectionTimeoutMs(Integer connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    /** @return whether the datasource is enabled */
    public Boolean getEnabled() {
        return enabled;
    }

    /** @param enabled whether the datasource is enabled */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /** @return the owning organization identifier */
    public UUID getOrganizationId() {
        return organizationId;
    }

    /** @param organizationId the owning organization identifier to set */
    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }
}
