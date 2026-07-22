package org.automatize.status.models;

import jakarta.persistence.*;
import org.automatize.status.models.scheduler.DbType;

import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing a reusable JDBC datasource configuration for scheduler SQL jobs.
 *
 * <p>Datasources are tenant-scoped and optionally scoped to an organisation.
 * Sensitive credentials (passwords) are stored in encrypted form via the
 * {@code password_enc} column and must be decrypted by the service layer
 * before use.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Entity
@Table(name = "scheduler_jdbc_datasources")
@EntityListeners(AuditTimestampListener.class)
public class SchedulerJdbcDatasource implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "name", nullable = false, length = 255)
    @Getter
    @Setter
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    @Getter
    @Setter
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "db_type", nullable = false, length = 50)
    private DbType dbType;

    @Column(name = "host", length = 1024)
    private String host;

    @Column(name = "port")
    private Integer port;

    @Column(name = "database_name", length = 255)
    private String databaseName;

    @Column(name = "schema_name", length = 255)
    private String schemaName;

    @Column(name = "jdbc_url_override", length = 2048)
    private String jdbcUrlOverride;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "password_enc", length = 2048)
    private String passwordEnc;

    @Column(name = "min_pool_size", nullable = false)
    private Integer minPoolSize = 1;

    @Column(name = "max_pool_size", nullable = false)
    private Integer maxPoolSize = 5;

    @Column(name = "connection_timeout_ms", nullable = false)
    private Integer connectionTimeoutMs = 5000;

    @Column(name = "extra_properties", columnDefinition = "TEXT")
    private String extraProperties;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    // Audit fields
    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_date")
    private ZonedDateTime createdDate;

    @Column(name = "created_date_technical")
    private Long createdDateTechnical;

    @Column(name = "last_modified_by", length = 255)
    private String lastModifiedBy;

    @Column(name = "last_modified_date")
    private ZonedDateTime lastModifiedDate;

    @Column(name = "last_modified_date_technical")
    private Long lastModifiedDateTechnical;

    /**
     * Default constructor required by JPA.
     */
    public SchedulerJdbcDatasource() {
    }

    /**
     * Gets the unique identifier of the datasource.
     *
     * @return the UUID of the datasource
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the datasource.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the tenant that owns this datasource.
     *
     * @return the associated {@link Tenant}
     */
    public Tenant getTenant() {
        return tenant;
    }

    /**
     * Sets the tenant that owns this datasource.
     *
     * @param tenant the {@link Tenant} to associate
     */
    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    /**
     * Gets the organisation this datasource is scoped to, if any.
     *
     * @return the associated {@link Organization}, or {@code null} if tenant-wide
     */
    public Organization getOrganization() {
        return organization;
    }

    /**
     * Sets the organisation this datasource is scoped to.
     *
     * @param organization the {@link Organization} to associate, or {@code null} for tenant-wide
     */
    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    /**
     * Gets the database type of the datasource.
     *
     * @return the {@link DbType}
     */
    public DbType getDbType() {
        return dbType;
    }

    /**
     * Sets the database type of the datasource.
     *
     * @param dbType the {@link DbType} to set
     */
    public void setDbType(DbType dbType) {
        this.dbType = dbType;
    }

    /**
     * Gets the database host.
     *
     * @return the host name or address
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the database host.
     *
     * @param host the host name or address to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Gets the database port.
     *
     * @return the port number
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Sets the database port.
     *
     * @param port the port number to set
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * Gets the database (catalog) name.
     *
     * @return the database name
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Sets the database (catalog) name.
     *
     * @param databaseName the database name to set
     */
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * Gets the schema name.
     *
     * @return the schema name
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Sets the schema name.
     *
     * @param schemaName the schema name to set
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * Gets the explicit JDBC URL that overrides the host/port/database values when set.
     *
     * @return the JDBC URL override, or {@code null} if not used
     */
    public String getJdbcUrlOverride() {
        return jdbcUrlOverride;
    }

    /**
     * Sets the explicit JDBC URL that overrides the host/port/database values.
     *
     * @param jdbcUrlOverride the JDBC URL override to set
     */
    public void setJdbcUrlOverride(String jdbcUrlOverride) {
        this.jdbcUrlOverride = jdbcUrlOverride;
    }

    /**
     * Gets the database username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the database username.
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the encrypted database password.
     *
     * @return the encrypted password value
     */
    public String getPasswordEnc() {
        return passwordEnc;
    }

    /**
     * Sets the encrypted database password.
     *
     * @param passwordEnc the encrypted password value to set
     */
    public void setPasswordEnc(String passwordEnc) {
        this.passwordEnc = passwordEnc;
    }

    /**
     * Gets the minimum connection pool size.
     *
     * @return the minimum pool size
     */
    public Integer getMinPoolSize() {
        return minPoolSize;
    }

    /**
     * Sets the minimum connection pool size.
     *
     * @param minPoolSize the minimum pool size to set
     */
    public void setMinPoolSize(Integer minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    /**
     * Gets the maximum connection pool size.
     *
     * @return the maximum pool size
     */
    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * Sets the maximum connection pool size.
     *
     * @param maxPoolSize the maximum pool size to set
     */
    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    /**
     * Gets the connection timeout in milliseconds.
     *
     * @return the connection timeout in milliseconds
     */
    public Integer getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    /**
     * Sets the connection timeout in milliseconds.
     *
     * @param connectionTimeoutMs the connection timeout in milliseconds to set
     */
    public void setConnectionTimeoutMs(Integer connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    /**
     * Gets the extra JDBC connection properties.
     *
     * @return the extra properties
     */
    public String getExtraProperties() {
        return extraProperties;
    }

    /**
     * Sets the extra JDBC connection properties.
     *
     * @param extraProperties the extra properties to set
     */
    public void setExtraProperties(String extraProperties) {
        this.extraProperties = extraProperties;
    }

    /**
     * Indicates whether this datasource is enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Sets whether this datasource is enabled.
     *
     * @param enabled the enabled flag to set
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the username of the user who created this datasource.
     *
     * @return the creator's username
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the username of the user who created this datasource.
     *
     * @param createdBy the creator's username to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the creation timestamp of the datasource.
     *
     * @return the creation date and time
     */
    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the creation timestamp of the datasource.
     *
     * @param createdDate the creation date and time to set
     */
    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Gets the technical creation timestamp in epoch milliseconds.
     *
     * @return the creation timestamp in milliseconds since epoch
     */
    public Long getCreatedDateTechnical() {
        return createdDateTechnical;
    }

    /**
     * Sets the technical creation timestamp in epoch milliseconds.
     *
     * @param createdDateTechnical the creation timestamp in milliseconds to set
     */
    public void setCreatedDateTechnical(Long createdDateTechnical) {
        this.createdDateTechnical = createdDateTechnical;
    }

    /**
     * Gets the username of the user who last modified this datasource.
     *
     * @return the last modifier's username
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Sets the username of the user who last modified this datasource.
     *
     * @param lastModifiedBy the last modifier's username to set
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * Gets the last modification timestamp of the datasource.
     *
     * @return the last modification date and time
     */
    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Sets the last modification timestamp of the datasource.
     *
     * @param lastModifiedDate the last modification date and time to set
     */
    public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * Gets the technical last modification timestamp in epoch milliseconds.
     *
     * @return the last modification timestamp in milliseconds since epoch
     */
    public Long getLastModifiedDateTechnical() {
        return lastModifiedDateTechnical;
    }

    /**
     * Sets the technical last modification timestamp in epoch milliseconds.
     *
     * @param lastModifiedDateTechnical the last modification timestamp in milliseconds to set
     */
    public void setLastModifiedDateTechnical(Long lastModifiedDateTechnical) {
        this.lastModifiedDateTechnical = lastModifiedDateTechnical;
    }
}
