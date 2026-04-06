package org.automatize.status.repositories;

import org.automatize.status.models.SchedulerJob;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.automatize.status.models.scheduler.JobStatus;
import org.automatize.status.models.scheduler.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link SchedulerJob} entities.
 *
 * <p>Provides CRUD operations and specialised finders for tenant-scoped job
 * management, scheduling evaluation, and dashboard statistics.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Repository
public interface SchedulerJobRepository extends JpaRepository<SchedulerJob, UUID> {

    /**
     * Returns a page of jobs belonging to the given tenant whose status is in
     * the supplied list.
     *
     * @param tenantId the tenant to scope the query to
     * @param statuses the set of statuses to include
     * @param pageable pagination and sorting parameters
     * @return a page of matching jobs
     */
    Page<SchedulerJob> findByTenantIdAndStatusIn(UUID tenantId, List<JobStatus> statuses, Pageable pageable);

    /**
     * Returns all enabled jobs in the given status; used by the scheduler
     * dispatcher to find candidates for execution.
     *
     * @param status the job status to filter by (typically {@code ACTIVE})
     * @return list of enabled jobs
     */
    List<SchedulerJob> findByEnabledTrueAndStatus(JobStatus status);

    /**
     * Fetches a single job by ID, scoped to the tenant for security.
     *
     * @param id       the job UUID
     * @param tenantId the tenant UUID
     * @return an Optional containing the job if found and owned by the tenant
     */
    Optional<SchedulerJob> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Counts jobs per job type within a tenant; used for dashboard metrics.
     *
     * @param tenantId the tenant UUID
     * @param type     the job type to count
     * @return the number of jobs of that type for the tenant
     */
    long countByTenantIdAndJobType(UUID tenantId, JobType type);

    /**
     * Counts jobs whose last run had the given status; used for dashboard metrics.
     *
     * @param tenantId the tenant UUID
     * @param status   the last-run status to count
     * @return the number of matching jobs
     */
    long countByTenantIdAndLastRunStatus(UUID tenantId, JobRunStatus status);

    /**
     * Returns all jobs belonging to the given tenant.
     *
     * @param tenantId the tenant UUID
     * @return list of all jobs in the tenant
     */
    List<SchedulerJob> findByTenantId(UUID tenantId);

    /**
     * Full-text search over job names within a tenant, filtered to ACTIVE jobs.
     * When {@code search} is {@code null} all ACTIVE jobs are returned.
     *
     * @param tenantId the tenant UUID to scope results to
     * @param search   optional search term matched case-insensitively against job name
     * @param pageable pagination and sorting parameters
     * @return a page of matching jobs
     */
    @Query("SELECT j FROM SchedulerJob j WHERE j.tenant.id = :tenantId AND j.status = 'ACTIVE' " +
           "AND (:search IS NULL OR LOWER(j.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<SchedulerJob> searchByTenant(@Param("tenantId") UUID tenantId,
                                      @Param("search") String search,
                                      Pageable pageable);
}
