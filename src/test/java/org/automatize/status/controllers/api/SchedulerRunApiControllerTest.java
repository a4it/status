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

    @MockitoBean
    private SchedulerJobRunRepository runRepository;

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

    @Test
    void listRuns_noJobId_returnsOkTenantPage() throws Exception {
        when(runRepository.findByTenantIdOrderByStartedAtDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleRun(UUID.randomUUID()))));

        mockMvc.perform(get("/api/scheduler/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"));
    }

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

    @Test
    void getRun_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(runRepository.findById(id)).thenReturn(Optional.of(sampleRun(id)));

        mockMvc.perform(get("/api/scheduler/runs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void getRun_notFound_propagatesRuntimeException() {
        UUID id = UUID.randomUUID();
        when(runRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(Exception.class,
                () -> mockMvc.perform(get("/api/scheduler/runs/{id}", id)));
    }

    // -------------------------------------------------------------------------
    // GET /api/scheduler/runs/job/{jobId}
    // -------------------------------------------------------------------------

    @Test
    void getRunsForJob_returnsOkPage() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(runRepository.findByJobIdOrderByStartedAtDesc(eq(jobId), any()))
                .thenReturn(new PageImpl<>(List.of(sampleRun(UUID.randomUUID()))));

        mockMvc.perform(get("/api/scheduler/runs/job/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/scheduler/runs/{id}
    // -------------------------------------------------------------------------

    @Test
    void deleteRun_found_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();
        SchedulerJobRun run = sampleRun(id);
        when(runRepository.findById(id)).thenReturn(Optional.of(run));

        mockMvc.perform(delete("/api/scheduler/runs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(runRepository).delete(run);
    }

    @Test
    void deleteRun_notFound_propagatesRuntimeException() {
        UUID id = UUID.randomUUID();
        when(runRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(Exception.class,
                () -> mockMvc.perform(delete("/api/scheduler/runs/{id}", id)));
    }
}
