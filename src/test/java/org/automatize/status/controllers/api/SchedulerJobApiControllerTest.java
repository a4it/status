package org.automatize.status.controllers.api;

import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.SchedulerJob;
import org.automatize.status.models.SchedulerJobRun;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.automatize.status.models.scheduler.JobStatus;
import org.automatize.status.models.scheduler.JobTriggerType;
import org.automatize.status.models.scheduler.JobType;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.SchedulerJdbcDatasourceRepository;
import org.automatize.status.security.UserPrincipal;
import org.automatize.status.services.scheduler.CronValidationService;
import org.automatize.status.services.scheduler.JobDispatcherService;
import org.automatize.status.services.scheduler.SchedulerJobService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link SchedulerJobApiController}. Covers CRUD and
 * lifecycle endpoints (pause/resume/trigger/next-runs/stats), bean validation
 * (400), JSON contract, and {@code @ResponseStatus} exception mapping (404).
 * The controller reads the {@code SecurityContext}, so a {@link UserPrincipal}
 * is installed per test. Not-found on manual trigger throws a plain
 * {@code RuntimeException} (no {@code @ResponseStatus}) which propagates out of
 * {@code mockMvc.perform}.
 */
@WebMvcTest(controllers = SchedulerJobApiController.class)
class SchedulerJobApiControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private SchedulerJobService schedulerJobService;

    @MockitoBean
    private JobDispatcherService jobDispatcherService;

    @MockitoBean
    private CronValidationService cronValidationService;

    @MockitoBean
    private OrganizationRepository organizationRepository;

    @MockitoBean
    private SchedulerJdbcDatasourceRepository datasourceRepository;

    @BeforeEach
    void setUpPrincipal() {
        UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(), "admin", "admin@test.local", "pw", "ADMIN",
                UUID.randomUUID(), true, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private SchedulerJob sampleJob(UUID id) {
        SchedulerJob job = new SchedulerJob();
        job.setId(id);
        job.setName("Nightly Backup");
        job.setJobType(JobType.PROGRAM);
        job.setCronExpression("0 0 0 * * *");
        job.setStatus(JobStatus.ACTIVE);
        return job;
    }

    private SchedulerJobRun sampleRun(UUID id) {
        SchedulerJobRun run = new SchedulerJobRun();
        run.setId(id);
        run.setStatus(JobRunStatus.SUCCESS);
        run.setTriggerType(JobTriggerType.MANUAL);
        return run;
    }

    private String validRequestBody() {
        return "{\"name\":\"Nightly Backup\",\"jobType\":\"PROGRAM\",\"cronExpression\":\"0 0 0 * * *\"}";
    }

    // -------------------------------------------------------------------------
    // GET /api/scheduler/jobs
    // -------------------------------------------------------------------------

    @Test
    void listJobs_returnsOkPage() throws Exception {
        when(schedulerJobService.listJobs(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleJob(UUID.randomUUID()))));

        mockMvc.perform(get("/api/scheduler/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Nightly Backup"))
                .andExpect(jsonPath("$.content[0].jobType").value("PROGRAM"));
    }

    // -------------------------------------------------------------------------
    // GET /api/scheduler/jobs/{id}
    // -------------------------------------------------------------------------

    @Test
    void getJob_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(schedulerJobService.getJob(eq(id), any())).thenReturn(sampleJob(id));

        mockMvc.perform(get("/api/scheduler/jobs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getJob_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(schedulerJobService.getJob(eq(id), any()))
                .thenThrow(new ResourceNotFoundException("Job not found"));

        mockMvc.perform(get("/api/scheduler/jobs/{id}", id))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/scheduler/jobs
    // -------------------------------------------------------------------------

    @Test
    void createJob_valid_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(schedulerJobService.createJob(any(), any(), any())).thenReturn(sampleJob(id));

        mockMvc.perform(post("/api/scheduler/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Nightly Backup"));
    }

    @Test
    void createJob_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/scheduler/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobType\":\"PROGRAM\",\"cronExpression\":\"0 0 0 * * *\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createJob_missingCronExpression_returns400() throws Exception {
        mockMvc.perform(post("/api/scheduler/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nightly Backup\",\"jobType\":\"PROGRAM\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createJob_serviceRejectsCron_propagatesIllegalArgument() {
        when(schedulerJobService.createJob(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid cron expression"));

        assertThrows(Exception.class,
                () -> mockMvc.perform(post("/api/scheduler/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody())));
    }

    // -------------------------------------------------------------------------
    // PUT /api/scheduler/jobs/{id}
    // -------------------------------------------------------------------------

    @Test
    void updateJob_valid_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(schedulerJobService.updateJob(eq(id), any(), any(), any())).thenReturn(sampleJob(id));

        mockMvc.perform(put("/api/scheduler/jobs/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nightly Backup"));
    }

    @Test
    void updateJob_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(schedulerJobService.updateJob(eq(id), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Job not found"));

        mockMvc.perform(put("/api/scheduler/jobs/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/scheduler/jobs/{id}
    // -------------------------------------------------------------------------

    @Test
    void deleteJob_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/scheduler/jobs/{id}", id))
                .andExpect(status().isNoContent());

        verify(schedulerJobService).deleteJob(eq(id), any());
    }

    @Test
    void deleteJob_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Job not found"))
                .when(schedulerJobService).deleteJob(eq(id), any());

        mockMvc.perform(delete("/api/scheduler/jobs/{id}", id))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/scheduler/jobs/{id}/pause  and  /resume
    // -------------------------------------------------------------------------

    @Test
    void pauseJob_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        SchedulerJob paused = sampleJob(id);
        paused.setStatus(JobStatus.PAUSED);
        when(schedulerJobService.pauseJob(eq(id), any(), any())).thenReturn(paused);

        mockMvc.perform(post("/api/scheduler/jobs/{id}/pause", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test
    void resumeJob_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(schedulerJobService.resumeJob(eq(id), any(), any())).thenReturn(sampleJob(id));

        mockMvc.perform(post("/api/scheduler/jobs/{id}/resume", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void resumeJob_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(schedulerJobService.resumeJob(eq(id), any(), any()))
                .thenThrow(new ResourceNotFoundException("Job not found"));

        mockMvc.perform(post("/api/scheduler/jobs/{id}/resume", id))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/scheduler/jobs/{id}/trigger
    // -------------------------------------------------------------------------

    @Test
    void triggerJob_returnsOkRun() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobDispatcherService.triggerManually(eq(id), any(), any()))
                .thenReturn(sampleRun(UUID.randomUUID()));

        mockMvc.perform(post("/api/scheduler/jobs/{id}/trigger", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void triggerJob_skipped_returnsOkEmptyBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobDispatcherService.triggerManually(eq(id), any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/scheduler/jobs/{id}/trigger", id))
                .andExpect(status().isOk());
    }

    @Test
    void triggerJob_notFound_propagatesRuntimeException() {
        UUID id = UUID.randomUUID();
        when(jobDispatcherService.triggerManually(eq(id), any(), any()))
                .thenThrow(new RuntimeException("Job not found or access denied"));

        assertThrows(Exception.class,
                () -> mockMvc.perform(post("/api/scheduler/jobs/{id}/trigger", id)));
    }

    // -------------------------------------------------------------------------
    // GET /api/scheduler/jobs/{id}/next-runs
    // -------------------------------------------------------------------------

    @Test
    void nextRuns_returnsOkList() throws Exception {
        UUID id = UUID.randomUUID();
        when(schedulerJobService.getJob(eq(id), any())).thenReturn(sampleJob(id));
        when(cronValidationService.getNextExecutions(any(), any(), anyInt()))
                .thenReturn(List.of(ZonedDateTime.now(), ZonedDateTime.now().plusDays(1)));

        mockMvc.perform(get("/api/scheduler/jobs/{id}/next-runs", id).param("count", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").exists());
    }

    // -------------------------------------------------------------------------
    // GET /api/scheduler/jobs/stats
    // -------------------------------------------------------------------------

    @Test
    void stats_returnsOkAggregates() throws Exception {
        when(schedulerJobService.listJobs(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 7));
        when(schedulerJobService.countByLastRunStatus(any(), eq(JobRunStatus.RUNNING))).thenReturn(2L);
        when(schedulerJobService.countRunsSince(any(), eq(JobRunStatus.SUCCESS), any())).thenReturn(5L);
        when(schedulerJobService.countRunsSince(any(), eq(JobRunStatus.FAILURE), any())).thenReturn(1L);

        mockMvc.perform(get("/api/scheduler/jobs/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalJobs").value(7))
                .andExpect(jsonPath("$.runningNow").value(2))
                .andExpect(jsonPath("$.succeededToday").value(5))
                .andExpect(jsonPath("$.failedToday").value(1));
    }
}
