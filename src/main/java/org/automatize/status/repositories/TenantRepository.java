package org.automatize.status.repositories;

import org.automatize.status.models.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <p>
 * Repository interface for managing {@link Tenant} entities.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for tenant data</li>
 *   <li>Support searching and filtering tenants by name and active status</li>
 *   <li>Enable existence checks for unique constraints</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see Tenant
 * @see OrganizationRepository
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Finds a tenant by its unique name.
     *
     * @param name the name of the tenant to find
     * @return an Optional containing the tenant if found, or empty if not found
     */
    Optional<Tenant> findByName(String name);

    /**
     * Finds all tenants by their active status.
     *
     * @param isActive true to find active tenants, false to find inactive ones
     * @return a list of tenants matching the active status
     */
    List<Tenant> findByIsActive(Boolean isActive);

    /**
     * Searches for tenants by name containing the search term.
     *
     * @param searchTerm the term to search for in tenant names
     * @return a list of tenants matching the search criteria
     */
    @Query("SELECT t FROM Tenant t WHERE t.name LIKE %:searchTerm%")
    List<Tenant> search(@Param("searchTerm") String searchTerm);

    /**
     * Finds all tenants created by a specific user.
     *
     * @param createdBy the identifier of the user who created the tenants
     * @return a list of tenants created by the specified user
     */
    @Query("SELECT t FROM Tenant t WHERE t.createdBy = :createdBy")
    List<Tenant> findByCreatedBy(@Param("createdBy") String createdBy);

    /**
     * Finds all tenants ordered by creation date in descending order.
     *
     * @return a list of all tenants ordered by creation date (newest first)
     */
    @Query("SELECT t FROM Tenant t ORDER BY t.createdDate DESC")
    List<Tenant> findAllOrderByCreatedDateDesc();

    /**
     * Checks if a tenant with the specified name already exists.
     *
     * @param name the name to check for existence
     * @return true if a tenant with the name exists, false otherwise
     */
    boolean existsByName(String name);
}