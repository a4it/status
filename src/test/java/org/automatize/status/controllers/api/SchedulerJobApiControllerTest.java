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

    private static final String NIGHTLY_BACKUP = "Nightly Backup";
    private static final String JOBS_PATH = JOBS_PATH;
    private static final String JOB_BY_ID_PATH = JOB_BY_ID_PATH;
    private static final String JOB_TRIGGER_PATH = JOB_TRIGGER_PATH;
    private static final String STATUS_JSON_PATH = "$.status";
    private static final String JOB_NOT_FOUND = "Job not found";

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

    /**
     * Installs an authenticated ADMIN {@link UserPrincipal} into the {@link SecurityContextHolder}
     * before each test, since the controller reads the current principal from the security context.
     */
    @BeforeEach
    void setUpPrincipal() {
        UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(), "admin", "admin@test.local", "pw", "ADMIN",
                UUID.randomUUID(), true, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    /**
     * Clears the {@link SecurityContextHolder} after each test to avoid principal leakage between tests.
     */
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Builds a sample {@link SchedulerJob} fixture for stubbing service calls.
     *
     * @param id the identifier to assign
     * @return an ACTIVE nightly-backup PROGRAM job with a midnight cron expression
     */
    private SchedulerJob sampleJob(UUID id) {
        SchedulerJob job = new SchedulerJob();
        job.setId(id);
        job.setName(NIGHTLY_BACKUP);
        job.setJobType(JobType.PROGRAM);
        job.setCronExpression("0 0 0 * * *");
        job.setStatus(JobStatus.ACTIVE);
        return job;
    }

    /**
     * Builds a sample {@link SchedulerJobRun} fixture for stubbing manual-trigger results.
     *
     * @param id the identifier to assign
     * @return a SUCCESS run with a MANUAL trigger type
     */
    private SchedulerJobRun sampleRun(UUID id) {
        SchedulerJobRun run = new SchedulerJobRun();
        run.setId(id);
        run.setStatus(JobRunStatus.SUCCESS);
        run.setTriggerType(JobTriggerType.MANUAL);
        return run;
    }

    /**
     * Provides a minimal valid JSON request body satisfying the create/update bean-validation constraints.
     *
     * @return a JSON string with name, jobType and cronExpression populated
     */
    private String validRequestBody() {
        return "{\"name\":\"Nightly Backup\",\"jobType\":\"PROGRAM\",\"cronExpression\":\"0 0 0 * * *\"}";
    }

    // -------------------------------------------------------------------------
    // GET /api/scheduler/jobs
    // -------------------------------------------------------------------------

    /**
     * Verifies GET /api/scheduler/jobs returns 200 with a paged list of jobs.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void listJobs_returnsOkPage() throws Exception {
        when(schedulerJobService.listJobs(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleJob(UUID.randomUUID()))));

        mockMvc.perform(get(JOBS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value(NIGHTLY_BACKUP))
                .andExpect(jsonPath("$.content[0].jobType").value("PROGRAM"));
    }

    // -------------------------------------------------------------------------
    // GET /api/scheduler/jobs/{id}
    // -------------------------------------------------------------------------

    /**
     * Verifies GET /api/scheduler/jobs/{id} returns 200 with the job when found.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getJob_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(schedulerJobService.getJob(eq(id), any())).thenReturn(sampleJob(id));

        mockMvc.perform(get(JOB_BY_ID_PATH, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS_JSON_PATH).value("ACTIVE"));
    }

    /**
     * Verifies GET /api/scheduler/jobs/{id} returns 404 when the service throws
     * {@code ResourceNotFoundException}.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getJob_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(schedulerJobService.getJob(eq(id), any()))
                .thenThrow(new ResourceNotFoundException(JOB_NOT_FOUND));

        mockMvc.perform(get(JOB_BY_ID_PATH, id))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/scheduler/jobs
    // -------------------------------------------------------------------------

    /**
     * Verifies POST /api/scheduler/jobs with a valid body returns 201 with the created job.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void createJob_valid_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(schedulerJobService.createJob(any(), any(), any())).thenReturn(sampleJob(id));

        mockMvc.perform(post(JOBS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(NIGHTLY_BACKUP));
    }

    /**
     * Verifies POST /api/scheduler/jobs returns 400 when the required name is missing
     * (bean validation failure).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void createJob_missingName_returns400() throws Exception {
        mockMvc.perform(post(JOBS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobType\":\"PROGRAM\",\"cronExpression\":\"0 0 0 * * *\"}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies POST /api/scheduler/jobs returns 400 when the required cronExpression is missing
     * (bean validation failure).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void createJob_missingCronExpression_returns400() throws Exception {
        mockMvc.perform(post(JOBS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nightly Backup\",\"jobType\":\"PROGRAM\"}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that when the service rejects the cron with {@code IllegalArgumentException} (no
     * {@code @ResponseStatus}), the exception propagates out of {@code mockMvc.perform}.
     */
    @Test
    void createJob_serviceRejectsCron_propagatesIllegalArgument() {
        when(schedulerJobService.createJob(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid cron expression"));

        assertThrows(Exception.class,
                () -> mockMvc.perform(post(JOBS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody())));
    }

    // -------------------------------------------------------------------------
    // PUT /api/scheduler/jobs/{id}
    // -------------------------------------------------------------------------

    /**
     * Verifies PUT /api/scheduler/jobs/{id} with a valid body returns 200 with the updated job.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void updateJob_valid_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(schedulerJobService.updateJob(eq(id), any(), any(), any())).thenReturn(sampleJob(id));

        mockMvc.perform(put(JOB_BY_ID_PATH, id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(NIGHTLY_BACKUP));
    }

    /**
     * Verifies PUT /api/scheduler/jobs/{id} returns 404 when the service throws
     * {@code ResourceNotFoundException}.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void updateJob_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(schedulerJobService.updateJob(eq(id), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException(JOB_NOT_FOUND));

        mockMvc.perform(put(JOB_BY_ID_PATH, id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/scheduler/jobs/{id}
    // -------------------------------------------------------------------------

    /**
     * Verifies DELETE /api/scheduler/jobs/{id} returns 204 and delegates deletion to the service.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void deleteJob_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete(JOB_BY_ID_PATH, id))
                .andExpect(status().isNoContent());

        verify(schedulerJobService).deleteJob(eq(id), any());
    }

    /**
     * Verifies DELETE /api/scheduler/jobs/{id} returns 404 when the service throws
     * {@code ResourceNotFoundException}.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void deleteJob_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ResourceNotFoundException(JOB_NOT_FOUND))
                .when(schedulerJobService).deleteJob(eq(id), any());

        mockMvc.perform(delete(JOB_BY_ID_PATH, id))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/scheduler/jobs/{id}/pause  and  /resume
    // -------------------------------------------------------------------------

    /**
     * Verifies POST /api/scheduler/jobs/{id}/pause returns 200 with the job now in PAUSED status.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void pauseJob_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        SchedulerJob paused = sampleJob(id);
        paused.setStatus(JobStatus.PAUSED);
        when(schedulerJobService.pauseJob(eq(id), any(), any())).thenReturn(paused);

        mockMvc.perform(post("/api/scheduler/jobs/{id}/pause", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS_JSON_PATH).value("PAUSED"));
    }

    /**
     * Verifies POST /api/scheduler/jobs/{id}/resume returns 200 with the job back in ACTIVE status.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void resumeJob_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(schedulerJobService.resumeJob(eq(id), any(), any())).thenReturn(sampleJob(id));

        mockMvc.perform(post("/api/scheduler/jobs/{id}/resume", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS_JSON_PATH).value("ACTIVE"));
    }

    /**
     * Verifies POST /api/scheduler/jobs/{id}/resume returns 404 when the service throws
     * {@code ResourceNotFoundException}.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void resumeJob_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(schedulerJobService.resumeJob(eq(id), any(), any()))
                .thenThrow(new ResourceNotFoundException(JOB_NOT_FOUND));

        mockMvc.perform(post("/api/scheduler/jobs/{id}/resume", id))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/scheduler/jobs/{id}/trigger
    // -------------------------------------------------------------------------

    /**
     * Verifies POST /api/scheduler/jobs/{id}/trigger returns 200 with the resulting run when a manual
     * dispatch is executed.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void triggerJob_returnsOkRun() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobDispatcherService.triggerManually(eq(id), any(), any()))
                .thenReturn(sampleRun(UUID.randomUUID()));

        mockMvc.perform(post(JOB_TRIGGER_PATH, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS_JSON_PATH).value("SUCCESS"));
    }

    /**
     * Verifies POST /api/scheduler/jobs/{id}/trigger returns 200 with an empty body when the dispatcher
     * skips the run (returns {@code null}).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void triggerJob_skipped_returnsOkEmptyBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobDispatcherService.triggerManually(eq(id), any(), any())).thenReturn(null);

        mockMvc.perform(post(JOB_TRIGGER_PATH, id))
                .andExpect(status().isOk());
    }

    /**
     * Verifies that when the dispatcher throws a plain {@code RuntimeException} (no
     * {@code @ResponseStatus}), the exception propagates out of {@code mockMvc.perform}.
     */
    @Test
    void triggerJob_notFound_propagatesRuntimeException() {
        UUID id = UUID.randomUUID();
        when(jobDispatcherService.triggerManually(eq(id), any(), any()))
                .thenThrow(new RuntimeException("Job not found or access denied"));

        assertThrows(Exception.class,
                () -> mockMvc.perform(post(JOB_TRIGGER_PATH, id)));
    }

    // -------------------------------------------------------------------------
    // GET /api/scheduler/jobs/{id}/next-runs
    // -------------------------------------------------------------------------

    /**
     * Verifies GET /api/scheduler/jobs/{id}/next-runs returns 200 with the array of upcoming execution
     * times computed from the job's cron expression.
     *
     * @throws Exception if the mock request fails
     */
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

    /**
     * Verifies GET /api/scheduler/jobs/stats returns 200 with aggregate counters: total jobs,
     * running now, succeeded today and failed today.
     *
     * @throws Exception if the mock request fails
     */
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
