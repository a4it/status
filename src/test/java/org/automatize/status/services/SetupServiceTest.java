package org.automatize.status.services;

import org.automatize.status.api.request.SetupAdminRequest;
import org.automatize.status.api.request.SetupOrganizationRequest;
import org.automatize.status.api.request.SetupTenantRequest;
import org.automatize.status.api.request.TenantRequest;
import org.automatize.status.api.request.OrganizationRequest;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.api.response.SetupStatusResponse;
import org.automatize.status.config.SetupFilter;
import org.automatize.status.models.Organization;
import org.automatize.status.models.Tenant;
import org.automatize.status.models.User;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.TenantRepository;
import org.automatize.status.repositories.UserRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SetupService}.
 *
 * <p>Dependencies are mocked with Mockito. The {@code @Value} config fields
 * ({@code setupCompleted}, {@code datasourceUrl}, {@code datasourceUsername})
 * are injected via {@link ReflectionTestUtils}.</p>
 *
 * <p>Methods performing real disk / network I/O are intentionally not
 * exercised here (see the note at the end of the file): {@code markSetupComplete},
 * {@code saveProperties}, {@code getGroupedProperties} and {@code testConnection}
 * all read/write {@code application.properties} on disk or open a live JDBC
 * connection via {@code DriverManager}, which is out of scope for pure unit tests.</p>
 */
@ExtendWith(MockitoExtension.class)
class SetupServiceTest {

    private static final String SETUP_COMPLETED_FIELD = "setupCompleted";
    private static final String FLYWAY_FIELD = "flyway";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_EMAIL = "admin@acme.test";
    private static final String ENCODED_PASSWORD = "ENC(supersecret)";
    private static final String RAW_PASSWORD = "supersecret";

    @Mock
    private TenantService tenantService;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private AuthService authService;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DataSource dataSource;

    @Mock
    private Flyway flyway;

    @Mock
    private SetupFilter setupFilter;

    @InjectMocks
    private SetupService setupService;

    /**
     * Injects the {@code @Value} config fields (setup flag, datasource URL and
     * username) into the service under test before each test.
     */
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(setupService, SETUP_COMPLETED_FIELD, false);
        ReflectionTestUtils.setField(setupService, "datasourceUrl", "jdbc:postgresql://localhost:5432/status");
        ReflectionTestUtils.setField(setupService, "datasourceUsername", "status_user");
    }

    // -------------------------------------------------------------------------
    // isSetupAlreadyComplete
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@code isSetupAlreadyComplete} returns {@code false} when the
     * setup-completed flag is unset.
     */
    @Test
    void isSetupAlreadyComplete_whenFlagFalse_returnsFalse() {
        ReflectionTestUtils.setField(setupService, SETUP_COMPLETED_FIELD, false);

        assertThat(setupService.isSetupAlreadyComplete()).isFalse();
    }

    /**
     * Verifies that {@code isSetupAlreadyComplete} returns {@code true} when the
     * setup-completed flag is set.
     */
    @Test
    void isSetupAlreadyComplete_whenFlagTrue_returnsTrue() {
        ReflectionTestUtils.setField(setupService, SETUP_COMPLETED_FIELD, true);

        assertThat(setupService.isSetupAlreadyComplete()).isTrue();
    }

    // -------------------------------------------------------------------------
    // getStatus
    // -------------------------------------------------------------------------

    /**
     * Verifies that with a valid DB connection, a resolvable Flyway version and no
     * tenants, the status reports a connected DB, the Flyway version and an empty
     * tenant/organization state.
     *
     * @throws Exception if the mocked connection setup throws
     */
    @Test
    void getStatus_whenDbConnectedAndNoTenants_reportsHealthyEmptyState() throws Exception {
        Connection conn = org.mockito.Mockito.mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.isValid(2)).thenReturn(true);

        MigrationInfoService infoService = org.mockito.Mockito.mock(MigrationInfoService.class);
        MigrationInfo current = org.mockito.Mockito.mock(MigrationInfo.class);
        when(flyway.info()).thenReturn(infoService);
        when(infoService.current()).thenReturn(current);
        when(current.getVersion()).thenReturn(MigrationVersion.fromVersion("42"));

        when(tenantRepository.findAll()).thenReturn(Collections.emptyList());

        SetupStatusResponse status = setupService.getStatus();

        assertThat(status.isSetupCompleted()).isFalse();
        assertThat(status.isDbConnected()).isTrue();
        assertThat(status.getDbError()).isNull();
        assertThat(status.getDbUrl()).isEqualTo("jdbc:postgresql://localhost:5432/status");
        assertThat(status.getDbUsername()).isEqualTo("status_user");
        assertThat(status.getFlywayVersion()).isEqualTo("42");
        assertThat(status.isTenantCreated()).isFalse();
        assertThat(status.getTenantId()).isNull();
        assertThat(status.isOrganizationCreated()).isFalse();
    }

    /**
     * Verifies that when acquiring a DB connection throws, the status reports the DB
     * as not connected, captures the error message and defaults the Flyway version
     * to "unknown".
     *
     * @throws Exception if the mocked connection setup throws
     */
    @Test
    void getStatus_whenDbConnectionThrows_reportsDbErrorAndNotConnected() throws Exception {
        when(dataSource.getConnection()).thenThrow(new java.sql.SQLException("connection refused"));
        when(tenantRepository.findAll()).thenReturn(Collections.emptyList());

        SetupStatusResponse status = setupService.getStatus();

        assertThat(status.isDbConnected()).isFalse();
        assertThat(status.getDbError()).isEqualTo("connection refused");
        // flyway mock returns null info() by default -> caught, "unknown"
        assertThat(status.getFlywayVersion()).isEqualTo("unknown");
    }

    /**
     * Verifies that a null Flyway bean results in a reported version of "unknown".
     *
     * @throws Exception if the mocked connection setup throws
     */
    @Test
    void getStatus_whenFlywayNull_reportsUnknownVersion() throws Exception {
        Connection conn = org.mockito.Mockito.mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.isValid(2)).thenReturn(true);
        ReflectionTestUtils.setField(setupService, FLYWAY_FIELD, null);
        when(tenantRepository.findAll()).thenReturn(Collections.emptyList());

        SetupStatusResponse status = setupService.getStatus();

        assertThat(status.getFlywayVersion()).isEqualTo("unknown");
    }

    /**
     * Verifies that when Flyway reports no current migration the status reports a
     * version of "none".
     *
     * @throws Exception if the mocked connection setup throws
     */
    @Test
    void getStatus_whenFlywayHasNoCurrentMigration_reportsNone() throws Exception {
        Connection conn = org.mockito.Mockito.mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.isValid(2)).thenReturn(true);

        MigrationInfoService infoService = org.mockito.Mockito.mock(MigrationInfoService.class);
        when(flyway.info()).thenReturn(infoService);
        when(infoService.current()).thenReturn(null);

        when(tenantRepository.findAll()).thenReturn(Collections.emptyList());

        SetupStatusResponse status = setupService.getStatus();

        assertThat(status.getFlywayVersion()).isEqualTo("none");
    }

    /**
     * Verifies that when a tenant and an organization already exist the status flags
     * both as created and exposes their ids.
     *
     * @throws Exception if the mocked connection setup throws
     */
    @Test
    void getStatus_whenTenantAndOrganizationExist_reportsBothCreatedWithIds() throws Exception {
        Connection conn = org.mockito.Mockito.mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.isValid(2)).thenReturn(true);
        ReflectionTestUtils.setField(setupService, FLYWAY_FIELD, null);

        UUID tenantId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        Organization org = new Organization();
        org.setId(orgId);

        when(tenantRepository.findAll()).thenReturn(List.of(tenant));
        when(organizationRepository.findAll()).thenReturn(List.of(org));

        SetupStatusResponse status = setupService.getStatus();

        assertThat(status.isTenantCreated()).isTrue();
        assertThat(status.getTenantId()).isEqualTo(tenantId);
        assertThat(status.isOrganizationCreated()).isTrue();
        assertThat(status.getOrganizationId()).isEqualTo(orgId);
    }

    /**
     * Verifies that when a tenant exists but no organization does, the status flags
     * the tenant as created and the organization as not created.
     *
     * @throws Exception if the mocked connection setup throws
     */
    @Test
    void getStatus_whenTenantExistsButNoOrganization_reportsTenantOnly() throws Exception {
        Connection conn = org.mockito.Mockito.mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.isValid(2)).thenReturn(true);
        ReflectionTestUtils.setField(setupService, FLYWAY_FIELD, null);

        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        when(tenantRepository.findAll()).thenReturn(List.of(tenant));
        when(organizationRepository.findAll()).thenReturn(Collections.emptyList());

        SetupStatusResponse status = setupService.getStatus();

        assertThat(status.isTenantCreated()).isTrue();
        assertThat(status.getTenantId()).isEqualTo(tenantId);
        assertThat(status.isOrganizationCreated()).isFalse();
        assertThat(status.getOrganizationId()).isNull();
    }

    // -------------------------------------------------------------------------
    // createTenant
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@code createTenant} delegates to {@link TenantService} with a
     * request carrying the supplied name and an active flag, returning the created
     * tenant.
     */
    @Test
    void createTenant_always_delegatesToTenantServiceWithActiveTenant() {
        SetupTenantRequest request = new SetupTenantRequest();
        request.setName("Acme");

        Tenant created = new Tenant();
        created.setId(UUID.randomUUID());
        when(tenantService.createTenant(any(TenantRequest.class))).thenReturn(created);

        Tenant result = setupService.createTenant(request);

        assertThat(result).isSameAs(created);

        ArgumentCaptor<TenantRequest> captor = ArgumentCaptor.forClass(TenantRequest.class);
        verify(tenantService).createTenant(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Acme");
        assertThat(captor.getValue().getIsActive()).isTrue();
    }

    // -------------------------------------------------------------------------
    // createOrganization
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@code createOrganization} delegates to
     * {@link OrganizationService} with a request carrying the supplied fields and an
     * ACTIVE status, returning the created organization.
     */
    @Test
    void createOrganization_always_delegatesToOrganizationServiceWithActiveStatus() {
        UUID tenantId = UUID.randomUUID();
        SetupOrganizationRequest request = new SetupOrganizationRequest();
        request.setName("Acme Org");
        request.setEmail("org@acme.test");
        request.setOrganizationType("ENTERPRISE");
        request.setTenantId(tenantId);

        Organization created = new Organization();
        created.setId(UUID.randomUUID());
        when(organizationService.createOrganization(any(OrganizationRequest.class))).thenReturn(created);

        Organization result = setupService.createOrganization(request);

        assertThat(result).isSameAs(created);

        ArgumentCaptor<OrganizationRequest> captor = ArgumentCaptor.forClass(OrganizationRequest.class);
        verify(organizationService).createOrganization(captor.capture());
        OrganizationRequest passed = captor.getValue();
        assertThat(passed.getName()).isEqualTo("Acme Org");
        assertThat(passed.getEmail()).isEqualTo("org@acme.test");
        assertThat(passed.getOrganizationType()).isEqualTo("ENTERPRISE");
        assertThat(passed.getTenantId()).isEqualTo(tenantId);
        assertThat(passed.getStatus()).isEqualTo("ACTIVE");
    }

    // -------------------------------------------------------------------------
    // createAdmin
    // -------------------------------------------------------------------------

    /**
     * Verifies that when the username is already taken {@code createAdmin} returns a
     * failure response and never persists a user.
     */
    @Test
    void createAdmin_whenUsernameTaken_returnsFailureAndDoesNotSave() {
        SetupAdminRequest request = adminRequest(UUID.randomUUID());
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        MessageResponse response = setupService.createAdmin(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Username is already taken!");
        verify(userRepository, never()).save(any());
    }

    /**
     * Verifies that when the email is already in use {@code createAdmin} returns a
     * failure response and never persists a user.
     */
    @Test
    void createAdmin_whenEmailInUse_returnsFailureAndDoesNotSave() {
        SetupAdminRequest request = adminRequest(UUID.randomUUID());
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@acme.test")).thenReturn(true);

        MessageResponse response = setupService.createAdmin(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Email is already in use!");
        verify(userRepository, never()).save(any());
    }

    /**
     * Verifies that a valid request linked to an existing organization persists a
     * SUPERADMIN user with an encoded password, the correct profile fields and the
     * resolved organization.
     */
    @Test
    void createAdmin_whenValidWithOrganization_savesSuperadminWithEncodedPassword() {
        UUID orgId = UUID.randomUUID();
        SetupAdminRequest request = adminRequest(orgId);
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@acme.test")).thenReturn(false);
        when(passwordEncoder.encode("supersecret")).thenReturn("ENC(supersecret)");
        Organization org = new Organization();
        org.setId(orgId);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = setupService.createAdmin(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("User registered successfully!");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getPassword()).isEqualTo("ENC(supersecret)");
        assertThat(saved.getEmail()).isEqualTo("admin@acme.test");
        assertThat(saved.getFullName()).isEqualTo("Site Admin");
        assertThat(saved.getRole()).isEqualTo("SUPERADMIN");
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getCreatedBy()).isEqualTo("admin");
        assertThat(saved.getLastModifiedBy()).isEqualTo("admin");
        assertThat(saved.getOrganization()).isSameAs(org);
    }

    /**
     * Verifies that when no organization id is supplied the admin is saved without an
     * organization and no organization lookup is performed.
     */
    @Test
    void createAdmin_whenOrganizationIdNull_savesAdminWithoutOrganizationLookup() {
        SetupAdminRequest request = adminRequest(null);
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@acme.test")).thenReturn(false);
        when(passwordEncoder.encode("supersecret")).thenReturn("ENC(supersecret)");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = setupService.createAdmin(request);

        assertThat(response.isSuccess()).isTrue();
        verify(organizationRepository, never()).findById(any());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getOrganization()).isNull();
    }

    /**
     * Verifies that when the supplied organization id does not resolve the admin is
     * still saved, but with a null organization.
     */
    @Test
    void createAdmin_whenOrganizationNotFound_savesAdminWithNullOrganization() {
        UUID orgId = UUID.randomUUID();
        SetupAdminRequest request = adminRequest(orgId);
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@acme.test")).thenReturn(false);
        when(passwordEncoder.encode("supersecret")).thenReturn("ENC(supersecret)");
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = setupService.createAdmin(request);

        assertThat(response.isSuccess()).isTrue();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getOrganization()).isNull();
    }

    /**
     * Builds a {@link SetupAdminRequest} fixture with fixed credentials and the given
     * organization id.
     *
     * @param organizationId the organization id to associate (may be {@code null})
     * @return a populated {@link SetupAdminRequest} instance
     */
    private SetupAdminRequest adminRequest(UUID organizationId) {
        SetupAdminRequest request = new SetupAdminRequest();
        request.setUsername("admin");
        request.setPassword("supersecret");
        request.setEmail("admin@acme.test");
        request.setFullName("Site Admin");
        request.setOrganizationId(organizationId);
        return request;
    }
}
