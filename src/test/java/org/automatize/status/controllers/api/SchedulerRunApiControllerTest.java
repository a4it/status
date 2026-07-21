package org.automatize.status.controllers.api;

import org.automatize.status.models.SchedulerJobRun;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.automatize.status.models.scheduler.JobTriggerType;
import org.automatize.status.repositories.SchedulerJobRunRepository;
import org.automatize.status.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link SchedulerRunApiController}. The controller talks
 * directly to {@link SchedulerJobRunRepository} (mocked). {@code listRuns} reads
 * the {@code SecurityContext}, so a {@link UserPrincipal} is installed per test.
 * The not-found paths throw a plain {@code RuntimeException} (no
 * {@code @ResponseStatus}), which propagates out of {@code mockMvc.perform}.
 */
@WebMvcTest(controllers = SchedulerRunApiController.class)
class SchedulerRunApiControllerTest extends AbstractApiControllerTest {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String RUNS_ID_PATH = "/api/scheduler/runs/{id}";

    @MockitoBean
    private SchedulerJobRunRepository runRepository;

    /**
     * Installs an authenticated ADMIN {@link UserPrincipal} into the {@link SecurityContextHolder}
     * before each test, since {@code listRuns} reads the current principal from the security context.
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
     * Builds a sample {@link SchedulerJobRun} fixture for stubbing repository calls.
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

    // -------------------------------------------------------------------------
    // GET /api/scheduler/runs
    // -------------------------------------------------------------------------

    /**
     * Verifies GET /api/scheduler/runs without a jobId returns 200 with the tenant-scoped page of runs.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void listRuns_noJobId_returnsOkTenantPage() throws Exception {
        when(runRepository.findByTenantIdOrderByStartedAtDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleRun(UUID.randomUUID()))));

        mockMvc.perform(get("/api/scheduler/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value(STATUS_SUCCESS));
    }

    /**
     * Verifies GET /api/scheduler/runs with a jobId param returns 200 with the page of runs for that job.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void listRuns_withJobId_returnsOkJobPage() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(runRepository.findByJobIdOrderByStartedAtDesc(eq(jobId), any()))
                .thenReturn(new PageImpl<>(List.of(sampleRun(UUID.randomUUID()))));

        mockMvc.perform(get("/api/scheduler/runs").param("jobId", jobId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].triggerType").value("MANUAL"));
    }

    // -------------------------------------------------------------------------
    // GET /api/scheduler/runs/{id}
    // -------------------------------------------------------------------------

    /**
     * Verifies GET /api/scheduler/runs/{id} returns 200 with the run when the repository finds it.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getRun_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(runRepository.findById(id)).thenReturn(Optional.of(sampleRun(id)));

        mockMvc.perform(get(RUNS_ID_PATH, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(STATUS_SUCCESS));
    }

    /**
     * Verifies that when the repository returns empty, the controller's plain {@code RuntimeException}
     * (no {@code @ResponseStatus}) propagates out of {@code mockMvc.perform}.
     */
    @Test
    void getRun_notFound_propagatesRuntimeException() {
        UUID id = UUID.randomUUID();
        when(runRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(Exception.class,
                () -> mockMvc.perform(get(RUNS_ID_PATH, id)));
    }

    // -------------------------------------------------------------------------
    // GET /api/scheduler/runs/job/{jobId}
    // -------------------------------------------------------------------------

    /**
     * Verifies GET /api/scheduler/runs/job/{jobId} returns 200 with the page of runs for the given job.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getRunsForJob_returnsOkPage() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(runRepository.findByJobIdOrderByStartedAtDesc(eq(jobId), any()))
                .thenReturn(new PageImpl<>(List.of(sampleRun(UUID.randomUUID()))));

        mockMvc.perform(get("/api/scheduler/runs/job/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value(STATUS_SUCCESS));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/scheduler/runs/{id}
    // -------------------------------------------------------------------------

    /**
     * Verifies DELETE /api/scheduler/runs/{id} returns 200 with a success message and deletes the run
     * when it exists.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void deleteRun_found_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();
        SchedulerJobRun run = sampleRun(id);
        when(runRepository.findById(id)).thenReturn(Optional.of(run));

        mockMvc.perform(delete(RUNS_ID_PATH, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(runRepository).delete(run);
    }

    /**
     * Verifies that deleting an unknown run causes the controller's plain {@code RuntimeException}
     * (no {@code @ResponseStatus}) to propagate out of {@code mockMvc.perform}.
     */
    @Test
    void deleteRun_notFound_propagatesRuntimeException() {
        UUID id = UUID.randomUUID();
        when(runRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(Exception.class,
                () -> mockMvc.perform(delete(RUNS_ID_PATH, id)));
    }
}
