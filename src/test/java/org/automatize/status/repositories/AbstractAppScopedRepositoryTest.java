package org.automatize.status.repositories;

import org.automatize.status.models.Organization;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.Tenant;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for repository {@code @DataJpaTest} classes that operate on a single
 * app: persists the default Tenant A → Org A → App A graph before each test and
 * exposes it via {@link #tenant}, {@link #organization} and {@link #app}.
 */
abstract class AbstractAppScopedRepositoryTest extends AbstractRepositoryTest {

    protected Tenant tenant;
    protected Organization organization;
    protected StatusApp app;

    @BeforeEach
    void persistBaseGraph() {
        tenant = persistTenant("Tenant A");
        organization = persistOrganization("Org A", tenant);
        app = persistApp("App A", "app-a", tenant, organization);
    }
}
