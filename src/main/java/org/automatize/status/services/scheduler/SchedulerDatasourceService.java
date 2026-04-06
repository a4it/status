package org.automatize.status.services.scheduler;

import org.automatize.status.models.SchedulerJdbcDatasource;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.SchedulerJdbcDatasourceRepository;
import org.automatize.status.repositories.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CRUD service for {@link SchedulerJdbcDatasource} (shared JDBC datasource configurations).
 *
 * <p>Passwords are encrypted at rest using {@link SchedulerEncryptionService} and are
 * never returned in plaintext via the API. Connection testing is delegated to
 * {@link SqlExecutorService#testConnection(SchedulerJdbcDatasource)}.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Service
@Transactional
public class SchedulerDatasourceService {

    @Autowired private SchedulerJdbcDatasourceRepository datasourceRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private SchedulerEncryptionService encryptionService;
    @Autowired private SqlExecutorService sqlExecutorService;

    /**
     * Returns all datasources for the given tenant.
     *
     * @param tenantId the tenant scope
     * @return list of datasources
     */
    @Transactional(readOnly = true)
    public List<SchedulerJdbcDatasource> list(UUID tenantId) {
        return datasourceRepository.findByTenantId(tenantId);
    }

    /**
     * Returns a single datasource scoped to the tenant, throwing when not found.
     *
     * @param id       the datasource UUID
     * @param tenantId the tenant scope
     * @return the datasource
     */
    @Transactional(readOnly = true)
    public SchedulerJdbcDatasource get(UUID id, UUID tenantId) {
        return datasourceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Datasource not found"));
    }

    /**
     * Creates a new datasource. The password (if provided) is encrypted before persistence.
     *
     * @param ds       the datasource to create
     * @param tenantId the tenant scope
     * @param username the creating user's login name (for audit)
     * @return the saved datasource
     */
    public SchedulerJdbcDatasource create(SchedulerJdbcDatasource ds, UUID tenantId, String username) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        ds.setTenant(tenant);
        ds.setCreatedBy(username);
        ds.setLastModifiedBy(username);
        if (ds.getPasswordEnc() != null && !ds.getPasswordEnc().isBlank()) {
            ds.setPasswordEnc(encryptionService.encrypt(ds.getPasswordEnc()));
        }
        return datasourceRepository.save(ds);
    }

    /**
     * Updates an existing datasource. The password is only replaced when a non-blank value
     * is supplied (enabling partial updates that leave the stored password unchanged).
     *
     * @param id       the datasource UUID to update
     * @param dsData   updated values
     * @param tenantId the tenant scope (for security)
     * @param username the modifying user's login name (for audit)
     * @return the updated datasource
     */
    public SchedulerJdbcDatasource update(UUID id, SchedulerJdbcDatasource dsData,
                                          UUID tenantId, String username) {
        SchedulerJdbcDatasource existing = datasourceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Datasource not found"));

        existing.setName(dsData.getName());
        existing.setDescription(dsData.getDescription());
        existing.setDbType(dsData.getDbType());
        existing.setHost(dsData.getHost());
        existing.setPort(dsData.getPort());
        existing.setDatabaseName(dsData.getDatabaseName());
        existing.setSchemaName(dsData.getSchemaName());
        existing.setJdbcUrlOverride(dsData.getJdbcUrlOverride());
        existing.setUsername(dsData.getUsername());
        if (dsData.getPasswordEnc() != null && !dsData.getPasswordEnc().isBlank()) {
            existing.setPasswordEnc(encryptionService.encrypt(dsData.getPasswordEnc()));
        }
        existing.setMinPoolSize(dsData.getMinPoolSize());
        existing.setMaxPoolSize(dsData.getMaxPoolSize());
        existing.setConnectionTimeoutMs(dsData.getConnectionTimeoutMs());
        existing.setEnabled(dsData.getEnabled());
        existing.setLastModifiedBy(username);

        return datasourceRepository.save(existing);
    }

    /**
     * Permanently deletes a datasource.
     *
     * @param id       the datasource UUID to delete
     * @param tenantId the tenant scope (for security)
     */
    public void delete(UUID id, UUID tenantId) {
        SchedulerJdbcDatasource ds = datasourceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Datasource not found"));
        datasourceRepository.delete(ds);
    }

    /**
     * Opens a test connection to the datasource and returns a result map containing
     * {@code success} (boolean), optional {@code error} (string), and {@code latencyMs} (long).
     *
     * @param id       the datasource UUID to test
     * @param tenantId the tenant scope (for security)
     * @return test result map
     */
    public Map<String, Object> testConnection(UUID id, UUID tenantId) {
        SchedulerJdbcDatasource ds = get(id, tenantId);
        return sqlExecutorService.testConnection(ds);
    }
}
