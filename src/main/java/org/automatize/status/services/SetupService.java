package org.automatize.status.services;

import org.automatize.status.api.request.*;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.api.response.SetupStatusResponse;
import org.automatize.status.config.SetupFilter;
import org.automatize.status.exceptions.SetupException;
import org.automatize.status.models.Organization;
import org.automatize.status.models.Tenant;
import org.automatize.status.models.User;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.TenantRepository;
import org.automatize.status.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/**
 * <p>
 * Service responsible for driving the first-run setup wizard.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Report setup status including database connectivity and migration state</li>
 *   <li>Create the initial tenant, organization, and SUPERADMIN admin user</li>
 *   <li>Test database connections and persist configuration to the properties file</li>
 *   <li>Expose and edit grouped application properties, masking sensitive values</li>
 *   <li>Mark the wizard as complete and lift the setup filter</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
@Service
public class SetupService {

    private static final Logger logger = LoggerFactory.getLogger(SetupService.class);

    private static final String PROP_DATASOURCE_URL = "spring.datasource.url";
    private static final String PROP_DATASOURCE_USERNAME = "spring.datasource.username";
    private static final String PROP_DATASOURCE_PASSWORD = "spring.datasource.password";
    private static final String PROP_JWT_SECRET = "jwt.secret";
    private static final String PROP_MAIL_PASSWORD = "spring.mail.password";

    @Autowired
    private TenantService tenantService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private AuthService authService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataSource dataSource;

    @Autowired(required = false)
    private Flyway flyway;

    @Autowired
    private SetupFilter setupFilter;

    @Value("${app.setup.completed:false}")
    private boolean setupCompleted;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    /**
     * Indicates whether the setup wizard has already been completed.
     *
     * @return true if setup is complete, false otherwise
     */
    public boolean isSetupAlreadyComplete() {
        return setupCompleted;
    }

    // -------------------------------------------------------------------------
    // Status
    // -------------------------------------------------------------------------

    /**
     * Builds the current setup status, including database connectivity, datasource config,
     * Flyway migration version, and whether the initial tenant/organization already exist.
     *
     * @return a SetupStatusResponse describing the current setup state
     */
    public SetupStatusResponse getStatus() {
        SetupStatusResponse status = new SetupStatusResponse();
        status.setSetupCompleted(setupCompleted);

        // DB connection check
        try (var conn = dataSource.getConnection()) {
            status.setDbConnected(conn.isValid(2));
        } catch (Exception e) {
            logger.warn("DB connection check failed during setup status: {}", e.getMessage(), e);
            status.setDbConnected(false);
            status.setDbError(e.getMessage());
        }

        // Expose current datasource config for pre-populating the DB step
        status.setDbUrl(datasourceUrl);
        status.setDbUsername(datasourceUsername);

        // Flyway migration version
        try {
            // Report the current migration version when Flyway is available
            if (flyway != null) {
                MigrationInfo current = flyway.info().current();
                status.setFlywayVersion(current != null ? current.getVersion().getVersion() : "none");
            } else {
                status.setFlywayVersion("unknown");
            }
        } catch (Exception e) {
            logger.warn("Could not read Flyway migration info during setup status: {}", e.getMessage(), e);
            status.setFlywayVersion("unknown");
        }

        // Resumability: check if tenant/org already exist
        List<Tenant> tenants = tenantRepository.findAll();
        // A pre-existing tenant means the wizard can resume rather than start fresh
        if (!tenants.isEmpty()) {
            status.setTenantCreated(true);
            status.setTenantId(tenants.get(0).getId());

            List<Organization> orgs = organizationRepository.findAll();
            // Mark the organization step complete when one already exists
            if (!orgs.isEmpty()) {
                status.setOrganizationCreated(true);
                status.setOrganizationId(orgs.get(0).getId());
            }
        }

        return status;
    }

    // -------------------------------------------------------------------------
    // Setup steps
    // -------------------------------------------------------------------------

    /**
     * Creates the initial tenant during setup.
     *
     * @param request the setup tenant request containing the tenant name
     * @return the created Tenant entity
     */
    @Transactional
    public Tenant createTenant(SetupTenantRequest request) {
        TenantRequest tenantRequest = new TenantRequest();
        tenantRequest.setName(request.getName());
        tenantRequest.setIsActive(true);
        return tenantService.createTenant(tenantRequest);
    }

    /**
     * Creates the initial organization during setup.
     *
     * @param request the setup organization request containing organization details and tenant id
     * @return the created Organization entity
     */
    @Transactional
    public Organization createOrganization(SetupOrganizationRequest request) {
        OrganizationRequest orgRequest = new OrganizationRequest();
        orgRequest.setName(request.getName());
        orgRequest.setEmail(request.getEmail());
        orgRequest.setOrganizationType(request.getOrganizationType());
        orgRequest.setTenantId(request.getTenantId());
        orgRequest.setStatus("ACTIVE");
        return organizationService.createOrganization(orgRequest);
    }

    /**
     * Creates the initial SUPERADMIN admin user during setup.
     * <p>
     * Bypasses the public registration path so it can assign the SUPERADMIN role directly,
     * and rejects usernames or emails that are already taken.
     * </p>
     *
     * @param request the setup admin request containing credentials and optional organization id
     * @return a MessageResponse indicating success or the reason for failure
     */
    @Transactional
    public MessageResponse createAdmin(SetupAdminRequest request) {
        // HIGH-03: setup admin creation bypasses registerUser() to assign SUPERADMIN role
        // directly, without going through the public registration path which now hard-codes
        // the role to "USER".
        // Reject a username that is already taken
        if (userRepository.existsByUsername(request.getUsername())) {
            return new MessageResponse("Username is already taken!", false);
        }
        // Reject an email that is already in use
        if (userRepository.existsByEmail(request.getEmail())) {
            return new MessageResponse("Email is already in use!", false);
        }
        User admin = new User();
        admin.setUsername(request.getUsername());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setEmail(request.getEmail());
        admin.setFullName(request.getFullName());
        admin.setRole("SUPERADMIN");
        admin.setEnabled(true);
        admin.setStatus("ACTIVE");
        admin.setCreatedBy(request.getUsername());
        admin.setLastModifiedBy(request.getUsername());
        // Associate the admin with the organization when one was provided
        if (request.getOrganizationId() != null) {
            organizationRepository.findById(request.getOrganizationId())
                    .ifPresent(admin::setOrganization);
        }
        userRepository.save(admin);
        return new MessageResponse("User registered successfully!", true);
    }

    // -------------------------------------------------------------------------
    // Database connection test
    // -------------------------------------------------------------------------

    /**
     * Tests a database connection using the supplied credentials and, on success, optionally
     * persists the datasource settings to the properties file.
     *
     * @param request the connection test request containing url, username, password, and save flag
     * @return a MessageResponse indicating whether the connection succeeded and whether settings were saved
     */
    public MessageResponse testConnection(SetupTestConnectionRequest request) {
        String password = request.getPassword() != null ? request.getPassword() : "";
        try (Connection conn = DriverManager.getConnection(request.getUrl(), request.getUsername(), password)) {
            // Connection is valid: optionally persist the settings and report success
            if (conn.isValid(5)) {
                // Persist datasource settings only when the caller asked to save them
                if (request.isSaveToProperties()) {
                    writeProperty(PROP_DATASOURCE_URL, request.getUrl());
                    writeProperty(PROP_DATASOURCE_USERNAME, request.getUsername());
                    // Only write the password when one was actually supplied
                    if (!password.isEmpty()) {
                        writeProperty(PROP_DATASOURCE_PASSWORD, password);
                    }
                }
                return new MessageResponse("Connection successful.", true);
            }
            return new MessageResponse("Connection established but database did not respond in time.", false);
        } catch (SQLException e) {
            logger.warn("DB connection test failed: {}", e.getMessage());
            return new MessageResponse("Connection failed: " + e.getMessage(), false);
        } catch (IOException e) {
            logger.error("Failed to save datasource properties after successful test", e);
            return new MessageResponse("Connection succeeded but could not save settings: " + e.getMessage(), false);
        }
    }

    // -------------------------------------------------------------------------
    // Mark complete (writes to disk + flips in-memory flag)
    // -------------------------------------------------------------------------

    /**
     * Marks the setup wizard as complete by persisting {@code app.setup.completed=true},
     * lifting the setup filter, and flipping the in-memory flag.
     *
     * @throws SetupException if the completion flag could not be persisted
     */
    public void markSetupComplete() {
        try {
            writeProperty("app.setup.completed", "true");
            setupFilter.markSetupComplete();
            this.setupCompleted = true;
            logger.info("Setup wizard completed. app.setup.completed=true written to properties file.");
        } catch (IOException e) {
            logger.error("Failed to write app.setup.completed=true to properties file", e);
            throw new SetupException("Could not persist setup completion: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Properties for step 5
    // -------------------------------------------------------------------------

    /**
     * Loads application properties from disk and groups them by importance
     * (Critical, Important, Optional, Advanced) for display in the wizard, masking sensitive values.
     *
     * @return an ordered map of group name to the property entries in that group
     * @throws IOException if the properties file cannot be located or read
     */
    public Map<String, List<PropertyEntry>> getGroupedProperties() throws IOException {
        Path path = resolvePropertiesPath();
        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(path)) {
            props.load(reader);
        }

        Map<String, String> descriptions = buildDescriptions();
        Map<String, Boolean> sensitive = buildSensitiveKeys();

        Map<String, List<PropertyEntry>> groups = new LinkedHashMap<>();
        groups.put("Critical", buildGroup(props, descriptions, sensitive,
                PROP_DATASOURCE_URL, PROP_DATASOURCE_USERNAME, PROP_DATASOURCE_PASSWORD,
                PROP_JWT_SECRET, "server.port"));
        groups.put("Important", buildGroup(props, descriptions, sensitive,
                "jwt.expiration", "jwt.refresh.expiration",
                "app.registration.enabled",
                "health-check.enabled", "health-check.scheduler-interval-ms",
                "health-check.default-interval-seconds"));
        groups.put("Optional", buildGroup(props, descriptions, sensitive,
                "app.email.enabled", "spring.mail.host", "spring.mail.port",
                "spring.mail.username", PROP_MAIL_PASSWORD));
        groups.put("Advanced", buildGroup(props, descriptions, sensitive,
                "logging.level.org.springframework.security",
                "logging.level.org.automatize.status",
                "logs.retention.days", "data.initializer.enabled",
                "spring.jpa.show-sql"));
        return groups;
    }

    /**
     * Persists edited application properties to disk, skipping any sensitive value that
     * still contains the masking placeholder (so masked values are never written back).
     *
     * @param request the properties request containing the key/value pairs to save
     * @throws IOException if the properties file cannot be located or written
     */
    public void saveProperties(SetupPropertiesRequest request) throws IOException {
        // Nothing to do when no properties were submitted
        if (request.getProperties() == null || request.getProperties().isEmpty()) {
            return;
        }
        Map<String, Boolean> sensitive = buildSensitiveKeys();
        for (Map.Entry<String, String> entry : request.getProperties().entrySet()) {
            // CRIT-02: never write the masking placeholder back to disk
            // Skip sensitive keys whose value is still the mask (unchanged by the user)
            if (Boolean.TRUE.equals(sensitive.get(entry.getKey()))
                    && SENSITIVE_MASK.equals(entry.getValue())) {
                continue;
            }
            writeProperty(entry.getKey(), entry.getValue());
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static final String SENSITIVE_MASK = "********";

    /**
     * Builds a list of property entries for the given keys, resolving descriptions and
     * masking the values of sensitive keys.
     *
     * @param props the loaded application properties
     * @param descriptions the map of key to human-readable description
     * @param sensitive the map of key to sensitivity flag
     * @param keys the property keys to include in this group
     * @return the list of PropertyEntry objects for the group
     */
    private List<PropertyEntry> buildGroup(Properties props,
                                           Map<String, String> descriptions,
                                           Map<String, Boolean> sensitive,
                                           String... keys) {
        List<PropertyEntry> entries = new ArrayList<>();
        for (String key : keys) {
            String rawValue = props.getProperty(key, "");
            boolean isSensitive = Boolean.TRUE.equals(sensitive.get(key));
            // CRIT-02: never return plaintext for sensitive keys
            String displayValue = isSensitive
                    ? (rawValue.isEmpty() ? "" : SENSITIVE_MASK)
                    : rawValue;
            entries.add(new PropertyEntry(key, displayValue,
                    descriptions.getOrDefault(key, ""),
                    isSensitive));
        }
        return entries;
    }

    /**
     * Line-preserving property file writer: finds the line with the given key
     * and replaces its value in-place, preserving all comments and ordering.
     * If the key is not found, it is appended at the end.
     *
     * @param key the property key to write
     * @param value the value to set for the key
     * @throws IOException if the properties file cannot be located, read, or written
     */
    private void writeProperty(String key, String value) throws IOException {
        Path path = resolvePropertiesPath();
        List<String> lines = Files.readAllLines(path);
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            // Match the existing line for this key and replace its value in-place
            if (trimmed.startsWith(key + "=") || trimmed.startsWith(key + " =")) {
                lines.set(i, key + "=" + value);
                found = true;
                break;
            }
        }
        // Append the key when it was not already present in the file
        if (!found) {
            lines.add(key + "=" + value);
        }
        Files.write(path, lines);
    }

    /**
     * Resolves the application.properties file on disk.
     * Checks the working directory first (fat-jar / production deployment),
     * then falls back to the classpath resource (IDE / exploded build).
     *
     * @return the resolved path to the application.properties file
     * @throws IOException if the properties file cannot be located or its URI cannot be resolved
     */
    private Path resolvePropertiesPath() throws IOException {
        // 1. Working directory (standard Spring Boot external config location)
        Path candidate = Paths.get("application.properties");
        // Prefer the working-directory copy when it exists
        if (Files.exists(candidate)) {
            return candidate;
        }
        // 2. Classpath (IDE with exploded target/classes)
        URL resource = getClass().getClassLoader().getResource("application.properties");
        // Fall back to the classpath resource when it is available
        if (resource != null) {
            try {
                return Paths.get(resource.toURI());
            } catch (Exception e) {
                throw new IOException("Cannot resolve properties file URI: " + e.getMessage(), e);
            }
        }
        throw new IOException("Cannot locate application.properties for writing. " +
                "Ensure the file is in the working directory or on the classpath.");
    }

    /**
     * Builds the map of property keys to their human-readable descriptions shown in the wizard.
     *
     * @return an ordered map of property key to description
     */
    private Map<String, String> buildDescriptions() {
        Map<String, String> d = new LinkedHashMap<>();
        d.put(PROP_DATASOURCE_URL, "PostgreSQL JDBC URL (jdbc:postgresql://host:port/dbname)");
        d.put(PROP_DATASOURCE_USERNAME, "Database username");
        d.put(PROP_DATASOURCE_PASSWORD, "Database password");
        d.put(PROP_JWT_SECRET, "JWT signing secret (Base64 encoded, min 256 bits)");
        d.put("server.port", "HTTP port the application listens on");
        d.put("jwt.expiration", "Access token expiry in milliseconds (86400000 = 24h)");
        d.put("jwt.refresh.expiration", "Refresh token expiry in milliseconds (604800000 = 7d)");
        d.put("app.registration.enabled", "Allow new users to self-register (true/false)");
        d.put("health-check.enabled", "Enable automated health checks (true/false)");
        d.put("health-check.scheduler-interval-ms", "How often the scheduler polls for due checks (ms)");
        d.put("health-check.default-interval-seconds", "Default check frequency per endpoint (seconds)");
        d.put("app.email.enabled", "Enable email sending via SMTP (true/false)");
        d.put("spring.mail.host", "SMTP server hostname");
        d.put("spring.mail.port", "SMTP server port (587 for STARTTLS)");
        d.put("spring.mail.username", "SMTP sender email address");
        d.put(PROP_MAIL_PASSWORD, "SMTP authentication password");
        d.put("logging.level.org.springframework.security", "Spring Security log level (DEBUG/INFO/WARN)");
        d.put("logging.level.org.automatize.status", "Application log level (DEBUG/INFO/WARN)");
        d.put("logs.retention.days", "Days to retain log entries before auto-deletion");
        d.put("data.initializer.enabled", "Seed default admin data on startup (disable in production)");
        d.put("spring.jpa.show-sql", "Log all SQL statements to console (true/false)");
        return d;
    }

    /**
     * Builds the set of property keys whose values are sensitive and must be masked.
     *
     * @return a map of sensitive property key to {@code true}
     */
    private Map<String, Boolean> buildSensitiveKeys() {
        Map<String, Boolean> s = new HashMap<>();
        s.put(PROP_DATASOURCE_PASSWORD, true);
        s.put(PROP_JWT_SECRET, true);
        s.put(PROP_MAIL_PASSWORD, true);
        return s;
    }

    // -------------------------------------------------------------------------
    // PropertyEntry inner class
    // -------------------------------------------------------------------------

    /**
     * Immutable view model representing a single editable application property in the setup wizard,
     * carrying its key, (possibly masked) value, description, and sensitivity flag.
     */
    public static class PropertyEntry {
        private String key;
        private String value;
        private String description;
        private boolean sensitive;

        /**
         * Constructs a new PropertyEntry.
         *
         * @param key the property key
         * @param value the property value (masked when sensitive)
         * @param description the human-readable description of the property
         * @param sensitive whether the property value is sensitive
         */
        public PropertyEntry(String key, String value, String description, boolean sensitive) {
            this.key = key;
            this.value = value;
            this.description = description;
            this.sensitive = sensitive;
        }

        /**
         * Returns the property key.
         *
         * @return the property key
         */
        public String getKey() { return key; }

        /**
         * Returns the property value (masked when sensitive).
         *
         * @return the property value
         */
        public String getValue() { return value; }

        /**
         * Returns the human-readable description of the property.
         *
         * @return the property description
         */
        public String getDescription() { return description; }

        /**
         * Indicates whether the property value is sensitive.
         *
         * @return true if the property is sensitive, false otherwise
         */
        public boolean isSensitive() { return sensitive; }
    }
}
