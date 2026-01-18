package org.automatize.status.repositories;

import org.automatize.status.models.StatusIncidentComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <p>
 * Repository interface for managing {@link StatusIncidentComponent} entities.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Manage associations between incidents and affected components</li>
 *   <li>Track component-specific status during incidents</li>
 *   <li>Support queries for active incidents affecting components</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see StatusIncidentComponent
 * @see StatusIncidentRepository
 * @see StatusComponentRepository
 */
@Repository
public interface StatusIncidentComponentRepository extends JpaRepository<StatusIncidentComponent, UUID> {

    /**
     * Finds all incident-component associations for a specific incident.
     *
     * @param incidentId the unique identifier of the incident
     * @return a list of incident-component associations for the specified incident
     */
    List<StatusIncidentComponent> findByIncidentId(UUID incidentId);

    /**
     * Finds all incident-component associations for a specific component.
     *
     * @param componentId the unique identifier of the component
     * @return a list of incident-component associations for the specified component
     */
    List<StatusIncidentComponent> findByComponentId(UUID componentId);

    /**
     * Finds a specific incident-component association by incident and component IDs.
     *
     * @param incidentId the unique identifier of the incident
     * @param componentId the unique identifier of the component
     * @return an Optional containing the association if found, or empty if not found
     */
    Optional<StatusIncidentComponent> findByIncidentIdAndComponentId(UUID incidentId, UUID componentId);

    /**
     * Finds all incident-component associations with a specific component status.
     *
     * @param componentStatus the component status to filter by (e.g., "DEGRADED", "OUTAGE")
     * @return a list of associations matching the specified component status
     */
    List<StatusIncidentComponent> findByComponentStatus(String componentStatus);

    /**
     * Finds all incident-component associations for an incident with a specific component status.
     *
     * @param incidentId the unique identifier of the incident
     * @param componentStatus the component status to filter by
     * @return a list of associations matching both the incident and component status criteria
     */
    List<StatusIncidentComponent> findByIncidentIdAndComponentStatus(UUID incidentId, String componentStatus);

    /**
     * Finds all incident-component associations within a specific tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a list of incident-component associations within the specified tenant
     */
    @Query("SELECT ic FROM StatusIncidentComponent ic WHERE ic.incident.app.tenant.id = :tenantId")
    List<StatusIncidentComponent> findByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Finds all incident-component associations within a specific organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return a list of incident-component associations within the specified organization
     */
    @Query("SELECT ic FROM StatusIncidentComponent ic WHERE ic.incident.app.organization.id = :organizationId")
    List<StatusIncidentComponent> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Finds all incident-component associations for a specific status app.
     *
     * @param appId the unique identifier of the status app
     * @return a list of incident-component associations for the specified app
     */
    @Query("SELECT ic FROM StatusIncidentComponent ic WHERE ic.incident.app.id = :appId")
    List<StatusIncidentComponent> findByAppId(@Param("appId") UUID appId);

    /**
     * Counts the total number of components affected by a specific incident.
     *
     * @param incidentId the unique identifier of the incident
     * @return the count of components affected by the incident
     */
    @Query("SELECT COUNT(ic) FROM StatusIncidentComponent ic WHERE ic.incident.id = :incidentId")
    Long countByIncidentId(@Param("incidentId") UUID incidentId);

    /**
     * Counts the total number of incidents affecting a specific component.
     *
     * @param componentId the unique identifier of the component
     * @return the count of incidents affecting the component
     */
    @Query("SELECT COUNT(ic) FROM StatusIncidentComponent ic WHERE ic.component.id = :componentId")
    Long countByComponentId(@Param("componentId") UUID componentId);

    /**
     * Finds all active (unresolved) incidents affecting a specific component.
     *
     * @param componentId the unique identifier of the component
     * @return a list of incident-component associations for active incidents
     */
    @Query("SELECT ic FROM StatusIncidentComponent ic WHERE ic.component.id = :componentId AND ic.incident.status != 'RESOLVED'")
    List<StatusIncidentComponent> findActiveIncidentsByComponentId(@Param("componentId") UUID componentId);

    /**
     * Deletes all incident-component associations for a specific incident.
     *
     * @param incidentId the unique identifier of the incident
     */
    void deleteByIncidentId(UUID incidentId);

    /**
     * Deletes a specific incident-component association by incident and component IDs.
     *
     * @param incidentId the unique identifier of the incident
     * @param componentId the unique identifier of the component
     */
    void deleteByIncidentIdAndComponentId(UUID incidentId, UUID componentId);

    /**
     * Checks if an association between the specified incident and component exists.
     *
     * @param incidentId the unique identifier of the incident
     * @param componentId the unique identifier of the component
     * @return true if the association exists, false otherwise
     */
    boolean existsByIncidentIdAndComponentId(UUID incidentId, UUID componentId);
}