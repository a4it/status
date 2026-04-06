package org.automatize.status.repositories;

import org.automatize.status.models.SchedulerJdbcDatasource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link SchedulerJdbcDatasource} entities.
 *
 * <p>Provides CRUD operations and tenant-scoped finders for datasource
 * management used by SQL-type scheduler jobs.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Repository
public interface SchedulerJdbcDatasourceRepository extends JpaRepository<SchedulerJdbcDatasource, UUID> {

    /**
     * Returns all datasources for a tenant filtered by their enabled state.
     *
     * @param tenantId the tenant UUID
     * @param enabled  {@code true} to return only enabled datasources
     * @return list of matching datasources
     */
    List<SchedulerJdbcDatasource> findByTenantIdAndEnabled(UUID tenantId, boolean enabled);

    /**
     * Fetches a single datasource by ID, scoped to the tenant for security.
     *
     * @param id       the datasource UUID
     * @param tenantId the tenant UUID
     * @return an Optional containing the datasource if found and owned by the tenant
     */
    Optional<SchedulerJdbcDatasource> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Returns all datasources belonging to the given tenant.
     *
     * @param tenantId the tenant UUID
     * @return list of all datasources in the tenant
     */
    List<SchedulerJdbcDatasource> findByTenantId(UUID tenantId);
}
