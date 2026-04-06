package org.automatize.status.models;

import jakarta.persistence.*;
import org.automatize.status.models.scheduler.DbType;

import java.time.ZonedDateTime;
import java.util.UUID;

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
public class SchedulerJdbcDatasource {

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
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
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

    @PrePersist
    public void prePersist() {
        ZonedDateTime now = ZonedDateTime.now();
        if (createdDate == null) {
            createdDate = now;
        }
        if (lastModifiedDate == null) {
            lastModifiedDate = now;
        }
        if (createdDateTechnical == null) {
            createdDateTechnical = System.currentTimeMillis();
        }
        if (lastModifiedDateTechnical == null) {
            lastModifiedDateTechnical = System.currentTimeMillis();
        }
    }

    @PreUpdate
    public void preUpdate() {
        lastModifiedDate = ZonedDateTime.now();
        lastModifiedDateTechnical = System.currentTimeMillis();
    }

    public SchedulerJdbcDatasource() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

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

    public DbType getDbType() {
        return dbType;
    }

    public void setDbType(DbType dbType) {
        this.dbType = dbType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getJdbcUrlOverride() {
        return jdbcUrlOverride;
    }

    public void setJdbcUrlOverride(String jdbcUrlOverride) {
        this.jdbcUrlOverride = jdbcUrlOverride;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordEnc() {
        return passwordEnc;
    }

    public void setPasswordEnc(String passwordEnc) {
        this.passwordEnc = passwordEnc;
    }

    public Integer getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(Integer minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public Integer getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(Integer connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public String getExtraProperties() {
        return extraProperties;
    }

    public void setExtraProperties(String extraProperties) {
        this.extraProperties = extraProperties;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Long getCreatedDateTechnical() {
        return createdDateTechnical;
    }

    public void setCreatedDateTechnical(Long createdDateTechnical) {
        this.createdDateTechnical = createdDateTechnical;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Long getLastModifiedDateTechnical() {
        return lastModifiedDateTechnical;
    }

    public void setLastModifiedDateTechnical(Long lastModifiedDateTechnical) {
        this.lastModifiedDateTechnical = lastModifiedDateTechnical;
    }
}
