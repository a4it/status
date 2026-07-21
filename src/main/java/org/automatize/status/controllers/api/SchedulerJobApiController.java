package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.SchedulerJobRequest;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.api.response.SchedulerJobResponse;
import org.automatize.status.api.response.SchedulerJobRunResponse;
import org.automatize.status.models.*;
import org.automatize.status.models.scheduler.*;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.SchedulerJdbcDatasourceRepository;
import org.automatize.status.security.UserPrincipal;
import org.automatize.status.services.scheduler.CronValidationService;
import org.automatize.status.services.scheduler.JobDispatcherService;
import org.automatize.status.services.scheduler.SchedulerJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for scheduler job CRUD and lifecycle operations.
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@RestController
@RequestMapping("/api/scheduler/jobs")
@PreAuthorize("isAuthenticated()")
public class SchedulerJobApiController {

    @Autowired
    private SchedulerJobService schedulerJobService;

    @Autowired
    private JobDispatcherService jobDispatcherService;

    @Autowired
    private CronValidationService cronValidationService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SchedulerJdbcDatasourceRepository datasourceRepository;

    // -------------------------------------------------------------------------
    // List jobs with optional filtering
    // -------------------------------------------------------------------------

    /**
     * Lists scheduler jobs for the current tenant with optional filtering and pagination.
     * <p>
     * HTTP GET {@code /api/scheduler/jobs}
     * </p>
     *
     * @param page the zero-based page index (default 0)
     * @param size the page size (default 20)
     * @param sort the property to sort by (default "name")
     * @param status optional filter by job status
     * @param type optional filter by job type
     * @param search optional free-text search term
     * @return ResponseEntity containing a page of job responses
     */
    @GetMapping
    public ResponseEntity<Page<SchedulerJobResponse>> listJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) JobType type,
            @RequestParam(required = false) String search) {

        UserPrincipal principal = currentPrincipal();
        UUID tenantId = principal.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<SchedulerJob> jobs = schedulerJobService.listJobs(tenantId, status, type, search, pageable);
        return ResponseEntity.ok(jobs.map(j -> SchedulerJobResponse.fromEntity(j, cronValidationService)));
    }

    // -------------------------------------------------------------------------
    // Get single job
    // -------------------------------------------------------------------------

    /**
     * Retrieves a single scheduler job by its identifier, scoped to the current tenant.
     * <p>
     * HTTP GET {@code /api/scheduler/jobs/{id}}
     * </p>
     *
     * @param id the UUID of the job
     * @return ResponseEntity containing the job response
     */
    @GetMapping("/{id}")
    public ResponseEntity<SchedulerJobResponse> getJob(@PathVariable UUID id) {
        UserPrincipal principal = currentPrincipal();
        SchedulerJob job = schedulerJobService.getJob(id, principal.getTenantId());
        return ResponseEntity.ok(SchedulerJobResponse.fromEntity(job, cronValidationService));
    }

    // -------------------------------------------------------------------------
    // Create job
    // -------------------------------------------------------------------------

    /**
     * Creates a new scheduler job for the current tenant.
     * <p>
     * HTTP POST {@code /api/scheduler/jobs}. Restricted to ADMIN or MANAGER roles.
     * </p>
     *
     * @param request the validated job creation request
     * @return ResponseEntity containing the created job with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SchedulerJobResponse> createJob(@Valid @RequestBody SchedulerJobRequest request) {
        UserPrincipal principal = currentPrincipal();
        SchedulerJob job = new SchedulerJob();
        buildJobFromRequest(request, job);
        SchedulerJob saved = schedulerJobService.createJob(job, principal.getTenantId(), principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SchedulerJobResponse.fromEntity(saved, cronValidationService));
    }

    // -------------------------------------------------------------------------
    // Update job
    // -------------------------------------------------------------------------

    /**
     * Updates an existing scheduler job.
     * <p>
     * HTTP PUT {@code /api/scheduler/jobs/{id}}. Restricted to ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the job to update
     * @param request the validated job update request
     * @return ResponseEntity containing the updated job response
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SchedulerJobResponse> updateJob(@PathVariable UUID id,
                                                          @Valid @RequestBody SchedulerJobRequest request) {
        UserPrincipal principal = currentPrincipal();
        SchedulerJob jobData = new SchedulerJob();
        buildJobFromRequest(request, jobData);
        SchedulerJob updated = schedulerJobService.updateJob(id, jobData, principal.getTenantId(), principal.getUsername());
        return ResponseEntity.ok(SchedulerJobResponse.fromEntity(updated, cronValidationService));
    }

    // -------------------------------------------------------------------------
    // Delete job
    // -------------------------------------------------------------------------

    /**
     * Deletes a scheduler job, scoped to the current tenant.
     * <p>
     * HTTP DELETE {@code /api/scheduler/jobs/{id}}. Restricted to ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the job to delete
     * @return ResponseEntity with HTTP 204 No Content status
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id) {
        UserPrincipal principal = currentPrincipal();
        schedulerJobService.deleteJob(id, principal.getTenantId());
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Pause job
    // -------------------------------------------------------------------------

    /**
     * Pauses an active scheduler job so it will not be triggered on schedule.
     * <p>
     * HTTP POST {@code /api/scheduler/jobs/{id}/pause}. Restricted to ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the job to pause
     * @return ResponseEntity containing the updated job response
     */
    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SchedulerJobResponse> pauseJob(@PathVariable UUID id) {
        UserPrincipal principal = currentPrincipal();
        SchedulerJob job = schedulerJobService.pauseJob(id, principal.getTenantId(), principal.getUsername());
        return ResponseEntity.ok(SchedulerJobResponse.fromEntity(job, cronValidationService));
    }

    // -------------------------------------------------------------------------
    // Resume job
    // -------------------------------------------------------------------------

    /**
     * Resumes a paused scheduler job so it will again be triggered on schedule.
     * <p>
     * HTTP POST {@code /api/scheduler/jobs/{id}/resume}. Restricted to ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the job to resume
     * @return ResponseEntity containing the updated job response
     */
    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SchedulerJobResponse> resumeJob(@PathVariable UUID id) {
        UserPrincipal principal = currentPrincipal();
        SchedulerJob job = schedulerJobService.resumeJob(id, principal.getTenantId(), principal.getUsername());
        return ResponseEntity.ok(SchedulerJobResponse.fromEntity(job, cronValidationService));
    }

    // -------------------------------------------------------------------------
    // Manually trigger job
    // -------------------------------------------------------------------------

    /**
     * Manually triggers immediate execution of a scheduler job.
     * <p>
     * HTTP POST {@code /api/scheduler/jobs/{id}/trigger}. Restricted to ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the job to trigger
     * @return ResponseEntity containing the started job run, or an empty body if no run was started
     */
    @PostMapping("/{id}/trigger")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SchedulerJobRunResponse> triggerJob(@PathVariable UUID id) {
        UserPrincipal principal = currentPrincipal();
        var run = jobDispatcherService.triggerManually(id, principal.getTenantId(), principal.getUsername());
        // No run was created (e.g. concurrent execution disallowed); return an empty OK response
        if (run == null) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(SchedulerJobRunResponse.fromEntity(run));
    }

    // -------------------------------------------------------------------------
    // Next runs preview
    // -------------------------------------------------------------------------

    /**
     * Previews the next scheduled execution times for a job based on its cron expression.
     * <p>
     * HTTP GET {@code /api/scheduler/jobs/{id}/next-runs}
     * </p>
     *
     * @param id the UUID of the job
     * @param count the number of upcoming executions to return (default 5)
     * @return ResponseEntity containing a list of formatted next-execution timestamps
     */
    @GetMapping("/{id}/next-runs")
    public ResponseEntity<List<String>> nextRuns(@PathVariable UUID id,
                                                 @RequestParam(defaultValue = "5") int count) {
        UserPrincipal principal = currentPrincipal();
        SchedulerJob job = schedulerJobService.getJob(id, principal.getTenantId());
        List<ZonedDateTime> nexts = cronValidationService.getNextExecutions(
                job.getCronExpression(),
                job.getTimeZone() != null ? job.getTimeZone() : "UTC",
                count);
        List<String> formatted = nexts.stream().map(ZonedDateTime::toString).toList();
        return ResponseEntity.ok(formatted);
    }

    // -------------------------------------------------------------------------
    // Dashboard stats
    // -------------------------------------------------------------------------

    /**
     * Returns aggregate dashboard statistics for the current tenant's jobs.
     * <p>
     * HTTP GET {@code /api/scheduler/jobs/stats}. Includes total job count, jobs
     * currently running, and counts of runs succeeded and failed today.
     * </p>
     *
     * @return ResponseEntity containing a map of statistic keys to their values
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        UserPrincipal principal = currentPrincipal();
        UUID tenantId = principal.getTenantId();

        long totalJobs = schedulerJobService.listJobs(
                tenantId, null, null, null,
                PageRequest.of(0, 1)).getTotalElements();

        long runningNow = schedulerJobService.countByLastRunStatus(tenantId, JobRunStatus.RUNNING);

        ZonedDateTime startOfToday = ZonedDateTime.now().toLocalDate().atStartOfDay(
                java.time.ZoneId.systemDefault());
        long succeededToday = schedulerJobService.countRunsSince(tenantId, JobRunStatus.SUCCESS, startOfToday);
        long failedToday = schedulerJobService.countRunsSince(tenantId, JobRunStatus.FAILURE, startOfToday);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalJobs", totalJobs);
        result.put("runningNow", runningNow);
        result.put("succeededToday", succeededToday);
        result.put("failedToday", failedToday);
        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------------------------
    // Helper: build SchedulerJob from request
    // -------------------------------------------------------------------------

    /**
     * Populates a {@link SchedulerJob} entity from a request, applying common fields,
     * the owning organization, and the type-specific configuration.
     *
     * @param req the incoming job request
     * @param job the job entity to populate
     */
    private void buildJobFromRequest(SchedulerJobRequest req, SchedulerJob job) {
        applyCommonFields(req, job);
        applyOrganization(req, job);

        // Dispatch to the configuration builder matching the job's type
        switch (job.getJobType()) {
            case PROGRAM -> applyProgramConfig(req, job);
            case SQL -> applySqlConfig(req, job);
            case REST -> applyRestConfig(req, job);
            case SOAP -> applySoapConfig(req, job);
        }
    }

    // -------------------------------------------------------------------------
    // Helper: base fields shared by every job type
    // -------------------------------------------------------------------------

    /**
     * Applies the base fields shared by every job type, defaulting optional values.
     *
     * @param req the incoming job request
     * @param job the job entity to populate
     */
    private void applyCommonFields(SchedulerJobRequest req, SchedulerJob job) {
        job.setName(req.getName());
        job.setDescription(req.getDescription());
        job.setCronExpression(req.getCronExpression());
        job.setTimeZone(req.getTimeZone() != null ? req.getTimeZone() : "UTC");
        job.setEnabled(req.getEnabled() != null ? req.getEnabled() : true);
        job.setAllowConcurrent(req.getAllowConcurrent() != null ? req.getAllowConcurrent() : false);
        job.setMaxRetryAttempts(req.getMaxRetryAttempts() != null ? req.getMaxRetryAttempts() : 0);
        job.setRetryDelaySeconds(req.getRetryDelaySeconds() != null ? req.getRetryDelaySeconds() : 60);
        job.setTimeoutSeconds(req.getTimeoutSeconds() != null ? req.getTimeoutSeconds() : 300);
        job.setMaxOutputBytes(req.getMaxOutputBytes() != null ? req.getMaxOutputBytes() : 102400);
        job.setTags(req.getTags());

        // Only set the job type when a value was supplied
        if (req.getJobType() != null) {
            job.setJobType(JobType.valueOf(req.getJobType().toUpperCase()));
        }
    }

    // -------------------------------------------------------------------------
    // Helper: resolve and attach the owning organization
    // -------------------------------------------------------------------------

    /**
     * Resolves and attaches the owning organization to the job when provided.
     *
     * @param req the incoming job request
     * @param job the job entity to populate
     */
    private void applyOrganization(SchedulerJobRequest req, SchedulerJob job) {
        // Attach the owning organization when an organization id was provided
        if (req.getOrganizationId() != null) {
            organizationRepository.findById(req.getOrganizationId())
                    .ifPresent(job::setOrganization);
        }
    }

    // -------------------------------------------------------------------------
    // Helper: PROGRAM job configuration
    // -------------------------------------------------------------------------

    /**
     * Builds and attaches the PROGRAM-type execution configuration to the job.
     *
     * @param req the incoming job request
     * @param job the job entity to populate
     */
    private void applyProgramConfig(SchedulerJobRequest req, SchedulerJob job) {
        // Nothing to do when no program configuration was supplied
        if (req.getProgramConfig() == null) {
            return;
        }
        SchedulerProgramConfig cfg = new SchedulerProgramConfig();
        cfg.setJob(job);
        cfg.setCommand(req.getProgramConfig().getCommand());
        cfg.setArguments(req.getProgramConfig().getArguments());
        cfg.setWorkingDirectory(req.getProgramConfig().getWorkingDirectory());
        cfg.setEnvironmentVars(req.getProgramConfig().getEnvironmentVars());
        cfg.setShellWrap(req.getProgramConfig().getShellWrap() != null
                ? req.getProgramConfig().getShellWrap() : false);
        cfg.setShellPath(req.getProgramConfig().getShellPath() != null
                ? req.getProgramConfig().getShellPath() : "/bin/bash");
        cfg.setRunAsUser(req.getProgramConfig().getRunAsUser());
        job.setProgramConfig(cfg);
    }

    // -------------------------------------------------------------------------
    // Helper: SQL job configuration
    // -------------------------------------------------------------------------

    /**
     * Builds and attaches the SQL-type execution configuration to the job.
     *
     * @param req the incoming job request
     * @param job the job entity to populate
     */
    private void applySqlConfig(SchedulerJobRequest req, SchedulerJob job) {
        // Nothing to do when no SQL configuration was supplied
        if (req.getSqlConfig() == null) {
            return;
        }
        SchedulerSqlConfig cfg = new SchedulerSqlConfig();
        cfg.setJob(job);
        SchedulerJobRequest.SqlConfigRequest sc = req.getSqlConfig();
        // Link a managed datasource when a datasource id was provided
        if (sc.getDatasourceId() != null) {
            datasourceRepository.findById(sc.getDatasourceId())
                    .ifPresent(cfg::setDatasource);
        }
        // Set the inline database type when provided (for ad-hoc connections)
        if (sc.getInlineDbType() != null) {
            cfg.setInlineDbType(DbType.valueOf(sc.getInlineDbType().toUpperCase()));
        }
        cfg.setInlineJdbcUrl(sc.getInlineJdbcUrl());
        cfg.setInlineUsername(sc.getInlineUsername());
        // Stored as plaintext; service layer will encrypt
        cfg.setInlinePasswordEnc(sc.getInlinePassword());
        cfg.setSqlStatement(sc.getSqlStatement());
        // Set the SQL statement type when provided
        if (sc.getSqlType() != null) {
            cfg.setSqlType(SqlType.valueOf(sc.getSqlType().toUpperCase()));
        }
        cfg.setCaptureResultSet(sc.getCaptureResultSet() != null ? sc.getCaptureResultSet() : false);
        cfg.setMaxResultRows(sc.getMaxResultRows() != null ? sc.getMaxResultRows() : 100);
        cfg.setQueryTimeoutSeconds(sc.getQueryTimeoutSeconds() != null ? sc.getQueryTimeoutSeconds() : 60);
        job.setSqlConfig(cfg);
    }

    // -------------------------------------------------------------------------
    // Helper: REST job configuration
    // -------------------------------------------------------------------------

    /**
     * Builds and attaches the REST-type execution configuration to the job.
     *
     * @param req the incoming job request
     * @param job the job entity to populate
     */
    private void applyRestConfig(SchedulerJobRequest req, SchedulerJob job) {
        // Nothing to do when no REST configuration was supplied
        if (req.getRestConfig() == null) {
            return;
        }
        SchedulerRestConfig cfg = new SchedulerRestConfig();
        cfg.setJob(job);
        SchedulerJobRequest.RestConfigRequest rc = req.getRestConfig();
        // Set the HTTP method when provided
        if (rc.getHttpMethod() != null) {
            cfg.setHttpMethod(HttpMethod.valueOf(rc.getHttpMethod().toUpperCase()));
        }
        cfg.setUrl(rc.getUrl());
        cfg.setRequestBody(rc.getRequestBody());
        cfg.setContentType(rc.getContentType() != null ? rc.getContentType() : "application/json");
        cfg.setHeaders(rc.getHeaders());
        cfg.setQueryParams(rc.getQueryParams());
        // Set the authentication type when provided
        if (rc.getAuthType() != null) {
            cfg.setAuthType(AuthType.valueOf(rc.getAuthType().toUpperCase()));
        }
        cfg.setAuthUsername(rc.getAuthUsername());
        // Plaintext; service will encrypt
        cfg.setAuthPasswordEnc(rc.getAuthPassword());
        cfg.setAuthTokenEnc(rc.getAuthToken());
        cfg.setAuthApiKeyName(rc.getAuthApiKeyName());
        cfg.setAuthApiKeyValueEnc(rc.getAuthApiKeyValue());
        // Set where the API key is placed (header or query) when provided
        if (rc.getAuthApiKeyLocation() != null) {
            cfg.setAuthApiKeyLocation(ApiKeyLocation.valueOf(rc.getAuthApiKeyLocation().toUpperCase()));
        }
        cfg.setAuthOauth2TokenUrl(rc.getAuthOauth2TokenUrl());
        cfg.setAuthOauth2ClientId(rc.getAuthOauth2ClientId());
        cfg.setAuthOauth2ClientSecretEnc(rc.getAuthOauth2ClientSecret());
        cfg.setAuthOauth2Scope(rc.getAuthOauth2Scope());
        cfg.setSslVerify(rc.getSslVerify() != null ? rc.getSslVerify() : true);
        cfg.setConnectTimeoutMs(rc.getConnectTimeoutMs() != null ? rc.getConnectTimeoutMs() : 5000);
        cfg.setReadTimeoutMs(rc.getReadTimeoutMs() != null ? rc.getReadTimeoutMs() : 30000);
        cfg.setFollowRedirects(rc.getFollowRedirects() != null ? rc.getFollowRedirects() : true);
        cfg.setMaxResponseBytes(rc.getMaxResponseBytes() != null ? rc.getMaxResponseBytes() : 102400);
        cfg.setAssertStatusCode(rc.getAssertStatusCode());
        cfg.setAssertBodyContains(rc.getAssertBodyContains());
        cfg.setAssertJsonPath(rc.getAssertJsonPath());
        cfg.setAssertJsonValue(rc.getAssertJsonValue());
        job.setRestConfig(cfg);
    }

    // -------------------------------------------------------------------------
    // Helper: SOAP job configuration
    // -------------------------------------------------------------------------

    /**
     * Builds and attaches the SOAP-type execution configuration to the job.
     *
     * @param req the incoming job request
     * @param job the job entity to populate
     */
    private void applySoapConfig(SchedulerJobRequest req, SchedulerJob job) {
        // Nothing to do when no SOAP configuration was supplied
        if (req.getSoapConfig() == null) {
            return;
        }
        SchedulerSoapConfig cfg = new SchedulerSoapConfig();
        cfg.setJob(job);
        SchedulerJobRequest.SoapConfigRequest sc = req.getSoapConfig();
        cfg.setWsdlUrl(sc.getWsdlUrl());
        cfg.setEndpointUrl(sc.getEndpointUrl());
        cfg.setServiceName(sc.getServiceName());
        cfg.setPortName(sc.getPortName());
        cfg.setOperationName(sc.getOperationName());
        cfg.setSoapAction(sc.getSoapAction());
        // Set the SOAP protocol version when provided
        if (sc.getSoapVersion() != null) {
            cfg.setSoapVersion(SoapVersion.valueOf(sc.getSoapVersion().toUpperCase()));
        }
        cfg.setSoapEnvelope(sc.getSoapEnvelope());
        cfg.setExtraHeaders(sc.getExtraHeaders());
        // Set the authentication type when provided
        if (sc.getAuthType() != null) {
            cfg.setAuthType(AuthType.valueOf(sc.getAuthType().toUpperCase()));
        }
        cfg.setAuthUsername(sc.getAuthUsername());
        // Plaintext; service will encrypt
        cfg.setAuthPasswordEnc(sc.getAuthPassword());
        cfg.setAuthTokenEnc(sc.getAuthToken());
        cfg.setSslVerify(sc.getSslVerify() != null ? sc.getSslVerify() : true);
        cfg.setConnectTimeoutMs(sc.getConnectTimeoutMs() != null ? sc.getConnectTimeoutMs() : 5000);
        cfg.setReadTimeoutMs(sc.getReadTimeoutMs() != null ? sc.getReadTimeoutMs() : 60000);
        cfg.setMaxResponseBytes(sc.getMaxResponseBytes() != null ? sc.getMaxResponseBytes() : 524288);
        job.setSoapConfig(cfg);
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
