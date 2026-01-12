package org.automatize.status.repositories;

import org.automatize.status.models.StatusMaintenanceComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatusMaintenanceComponentRepository extends JpaRepository<StatusMaintenanceComponent, UUID> {
    
    List<StatusMaintenanceComponent> findByMaintenanceId(UUID maintenanceId);
    
    List<StatusMaintenanceComponent> findByComponentId(UUID componentId);
    
    Optional<StatusMaintenanceComponent> findByMaintenanceIdAndComponentId(UUID maintenanceId, UUID componentId);
    
    @Query("SELECT mc FROM StatusMaintenanceComponent mc WHERE mc.maintenance.app.tenant.id = :tenantId")
    List<StatusMaintenanceComponent> findByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT mc FROM StatusMaintenanceComponent mc WHERE mc.maintenance.app.organization.id = :organizationId")
    List<StatusMaintenanceComponent> findByOrganizationId(@Param("organizationId") UUID organizationId);
    
    @Query("SELECT mc FROM StatusMaintenanceComponent mc WHERE mc.maintenance.app.id = :appId")
    List<StatusMaintenanceComponent> findByAppId(@Param("appId") UUID appId);
    
    @Query("SELECT COUNT(mc) FROM StatusMaintenanceComponent mc WHERE mc.maintenance.id = :maintenanceId")
    Long countByMaintenanceId(@Param("maintenanceId") UUID maintenanceId);
    
    @Query("SELECT COUNT(mc) FROM StatusMaintenanceComponent mc WHERE mc.component.id = :componentId")
    Long countByComponentId(@Param("componentId") UUID componentId);
    
    @Query("SELECT mc FROM StatusMaintenanceComponent mc WHERE mc.component.id = :componentId AND mc.maintenance.status IN ('SCHEDULED', 'IN_PROGRESS')")
    List<StatusMaintenanceComponent> findActiveMaintenanceByComponentId(@Param("componentId") UUID componentId);
    
    void deleteByMaintenanceId(UUID maintenanceId);
    
    void deleteByMaintenanceIdAndComponentId(UUID maintenanceId, UUID componentId);
    
    boolean existsByMaintenanceIdAndComponentId(UUID maintenanceId, UUID componentId);
}