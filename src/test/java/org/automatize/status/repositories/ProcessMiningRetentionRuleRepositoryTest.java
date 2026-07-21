package org.automatize.status.repositories;

import org.automatize.status.models.ProcessMiningRetentionRule;
import org.automatize.status.models.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @DataJpaTest} coverage for {@link ProcessMiningRetentionRuleRepository}'s
 * custom derived and JPQL queries against H2 (PostgreSQL compatibility mode). Focus
 * is enabled-only filtering and tenant scoping.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ProcessMiningRetentionRuleRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ProcessMiningRetentionRuleRepository repository;

    private Tenant persistTenant(String name) {
        Tenant t = new Tenant();
        t.setName(name);
        t.setCreatedBy("test");
        t.setLastModifiedBy("test");
        return em.persistAndFlush(t);
    }

    private ProcessMiningRetentionRule persistRule(Tenant tenant, int retentionDays, boolean enabled) {
        ProcessMiningRetentionRule r = new ProcessMiningRetentionRule();
        r.setTenant(tenant);
        r.setRetentionDays(retentionDays);
        r.setEnabled(enabled);
        return em.persistAndFlush(r);
    }

    @Test
    void findByEnabledTrue_returnsOnlyEnabledRules() {
        Tenant t = persistTenant("T1");
        persistRule(t, 30, true);
        persistRule(t, 60, true);
        persistRule(t, 90, false);

        assertThat(repository.findByEnabledTrue())
                .hasSize(2)
                .allMatch(ProcessMiningRetentionRule::isEnabled);
    }

    @Test
    void findByTenantId_scopesToTenant() {
        Tenant t1 = persistTenant("T1");
        Tenant t2 = persistTenant("T2");
        persistRule(t1, 30, true);
        persistRule(t1, 45, false);
        persistRule(t2, 30, true);

        assertThat(repository.findByTenantId(t1.getId()))
                .hasSize(2)
                .allMatch(r -> r.getTenant().getId().equals(t1.getId()));
    }
}
