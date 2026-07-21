package org.automatize.status.api.response;

import java.util.UUID;

/**
 * <p>
 * Response object reporting the progress and health of the first-run setup
 * wizard of the status-monitoring application.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Indicate whether the initial setup has been completed</li>
 *   <li>Report database connectivity, connection details and any error</li>
 *   <li>Expose the applied Flyway migration version</li>
 *   <li>Track whether the bootstrap tenant and organization have been created</li>
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
public class SetupStatusResponse {

    private boolean setupCompleted;
    private boolean dbConnected;
    private String dbError;
    private String dbUrl;
    private String dbUsername;
    private String flywayVersion;
    private boolean tenantCreated;
    private UUID tenantId;
    private boolean organizationCreated;
    private UUID organizationId;

    /** Gets whether setup is completed. @return {@code true} if setup is completed */
    public boolean isSetupCompleted() { return setupCompleted; }
    /** Sets whether setup is completed. @param setupCompleted the completion flag to set */
    public void setSetupCompleted(boolean setupCompleted) { this.setupCompleted = setupCompleted; }

    /** Gets whether the database is connected. @return {@code true} if the database is connected */
    public boolean isDbConnected() { return dbConnected; }
    /** Sets whether the database is connected. @param dbConnected the connection flag to set */
    public void setDbConnected(boolean dbConnected) { this.dbConnected = dbConnected; }

    /** Gets the database error message. @return the database error message, or {@code null} if none */
    public String getDbError() { return dbError; }
    /** Sets the database error message. @param dbError the database error message to set */
    public void setDbError(String dbError) { this.dbError = dbError; }

    /** Gets the database URL. @return the database URL */
    public String getDbUrl() { return dbUrl; }
    /** Sets the database URL. @param dbUrl the database URL to set */
    public void setDbUrl(String dbUrl) { this.dbUrl = dbUrl; }

    /** Gets the database username. @return the database username */
    public String getDbUsername() { return dbUsername; }
    /** Sets the database username. @param dbUsername the database username to set */
    public void setDbUsername(String dbUsername) { this.dbUsername = dbUsername; }

    /** Gets the applied Flyway migration version. @return the Flyway version */
    public String getFlywayVersion() { return flywayVersion; }
    /** Sets the applied Flyway migration version. @param flywayVersion the Flyway version to set */
    public void setFlywayVersion(String flywayVersion) { this.flywayVersion = flywayVersion; }

    /** Gets whether the bootstrap tenant was created. @return {@code true} if the tenant was created */
    public boolean isTenantCreated() { return tenantCreated; }
    /** Sets whether the bootstrap tenant was created. @param tenantCreated the flag to set */
    public void setTenantCreated(boolean tenantCreated) { this.tenantCreated = tenantCreated; }

    /** Gets the bootstrap tenant id. @return the tenant id */
    public UUID getTenantId() { return tenantId; }
    /** Sets the bootstrap tenant id. @param tenantId the tenant id to set */
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    /** Gets whether the bootstrap organization was created. @return {@code true} if the organization was created */
    public boolean isOrganizationCreated() { return organizationCreated; }
    /** Sets whether the bootstrap organization was created. @param organizationCreated the flag to set */
    public void setOrganizationCreated(boolean organizationCreated) { this.organizationCreated = organizationCreated; }

    /** Gets the bootstrap organization id. @return the organization id */
    public UUID getOrganizationId() { return organizationId; }
    /** Sets the bootstrap organization id. @param organizationId the organization id to set */
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
}
