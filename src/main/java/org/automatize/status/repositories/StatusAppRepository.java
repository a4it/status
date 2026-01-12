package org.automatize.status.repositories;

import org.automatize.status.models.StatusApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatusAppRepository extends JpaRepository<StatusApp, UUID> {
    
    Optional<StatusApp> findBySlug(String slug);
    
    Optional<StatusApp> findByTenantIdAndSlug(UUID tenantId, String slug);
    
    List<StatusApp> findByTenantId(UUID tenantId);
    
    List<StatusApp> findByOrganizationId(UUID organizationId);
    
    List<StatusApp> findByStatus(String status);
    
    List<StatusApp> findByIsPublic(Boolean isPublic);
    
    List<StatusApp> findByTenantIdAndIsPublic(UUID tenantId, Boolean isPublic);
    
    List<StatusApp> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    @Query("SELECT s FROM StatusApp s WHERE s.tenant.id = :tenantId AND (s.name LIKE %:searchTerm% OR s.description LIKE %:searchTerm% OR s.slug LIKE %:searchTerm%)")
    List<StatusApp> searchByTenantId(@Param("tenantId") UUID tenantId, @Param("searchTerm") String searchTerm);
    
    @Query("SELECT s FROM StatusApp s WHERE s.organization.id = :organizationId AND (s.name LIKE %:searchTerm% OR s.description LIKE %:searchTerm%)")
    List<StatusApp> searchByOrganizationId(@Param("organizationId") UUID organizationId, @Param("searchTerm") String searchTerm);
    
    @Query("SELECT s FROM StatusApp s WHERE s.name LIKE %:searchTerm% OR s.description LIKE %:searchTerm% OR s.slug LIKE %:searchTerm%")
    List<StatusApp> search(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT COUNT(s) FROM StatusApp s WHERE s.tenant.id = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT COUNT(s) FROM StatusApp s WHERE s.organization.id = :organizationId")
    Long countByOrganizationId(@Param("organizationId") UUID organizationId);
    
    boolean existsBySlug(String slug);
    
    boolean existsByTenantIdAndSlug(UUID tenantId, String slug);
}