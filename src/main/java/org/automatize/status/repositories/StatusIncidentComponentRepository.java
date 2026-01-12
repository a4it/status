package org.automatize.status.repositories;

import org.automatize.status.models.StatusIncidentComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatusIncidentComponentRepository extends JpaRepository<StatusIncidentComponent, UUID> {
    
    List<StatusIncidentComponent> findByIncidentId(UUID incidentId);
    
    List<StatusIncidentComponent> findByComponentId(UUID componentId);
    
    Optional<StatusIncidentComponent> findByIncidentIdAndComponentId(UUID incidentId, UUID componentId);
    
    List<StatusIncidentComponent> findByComponentStatus(String componentStatus);
    
    List<StatusIncidentComponent> findByIncidentIdAndComponentStatus(UUID incidentId, String componentStatus);
    
    @Query("SELECT ic FROM StatusIncidentComponent ic WHERE ic.incident.app.tenant.id = :tenantId")
    List<StatusIncidentComponent> findByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT ic FROM StatusIncidentComponent ic WHERE ic.incident.app.organization.id = :organizationId")
    List<StatusIncidentComponent> findByOrganizationId(@Param("organizationId") UUID organizationId);
    
    @Query("SELECT ic FROM StatusIncidentComponent ic WHERE ic.incident.app.id = :appId")
    List<StatusIncidentComponent> findByAppId(@Param("appId") UUID appId);
    
    @Query("SELECT COUNT(ic) FROM StatusIncidentComponent ic WHERE ic.incident.id = :incidentId")
    Long countByIncidentId(@Param("incidentId") UUID incidentId);
    
    @Query("SELECT COUNT(ic) FROM StatusIncidentComponent ic WHERE ic.component.id = :componentId")
    Long countByComponentId(@Param("componentId") UUID componentId);
    
    @Query("SELECT ic FROM StatusIncidentComponent ic WHERE ic.component.id = :componentId AND ic.incident.status != 'RESOLVED'")
    List<StatusIncidentComponent> findActiveIncidentsByComponentId(@Param("componentId") UUID componentId);
    
    void deleteByIncidentId(UUID incidentId);
    
    void deleteByIncidentIdAndComponentId(UUID incidentId, UUID componentId);
    
    boolean existsByIncidentIdAndComponentId(UUID incidentId, UUID componentId);
}