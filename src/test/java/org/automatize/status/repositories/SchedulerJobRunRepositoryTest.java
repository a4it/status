package org.automatize.status.repositories;

import org.automatize.status.models.SchedulerJob;
import org.automatize.status.models.SchedulerJobRun;
import org.automatize.status.models.Tenant;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.automatize.status.models.scheduler.JobStatus;
import org.automatize.status.models.scheduler.JobType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @DataJpaTest} coverage for {@link SchedulerJobRunRepository}'s custom derived
 * and JPQL queries against H2 (PostgreSQL compatibility mode). Focus is job/tenant
 * run history ordering, status filtering, time-window counting, retention delete and
 * top-N limiting. Timestamps are set explicitly since the entity has no lifecycle
 * callbacks.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SchedulerJobRunRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SchedulerJobRunRepository repository;

    private static final ZonedDateTime BASE = ZonedDateTime.parse("2026-01-01T00:00:00Z");

    private Tenant persistTenant(String name) {
        Tenant t = new Tenant();
        t.setName(name);
        t.setCreatedBy("test");
        t.setLastModifiedBy("test");
        return em.persistAndFlush(t);
    }

    private SchedulerJob persistJob(String name, Tenant tenant) {
        SchedulerJob j = new SchedulerJob();
        j.setName(name);
        j.setTenant(tenant);
        j.setJobType(JobType.REST);
        j.setStatus(JobStatus.ACTIVE);
        j.setCronExpression("0 0 * * * *");
        j.setCreatedBy("test");
        j.setLastModifiedBy("test");
        return em.persistAndFlush(j);
    }

    private SchedulerJobRun persistRun(SchedulerJob job, Tenant tenant, JobRunStatus status, ZonedDateTime startedAt) {
        SchedulerJobRun r = new SchedulerJobRun();
        r.setJob(job);
        r.setTenant(tenant);
        r.setStatus(status);
        r.setStartedAt(startedAt);
        return em.persistAndFlush(r);
    }

    @Test
    void findByJobIdOrderByStartedAtDesc_returnsRunsForJobNewestFirst() {
        Tenant t = persistTenant("T1");
        SchedulerJob job = persistJob("Job", t);
        SchedulerJob other = persistJob("Other", t);
        persistRun(job, t, JobRunStatus.SUCCESS, BASE.plusMinutes(1));
        persistRun(job, t, JobRunStatus.SUCCESS, BASE.plusMinutes(3));
        persistRun(job, t, JobRunStatus.SUCCESS, BASE.plusMinutes(2));
        persistRun(other, t, JobRunStatus.SUCCESS, BASE.plusMinutes(5));

        Page<SchedulerJobRun> result = repository.findByJobIdOrderByStartedAtDesc(job.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(SchedulerJobRun::getStartedAt)
                .containsExactly(BASE.plusMinutes(3), BASE.plusMinutes(2), BASE.plusMinutes(1));
    }

    @Test
    void findByTenantIdOrderByStartedAtDesc_returnsTenantRunsNewestFirst() {
        Tenant t1 = persistTenant("T1");
        Tenant t2 = persistTenant("T2");
        SchedulerJob j1 = persistJob("J1", t1);
        SchedulerJob j2 = persistJob("J2", t2);
        persistRun(j1, t1, JobRunStatus.SUCCESS, BASE.plusMinutes(1));
        persistRun(j1, t1, JobRunStatus.SUCCESS, BASE.plusMinutes(4));
        persistRun(j2, t2, JobRunStatus.SUCCESS, BASE.plusMinutes(9));

        Page<SchedulerJobRun> result = repository.findByTenantIdOrderByStartedAtDesc(t1.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(SchedulerJobRun::getStartedAt)
                .containsExactly(BASE.plusMinutes(4), BASE.plusMinutes(1));
    }

    @Test
    void findTopByJobIdAndStatusOrderByStartedAtDesc_returnsMostRecentWithStatus() {
        Tenant t = persistTenant("T1");
        SchedulerJob job = persistJob("Job", t);
        persistRun(job, t, JobRunStatus.SUCCESS, BASE.plusMinutes(1));
        persistRun(job, t, JobRunStatus.SUCCESS, BASE.plusMinutes(3));
        persistRun(job, t, JobRunStatus.FAILURE, BASE.plusMinutes(5));

        assertThat(repository.findTopByJobIdAndStatusOrderByStartedAtDesc(job.getId(), JobRunStatus.SUCCESS))
                .isPresent().get().extracting(SchedulerJobRun::getStartedAt).isEqualTo(BASE.plusMinutes(3));
    }

    @Test
    void findByJobIdAndStatus_returnsAllMatchingRuns() {
        Tenant t = persistTenant("T1");
        SchedulerJob job = persistJob("Job", t);
        persistRun(job, t, JobRunStatus.FAILURE, BASE.plusMinutes(1));
        persistRun(job, t, JobRunStatus.FAILURE, BASE.plusMinutes(2));
        persistRun(job, t, JobRunStatus.SUCCESS, BASE.plusMinutes(3));

        assertThat(repository.findByJobIdAndStatus(job.getId(), JobRunStatus.FAILURE)).hasSize(2);
    }

    @Test
    void countByTenantIdAndStatusAndStartedAtAfter_countsRunsInTimeWindow() {
        Tenant t = persistTenant("T1");
        SchedulerJob job = persistJob("Job", t);
        persistRun(job, t, JobRunStatus.SUCCESS, BASE.plusMinutes(1));
        persistRun(job, t, JobRunStatus.SUCCESS, BASE.plusMinutes(10));
        persistRun(job, t, JobRunStatus.SUCCESS, BASE.plusMinutes(20));
        persistRun(job, t, JobRunStatus.FAILURE, BASE.plusMinutes(30));

        long count = repository.countByTenantIdAndStatusAndStartedAtAfter(
                t.getId(), JobRunStatus.SUCCESS, BASE.plusMinutes(5));

        assertThat(count).isEqualTo(2L);
    }

    @Test
    void deleteByJobIdAndStartedAtBefore_removesOnlyOlderRuns() {
        Tenant t = persistTenant("T1");
        SchedulerJob job = persistJob("Job", t);
        persistRun(job, t, JobRunStatus.SUCCESS, BASE.plusMinutes(1));
        persistRun(job, t, JobRunStatus.SUCCESS, BASE.plusMinutes(2));
        SchedulerJobRun kept = persistRun(job, t, JobRunStatus.SUCCESS, BASE.plusMinutes(10));

        repository.deleteByJobIdAndStartedAtBefore(job.getId(), BASE.plusMinutes(5));
        em.flush();
        em.clear();

        // Assert by id: only the newest run (after the cutoff) survives. Exact
        // timestamp equality is avoided because Hibernate/H2 round-trips the stored
        // ZonedDateTime through a zone conversion.
        assertThat(repository.findByJobIdOrderByStartedAtDesc(job.getId(), PageRequest.of(0, 10)).getContent())
                .extracting(SchedulerJobRun::getId)
                .containsExactly(kept.getId());
    }

    @Test
    void findTop100ByJobIdOrderByStartedAtDesc_limitsAndOrders() {
        Tenant t = persistTenant("T1");
        SchedulerJob job = persistJob("Job", t);
        for (int i = 0; i < 105; i++) {
            persistRun(job, t, JobRunStatus.SUCCESS, BASE.plusMinutes(i));
        }

        List<SchedulerJobRun> result = repository.findTop100ByJobIdOrderByStartedAtDesc(job.getId());

        assertThat(result).hasSize(100);
        assertThat(result.get(0).getStartedAt()).isEqualTo(BASE.plusMinutes(104));
        assertThat(result.get(99).getStartedAt()).isEqualTo(BASE.plusMinutes(5));
    }
}
