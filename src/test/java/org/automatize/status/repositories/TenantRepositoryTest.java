package org.automatize.status.repositories;

import org.automatize.status.models.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @DataJpaTest} coverage for {@link TenantRepository}'s custom derived and
 * JPQL queries against H2 (PostgreSQL compatibility mode). Focus is name lookups,
 * active-status filtering, search, created-by filtering and ordering.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TenantRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private TenantRepository repository;

    private Tenant persistTenant(String name, boolean active, String createdBy, long createdMillis) {
        Tenant t = new Tenant();
        t.setName(name);
        t.setIsActive(active);
        t.setCreatedBy(createdBy);
        t.setLastModifiedBy(createdBy);
        // Explicit timestamps drive ordering assertions deterministically.
        t.setCreatedDateTechnical(createdMillis);
        t.setCreatedDate(ZonedDateTime.parse("2024-01-01T00:00:00Z").plusSeconds(createdMillis));
        return em.persistAndFlush(t);
    }

    @Test
    void findByName_existingTenant_returnsTenant() {
        persistTenant("Acme", true, "test", 1000L);

        assertThat(repository.findByName("Acme")).isPresent().get()
                .extracting(Tenant::getName).isEqualTo("Acme");
    }

    @Test
    void findByName_missingTenant_returnsEmpty() {
        assertThat(repository.findByName("Nope")).isEmpty();
    }

    @Test
    void findByIsActive_filtersByActiveFlag() {
        persistTenant("Active One", true, "test", 1000L);
        persistTenant("Inactive One", false, "test", 1000L);

        assertThat(repository.findByIsActive(true)).extracting(Tenant::getName).containsExactly("Active One");
        assertThat(repository.findByIsActive(false)).extracting(Tenant::getName).containsExactly("Inactive One");
    }

    @Test
    void search_matchesNameSubstring() {
        persistTenant("Payment Corp", true, "test", 1000L);
        persistTenant("Logistics", true, "test", 1000L);

        assertThat(repository.search("Corp")).extracting(Tenant::getName).containsExactly("Payment Corp");
    }

    @Test
    void findByCreatedBy_filtersByCreator() {
        persistTenant("By Alice", true, "alice", 1000L);
        persistTenant("By Bob", true, "bob", 1000L);

        assertThat(repository.findByCreatedBy("alice")).extracting(Tenant::getName).containsExactly("By Alice");
    }

    @Test
    void findAllOrderByCreatedDateDesc_ordersNewestFirst() {
        persistTenant("Oldest", true, "test", 1000L);
        persistTenant("Newest", true, "test", 3000L);
        persistTenant("Middle", true, "test", 2000L);

        List<Tenant> result = repository.findAllOrderByCreatedDateDesc();

        assertThat(result).extracting(Tenant::getName).containsExactly("Newest", "Middle", "Oldest");
    }

    @Test
    void existsByName_reflectsPresence() {
        persistTenant("Exists", true, "test", 1000L);

        assertThat(repository.existsByName("Exists")).isTrue();
        assertThat(repository.existsByName("Missing")).isFalse();
    }
}
