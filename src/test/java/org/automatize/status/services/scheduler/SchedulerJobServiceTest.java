package org.automatize.status.services.scheduler;

import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.SchedulerJob;
import org.automatize.status.models.SchedulerRestConfig;
import org.automatize.status.models.Tenant;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.automatize.status.models.scheduler.JobStatus;
import org.automatize.status.models.scheduler.JobType;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.SchedulerJdbcDatasourceRepository;
import org.automatize.status.repositories.SchedulerJobRepository;
import org.automatize.status.repositories.SchedulerJobRunRepository;
import org.automatize.status.repositories.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerJobServiceTest {

    @Mock private SchedulerJobRepository jobRepository;
    @Mock private SchedulerJobRunRepository runRepository;
    @Mock private SchedulerJdbcDatasourceRepository datasourceRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private SchedulerEncryptionService encryptionService;
    @Mock private CronValidationService cronValidationService;
    @Mock private SchedulerEngineService engineService;

    @InjectMocks private SchedulerJobService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();

    private SchedulerJob newJob() {
        SchedulerJob job = new SchedulerJob();
        job.setId(jobId);
        job.setName("nightly");
        job.setJobType(JobType.REST);
        job.setCronExpression("0 0 3 * * *");
        job.setTimeZone("UTC");
        job.setEnabled(true);
        job.setStatus(JobStatus.ACTIVE);
        return job;
    }

    // ---------------------------------------------------------------------
    // listJobs
    // ---------------------------------------------------------------------

    @Test
    void listJobs_withSearchTerm_usesSearchByTenant() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SchedulerJob> page = new PageImpl<>(List.of(newJob()));
        when(jobRepository.searchByTenant(tenantId, "abc", pageable)).thenReturn(page);

        Page<SchedulerJob> result = service.listJobs(tenantId, null, null, "abc", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(jobRepository).searchByTenant(tenantId, "abc", pageable);
    }

    @Test
    void listJobs_withStatus_filtersBySingleStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SchedulerJob> page = new PageImpl<>(List.of(newJob()));
        when(jobRepository.findByTenantIdAndStatusIn(eq(tenantId), eq(List.of(JobStatus.PAUSED)), eq(pageable)))
                .thenReturn(page);

        Page<SchedulerJob> result = service.listJobs(tenantId, JobStatus.PAUSED, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(jobRepository).findByTenantIdAndStatusIn(tenantId, List.of(JobStatus.PAUSED), pageable);
    }

    @Test
    void listJobs_noFilters_defaultsToActivePausedDisabled() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SchedulerJob> page = new PageImpl<>(List.of());
        when(jobRepository.findByTenantIdAndStatusIn(eq(tenantId), anyList(), eq(pageable))).thenReturn(page);

        service.listJobs(tenantId, null, null, "  ", pageable);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JobStatus>> captor = ArgumentCaptor.forClass(List.class);
        verify(jobRepository).findByTenantIdAndStatusIn(eq(tenantId), captor.capture(), eq(pageable));
        assertThat(captor.getValue())
                .containsExactlyInAnyOrder(JobStatus.ACTIVE, JobStatus.PAUSED, JobStatus.DISABLED);
    }

    // ---------------------------------------------------------------------
    // getJob
    // ---------------------------------------------------------------------

    @Test
    void getJob_found_returnsJob() {
        SchedulerJob job = newJob();
        when(jobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.of(job));

        assertThat(service.getJob(jobId, tenantId)).isSameAs(job);
    }

    @Test
    void getJob_notFound_throwsResourceNotFound() {
        when(jobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getJob(jobId, tenantId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job not found");
    }

    // ---------------------------------------------------------------------
    // createJob
    // ---------------------------------------------------------------------

    @Test
    void createJob_validEnabledActive_savesAndRegisters() {
        SchedulerJob job = newJob();
        Tenant tenant = new Tenant();
        when(cronValidationService.isValid("0 0 3 * * *")).thenReturn(true);
        when(cronValidationService.calculateNextRun("0 0 3 * * *", "UTC")).thenReturn(ZonedDateTime.now());
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(jobRepository.save(any(SchedulerJob.class))).thenAnswer(inv -> inv.getArgument(0));

        SchedulerJob saved = service.createJob(job, tenantId, "tim");

        assertThat(saved.getTenant()).isSameAs(tenant);
        assertThat(saved.getCreatedBy()).isEqualTo("tim");
        assertThat(saved.getLastModifiedBy()).isEqualTo("tim");
        assertThat(saved.getNextRunAt()).isNotNull();
        verify(engineService).registerJob(saved);
    }

    @Test
    void createJob_disabled_doesNotRegister() {
        SchedulerJob job = newJob();
        job.setEnabled(false);
        when(cronValidationService.isValid("0 0 3 * * *")).thenReturn(true);
        when(cronValidationService.calculateNextRun("0 0 3 * * *", "UTC")).thenReturn(ZonedDateTime.now());
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(new Tenant()));
        when(jobRepository.save(any(SchedulerJob.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createJob(job, tenantId, "tim");

        verify(engineService, never()).registerJob(any());
    }

    @Test
    void createJob_invalidCron_throwsIllegalArgument() {
        SchedulerJob job = newJob();
        when(cronValidationService.isValid("0 0 3 * * *")).thenReturn(false);
        when(cronValidationService.getValidationError("0 0 3 * * *")).thenReturn("bad");

        assertThatThrownBy(() -> service.createJob(job, tenantId, "tim"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid cron expression");
        verify(jobRepository, never()).save(any());
    }

    @Test
    void createJob_tenantNotFound_throwsResourceNotFound() {
        SchedulerJob job = newJob();
        when(cronValidationService.isValid("0 0 3 * * *")).thenReturn(true);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createJob(job, tenantId, "tim"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tenant not found");
    }

    @Test
    void createJob_plaintextRestSecret_getsEncrypted() {
        SchedulerJob job = newJob();
        SchedulerRestConfig rc = new SchedulerRestConfig();
        rc.setAuthPasswordEnc("plainpw");
        job.setRestConfig(rc);
        when(cronValidationService.isValid("0 0 3 * * *")).thenReturn(true);
        when(cronValidationService.calculateNextRun("0 0 3 * * *", "UTC")).thenReturn(ZonedDateTime.now());
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(new Tenant()));
        when(encryptionService.decrypt("plainpw")).thenThrow(new RuntimeException("not ciphertext"));
        when(encryptionService.encrypt("plainpw")).thenReturn("ENC(plainpw)");
        when(jobRepository.save(any(SchedulerJob.class))).thenAnswer(inv -> inv.getArgument(0));

        SchedulerJob saved = service.createJob(job, tenantId, "tim");

        assertThat(saved.getRestConfig().getAuthPasswordEnc()).isEqualTo("ENC(plainpw)");
        verify(encryptionService).encrypt("plainpw");
    }

    // ---------------------------------------------------------------------
    // updateJob
    // ---------------------------------------------------------------------

    @Test
    void updateJob_notFound_throwsResourceNotFound() {
        when(jobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateJob(jobId, newJob(), tenantId, "tim"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job not found");
    }

    @Test
    void updateJob_sameCron_savesAndReschedules() {
        SchedulerJob existing = newJob();
        SchedulerJob data = newJob();
        data.setName("renamed");
        when(jobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.of(existing));
        when(jobRepository.save(any(SchedulerJob.class))).thenAnswer(inv -> inv.getArgument(0));

        SchedulerJob saved = service.updateJob(jobId, data, tenantId, "tim");

        assertThat(saved.getName()).isEqualTo("renamed");
        assertThat(saved.getLastModifiedBy()).isEqualTo("tim");
        verify(engineService).rescheduleJob(saved);
        verify(cronValidationService, never()).isValid(any());
    }

    @Test
    void updateJob_changedCronInvalid_throwsIllegalArgument() {
        SchedulerJob existing = newJob();
        SchedulerJob data = newJob();
        data.setCronExpression("bad cron");
        when(jobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.of(existing));
        when(cronValidationService.isValid("bad cron")).thenReturn(false);
        when(cronValidationService.getValidationError("bad cron")).thenReturn("nope");

        assertThatThrownBy(() -> service.updateJob(jobId, data, tenantId, "tim"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid cron expression");
        verify(jobRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // deleteJob / pauseJob / resumeJob
    // ---------------------------------------------------------------------

    @Test
    void deleteJob_found_unregistersAndDeletes() {
        SchedulerJob job = newJob();
        when(jobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.of(job));

        service.deleteJob(jobId, tenantId);

        verify(engineService).unregisterJob(jobId);
        verify(jobRepository).delete(job);
    }

    @Test
    void deleteJob_notFound_throwsResourceNotFound() {
        when(jobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteJob(jobId, tenantId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(engineService, never()).unregisterJob(any());
    }

    @Test
    void pauseJob_found_setsPausedAndUnregisters() {
        SchedulerJob job = newJob();
        when(jobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(SchedulerJob.class))).thenAnswer(inv -> inv.getArgument(0));

        SchedulerJob result = service.pauseJob(jobId, tenantId, "tim");

        assertThat(result.getStatus()).isEqualTo(JobStatus.PAUSED);
        assertThat(result.getLastModifiedBy()).isEqualTo("tim");
        verify(engineService).unregisterJob(jobId);
    }

    @Test
    void resumeJob_found_setsActiveRecalculatesAndRegisters() {
        SchedulerJob job = newJob();
        job.setStatus(JobStatus.PAUSED);
        ZonedDateTime next = ZonedDateTime.now().plusHours(1);
        when(jobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.of(job));
        when(cronValidationService.calculateNextRun("0 0 3 * * *", "UTC")).thenReturn(next);
        when(jobRepository.save(any(SchedulerJob.class))).thenAnswer(inv -> inv.getArgument(0));

        SchedulerJob result = service.resumeJob(jobId, tenantId, "tim");

        assertThat(result.getStatus()).isEqualTo(JobStatus.ACTIVE);
        assertThat(result.getNextRunAt()).isEqualTo(next);
        verify(engineService).registerJob(result);
    }

    @Test
    void resumeJob_notFound_throwsResourceNotFound() {
        when(jobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resumeJob(jobId, tenantId, "tim"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------------
    // statistics helpers
    // ---------------------------------------------------------------------

    @Test
    void countByType_delegatesToRepository() {
        when(jobRepository.countByTenantIdAndJobType(tenantId, JobType.SQL)).thenReturn(4L);
        assertThat(service.countByType(tenantId, JobType.SQL)).isEqualTo(4L);
    }

    @Test
    void countByLastRunStatus_delegatesToRepository() {
        when(jobRepository.countByTenantIdAndLastRunStatus(tenantId, JobRunStatus.FAILURE)).thenReturn(2L);
        assertThat(service.countByLastRunStatus(tenantId, JobRunStatus.FAILURE)).isEqualTo(2L);
    }

    @Test
    void countRunsSince_delegatesToRunRepository() {
        ZonedDateTime since = ZonedDateTime.now().minusDays(1);
        when(runRepository.countByTenantIdAndStatusAndStartedAtAfter(tenantId, JobRunStatus.SUCCESS, since))
                .thenReturn(9L);
        assertThat(service.countRunsSince(tenantId, JobRunStatus.SUCCESS, since)).isEqualTo(9L);
        verify(runRepository, times(1))
                .countByTenantIdAndStatusAndStartedAtAfter(tenantId, JobRunStatus.SUCCESS, since);
    }
}
