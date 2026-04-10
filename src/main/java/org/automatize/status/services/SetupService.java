package org.automatize.status.services;

import org.automatize.status.api.request.*;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.api.response.SetupStatusResponse;
import org.automatize.status.config.SetupFilter;
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

@Service
public class SetupService {

    private static final Logger logger = LoggerFactory.getLogger(SetupService.class);

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

    public boolean isSetupAlreadyComplete() {
        return setupCompleted;
    }

    // -------------------------------------------------------------------------
    // Status
    // -------------------------------------------------------------------------

    public SetupStatusResponse getStatus() {
        SetupStatusResponse status = new SetupStatusResponse();
        status.setSetupCompleted(setupCompleted);

        // DB connection check
        try (var conn = dataSource.getConnection()) {
            status.setDbConnected(conn.isValid(2));
        } catch (Exception e) {
            status.setDbConnected(false);
            status.setDbError(e.getMessage());
        }

        // Expose current datasource config for pre-populating the DB step
        status.setDbUrl(datasourceUrl);
        status.setDbUsername(datasourceUsername);

        // Flyway migration version
        try {
            if (flyway != null) {
                MigrationInfo current = flyway.info().current();
                status.setFlywayVersion(current != null ? current.getVersion().getVersion() : "none");
            } else {
                status.setFlywayVersion("unknown");
            }
        } catch (Exception e) {
            status.setFlywayVersion("unknown");
        }

        // Resumability: check if tenant/org already exist
        List<Tenant> tenants = tenantRepository.findAll();
        if (!tenants.isEmpty()) {
            status.setTenantCreated(true);
            status.setTenantId(tenants.get(0).getId());

            List<Organization> orgs = organizationRepository.findAll();
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

    @Transactional
    public Tenant createTenant(SetupTenantRequest request) {
        TenantRequest tenantRequest = new TenantRequest();
        tenantRequest.setName(request.getName());
        tenantRequest.setIsActive(true);
        return tenantService.createTenant(tenantRequest);
    }

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

    @Transactional
    public MessageResponse createAdmin(SetupAdminRequest request) {
        // HIGH-03: setup admin creation bypasses registerUser() to assign SUPERADMIN role
        // directly, without going through the public registration path which now hard-codes
        // the role to "USER".
        if (userRepository.existsByUsername(request.getUsername())) {
            return new MessageResponse("Username is already taken!", false);
        }
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

    public MessageResponse testConnection(SetupTestConnectionRequest request) {
        String password = request.getPassword() != null ? request.getPassword() : "";
        try (Connection conn = DriverManager.getConnection(request.getUrl(), request.getUsername(), password)) {
            if (conn.isValid(5)) {
                if (request.isSaveToProperties()) {
                    writeProperty("spring.datasource.url", request.getUrl());
                    writeProperty("spring.datasource.username", request.getUsername());
                    if (!password.isEmpty()) {
                        writeProperty("spring.datasource.password", password);
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

    public void markSetupComplete() {
        try {
            writeProperty("app.setup.completed", "true");
            setupFilter.markSetupComplete();
            this.setupCompleted = true;
            logger.info("Setup wizard completed. app.setup.completed=true written to properties file.");
        } catch (IOException e) {
            logger.error("Failed to write app.setup.completed=true to properties file", e);
            throw new RuntimeException("Could not persist setup completion: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Properties for step 5
    // -------------------------------------------------------------------------

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
                "spring.datasource.url", "spring.datasource.username", "spring.datasource.password",
                "jwt.secret", "server.port"));
        groups.put("Important", buildGroup(props, descriptions, sensitive,
                "jwt.expiration", "jwt.refresh.expiration",
                "app.registration.enabled",
                "health-check.enabled", "health-check.scheduler-interval-ms",
                "health-check.default-interval-seconds"));
        groups.put("Optional", buildGroup(props, descriptions, sensitive,
                "app.email.enabled", "spring.mail.host", "spring.mail.port",
                "spring.mail.username", "spring.mail.password"));
        groups.put("Advanced", buildGroup(props, descriptions, sensitive,
                "logging.level.org.springframework.security",
                "logging.level.org.automatize.status",
                "logs.retention.days", "data.initializer.enabled",
                "spring.jpa.show-sql"));
        return groups;
    }

    public void saveProperties(SetupPropertiesRequest request) throws IOException {
        if (request.getProperties() == null || request.getProperties().isEmpty()) {
            return;
        }
        Map<String, Boolean> sensitive = buildSensitiveKeys();
        for (Map.Entry<String, String> entry : request.getProperties().entrySet()) {
            // CRIT-02: never write the masking placeholder back to disk
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
     */
    private void writeProperty(String key, String value) throws IOException {
        Path path = resolvePropertiesPath();
        List<String> lines = Files.readAllLines(path);
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith(key + "=") || trimmed.startsWith(key + " =")) {
                lines.set(i, key + "=" + value);
                found = true;
                break;
            }
        }
        if (!found) {
            lines.add(key + "=" + value);
        }
        Files.write(path, lines);
    }

    /**
     * Resolves the application.properties file on disk.
     * Checks the working directory first (fat-jar / production deployment),
     * then falls back to the classpath resource (IDE / exploded build).
     */
    private Path resolvePropertiesPath() throws IOException {
        // 1. Working directory (standard Spring Boot external config location)
        Path candidate = Paths.get("application.properties");
        if (Files.exists(candidate)) {
            return candidate;
        }
        // 2. Classpath (IDE with exploded target/classes)
        URL resource = getClass().getClassLoader().getResource("application.properties");
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

    private Map<String, String> buildDescriptions() {
        Map<String, String> d = new LinkedHashMap<>();
        d.put("spring.datasource.url", "PostgreSQL JDBC URL (jdbc:postgresql://host:port/dbname)");
        d.put("spring.datasource.username", "Database username");
        d.put("spring.datasource.password", "Database password");
        d.put("jwt.secret", "JWT signing secret (Base64 encoded, min 256 bits)");
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
        d.put("spring.mail.password", "SMTP authentication password");
        d.put("logging.level.org.springframework.security", "Spring Security log level (DEBUG/INFO/WARN)");
        d.put("logging.level.org.automatize.status", "Application log level (DEBUG/INFO/WARN)");
        d.put("logs.retention.days", "Days to retain log entries before auto-deletion");
        d.put("data.initializer.enabled", "Seed default admin data on startup (disable in production)");
        d.put("spring.jpa.show-sql", "Log all SQL statements to console (true/false)");
        return d;
    }

    private Map<String, Boolean> buildSensitiveKeys() {
        Map<String, Boolean> s = new HashMap<>();
        s.put("spring.datasource.password", true);
        s.put("jwt.secret", true);
        s.put("spring.mail.password", true);
        return s;
    }

    // -------------------------------------------------------------------------
    // PropertyEntry inner class
    // -------------------------------------------------------------------------

    public static class PropertyEntry {
        private String key;
        private String value;
        private String description;
        private boolean sensitive;

        public PropertyEntry(String key, String value, String description, boolean sensitive) {
            this.key = key;
            this.value = value;
            this.description = description;
            this.sensitive = sensitive;
        }

        public String getKey() { return key; }
        public String getValue() { return value; }
        public String getDescription() { return description; }
        public boolean isSensitive() { return sensitive; }
    }
}
