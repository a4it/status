package org.automatize.status.repositories;

import org.automatize.status.models.StatusMaintenanceComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <p>
 * Repository interface for managing {@link StatusMaintenanceComponent} entities.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Manage associations between maintenance windows and affected components</li>
 *   <li>Track which components will be impacted during maintenance</li>
 *   <li>Support queries for active maintenance affecting components</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see StatusMaintenanceComponent
 * @see StatusMaintenanceRepository
 * @see StatusComponentRepository
 */
@Repository
public interface StatusMaintenanceComponentRepository extends JpaRepository<StatusMaintenanceComponent, UUID> {

    /**
     * Finds all maintenance-component associations for a specific maintenance window.
     *
     * @param maintenanceId the unique identifier of the maintenance window
     * @return a list of maintenance-component associations for the specified maintenance
     */
    List<StatusMaintenanceComponent> findByMaintenanceId(UUID maintenanceId);

    /**
     * Finds all maintenance-component associations for a specific component.
     *
     * @param componentId the unique identifier of the component
     * @return a list of maintenance-component associations for the specified component
     */
    List<StatusMaintenanceComponent> findByComponentId(UUID componentId);

    /**
     * Finds a specific maintenance-component association by maintenance and component IDs.
     *
     * @param maintenanceId the unique identifier of the maintenance window
     * @param componentId the unique identifier of the component
     * @return an Optional containing the association if found, or empty if not found
     */
    Optional<StatusMaintenanceComponent> findByMaintenanceIdAndComponentId(UUID maintenanceId, UUID componentId);

    /**
     * Finds all maintenance-component associations within a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a list of maintenance-component associations within the specified tenant
     */
    @Query("SELECT mc FROM StatusMaintenanceComponent mc WHERE mc.maintenance.app.tenant.id = :tenantId")
    List<StatusMaintenanceComponent> findByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Finds all maintenance-component associations within a specific organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return a list of maintenance-component associations within the specified organization
     */
    @Query("SELECT mc FROM StatusMaintenanceComponent mc WHERE mc.maintenance.app.organization.id = :organizationId")
    List<StatusMaintenanceComponent> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Finds all maintenance-component associations for a specific status app.
     *
     * @param appId the unique identifier of the status app
     * @return a list of maintenance-component associations for the specified app
     */
    @Query("SELECT mc FROM StatusMaintenanceComponent mc WHERE mc.maintenance.app.id = :appId")
    List<StatusMaintenanceComponent> findByAppId(@Param("appId") UUID appId);

    /**
     * Counts the total number of components affected by a specific maintenance window.
     *
     * @param maintenanceId the unique identifier of the maintenance window
     * @return the count of components affected by the maintenance
     */
    @Query("SELECT COUNT(mc) FROM StatusMaintenanceComponent mc WHERE mc.maintenance.id = :maintenanceId")
    Long countByMaintenanceId(@Param("maintenanceId") UUID maintenanceId);

    /**
     * Counts the total number of maintenance windows affecting a specific component.
     *
     * @param componentId the unique identifier of the component
     * @return the count of maintenance windows affecting the component
     */
    @Query("SELECT COUNT(mc) FROM StatusMaintenanceComponent mc WHERE mc.component.id = :componentId")
    Long countByComponentId(@Param("componentId") UUID componentId);

    /**
     * Finds all active (scheduled or in progress) maintenance affecting a specific component.
     *
     * @param componentId the unique identifier of the component
     * @return a list of maintenance-component associations for active maintenance windows
     */
    @Query("SELECT mc FROM StatusMaintenanceComponent mc WHERE mc.component.id = :componentId AND mc.maintenance.status IN ('SCHEDULED', 'IN_PROGRESS')")
    List<StatusMaintenanceComponent> findActiveMaintenanceByComponentId(@Param("componentId") UUID componentId);

    /**
     * Deletes all maintenance-component associations for a specific maintenance window.
     *
     * @param maintenanceId the unique identifier of the maintenance window
     */
    void deleteByMaintenanceId(UUID maintenanceId);

    /**
     * Deletes a specific maintenance-component association by maintenance and component IDs.
     *
     * @param maintenanceId the unique identifier of the maintenance window
     * @param componentId the unique identifier of the component
     */
    void deleteByMaintenanceIdAndComponentId(UUID maintenanceId, UUID componentId);

    /**
     * Checks if an association between the specified maintenance and component exists.
     *
     * @param maintenanceId the unique identifier of the maintenance window
     * @param componentId the unique identifier of the component
     * @return true if the association exists, false otherwise
     */
    boolean existsByMaintenanceIdAndComponentId(UUID maintenanceId, UUID componentId);
}