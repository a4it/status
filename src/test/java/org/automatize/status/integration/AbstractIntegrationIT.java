package org.automatize.status.integration;

import com.jayway.jsonpath.JsonPath;
import org.automatize.status.models.Organization;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.models.Tenant;
import org.automatize.status.models.User;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.automatize.status.repositories.TenantRepository;
import org.automatize.status.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared base for end-to-end integration tests ({@code *IT}).
 *
 * <p>Boots the <em>full</em> application context under the {@code test} profile
 * (H2 in PostgreSQL mode, Hibernate {@code create-drop}, Flyway/schedulers/data
 * initializer disabled, fixed {@code jwt.secret}) with {@link MockMvc} wired
 * through the <strong>real</strong> Spring Security JWT filter chain — security
 * filters are intentionally NOT disabled here, so the tests exercise genuine
 * authentication/authorization behaviour.</p>
 *
 * <p>{@link Transactional} rolls the database back after every test, so each
 * test seeds its own data in a {@code @BeforeEach} and stays independent.</p>
 *
 * <p>The {@link SpringBootTest}, {@link AutoConfigureMockMvc},
 * {@link ActiveProfiles} and {@link Transactional} annotations are inherited by
 * subclasses via Spring's TestContext framework.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class AbstractIntegrationIT {

    /** Distinct client IP per login so the shared per-IP login rate limiter never trips across the suite. */
    private static final AtomicInteger IP_COUNTER = new AtomicInteger();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected TenantRepository tenantRepository;

    @Autowired
    protected OrganizationRepository organizationRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected StatusAppRepository statusAppRepository;

    @Autowired
    protected StatusComponentRepository statusComponentRepository;

    // ---------------------------------------------------------------------
    // Seed helpers (mirror the proven approach in StatusComponentRepositoryTest)
    // ---------------------------------------------------------------------

    protected Tenant persistTenant(String name) {
        Tenant t = new Tenant();
        t.setName(name);
        t.setCreatedBy("test");
        t.setLastModifiedBy("test");
        return tenantRepository.save(t);
    }

    protected Organization persistOrganization(String name, Tenant tenant) {
        Organization o = new Organization();
        o.setName(name);
        o.setOrganizationType("BUSINESS");
        o.setTenant(tenant);
        o.setCreatedBy("test");
        o.setLastModifiedBy("test");
        return organizationRepository.save(o);
    }

    /**
     * Persists a user with a BCrypt-hashed password via the application's
     * {@link PasswordEncoder} bean, so a real login succeeds against it.
     */
    protected User persistUser(String username, String rawPassword, String role, Organization org) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRole(role);
        u.setStatus("ACTIVE");
        u.setEnabled(true);
        u.setOrganization(org);
        u.setCreatedBy("test");
        u.setLastModifiedBy("test");
        return userRepository.save(u);
    }

    protected StatusApp persistApp(String name, String slug, Tenant tenant, Organization org) {
        StatusApp a = new StatusApp();
        a.setName(name);
        a.setSlug(slug);
        a.setTenant(tenant);
        a.setOrganization(org);
        a.setCreatedBy("test");
        a.setLastModifiedBy("test");
        return statusAppRepository.save(a);
    }

    protected StatusComponent persistComponent(String name, StatusApp app) {
        StatusComponent c = new StatusComponent();
        c.setName(name);
        c.setApp(app);
        c.setCreatedBy("test");
        c.setLastModifiedBy("test");
        return statusComponentRepository.save(c);
    }

    // ---------------------------------------------------------------------
    // HTTP helpers
    // ---------------------------------------------------------------------

    protected static String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    /**
     * Assigns a unique remote address to each request. The login endpoint rate
     * limits per client IP (10/min) using a singleton bean shared across the
     * whole test context, so distinct IPs keep tests fully decoupled.
     */
    protected static RequestPostProcessor uniqueIp() {
        return request -> {
            request.setRemoteAddr("10.77." + (IP_COUNTER.incrementAndGet() & 0xFF) + ".1");
            return request;
        };
    }

    /**
     * Performs a real login and returns the {@code accessToken} from the
     * {@link org.automatize.status.api.response.AuthResponse} JSON.
     */
    protected String obtainAccessToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password))
                        .with(uniqueIp()))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
