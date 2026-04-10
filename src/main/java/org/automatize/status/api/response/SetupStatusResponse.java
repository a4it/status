package org.automatize.status.api.response;

import java.util.UUID;

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

    public boolean isSetupCompleted() { return setupCompleted; }
    public void setSetupCompleted(boolean setupCompleted) { this.setupCompleted = setupCompleted; }

    public boolean isDbConnected() { return dbConnected; }
    public void setDbConnected(boolean dbConnected) { this.dbConnected = dbConnected; }

    public String getDbError() { return dbError; }
    public void setDbError(String dbError) { this.dbError = dbError; }

    public String getDbUrl() { return dbUrl; }
    public void setDbUrl(String dbUrl) { this.dbUrl = dbUrl; }

    public String getDbUsername() { return dbUsername; }
    public void setDbUsername(String dbUsername) { this.dbUsername = dbUsername; }

    public String getFlywayVersion() { return flywayVersion; }
    public void setFlywayVersion(String flywayVersion) { this.flywayVersion = flywayVersion; }

    public boolean isTenantCreated() { return tenantCreated; }
    public void setTenantCreated(boolean tenantCreated) { this.tenantCreated = tenantCreated; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public boolean isOrganizationCreated() { return organizationCreated; }
    public void setOrganizationCreated(boolean organizationCreated) { this.organizationCreated = organizationCreated; }

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
}
