package org.automatize.status.services.scheduler;

import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.*;
import org.automatize.status.models.scheduler.*;
import org.automatize.status.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Primary CRUD and lifecycle service for {@link SchedulerJob} entities.
 *
 * <p>All write operations keep the in-memory {@link SchedulerEngineService}
 * in sync by registering, unregistering, or rescheduling jobs as required.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Service
@Transactional
public class SchedulerJobService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerJobService.class);

    private static final String JOB_NOT_FOUND = "Job not found";

    @Autowired private SchedulerJobRepository jobRepository;
    @Autowired private SchedulerJobRunRepository runRepository;
    @Autowired private SchedulerJdbcDatasourceRepository datasourceRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private SchedulerEncryptionService encryptionService;
    @Autowired private CronValidationService cronValidationService;
    @Autowired private SchedulerEngineService engineService;

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Returns a page of jobs for the tenant with optional status and free-text filters.
     *
     * @param tenantId the tenant scope
     * @param status   optional status filter; when {@code null} ACTIVE/PAUSED/DISABLED are included
     * @param type     optional job type filter (currently applied in-memory when combined with status)
     * @param search   optional name search term
     * @param pageable pagination and sorting
     * @return page of matching jobs
     */
    @Transactional(readOnly = true)
    public Page<SchedulerJob> listJobs(UUID tenantId, JobStatus status, JobType type,
                                       String search, Pageable pageable) {
        // A free-text search term takes precedence over other filters
        if (search != null && !search.isBlank()) {
            return jobRepository.searchByTenant(tenantId, search, pageable);
        }
        // A specific status filter was provided
        if (status != null) {
            return jobRepository.findByTenantIdAndStatusIn(tenantId, List.of(status), pageable);
        }
        return jobRepository.findByTenantIdAndStatusIn(
                tenantId, List.of(JobStatus.ACTIVE, JobStatus.PAUSED, JobStatus.DISABLED), pageable);
    }

    /**
     * Returns a single job scoped to the tenant, throwing when not found.
     *
     * @param jobId    the job UUID
     * @param tenantId the tenant scope
     * @return the job
     */
    @Transactional(readOnly = true)
    public SchedulerJob getJob(UUID jobId, UUID tenantId) {
        return jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(JOB_NOT_FOUND));
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    /**
     * Creates a new job, validates the cron expression, encrypts sensitive config fields,
     * persists the entity, and registers it with the scheduler engine.
     *
     * @param jobData  the job to create (tenant will be set from {@code tenantId})
     * @param tenantId the tenant scope
     * @param username the creating user's login name (for audit)
     * @return the saved job
     */
    public SchedulerJob createJob(SchedulerJob jobData, UUID tenantId, String username) {
        // Reject creation when the cron expression is invalid
        if (!cronValidationService.isValid(jobData.getCronExpression())) {
            throw new IllegalArgumentException("Invalid cron expression: "
                    + cronValidationService.getValidationError(jobData.getCronExpression()));
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        jobData.setTenant(tenant);
        jobData.setCreatedBy(username);
        jobData.setLastModifiedBy(username);
        // Default new jobs to ACTIVE status when none supplied
        if (jobData.getStatus() == null) jobData.setStatus(JobStatus.ACTIVE);
        // Default new jobs to enabled when not specified
        if (jobData.getEnabled() == null) jobData.setEnabled(true);

        String tz = jobData.getTimeZone() != null ? jobData.getTimeZone() : "UTC";
        jobData.setNextRunAt(cronValidationService.calculateNextRun(jobData.getCronExpression(), tz));

        encryptConfigSensitiveFields(jobData);

        SchedulerJob saved = jobRepository.save(jobData);

        // Register with the cron engine only when enabled and ACTIVE
        if (Boolean.TRUE.equals(saved.getEnabled()) && saved.getStatus() == JobStatus.ACTIVE) {
            engineService.registerJob(saved);
        }
        logger.info("Created job '{}' ({}) for tenant {}", saved.getName(), saved.getId(), tenantId);
        return saved;
    }

    /**
     * Updates an existing job. Validates the cron expression when changed,
     * encrypts sensitive fields, persists the update, and reschedules the job.
     *
     * @param jobId    the job to update
     * @param jobData  updated values
     * @param tenantId the tenant scope (for security)
     * @param username the modifying user's login name (for audit)
     * @return the updated job
     */
    public SchedulerJob updateJob(UUID jobId, SchedulerJob jobData, UUID tenantId, String username) {
        SchedulerJob existing = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(JOB_NOT_FOUND));

        // Re-validate and recompute next run only when the cron expression changed
        if (!existing.getCronExpression().equals(jobData.getCronExpression())) {
            // Reject the update when the new cron expression is invalid
            if (!cronValidationService.isValid(jobData.getCronExpression())) {
                throw new IllegalArgumentException("Invalid cron expression: "
                        + cronValidationService.getValidationError(jobData.getCronExpression()));
            }
            String tz = jobData.getTimeZone() != null ? jobData.getTimeZone() : "UTC";
            existing.setNextRunAt(cronValidationService.calculateNextRun(jobData.getCronExpression(), tz));
        }

        existing.setName(jobData.getName());
        existing.setDescription(jobData.getDescription());
        existing.setCronExpression(jobData.getCronExpression());
        existing.setTimeZone(jobData.getTimeZone());
        existing.setEnabled(jobData.getEnabled());
        existing.setAllowConcurrent(jobData.getAllowConcurrent());
        existing.setMaxRetryAttempts(jobData.getMaxRetryAttempts());
        existing.setRetryDelaySeconds(jobData.getRetryDelaySeconds());
        existing.setTimeoutSeconds(jobData.getTimeoutSeconds());
        existing.setMaxOutputBytes(jobData.getMaxOutputBytes());
        existing.setTags(jobData.getTags());
        existing.setLastModifiedBy(username);

        updateJobConfig(existing, jobData);
        encryptConfigSensitiveFields(existing);

        SchedulerJob saved = jobRepository.save(existing);
        engineService.rescheduleJob(saved);
        return saved;
    }

    /**
     * Permanently deletes a job and its run history, and unregisters it from the engine.
     *
     * @param jobId    the job UUID to delete
     * @param tenantId the tenant scope (for security)
     */
    public void deleteJob(UUID jobId, UUID tenantId) {
        SchedulerJob job = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(JOB_NOT_FOUND));
        engineService.unregisterJob(jobId);
        jobRepository.delete(job);
    }

    /**
     * Pauses a job: sets status to PAUSED and unregisters it from the cron engine.
     *
     * @param jobId    the job UUID to pause
     * @param tenantId the tenant scope (for security)
     * @param username the operator's login name (for audit)
     * @return the updated job
     */
    public SchedulerJob pauseJob(UUID jobId, UUID tenantId, String username) {
        SchedulerJob job = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(JOB_NOT_FOUND));
        job.setStatus(JobStatus.PAUSED);
        job.setLastModifiedBy(username);
        engineService.unregisterJob(jobId);
        return jobRepository.save(job);
    }

    /**
     * Resumes a paused job: sets status to ACTIVE, recalculates the next run time,
     * and registers it with the cron engine.
     *
     * @param jobId    the job UUID to resume
     * @param tenantId the tenant scope (for security)
     * @param username the operator's login name (for audit)
     * @return the updated job
     */
    public SchedulerJob resumeJob(UUID jobId, UUID tenantId, String username) {
        SchedulerJob job = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(JOB_NOT_FOUND));
        job.setStatus(JobStatus.ACTIVE);
        job.setLastModifiedBy(username);
        String tz = job.getTimeZone() != null ? job.getTimeZone() : "UTC";
        job.setNextRunAt(cronValidationService.calculateNextRun(job.getCronExpression(), tz));
        SchedulerJob saved = jobRepository.save(job);
        engineService.registerJob(saved);
        return saved;
    }

    // -------------------------------------------------------------------------
    // Statistics helpers (for dashboard)
    // -------------------------------------------------------------------------

    /**
     * Counts jobs of a given type for the tenant.
     *
     * @param tenantId the tenant scope
     * @param type     the job type to count
     * @return the number of matching jobs
     */
    @Transactional(readOnly = true)
    public long countByType(UUID tenantId, JobType type) {
        return jobRepository.countByTenantIdAndJobType(tenantId, type);
    }

    /**
     * Counts jobs whose most recent run ended with the given status.
     *
     * @param tenantId the tenant scope
     * @param status   the last-run status to count
     * @return the number of matching jobs
     */
    @Transactional(readOnly = true)
    public long countByLastRunStatus(UUID tenantId, JobRunStatus status) {
        return jobRepository.countByTenantIdAndLastRunStatus(tenantId, status);
    }

    /**
     * Counts runs with the given status that started after the supplied timestamp.
     *
     * @param tenantId the tenant scope
     * @param status   the run status to count
     * @param since    the lower bound (exclusive) on the run start time
     * @return the number of matching runs
     */
    @Transactional(readOnly = true)
    public long countRunsSince(UUID tenantId, JobRunStatus status, ZonedDateTime since) {
        return runRepository.countByTenantIdAndStatusAndStartedAtAfter(tenantId, status, since);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Encrypts plaintext sensitive fields in job configs. Fields that are already
     * valid ciphertext (i.e. can be decrypted without error) are left as-is to prevent
     * double-encryption.
     */
    private void encryptConfigSensitiveFields(SchedulerJob job) {
        // Encrypt REST auth secrets when a REST config is present
        if (job.getRestConfig() != null) {
            SchedulerRestConfig rc = job.getRestConfig();
            rc.setAuthPasswordEnc(encryptIfPlaintext(rc.getAuthPasswordEnc()));
            rc.setAuthTokenEnc(encryptIfPlaintext(rc.getAuthTokenEnc()));
            rc.setAuthApiKeyValueEnc(encryptIfPlaintext(rc.getAuthApiKeyValueEnc()));
        }
        // Encrypt inline SQL password when a SQL config is present
        if (job.getSqlConfig() != null) {
            SchedulerSqlConfig sc = job.getSqlConfig();
            sc.setInlinePasswordEnc(encryptIfPlaintext(sc.getInlinePasswordEnc()));
        }
        // Encrypt SOAP auth secrets when a SOAP config is present
        if (job.getSoapConfig() != null) {
            SchedulerSoapConfig sc = job.getSoapConfig();
            sc.setAuthPasswordEnc(encryptIfPlaintext(sc.getAuthPasswordEnc()));
            sc.setAuthTokenEnc(encryptIfPlaintext(sc.getAuthTokenEnc()));
        }
    }

    /**
     * Encrypts a value only when it is non-blank and appears to be plaintext
     * (i.e. decryption fails). Returns the original value unchanged when it is
     * already encrypted, and returns {@code null} for blank/null input.
     */
    private String encryptIfPlaintext(String value) {
        // Blank or null values are returned unchanged
        if (value == null || value.isBlank()) return value;
        try {
            encryptionService.decrypt(value);
            // Decryption succeeded — value is already encrypted
            return value;
        } catch (Exception e) {
            // Decryption failed — value is plaintext, so encrypt it
            logger.trace("Value is not valid ciphertext; treating as plaintext and encrypting", e);
            return encryptionService.encrypt(value);
        }
    }

    /**
     * Copies the mutable config fields from {@code newData} onto {@code existing} based on job type.
     * Relies on JPA cascading to persist child config entities.
     */
    private void updateJobConfig(SchedulerJob existing, SchedulerJob newData) {
        // Copy the config subtype matching the job's type
        switch (existing.getJobType()) {
            case REST -> {
                // Only copy when both existing and incoming REST configs are present
                if (newData.getRestConfig() != null && existing.getRestConfig() != null) {
                    copyRestConfig(existing.getRestConfig(), newData.getRestConfig());
                }
            }
            case SQL -> {
                // Only copy when both existing and incoming SQL configs are present
                if (newData.getSqlConfig() != null && existing.getSqlConfig() != null) {
                    copySqlConfig(existing.getSqlConfig(), newData.getSqlConfig());
                }
            }
            case PROGRAM -> {
                // Only copy when both existing and incoming PROGRAM configs are present
                if (newData.getProgramConfig() != null && existing.getProgramConfig() != null) {
                    copyProgramConfig(existing.getProgramConfig(), newData.getProgramConfig());
                }
            }
            case SOAP -> {
                // Only copy when both existing and incoming SOAP configs are present
                if (newData.getSoapConfig() != null && existing.getSoapConfig() != null) {
                    copySoapConfig(existing.getSoapConfig(), newData.getSoapConfig());
                }
            }
        }
    }

    private void copyRestConfig(SchedulerRestConfig target, SchedulerRestConfig source) {
        target.setHttpMethod(source.getHttpMethod());
        target.setUrl(source.getUrl());
        target.setRequestBody(source.getRequestBody());
        target.setContentType(source.getContentType());
        target.setHeaders(source.getHeaders());
        target.setQueryParams(source.getQueryParams());
        target.setAuthType(source.getAuthType());
        target.setAuthUsername(source.getAuthUsername());
        if (source.getAuthPasswordEnc() != null && !source.getAuthPasswordEnc().isBlank())
            target.setAuthPasswordEnc(source.getAuthPasswordEnc());
        if (source.getAuthTokenEnc() != null && !source.getAuthTokenEnc().isBlank())
            target.setAuthTokenEnc(source.getAuthTokenEnc());
        target.setAuthApiKeyName(source.getAuthApiKeyName());
        if (source.getAuthApiKeyValueEnc() != null && !source.getAuthApiKeyValueEnc().isBlank())
            target.setAuthApiKeyValueEnc(source.getAuthApiKeyValueEnc());
        target.setAuthApiKeyLocation(source.getAuthApiKeyLocation());
        target.setSslVerify(source.getSslVerify());
        target.setConnectTimeoutMs(source.getConnectTimeoutMs());
        target.setReadTimeoutMs(source.getReadTimeoutMs());
        target.setFollowRedirects(source.getFollowRedirects());
        target.setMaxResponseBytes(source.getMaxResponseBytes());
        target.setAssertStatusCode(source.getAssertStatusCode());
        target.setAssertBodyContains(source.getAssertBodyContains());
    }

    private void copySqlConfig(SchedulerSqlConfig target, SchedulerSqlConfig source) {
        target.setDatasource(source.getDatasource());
        target.setInlineDbType(source.getInlineDbType());
        target.setInlineJdbcUrl(source.getInlineJdbcUrl());
        target.setInlineUsername(source.getInlineUsername());
        if (source.getInlinePasswordEnc() != null && !source.getInlinePasswordEnc().isBlank())
            target.setInlinePasswordEnc(source.getInlinePasswordEnc());
        target.setSqlStatement(source.getSqlStatement());
        target.setSqlType(source.getSqlType());
        target.setCaptureResultSet(source.getCaptureResultSet());
        target.setMaxResultRows(source.getMaxResultRows());
        target.setQueryTimeoutSeconds(source.getQueryTimeoutSeconds());
    }

    private void copyProgramConfig(SchedulerProgramConfig target, SchedulerProgramConfig source) {
        target.setCommand(source.getCommand());
        target.setArguments(source.getArguments());
        target.setWorkingDirectory(source.getWorkingDirectory());
        target.setEnvironmentVars(source.getEnvironmentVars());
        target.setShellWrap(source.getShellWrap());
        target.setShellPath(source.getShellPath());
        target.setRunAsUser(source.getRunAsUser());
    }

    private void copySoapConfig(SchedulerSoapConfig target, SchedulerSoapConfig source) {
        target.setWsdlUrl(source.getWsdlUrl());
        target.setEndpointUrl(source.getEndpointUrl());
        target.setServiceName(source.getServiceName());
        target.setPortName(source.getPortName());
        target.setOperationName(source.getOperationName());
        target.setSoapAction(source.getSoapAction());
        target.setSoapVersion(source.getSoapVersion());
        target.setSoapEnvelope(source.getSoapEnvelope());
        target.setExtraHeaders(source.getExtraHeaders());
        target.setAuthType(source.getAuthType());
        target.setAuthUsername(source.getAuthUsername());
        if (source.getAuthPasswordEnc() != null && !source.getAuthPasswordEnc().isBlank())
            target.setAuthPasswordEnc(source.getAuthPasswordEnc());
        if (source.getAuthTokenEnc() != null && !source.getAuthTokenEnc().isBlank())
            target.setAuthTokenEnc(source.getAuthTokenEnc());
        target.setSslVerify(source.getSslVerify());
        target.setConnectTimeoutMs(source.getConnectTimeoutMs());
        target.setReadTimeoutMs(source.getReadTimeoutMs());
        target.setMaxResponseBytes(source.getMaxResponseBytes());
    }
}
