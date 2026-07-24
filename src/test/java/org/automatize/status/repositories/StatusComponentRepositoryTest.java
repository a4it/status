package org.automatize.status.repositories;

import org.automatize.status.models.Organization;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.models.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @DataJpaTest} coverage for {@link StatusComponentRepository}'s custom
 * JPQL / derived queries against H2 (PostgreSQL compatibility mode). Focus is
 * tenant/organization-scoped queries, search, counts and existence checks.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class StatusComponentRepositoryTest extends AbstractAppScopedRepositoryTest {

    @Autowired
    private StatusComponentRepository repository;

    @Test
    void findByAppId_returnsComponentsOfApp() {
        persistComponent("API", app);
        persistComponent("Web", app);

        List<StatusComponent> result = repository.findByAppId(app.getId());

        assertThat(result).hasSize(2).extracting(StatusComponent::getName)
                .containsExactlyInAnyOrder("API", "Web");
    }

    @Test
    void existsByAppIdAndName_matchesExistingComponent() {
        persistComponent("API", app);

        assertThat(repository.existsByAppIdAndName(app.getId(), "API")).isTrue();
        assertThat(repository.existsByAppIdAndName(app.getId(), "Nope")).isFalse();
    }

    @Test
    void countByAppId_countsComponents() {
        persistComponent("API", app);
        persistComponent("Web", app);

        assertThat(repository.countByAppId(app.getId())).isEqualTo(2L);
    }

    @Test
    void findByTenantId_scopesByAppTenant() {
        persistComponent("API", app);

        Tenant otherTenant = persistTenant("Tenant B");
        Organization otherOrg = persistOrganization("Org B", otherTenant);
        StatusApp otherApp = persistApp("App B", "app-b", otherTenant, otherOrg);
        persistComponent("Other", otherApp);

        List<StatusComponent> result = repository.findByTenantId(tenant.getId());

        assertThat(result).extracting(StatusComponent::getName).containsExactly("API");
    }

    @Test
    void searchByAppId_matchesNameSubstring() {
        persistComponent("Payment API", app);
        persistComponent("Web", app);

        List<StatusComponent> result = repository.searchByAppId(app.getId(), "API");

        assertThat(result).extracting(StatusComponent::getName).containsExactly("Payment API");
    }
}
