package org.automatize.status.repositories;

import org.automatize.status.models.StatusComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatusComponentRepository extends JpaRepository<StatusComponent, UUID> {
    
    List<StatusComponent> findByAppId(UUID appId);
    
    Optional<StatusComponent> findByAppIdAndName(UUID appId, String name);
    
    List<StatusComponent> findByAppIdOrderByPosition(UUID appId);
    
    List<StatusComponent> findByStatus(String status);
    
    List<StatusComponent> findByAppIdAndStatus(UUID appId, String status);
    
    List<StatusComponent> findByGroupName(String groupName);
    
    List<StatusComponent> findByAppIdAndGroupName(UUID appId, String groupName);
    
    @Query("SELECT c FROM StatusComponent c WHERE c.app.tenant.id = :tenantId")
    List<StatusComponent> findByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT c FROM StatusComponent c WHERE c.app.organization.id = :organizationId")
    List<StatusComponent> findByOrganizationId(@Param("organizationId") UUID organizationId);
    
    @Query("SELECT c FROM StatusComponent c WHERE c.app.id = :appId AND (c.name LIKE %:searchTerm% OR c.description LIKE %:searchTerm%)")
    List<StatusComponent> searchByAppId(@Param("appId") UUID appId, @Param("searchTerm") String searchTerm);
    
    @Query("SELECT c FROM StatusComponent c WHERE c.name LIKE %:searchTerm% OR c.description LIKE %:searchTerm%")
    List<StatusComponent> search(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT COUNT(c) FROM StatusComponent c WHERE c.app.id = :appId")
    Long countByAppId(@Param("appId") UUID appId);
    
    @Query("SELECT COUNT(c) FROM StatusComponent c WHERE c.app.tenant.id = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);
    
    boolean existsByAppIdAndName(UUID appId, String name);
}