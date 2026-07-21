package org.automatize.status.controllers.api;

import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.api.response.SchedulerJobRunResponse;
import org.automatize.status.models.SchedulerJobRun;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.automatize.status.repositories.SchedulerJobRunRepository;
import org.automatize.status.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API controller for viewing and managing scheduler job run records.
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@RestController
@RequestMapping("/api/scheduler/runs")
@PreAuthorize("isAuthenticated()")
public class SchedulerRunApiController {

    @Autowired
    private SchedulerJobRunRepository runRepository;

    // -------------------------------------------------------------------------
    // List runs for the tenant with optional filters
    // -------------------------------------------------------------------------

    /**
     * Lists job run records for the current tenant, optionally filtered by job.
     * <p>
     * HTTP GET {@code /api/scheduler/runs}
     * </p>
     *
     * @param page the zero-based page index (default 0)
     * @param size the page size (default 20)
     * @param jobId optional filter restricting results to a single job
     * @param status optional filter by run status
     * @return ResponseEntity containing a page of job run responses
     */
    @GetMapping
    public ResponseEntity<Page<SchedulerJobRunResponse>> listRuns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) JobRunStatus status) {

        UserPrincipal principal = currentPrincipal();
        UUID tenantId = principal.getTenantId();
        Pageable pageable = PageRequest.of(page, size);

        Page<SchedulerJobRun> runs;
        // When a job id is supplied, scope the query to that job's runs
        if (jobId != null) {
            runs = runRepository.findByJobIdOrderByStartedAtDesc(jobId, pageable);
        } else {
            // Otherwise return all runs belonging to the tenant
            runs = runRepository.findByTenantIdOrderByStartedAtDesc(tenantId, pageable);
        }

        return ResponseEntity.ok(runs.map(SchedulerJobRunResponse::fromEntity));
    }

    // -------------------------------------------------------------------------
    // Get single run with full output
    // -------------------------------------------------------------------------

    /**
     * Retrieves a single job run, including its full captured output.
     * <p>
     * HTTP GET {@code /api/scheduler/runs/{id}}
     * </p>
     *
     * @param id the UUID of the job run
     * @return ResponseEntity containing the job run response
     * @throws RuntimeException if no run exists with the given id
     */
    @GetMapping("/{id}")
    public ResponseEntity<SchedulerJobRunResponse> getRun(@PathVariable UUID id) {
        SchedulerJobRun run = runRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Run not found"));
        return ResponseEntity.ok(SchedulerJobRunResponse.fromEntity(run));
    }

    // -------------------------------------------------------------------------
    // Get runs for a specific job
    // -------------------------------------------------------------------------

    /**
     * Retrieves a paginated list of run records for a specific job.
     * <p>
     * HTTP GET {@code /api/scheduler/runs/job/{jobId}}
     * </p>
     *
     * @param jobId the UUID of the job whose runs are requested
     * @param page the zero-based page index (default 0)
     * @param size the page size (default 20)
     * @return ResponseEntity containing a page of job run responses
     */
    @GetMapping("/job/{jobId}")
    public ResponseEntity<Page<SchedulerJobRunResponse>> getRunsForJob(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<SchedulerJobRun> runs = runRepository.findByJobIdOrderByStartedAtDesc(jobId, pageable);
        return ResponseEntity.ok(runs.map(SchedulerJobRunResponse::fromEntity));
    }

    // -------------------------------------------------------------------------
    // Delete a run
    // -------------------------------------------------------------------------

    /**
     * Deletes a job run record.
     * <p>
     * HTTP DELETE {@code /api/scheduler/runs/{id}}. Restricted to the ADMIN role.
     * </p>
     *
     * @param id the UUID of the job run to delete
     * @return ResponseEntity containing a success message
     * @throws RuntimeException if no run exists with the given id
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteRun(@PathVariable UUID id) {
        SchedulerJobRun run = runRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Run not found"));
        runRepository.delete(run);
        return ResponseEntity.ok(new MessageResponse("Run deleted", true));
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /**
     * Resolves the currently authenticated user from the security context.
     *
     * @return the {@link UserPrincipal} for the current request
     */
    private UserPrincipal currentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
