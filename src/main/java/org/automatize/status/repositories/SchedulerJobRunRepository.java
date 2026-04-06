package org.automatize.status.repositories;

import org.automatize.status.models.SchedulerJobRun;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link SchedulerJobRun} entities.
 *
 * <p>Provides run-history lookups for individual jobs and tenant-wide views,
 * as well as retention-based bulk-delete operations.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Repository
public interface SchedulerJobRunRepository extends JpaRepository<SchedulerJobRun, UUID> {

    /**
     * Returns the run history for a specific job, most recent first.
     *
     * @param jobId    the job UUID
     * @param pageable pagination parameters
     * @return a page of run records for the job
     */
    Page<SchedulerJobRun> findByJobIdOrderByStartedAtDesc(UUID jobId, Pageable pageable);

    /**
     * Returns the run history across all jobs for a tenant, most recent first.
     *
     * @param tenantId the tenant UUID
     * @param pageable pagination parameters
     * @return a page of run records for the tenant
     */
    Page<SchedulerJobRun> findByTenantIdOrderByStartedAtDesc(UUID tenantId, Pageable pageable);

    /**
     * Returns the most recent run for a job that has the given status.
     *
     * @param jobId  the job UUID
     * @param status the run status to filter by
     * @return an Optional containing the most recent run with that status
     */
    Optional<SchedulerJobRun> findTopByJobIdAndStatusOrderByStartedAtDesc(UUID jobId, JobRunStatus status);

    /**
     * Returns all runs for a job with the given status; used for retry logic.
     *
     * @param jobId  the job UUID
     * @param status the run status to filter by
     * @return list of matching runs
     */
    List<SchedulerJobRun> findByJobIdAndStatus(UUID jobId, JobRunStatus status);

    /**
     * Counts runs for a tenant that have the given status and started after the
     * given timestamp; used for dashboard SLA metrics.
     *
     * @param tenantId the tenant UUID
     * @param status   the run status to count
     * @param since    the lower bound of the time window (exclusive)
     * @return count of matching runs
     */
    long countByTenantIdAndStatusAndStartedAtAfter(UUID tenantId, JobRunStatus status, ZonedDateTime since);

    /**
     * Deletes all runs for a job that started before the given cutoff date;
     * used for retention / housekeeping.
     *
     * @param jobId  the job UUID whose old runs should be removed
     * @param cutoff runs with {@code started_at} strictly before this timestamp are deleted
     */
    @Modifying
    @Query("DELETE FROM SchedulerJobRun r WHERE r.job.id = :jobId AND r.startedAt < :cutoff")
    void deleteByJobIdAndStartedAtBefore(@Param("jobId") UUID jobId, @Param("cutoff") ZonedDateTime cutoff);

    /**
     * Returns the 100 most recent runs for a job; used for run-history views.
     *
     * @param jobId the job UUID
     * @return list of up to 100 most recent runs, ordered most recent first
     */
    List<SchedulerJobRun> findTop100ByJobIdOrderByStartedAtDesc(UUID jobId);
}
