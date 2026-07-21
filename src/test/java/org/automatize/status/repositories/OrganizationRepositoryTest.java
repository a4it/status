package org.automatize.status.repositories;

import org.automatize.status.models.Organization;
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
 * {@code @DataJpaTest} coverage for {@link OrganizationRepository}'s custom derived
 * and JPQL queries against H2 (PostgreSQL compatibility mode). Focus is tenant
 * scoping, status/type/country filters, search, ordering, counts and existence.
 * Organization names are unique, so each fixture uses a distinct name.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class OrganizationRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private OrganizationRepository repository;

    private Tenant persistTenant(String name) {
        Tenant t = new Tenant();
        t.setName(name);
        t.setCreatedBy("test");
        t.setLastModifiedBy("test");
        return em.persistAndFlush(t);
    }

    private Organization persistOrg(String name, Tenant tenant, String status, String type,
                                    String country, String email, String description, long createdMillis) {
        Organization o = new Organization();
        o.setName(name);
        o.setTenant(tenant);
        o.setStatus(status);
        o.setOrganizationType(type);
        o.setCountry(country);
        o.setEmail(email);
        o.setDescription(description);
        o.setCreatedBy("test");
        o.setLastModifiedBy("test");
        o.setCreatedDate(ZonedDateTime.parse("2024-01-01T00:00:00Z").plusSeconds(createdMillis));
        return em.persistAndFlush(o);
    }

    @Test
    void findByName_existingOrganization_returnsOrganization() {
        Tenant t = persistTenant("T1");
        persistOrg("Acme", t, "ACTIVE", "BUSINESS", "BE", "info@acme.com", "d", 1L);

        assertThat(repository.findByName("Acme")).isPresent().get()
                .extracting(Organization::getName).isEqualTo("Acme");
    }

    @Test
    void findByTenantId_scopesToTenant() {
        Tenant t1 = persistTenant("T1");
        Tenant t2 = persistTenant("T2");
        persistOrg("In1", t1, "ACTIVE", "BUSINESS", "BE", "a@x.com", "d", 1L);
        persistOrg("In2", t2, "ACTIVE", "BUSINESS", "BE", "b@x.com", "d", 1L);

        assertThat(repository.findByTenantId(t1.getId())).extracting(Organization::getName).containsExactly("In1");
    }

    @Test
    void findByStatus_filtersByStatus() {
        Tenant t = persistTenant("T1");
        persistOrg("ActiveOrg", t, "ACTIVE", "BUSINESS", "BE", "a@x.com", "d", 1L);
        persistOrg("SuspendedOrg", t, "SUSPENDED", "BUSINESS", "BE", "b@x.com", "d", 1L);

        assertThat(repository.findByStatus("SUSPENDED")).extracting(Organization::getName).containsExactly("SuspendedOrg");
    }

    @Test
    void findByTenantIdAndStatus_appliesBothFilters() {
        Tenant t1 = persistTenant("T1");
        Tenant t2 = persistTenant("T2");
        persistOrg("Keep", t1, "ACTIVE", "BUSINESS", "BE", "a@x.com", "d", 1L);
        persistOrg("WrongStatus", t1, "INACTIVE", "BUSINESS", "BE", "b@x.com", "d", 1L);
        persistOrg("WrongTenant", t2, "ACTIVE", "BUSINESS", "BE", "c@x.com", "d", 1L);

        assertThat(repository.findByTenantIdAndStatus(t1.getId(), "ACTIVE"))
                .extracting(Organization::getName).containsExactly("Keep");
    }

    @Test
    void findByOrganizationType_filtersByType() {
        Tenant t = persistTenant("T1");
        persistOrg("Ent", t, "ACTIVE", "ENTERPRISE", "BE", "a@x.com", "d", 1L);
        persistOrg("Small", t, "ACTIVE", "SMALL", "BE", "b@x.com", "d", 1L);

        assertThat(repository.findByOrganizationType("ENTERPRISE")).extracting(Organization::getName).containsExactly("Ent");
    }

    @Test
    void findByCountry_filtersByCountry() {
        Tenant t = persistTenant("T1");
        persistOrg("BelgianOrg", t, "ACTIVE", "BUSINESS", "BE", "a@x.com", "d", 1L);
        persistOrg("DutchOrg", t, "ACTIVE", "BUSINESS", "NL", "b@x.com", "d", 1L);

        assertThat(repository.findByCountry("NL")).extracting(Organization::getName).containsExactly("DutchOrg");
    }

    @Test
    void searchByTenantId_matchesNameOrEmailWithinTenant() {
        Tenant t1 = persistTenant("T1");
        Tenant t2 = persistTenant("T2");
        persistOrg("Payment Systems", t1, "ACTIVE", "BUSINESS", "BE", "pay@x.com", "d", 1L);
        persistOrg("Logistics", t1, "ACTIVE", "BUSINESS", "BE", "log@x.com", "d", 1L);
        persistOrg("Payment Other", t2, "ACTIVE", "BUSINESS", "BE", "pay2@x.com", "d", 1L);

        assertThat(repository.searchByTenantId(t1.getId(), "Payment"))
                .extracting(Organization::getName).containsExactly("Payment Systems");
    }

    @Test
    void search_matchesNameEmailOrDescriptionGlobally() {
        Tenant t = persistTenant("T1");
        persistOrg("Alpha", t, "ACTIVE", "BUSINESS", "BE", "alpha@x.com", "special description", 1L);
        persistOrg("Beta", t, "ACTIVE", "BUSINESS", "BE", "beta@x.com", "ordinary", 1L);

        assertThat(repository.search("special")).extracting(Organization::getName).containsExactly("Alpha");
    }

    @Test
    void findByTenantIdOrderByCreatedDateDesc_ordersNewestFirst() {
        Tenant t = persistTenant("T1");
        persistOrg("Old", t, "ACTIVE", "BUSINESS", "BE", "a@x.com", "d", 1000L);
        persistOrg("New", t, "ACTIVE", "BUSINESS", "BE", "b@x.com", "d", 3000L);
        persistOrg("Mid", t, "ACTIVE", "BUSINESS", "BE", "c@x.com", "d", 2000L);

        List<Organization> result = repository.findByTenantIdOrderByCreatedDateDesc(t.getId());

        assertThat(result).extracting(Organization::getName).containsExactly("New", "Mid", "Old");
    }

    @Test
    void countByTenantId_countsScopedOrganizations() {
        Tenant t1 = persistTenant("T1");
        Tenant t2 = persistTenant("T2");
        persistOrg("C1", t1, "ACTIVE", "BUSINESS", "BE", "a@x.com", "d", 1L);
        persistOrg("C2", t1, "ACTIVE", "BUSINESS", "BE", "b@x.com", "d", 1L);
        persistOrg("C3", t2, "ACTIVE", "BUSINESS", "BE", "c@x.com", "d", 1L);

        assertThat(repository.countByTenantId(t1.getId())).isEqualTo(2L);
    }

    @Test
    void existsByName_reflectsPresence() {
        Tenant t = persistTenant("T1");
        persistOrg("Exists", t, "ACTIVE", "BUSINESS", "BE", "e@x.com", "d", 1L);

        assertThat(repository.existsByName("Exists")).isTrue();
        assertThat(repository.existsByName("Missing")).isFalse();
    }

    @Test
    void existsByEmail_reflectsPresence() {
        Tenant t = persistTenant("T1");
        persistOrg("Emailer", t, "ACTIVE", "BUSINESS", "BE", "emailer@x.com", "d", 1L);

        assertThat(repository.existsByEmail("emailer@x.com")).isTrue();
        assertThat(repository.existsByEmail("no@x.com")).isFalse();
    }
}
