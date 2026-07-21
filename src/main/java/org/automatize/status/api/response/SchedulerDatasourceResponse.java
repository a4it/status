package org.automatize.status.api.response;

import org.automatize.status.models.SchedulerJdbcDatasource;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * <p>
 * Response class for a scheduler JDBC datasource.
 * The {@code passwordEnc} field is never returned.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Expose the connection metadata of a scheduler JDBC datasource to API clients</li>
 *   <li>Deliberately omit the encrypted password so credentials are never leaked</li>
 *   <li>Carry the owning tenant and organization identifiers for multi-tenant scoping</li>
 * </ul>
 * </p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
public class SchedulerDatasourceResponse {

    /** The unique identifier of the datasource. */
    private UUID id;

    /** The display name of the datasource. */
    private String name;

    /** The description of the datasource. */
    private String description;

    /** The database type (e.g. POSTGRES, MYSQL). */
    private String dbType;

    /** The database host. */
    private String host;

    /** The database port. */
    private Integer port;

    /** The name of the target database. */
    private String databaseName;

    /** The name of the target schema. */
    private String schemaName;

    /** An optional explicit JDBC URL overriding the derived one. */
    private String jdbcUrlOverride;

    /** The username used to connect. */
    private String username;

    /** Whether the datasource is enabled. */
    private Boolean enabled;

    /** When the datasource was created. */
    private ZonedDateTime createdDate;

    /** The identifier of the owning tenant. */
    private UUID tenantId;

    /** The identifier of the owning organization. */
    private UUID organizationId;

    // -------------------------------------------------------------------------
    // Factory method
    // -------------------------------------------------------------------------

    /**
     * Builds a response from a {@link SchedulerJdbcDatasource} entity, intentionally
     * excluding the encrypted password so it is never exposed to API clients.
     *
     * @param ds the datasource entity to convert
     * @return a populated response mirroring the entity, without the password
     */
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

    /** Gets the ID. @return the ID */
    public UUID getId() { return id; }
    /** Sets the ID. @param id the ID to set */
    public void setId(UUID id) { this.id = id; }

    /** Gets the name. @return the name */
    public String getName() { return name; }
    /** Sets the name. @param name the name to set */
    public void setName(String name) { this.name = name; }

    /** Gets the description. @return the description */
    public String getDescription() { return description; }
    /** Sets the description. @param description the description to set */
    public void setDescription(String description) { this.description = description; }

    /** Gets the database type. @return the database type */
    public String getDbType() { return dbType; }
    /** Sets the database type. @param dbType the database type to set */
    public void setDbType(String dbType) { this.dbType = dbType; }

    /** Gets the host. @return the host */
    public String getHost() { return host; }
    /** Sets the host. @param host the host to set */
    public void setHost(String host) { this.host = host; }

    /** Gets the port. @return the port */
    public Integer getPort() { return port; }
    /** Sets the port. @param port the port to set */
    public void setPort(Integer port) { this.port = port; }

    /** Gets the database name. @return the database name */
    public String getDatabaseName() { return databaseName; }
    /** Sets the database name. @param databaseName the database name to set */
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }

    /** Gets the schema name. @return the schema name */
    public String getSchemaName() { return schemaName; }
    /** Sets the schema name. @param schemaName the schema name to set */
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    /** Gets the JDBC URL override. @return the JDBC URL override */
    public String getJdbcUrlOverride() { return jdbcUrlOverride; }
    /** Sets the JDBC URL override. @param jdbcUrlOverride the JDBC URL override to set */
    public void setJdbcUrlOverride(String jdbcUrlOverride) { this.jdbcUrlOverride = jdbcUrlOverride; }

    /** Gets the username. @return the username */
    public String getUsername() { return username; }
    /** Sets the username. @param username the username to set */
    public void setUsername(String username) { this.username = username; }

    /** Gets the enabled flag. @return true if enabled, false otherwise */
    public Boolean getEnabled() { return enabled; }
    /** Sets the enabled flag. @param enabled the enabled flag to set */
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    /** Gets the creation date. @return the creation date */
    public ZonedDateTime getCreatedDate() { return createdDate; }
    /** Sets the creation date. @param createdDate the creation date to set */
    public void setCreatedDate(ZonedDateTime createdDate) { this.createdDate = createdDate; }

    /** Gets the tenant ID. @return the tenant ID */
    public UUID getTenantId() { return tenantId; }
    /** Sets the tenant ID. @param tenantId the tenant ID to set */
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    /** Gets the organization ID. @return the organization ID */
    public UUID getOrganizationId() { return organizationId; }
    /** Sets the organization ID. @param organizationId the organization ID to set */
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
}
