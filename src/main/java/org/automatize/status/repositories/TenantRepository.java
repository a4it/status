package org.automatize.status.repositories;

import org.automatize.status.models.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByName(String name);

    List<Tenant> findByIsActive(Boolean isActive);

    @Query("SELECT t FROM Tenant t WHERE t.name LIKE %:searchTerm%")
    List<Tenant> search(@Param("searchTerm") String searchTerm);

    @Query("SELECT t FROM Tenant t WHERE t.createdBy = :createdBy")
    List<Tenant> findByCreatedBy(@Param("createdBy") String createdBy);

    @Query("SELECT t FROM Tenant t ORDER BY t.createdDate DESC")
    List<Tenant> findAllOrderByCreatedDateDesc();

    boolean existsByName(String name);
}