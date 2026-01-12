package org.automatize.status.repositories;

import org.automatize.status.models.StatusMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StatusMaintenanceRepository extends JpaRepository<StatusMaintenance, UUID> {
    
    List<StatusMaintenance> findByAppId(UUID appId);
    
    List<StatusMaintenance> findByStatus(String status);
    
    List<StatusMaintenance> findByIsPublic(Boolean isPublic);
    
    List<StatusMaintenance> findByAppIdAndStatus(UUID appId, String status);
    
    List<StatusMaintenance> findByAppIdAndIsPublic(UUID appId, Boolean isPublic);
    
    List<StatusMaintenance> findByStartsAtBetween(ZonedDateTime startDate, ZonedDateTime endDate);
    
    List<StatusMaintenance> findByEndsAtBetween(ZonedDateTime startDate, ZonedDateTime endDate);
    
    @Query("SELECT m FROM StatusMaintenance m WHERE m.app.tenant.id = :tenantId")
    List<StatusMaintenance> findByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT m FROM StatusMaintenance m WHERE m.app.organization.id = :organizationId")
    List<StatusMaintenance> findByOrganizationId(@Param("organizationId") UUID organizationId);
    
    @Query("SELECT m FROM StatusMaintenance m WHERE m.app.id = :appId ORDER BY m.startsAt DESC")
    List<StatusMaintenance> findByAppIdOrderByStartsAtDesc(@Param("appId") UUID appId);
    
    @Query("SELECT m FROM StatusMaintenance m WHERE m.startsAt <= :currentTime AND m.endsAt >= :currentTime")
    List<StatusMaintenance> findActiveMaintenance(@Param("currentTime") ZonedDateTime currentTime);
    
    @Query("SELECT m FROM StatusMaintenance m WHERE m.app.id = :appId AND m.startsAt >= :startDate")
    List<StatusMaintenance> findUpcomingMaintenanceByAppId(@Param("appId") UUID appId, @Param("startDate") ZonedDateTime startDate);
    
    @Query("SELECT m FROM StatusMaintenance m WHERE m.title LIKE %:searchTerm% OR m.description LIKE %:searchTerm%")
    List<StatusMaintenance> search(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT COUNT(m) FROM StatusMaintenance m WHERE m.app.id = :appId")
    Long countByAppId(@Param("appId") UUID appId);
    
    @Query("SELECT COUNT(m) FROM StatusMaintenance m WHERE m.app.tenant.id = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT COUNT(m) FROM StatusMaintenance m WHERE m.app.id = :appId AND m.status IN ('SCHEDULED', 'IN_PROGRESS')")
    Long countActiveMaintenanceByAppId(@Param("appId") UUID appId);
}