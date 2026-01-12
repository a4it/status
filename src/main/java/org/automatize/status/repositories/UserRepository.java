package org.automatize.status.repositories;

import org.automatize.status.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    List<User> findByOrganizationId(UUID organizationId);
    
    List<User> findByEnabled(Boolean enabled);
    
    List<User> findByRole(String role);
    
    List<User> findByStatus(String status);
    
    List<User> findByOrganizationIdAndEnabled(UUID organizationId, Boolean enabled);
    
    List<User> findByOrganizationIdAndRole(UUID organizationId, String role);
    
    @Query("SELECT u FROM User u WHERE u.organization.tenant.id = :tenantId")
    List<User> findByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT u FROM User u WHERE u.organization.id = :organizationId AND (u.username LIKE %:searchTerm% OR u.email LIKE %:searchTerm% OR u.fullName LIKE %:searchTerm%)")
    List<User> searchByOrganizationId(@Param("organizationId") UUID organizationId, @Param("searchTerm") String searchTerm);
    
    @Query("SELECT u FROM User u WHERE u.username LIKE %:searchTerm% OR u.email LIKE %:searchTerm% OR u.fullName LIKE %:searchTerm%")
    List<User> search(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.organization.id = :organizationId")
    Long countByOrganizationId(@Param("organizationId") UUID organizationId);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.organization.tenant.id = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
}