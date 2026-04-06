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

    @GetMapping("/{id}")
    public ResponseEntity<SchedulerJobResponse> getJob(@PathVariable UUID id) {
        UserPrincipal principal = currentPrincipal();
        SchedulerJob job = schedulerJobService.getJob(id, principal.getTenantId());
        return ResponseEntity.ok(SchedulerJobResponse.fromEntity(job, cronValidationService));
    }

    // -------------------------------------------------------------------------
    // Create job
    // -------------------------------------------------------------------------

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

    @PostMapping("/{id}/trigger")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SchedulerJobRunResponse> triggerJob(@PathVariable UUID id) {
        UserPrincipal principal = currentPrincipal();
        var run = jobDispatcherService.triggerManually(id, principal.getTenantId(), principal.getUsername());
        if (run == null) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(SchedulerJobRunResponse.fromEntity(run));
    }

    // -------------------------------------------------------------------------
    // Next runs preview
    // -------------------------------------------------------------------------

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

    private void buildJobFromRequest(SchedulerJobRequest req, SchedulerJob job) {
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

        if (req.getJobType() != null) {
            job.setJobType(JobType.valueOf(req.getJobType().toUpperCase()));
        }

        if (req.getOrganizationId() != null) {
            organizationRepository.findById(req.getOrganizationId())
                    .ifPresent(job::setOrganization);
        }

        switch (job.getJobType()) {
            case PROGRAM -> {
                if (req.getProgramConfig() != null) {
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
            }
            case SQL -> {
                if (req.getSqlConfig() != null) {
                    SchedulerSqlConfig cfg = new SchedulerSqlConfig();
                    cfg.setJob(job);
                    SchedulerJobRequest.SqlConfigRequest sc = req.getSqlConfig();
                    if (sc.getDatasourceId() != null) {
                        datasourceRepository.findById(sc.getDatasourceId())
                                .ifPresent(cfg::setDatasource);
                    }
                    if (sc.getInlineDbType() != null) {
                        cfg.setInlineDbType(DbType.valueOf(sc.getInlineDbType().toUpperCase()));
                    }
                    cfg.setInlineJdbcUrl(sc.getInlineJdbcUrl());
                    cfg.setInlineUsername(sc.getInlineUsername());
                    // Stored as plaintext; service layer will encrypt
                    cfg.setInlinePasswordEnc(sc.getInlinePassword());
                    cfg.setSqlStatement(sc.getSqlStatement());
                    if (sc.getSqlType() != null) {
                        cfg.setSqlType(SqlType.valueOf(sc.getSqlType().toUpperCase()));
                    }
                    cfg.setCaptureResultSet(sc.getCaptureResultSet() != null ? sc.getCaptureResultSet() : false);
                    cfg.setMaxResultRows(sc.getMaxResultRows() != null ? sc.getMaxResultRows() : 100);
                    cfg.setQueryTimeoutSeconds(sc.getQueryTimeoutSeconds() != null ? sc.getQueryTimeoutSeconds() : 60);
                    job.setSqlConfig(cfg);
                }
            }
            case REST -> {
                if (req.getRestConfig() != null) {
                    SchedulerRestConfig cfg = new SchedulerRestConfig();
                    cfg.setJob(job);
                    SchedulerJobRequest.RestConfigRequest rc = req.getRestConfig();
                    if (rc.getHttpMethod() != null) {
                        cfg.setHttpMethod(HttpMethod.valueOf(rc.getHttpMethod().toUpperCase()));
                    }
                    cfg.setUrl(rc.getUrl());
                    cfg.setRequestBody(rc.getRequestBody());
                    cfg.setContentType(rc.getContentType() != null ? rc.getContentType() : "application/json");
                    cfg.setHeaders(rc.getHeaders());
                    cfg.setQueryParams(rc.getQueryParams());
                    if (rc.getAuthType() != null) {
                        cfg.setAuthType(AuthType.valueOf(rc.getAuthType().toUpperCase()));
                    }
                    cfg.setAuthUsername(rc.getAuthUsername());
                    // Plaintext; service will encrypt
                    cfg.setAuthPasswordEnc(rc.getAuthPassword());
                    cfg.setAuthTokenEnc(rc.getAuthToken());
                    cfg.setAuthApiKeyName(rc.getAuthApiKeyName());
                    cfg.setAuthApiKeyValueEnc(rc.getAuthApiKeyValue());
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
            }
            case SOAP -> {
                if (req.getSoapConfig() != null) {
                    SchedulerSoapConfig cfg = new SchedulerSoapConfig();
                    cfg.setJob(job);
                    SchedulerJobRequest.SoapConfigRequest sc = req.getSoapConfig();
                    cfg.setWsdlUrl(sc.getWsdlUrl());
                    cfg.setEndpointUrl(sc.getEndpointUrl());
                    cfg.setServiceName(sc.getServiceName());
                    cfg.setPortName(sc.getPortName());
                    cfg.setOperationName(sc.getOperationName());
                    cfg.setSoapAction(sc.getSoapAction());
                    if (sc.getSoapVersion() != null) {
                        cfg.setSoapVersion(SoapVersion.valueOf(sc.getSoapVersion().toUpperCase()));
                    }
                    cfg.setSoapEnvelope(sc.getSoapEnvelope());
                    cfg.setExtraHeaders(sc.getExtraHeaders());
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
            }
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private UserPrincipal currentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
