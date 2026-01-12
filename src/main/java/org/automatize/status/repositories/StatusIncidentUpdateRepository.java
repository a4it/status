package org.automatize.status.repositories;

import org.automatize.status.models.StatusIncidentUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StatusIncidentUpdateRepository extends JpaRepository<StatusIncidentUpdate, UUID> {
    
    List<StatusIncidentUpdate> findByIncidentId(UUID incidentId);
    
    List<StatusIncidentUpdate> findByIncidentIdOrderByUpdateTime(UUID incidentId);
    
    List<StatusIncidentUpdate> findByIncidentIdOrderByUpdateTimeDesc(UUID incidentId);
    
    List<StatusIncidentUpdate> findByStatus(String status);
    
    List<StatusIncidentUpdate> findByUpdateTimeBetween(ZonedDateTime startDate, ZonedDateTime endDate);
    
    @Query("SELECT u FROM StatusIncidentUpdate u WHERE u.incident.app.tenant.id = :tenantId")
    List<StatusIncidentUpdate> findByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT u FROM StatusIncidentUpdate u WHERE u.incident.app.organization.id = :organizationId")
    List<StatusIncidentUpdate> findByOrganizationId(@Param("organizationId") UUID organizationId);
    
    @Query("SELECT u FROM StatusIncidentUpdate u WHERE u.incident.app.id = :appId")
    List<StatusIncidentUpdate> findByAppId(@Param("appId") UUID appId);
    
    @Query("SELECT u FROM StatusIncidentUpdate u WHERE u.message LIKE %:searchTerm%")
    List<StatusIncidentUpdate> search(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT u FROM StatusIncidentUpdate u WHERE u.incident.id = :incidentId AND u.message LIKE %:searchTerm%")
    List<StatusIncidentUpdate> searchByIncidentId(@Param("incidentId") UUID incidentId, @Param("searchTerm") String searchTerm);
    
    @Query("SELECT COUNT(u) FROM StatusIncidentUpdate u WHERE u.incident.id = :incidentId")
    Long countByIncidentId(@Param("incidentId") UUID incidentId);
    
    @Query("SELECT u FROM StatusIncidentUpdate u WHERE u.incident.id = :incidentId ORDER BY u.updateTime DESC LIMIT 1")
    StatusIncidentUpdate findLatestByIncidentId(@Param("incidentId") UUID incidentId);
}