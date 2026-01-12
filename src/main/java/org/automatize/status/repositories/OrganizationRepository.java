package org.automatize.status.repositories;

import org.automatize.status.models.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    
    Optional<Organization> findByName(String name);
    
    List<Organization> findByTenantId(UUID tenantId);
    
    List<Organization> findByStatus(String status);
    
    List<Organization> findByTenantIdAndStatus(UUID tenantId, String status);
    
    List<Organization> findByOrganizationType(String organizationType);
    
    List<Organization> findByCountry(String country);
    
    @Query("SELECT o FROM Organization o WHERE o.tenant.id = :tenantId AND (o.name LIKE %:searchTerm% OR o.email LIKE %:searchTerm%)")
    List<Organization> searchByTenantId(@Param("tenantId") UUID tenantId, @Param("searchTerm") String searchTerm);
    
    @Query("SELECT o FROM Organization o WHERE o.name LIKE %:searchTerm% OR o.email LIKE %:searchTerm% OR o.description LIKE %:searchTerm%")
    List<Organization> search(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT o FROM Organization o WHERE o.tenant.id = :tenantId ORDER BY o.createdDate DESC")
    List<Organization> findByTenantIdOrderByCreatedDateDesc(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT COUNT(o) FROM Organization o WHERE o.tenant.id = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);
    
    boolean existsByName(String name);
    
    boolean existsByEmail(String email);
}