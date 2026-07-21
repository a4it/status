package org.automatize.status.repositories;

import org.automatize.status.models.SchedulerJob;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @DataJpaTest} coverage for {@link SchedulerJobRepository}'s custom derived
 * and JPQL queries against H2 (PostgreSQL compatibility mode). Focus is tenant
 * scoping, status/enabled filtering, dashboard counts, paging and search.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SchedulerJobRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SchedulerJobRepository repository;

    private Tenant persistTenant(String name) {
        Tenant t = new Tenant();
        t.setName(name);
        t.setCreatedBy("test");
        t.setLastModifiedBy("test");
        return em.persistAndFlush(t);
    }

    private SchedulerJob persistJob(String name, Tenant tenant, JobType type, JobStatus status,
                                    boolean enabled, JobRunStatus lastRunStatus) {
        SchedulerJob j = new SchedulerJob();
        j.setName(name);
        j.setTenant(tenant);
        j.setJobType(type);
        j.setStatus(status);
        j.setEnabled(enabled);
        j.setLastRunStatus(lastRunStatus);
        j.setCronExpression("0 0 * * * *");
        j.setCreatedBy("test");
        j.setLastModifiedBy("test");
        return em.persistAndFlush(j);
    }

    @Test
    void findByTenantIdAndStatusIn_scopesByTenantAndStatuses() {
        Tenant t1 = persistTenant("T1");
        Tenant t2 = persistTenant("T2");
        persistJob("Active1", t1, JobType.REST, JobStatus.ACTIVE, true, null);
        persistJob("Paused1", t1, JobType.REST, JobStatus.PAUSED, true, null);
        persistJob("Disabled1", t1, JobType.REST, JobStatus.DISABLED, true, null);
        persistJob("Active2", t2, JobType.REST, JobStatus.ACTIVE, true, null);

        Page<SchedulerJob> result = repository.findByTenantIdAndStatusIn(
                t1.getId(), List.of(JobStatus.ACTIVE, JobStatus.PAUSED), PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(SchedulerJob::getName)
                .containsExactlyInAnyOrder("Active1", "Paused1");
    }

    @Test
    void findByEnabledTrueAndStatus_returnsEnabledJobsInStatus() {
        Tenant t = persistTenant("T1");
        persistJob("EnabledActive", t, JobType.SQL, JobStatus.ACTIVE, true, null);
        persistJob("DisabledActive", t, JobType.SQL, JobStatus.ACTIVE, false, null);
        persistJob("EnabledPaused", t, JobType.SQL, JobStatus.PAUSED, true, null);

        assertThat(repository.findByEnabledTrueAndStatus(JobStatus.ACTIVE))
                .extracting(SchedulerJob::getName).containsExactly("EnabledActive");
    }

    @Test
    void findByIdAndTenantId_matchesOnlyWhenTenantOwnsJob() {
        Tenant t1 = persistTenant("T1");
        Tenant t2 = persistTenant("T2");
        SchedulerJob job = persistJob("Owned", t1, JobType.PROGRAM, JobStatus.ACTIVE, true, null);

        assertThat(repository.findByIdAndTenantId(job.getId(), t1.getId())).isPresent();
        assertThat(repository.findByIdAndTenantId(job.getId(), t2.getId())).isEmpty();
    }

    @Test
    void countByTenantIdAndJobType_countsScopedByType() {
        Tenant t1 = persistTenant("T1");
        Tenant t2 = persistTenant("T2");
        persistJob("R1", t1, JobType.REST, JobStatus.ACTIVE, true, null);
        persistJob("R2", t1, JobType.REST, JobStatus.ACTIVE, true, null);
        persistJob("S1", t1, JobType.SQL, JobStatus.ACTIVE, true, null);
        persistJob("R3", t2, JobType.REST, JobStatus.ACTIVE, true, null);

        assertThat(repository.countByTenantIdAndJobType(t1.getId(), JobType.REST)).isEqualTo(2L);
    }

    @Test
    void countByTenantIdAndLastRunStatus_countsScopedByLastRunStatus() {
        Tenant t = persistTenant("T1");
        persistJob("Ok1", t, JobType.REST, JobStatus.ACTIVE, true, JobRunStatus.SUCCESS);
        persistJob("Ok2", t, JobType.REST, JobStatus.ACTIVE, true, JobRunStatus.SUCCESS);
        persistJob("Bad", t, JobType.REST, JobStatus.ACTIVE, true, JobRunStatus.FAILURE);

        assertThat(repository.countByTenantIdAndLastRunStatus(t.getId(), JobRunStatus.SUCCESS)).isEqualTo(2L);
        assertThat(repository.countByTenantIdAndLastRunStatus(t.getId(), JobRunStatus.FAILURE)).isEqualTo(1L);
    }

    @Test
    void findByTenantId_returnsAllJobsInTenant() {
        Tenant t1 = persistTenant("T1");
        Tenant t2 = persistTenant("T2");
        persistJob("A", t1, JobType.REST, JobStatus.ACTIVE, true, null);
        persistJob("B", t1, JobType.REST, JobStatus.PAUSED, true, null);
        persistJob("C", t2, JobType.REST, JobStatus.ACTIVE, true, null);

        assertThat(repository.findByTenantId(t1.getId())).extracting(SchedulerJob::getName)
                .containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void searchByTenant_withTerm_matchesActiveJobsByNameCaseInsensitive() {
        Tenant t = persistTenant("T1");
        persistJob("Nightly Backup", t, JobType.PROGRAM, JobStatus.ACTIVE, true, null);
        persistJob("Hourly Sync", t, JobType.PROGRAM, JobStatus.ACTIVE, true, null);
        persistJob("Nightly Report Paused", t, JobType.PROGRAM, JobStatus.PAUSED, true, null);

        Page<SchedulerJob> result = repository.searchByTenant(t.getId(), "nightly", PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(SchedulerJob::getName).containsExactly("Nightly Backup");
    }

    @Test
    void searchByTenant_withNullTerm_returnsAllActiveJobs() {
        Tenant t = persistTenant("T1");
        persistJob("Active A", t, JobType.PROGRAM, JobStatus.ACTIVE, true, null);
        persistJob("Active B", t, JobType.PROGRAM, JobStatus.ACTIVE, true, null);
        persistJob("Paused C", t, JobType.PROGRAM, JobStatus.PAUSED, true, null);

        Page<SchedulerJob> result = repository.searchByTenant(t.getId(), null, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(SchedulerJob::getName)
                .containsExactlyInAnyOrder("Active A", "Active B");
    }
}
