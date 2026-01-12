package org.automatize.status.repositories;

import org.automatize.status.models.StatusIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StatusIncidentRepository extends JpaRepository<StatusIncident, UUID> {
    
    List<StatusIncident> findByAppId(UUID appId);
    
    List<StatusIncident> findByStatus(String status);
    
    List<StatusIncident> findBySeverity(String severity);
    
    List<StatusIncident> findByIsPublic(Boolean isPublic);
    
    List<StatusIncident> findByAppIdAndStatus(UUID appId, String status);
    
    List<StatusIncident> findByAppIdAndIsPublic(UUID appId, Boolean isPublic);
    
    List<StatusIncident> findByStartedAtBetween(ZonedDateTime startDate, ZonedDateTime endDate);
    
    List<StatusIncident> findByResolvedAtIsNull();
    
    List<StatusIncident> findByResolvedAtIsNotNull();
    
    @Query("SELECT i FROM StatusIncident i WHERE i.app.tenant.id = :tenantId")
    List<StatusIncident> findByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT i FROM StatusIncident i WHERE i.app.organization.id = :organizationId")
    List<StatusIncident> findByOrganizationId(@Param("organizationId") UUID organizationId);
    
    @Query("SELECT i FROM StatusIncident i WHERE i.app.id = :appId ORDER BY i.startedAt DESC")
    List<StatusIncident> findByAppIdOrderByStartedAtDesc(@Param("appId") UUID appId);
    
    @Query("SELECT i FROM StatusIncident i WHERE i.app.id = :appId AND i.startedAt >= :startDate")
    List<StatusIncident> findRecentIncidentsByAppId(@Param("appId") UUID appId, @Param("startDate") ZonedDateTime startDate);
    
    @Query("SELECT i FROM StatusIncident i WHERE i.title LIKE %:searchTerm% OR i.description LIKE %:searchTerm%")
    List<StatusIncident> search(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT COUNT(i) FROM StatusIncident i WHERE i.app.id = :appId")
    Long countByAppId(@Param("appId") UUID appId);
    
    @Query("SELECT COUNT(i) FROM StatusIncident i WHERE i.app.tenant.id = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT COUNT(i) FROM StatusIncident i WHERE i.app.id = :appId AND i.status != 'RESOLVED'")
    Long countActiveIncidentsByAppId(@Param("appId") UUID appId);
}