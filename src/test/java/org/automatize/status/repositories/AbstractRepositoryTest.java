package org.automatize.status.repositories;

import org.automatize.status.models.Organization;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.models.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Shared fixture helpers for repository {@code @DataJpaTest} classes: persists
 * the common Tenant → Organization → App → Component graph every repository test
 * builds on.
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

    protected StatusApp persistApp(String name, String slug, Tenant tenant, Organization org) {
        StatusApp a = new StatusApp();
        a.setName(name);
        a.setSlug(slug);
        a.setTenant(tenant);
        a.setOrganization(org);
        a.setCreatedBy("test");
        a.setLastModifiedBy("test");
        return em.persistAndFlush(a);
    }

    protected StatusComponent persistComponent(String name, StatusApp app) {
        StatusComponent c = new StatusComponent();
        c.setName(name);
        c.setApp(app);
        c.setCreatedBy("test");
        c.setLastModifiedBy("test");
        return em.persistAndFlush(c);
    }
}
