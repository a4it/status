package org.automatize.status.repositories;

import org.automatize.status.models.Organization;
import org.automatize.status.models.Tenant;
import org.automatize.status.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @DataJpaTest} coverage for {@link UserRepository}'s custom derived and
 * JPQL queries against H2 (PostgreSQL compatibility mode). Focus is
 * authentication lookups, organization/tenant scoping, search, counts and
 * existence checks.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest extends AbstractRepositoryTest {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String SEARCH_TERM = "searchme";

    @Autowired
    private UserRepository repository;

    private User persistUser(String username, String email, Organization org,
                             String role, String status, boolean enabled) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword("secret");
        u.setFullName(username + " Full");
        u.setOrganization(org);
        u.setRole(role);
        u.setStatus(status);
        u.setEnabled(enabled);
        u.setCreatedBy("test");
        u.setLastModifiedBy("test");
        return em.persistAndFlush(u);
    }

    @Test
    void findByUsername_existingUser_returnsUser() {
        Tenant t = persistTenant("T1");
        Organization o = persistOrganization("O1", t);
        persistUser("alice", "alice@x.com", o, ROLE_ADMIN, STATUS_ACTIVE, true);

        Optional<User> result = repository.findByUsername("alice");

        assertThat(result).isPresent().get().extracting(User::getEmail).isEqualTo("alice@x.com");
    }

    @Test
    void findByUsername_missingUser_returnsEmpty() {
        assertThat(repository.findByUsername("nobody")).isEmpty();
    }

    @Test
    void findByEmail_existingUser_returnsUser() {
        Tenant t = persistTenant("T1");
        Organization o = persistOrganization("O1", t);
        persistUser("bob", "bob@x.com", o, "USER", STATUS_ACTIVE, true);

        assertThat(repository.findByEmail("bob@x.com")).isPresent().get()
                .extracting(User::getUsername).isEqualTo("bob");
    }

    @Test
    void findByUsernameOrEmail_matchesEitherCredential_returnsUser() {
        Tenant t = persistTenant("T1");
        Organization o = persistOrganization("O1", t);
        persistUser("carol", "carol@x.com", o, "USER", STATUS_ACTIVE, true);

        assertThat(repository.findByUsernameOrEmail("carol", "none@x.com")).isPresent();
        assertThat(repository.findByUsernameOrEmail("none", "carol@x.com")).isPresent();
        assertThat(repository.findByUsernameOrEmail("none", "none@x.com")).isEmpty();
    }

    @Test
    void findByOrganizationId_scopesToOrganization() {
        Tenant t = persistTenant("T1");
        Organization o1 = persistOrganization("O1", t);
        Organization o2 = persistOrganization("O2", t);
        persistUser("u1", "u1@x.com", o1, "USER", STATUS_ACTIVE, true);
        persistUser("u2", "u2@x.com", o2, "USER", STATUS_ACTIVE, true);

        List<User> result = repository.findByOrganizationId(o1.getId());

        assertThat(result).extracting(User::getUsername).containsExactly("u1");
    }

    @Test
    void findByEnabled_filtersByEnabledFlag() {
        Tenant t = persistTenant("T1");
        Organization o = persistOrganization("O1", t);
        persistUser("on", "on@x.com", o, "USER", STATUS_ACTIVE, true);
        persistUser("off", "off@x.com", o, "USER", "INACTIVE", false);

        assertThat(repository.findByEnabled(true)).extracting(User::getUsername).containsExactly("on");
        assertThat(repository.findByEnabled(false)).extracting(User::getUsername).containsExactly("off");
    }

    @Test
    void findByRole_filtersByRole() {
        Tenant t = persistTenant("T1");
        Organization o = persistOrganization("O1", t);
        persistUser("admin", "admin@x.com", o, ROLE_ADMIN, STATUS_ACTIVE, true);
        persistUser("user", "user@x.com", o, "USER", STATUS_ACTIVE, true);

        assertThat(repository.findByRole(ROLE_ADMIN)).extracting(User::getUsername).containsExactly("admin");
    }

    @Test
    void findByStatus_filtersByStatus() {
        Tenant t = persistTenant("T1");
        Organization o = persistOrganization("O1", t);
        persistUser("act", "act@x.com", o, "USER", STATUS_ACTIVE, true);
        persistUser("pend", "pend@x.com", o, "USER", "PENDING", true);

        assertThat(repository.findByStatus("PENDING")).extracting(User::getUsername).containsExactly("pend");
    }

    @Test
    void findByOrganizationIdAndEnabled_appliesBothFilters() {
        Tenant t = persistTenant("T1");
        Organization o = persistOrganization("O1", t);
        persistUser("a", "a@x.com", o, "USER", STATUS_ACTIVE, true);
        persistUser("b", "b@x.com", o, "USER", "INACTIVE", false);

        assertThat(repository.findByOrganizationIdAndEnabled(o.getId(), true))
                .extracting(User::getUsername).containsExactly("a");
    }

    @Test
    void findByOrganizationIdAndRole_appliesBothFilters() {
        Tenant t = persistTenant("T1");
        Organization o = persistOrganization("O1", t);
        persistUser("adm", "adm@x.com", o, ROLE_ADMIN, STATUS_ACTIVE, true);
        persistUser("usr", "usr@x.com", o, "USER", STATUS_ACTIVE, true);

        assertThat(repository.findByOrganizationIdAndRole(o.getId(), ROLE_ADMIN))
                .extracting(User::getUsername).containsExactly("adm");
    }

    @Test
    void findByTenantId_walksOrganizationTenantRelation() {
        Tenant t1 = persistTenant("T1");
        Tenant t2 = persistTenant("T2");
        Organization o1 = persistOrganization("O1", t1);
        Organization o2 = persistOrganization("O2", t2);
        persistUser("in", "in@x.com", o1, "USER", STATUS_ACTIVE, true);
        persistUser("out", "out@x.com", o2, "USER", STATUS_ACTIVE, true);

        assertThat(repository.findByTenantId(t1.getId())).extracting(User::getUsername).containsExactly("in");
    }

    @Test
    void searchByOrganizationId_matchesUsernameEmailOrFullName() {
        Tenant t = persistTenant("T1");
        Organization o = persistOrganization("O1", t);
        Organization other = persistOrganization("O2", t);
        persistUser(SEARCH_TERM, "searchme@x.com", o, "USER", STATUS_ACTIVE, true);
        persistUser("nomatch", "nomatch@x.com", o, "USER", STATUS_ACTIVE, true);
        persistUser("searchme2", "searchme2@x.com", other, "USER", STATUS_ACTIVE, true);

        List<User> result = repository.searchByOrganizationId(o.getId(), SEARCH_TERM);

        assertThat(result).extracting(User::getUsername).containsExactly(SEARCH_TERM);
    }

    @Test
    void search_matchesGloballyAcrossFields() {
        Tenant t = persistTenant("T1");
        Organization o = persistOrganization("O1", t);
        persistUser("zeus", "zeus@x.com", o, "USER", STATUS_ACTIVE, true);
        persistUser("hera", "hera@x.com", o, "USER", STATUS_ACTIVE, true);

        assertThat(repository.search("zeus")).extracting(User::getUsername).containsExactly("zeus");
    }

    @Test
    void countByOrganizationId_countsScopedUsers() {
        Tenant t = persistTenant("T1");
        Organization o1 = persistOrganization("O1", t);
        Organization o2 = persistOrganization("O2", t);
        persistUser("c1", "c1@x.com", o1, "USER", STATUS_ACTIVE, true);
        persistUser("c2", "c2@x.com", o1, "USER", STATUS_ACTIVE, true);
        persistUser("c3", "c3@x.com", o2, "USER", STATUS_ACTIVE, true);

        assertThat(repository.countByOrganizationId(o1.getId())).isEqualTo(2L);
    }

    @Test
    void countByTenantId_countsUsersInTenant() {
        Tenant t1 = persistTenant("T1");
        Tenant t2 = persistTenant("T2");
        Organization o1 = persistOrganization("O1", t1);
        Organization o2 = persistOrganization("O2", t2);
        persistUser("t1a", "t1a@x.com", o1, "USER", STATUS_ACTIVE, true);
        persistUser("t1b", "t1b@x.com", o1, "USER", STATUS_ACTIVE, true);
        persistUser("t2a", "t2a@x.com", o2, "USER", STATUS_ACTIVE, true);

        assertThat(repository.countByTenantId(t1.getId())).isEqualTo(2L);
    }

    @Test
    void existsByUsername_reflectsPresence() {
        Tenant t = persistTenant("T1");
        Organization o = persistOrganization("O1", t);
        persistUser("exists", "exists@x.com", o, "USER", STATUS_ACTIVE, true);

        assertThat(repository.existsByUsername("exists")).isTrue();
        assertThat(repository.existsByUsername("missing")).isFalse();
    }

    @Test
    void existsByEmail_reflectsPresence() {
        Tenant t = persistTenant("T1");
        Organization o = persistOrganization("O1", t);
        persistUser("emailer", "emailer@x.com", o, "USER", STATUS_ACTIVE, true);

        assertThat(repository.existsByEmail("emailer@x.com")).isTrue();
        assertThat(repository.existsByEmail("no@x.com")).isFalse();
    }
}
