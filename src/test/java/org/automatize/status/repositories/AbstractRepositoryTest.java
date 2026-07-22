package org.automatize.status.repositories;

import org.automatize.status.models.Organization;
import org.automatize.status.models.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Shared fixture helpers for repository {@code @DataJpaTest} classes: persists
 * the common Tenant → Organization graph every repository test needs.
 */
abstract class AbstractRepositoryTest {

    @Autowired
    protected TestEntityManager em;

    protected Tenant persistTenant(String name) {
        Tenant t = new Tenant();
        t.setName(name);
        t.setCreatedBy("test");
        t.setLastModifiedBy("test");
        return em.persistAndFlush(t);
    }

    protected Organization persistOrganization(String name, Tenant tenant) {
        Organization o = new Organization();
        o.setName(name);
        o.setOrganizationType("BUSINESS");
        o.setTenant(tenant);
        o.setCreatedBy("test");
        o.setLastModifiedBy("test");
        return em.persistAndFlush(o);
    }
}
