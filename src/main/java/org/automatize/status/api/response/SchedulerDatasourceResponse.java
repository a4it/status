package org.automatize.status.api.response;

import org.automatize.status.models.SchedulerJdbcDatasource;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Response class for a scheduler JDBC datasource.
 * The {@code passwordEnc} field is never returned.
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
public class SchedulerDatasourceResponse {

    private UUID id;
    private String name;
    private String description;
    private String dbType;
    private String host;
    private Integer port;
    private String databaseName;
    private String schemaName;
    private String jdbcUrlOverride;
    private String username;
    private Boolean enabled;
    private ZonedDateTime createdDate;
    private UUID tenantId;
    private UUID organizationId;

    // -------------------------------------------------------------------------
    // Factory method
    // -------------------------------------------------------------------------

    public static SchedulerDatasourceResponse fromEntity(SchedulerJdbcDatasource ds) {
        SchedulerDatasourceResponse r = new SchedulerDatasourceResponse();
        r.id = ds.getId();
        r.name = ds.getName();
        r.description = ds.getDescription();
        r.dbType = ds.getDbType() != null ? ds.getDbType().name() : null;
        r.host = ds.getHost();
        r.port = ds.getPort();
        r.databaseName = ds.getDatabaseName();
        r.schemaName = ds.getSchemaName();
        r.jdbcUrlOverride = ds.getJdbcUrlOverride();
        r.username = ds.getUsername();
        // passwordEnc is intentionally excluded
        r.enabled = ds.getEnabled();
        r.createdDate = ds.getCreatedDate();
        r.tenantId = ds.getTenant() != null ? ds.getTenant().getId() : null;
        r.organizationId = ds.getOrganization() != null ? ds.getOrganization().getId() : null;
        return r;
    }

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDbType() { return dbType; }
    public void setDbType(String dbType) { this.dbType = dbType; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }

    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    public String getJdbcUrlOverride() { return jdbcUrlOverride; }
    public void setJdbcUrlOverride(String jdbcUrlOverride) { this.jdbcUrlOverride = jdbcUrlOverride; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public ZonedDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(ZonedDateTime createdDate) { this.createdDate = createdDate; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
}
