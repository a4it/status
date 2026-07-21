package org.automatize.status.repositories;

import org.automatize.status.models.NotificationSubscriber;
import org.automatize.status.models.Organization;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @DataJpaTest} coverage for {@link NotificationSubscriberRepository}'s custom
 * derived and JPQL queries against H2 (PostgreSQL compatibility mode). Focus is
 * per-app scoping, active/verified filtering, verification-token lookup, search,
 * counts, existence and bulk delete.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class NotificationSubscriberRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private NotificationSubscriberRepository repository;

    private StatusApp app;
    private StatusApp otherApp;

    @BeforeEach
    void setUp() {
        Tenant tenant = persistTenant("Tenant A");
        Organization org = persistOrganization("Org A", tenant);
        app = persistApp("App A", "app-a", tenant, org);
        otherApp = persistApp("App B", "app-b", tenant, org);
    }

    private Tenant persistTenant(String name) {
        Tenant t = new Tenant();
        t.setName(name);
        t.setCreatedBy("test");
        t.setLastModifiedBy("test");
        return em.persistAndFlush(t);
    }

    private Organization persistOrganization(String name, Tenant tenant) {
        Organization o = new Organization();
        o.setName(name);
        o.setOrganizationType("BUSINESS");
        o.setTenant(tenant);
        o.setCreatedBy("test");
        o.setLastModifiedBy("test");
        return em.persistAndFlush(o);
    }

    private StatusApp persistApp(String name, String slug, Tenant tenant, Organization org) {
        StatusApp a = new StatusApp();
        a.setName(name);
        a.setSlug(slug);
        a.setTenant(tenant);
        a.setOrganization(org);
        a.setCreatedBy("test");
        a.setLastModifiedBy("test");
        return em.persistAndFlush(a);
    }

    private NotificationSubscriber persistSubscriber(StatusApp app, String email, String name,
                                                     boolean active, boolean verified, String token) {
        NotificationSubscriber s = new NotificationSubscriber();
        s.setApp(app);
        s.setEmail(email);
        s.setName(name);
        s.setIsActive(active);
        s.setIsVerified(verified);
        s.setVerificationToken(token);
        s.setCreatedBy("test");
        s.setLastModifiedBy("test");
        return em.persistAndFlush(s);
    }

    @Test
    void findByAppId_scopesToApp() {
        persistSubscriber(app, "a@x.com", "A", true, true, null);
        persistSubscriber(otherApp, "b@x.com", "B", true, true, null);

        assertThat(repository.findByAppId(app.getId())).extracting(NotificationSubscriber::getEmail)
                .containsExactly("a@x.com");
    }

    @Test
    void findByAppIdAndIsActive_filtersByActiveFlag() {
        persistSubscriber(app, "on@x.com", "On", true, true, null);
        persistSubscriber(app, "off@x.com", "Off", false, true, null);

        assertThat(repository.findByAppIdAndIsActive(app.getId(), true))
                .extracting(NotificationSubscriber::getEmail).containsExactly("on@x.com");
    }

    @Test
    void findActiveVerifiedByAppId_returnsOnlyActiveAndVerified() {
        persistSubscriber(app, "good@x.com", "Good", true, true, null);
        persistSubscriber(app, "unverified@x.com", "U", true, false, null);
        persistSubscriber(app, "inactive@x.com", "I", false, true, null);
        persistSubscriber(otherApp, "otherapp@x.com", "O", true, true, null);

        assertThat(repository.findActiveVerifiedByAppId(app.getId()))
                .extracting(NotificationSubscriber::getEmail).containsExactly("good@x.com");
    }

    @Test
    void findByAppIdAndEmail_returnsMatchingSubscriber() {
        persistSubscriber(app, "find@x.com", "F", true, true, null);

        assertThat(repository.findByAppIdAndEmail(app.getId(), "find@x.com")).isPresent();
        assertThat(repository.findByAppIdAndEmail(app.getId(), "none@x.com")).isEmpty();
    }

    @Test
    void findByVerificationToken_returnsMatchingSubscriber() {
        persistSubscriber(app, "tok@x.com", "T", true, false, "token-123");

        assertThat(repository.findByVerificationToken("token-123")).isPresent().get()
                .extracting(NotificationSubscriber::getEmail).isEqualTo("tok@x.com");
        assertThat(repository.findByVerificationToken("nope")).isEmpty();
    }

    @Test
    void existsByAppIdAndEmail_reflectsPresence() {
        persistSubscriber(app, "exists@x.com", "E", true, true, null);

        assertThat(repository.existsByAppIdAndEmail(app.getId(), "exists@x.com")).isTrue();
        assertThat(repository.existsByAppIdAndEmail(app.getId(), "missing@x.com")).isFalse();
        assertThat(repository.existsByAppIdAndEmail(otherApp.getId(), "exists@x.com")).isFalse();
    }

    @Test
    void countByAppId_countsAllSubscribersForApp() {
        persistSubscriber(app, "c1@x.com", "C1", true, true, null);
        persistSubscriber(app, "c2@x.com", "C2", false, false, null);
        persistSubscriber(otherApp, "c3@x.com", "C3", true, true, null);

        assertThat(repository.countByAppId(app.getId())).isEqualTo(2L);
    }

    @Test
    void countActiveByAppId_countsOnlyActiveSubscribers() {
        persistSubscriber(app, "a1@x.com", "A1", true, true, null);
        persistSubscriber(app, "a2@x.com", "A2", true, false, null);
        persistSubscriber(app, "a3@x.com", "A3", false, true, null);

        assertThat(repository.countActiveByAppId(app.getId())).isEqualTo(2L);
    }

    @Test
    void searchByAppId_matchesEmailOrNameWithinApp() {
        persistSubscriber(app, "search@x.com", "Findable", true, true, null);
        persistSubscriber(app, "other@x.com", "Nomatch", true, true, null);
        persistSubscriber(otherApp, "search2@x.com", "Findable", true, true, null);

        assertThat(repository.searchByAppId(app.getId(), "Findable"))
                .extracting(NotificationSubscriber::getEmail).containsExactly("search@x.com");
    }

    @Test
    void deleteByAppId_removesAllSubscribersForApp() {
        persistSubscriber(app, "d1@x.com", "D1", true, true, null);
        persistSubscriber(app, "d2@x.com", "D2", true, true, null);
        persistSubscriber(otherApp, "keep@x.com", "Keep", true, true, null);

        repository.deleteByAppId(app.getId());
        em.flush();
        em.clear();

        assertThat(repository.findByAppId(app.getId())).isEmpty();
        assertThat(repository.findByAppId(otherApp.getId())).extracting(NotificationSubscriber::getEmail)
                .containsExactly("keep@x.com");
    }
}
