package org.automatize.status.config;

import org.automatize.status.models.Organization;
import org.automatize.status.models.Tenant;
import org.automatize.status.models.User;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.TenantRepository;
import org.automatize.status.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                          TenantRepository tenantRepository,
                          OrganizationRepository organizationRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByUsername("admin")) {
            logger.info("Admin user already exists, skipping initialization");
            return;
        }

        logger.info("Initializing default admin account...");

        Tenant tenant = tenantRepository.findByName("Default Tenant")
                .orElseGet(() -> {
                    Tenant newTenant = new Tenant();
                    newTenant.setName("Default Tenant");
                    newTenant.setIsActive(true);
                    newTenant.setCreatedBy("system");
                    newTenant.setLastModifiedBy("system");
                    return tenantRepository.save(newTenant);
                });

        Organization organization = organizationRepository.findByName("Default Organization")
                .orElseGet(() -> {
                    Organization newOrg = new Organization();
                    newOrg.setName("Default Organization");
                    newOrg.setDescription("Default organization for system administrators");
                    newOrg.setStatus("ACTIVE");
                    newOrg.setOrganizationType("INTERNAL");
                    newOrg.setTenant(tenant);
                    newOrg.setCreatedBy("system");
                    newOrg.setLastModifiedBy("system");
                    return organizationRepository.save(newOrg);
                });

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin"));
        admin.setEmail("admin@status.local");
        admin.setFullName("System Administrator");
        admin.setRole("ADMIN");
        admin.setEnabled(true);
        admin.setStatus("ACTIVE");
        admin.setOrganization(organization);
        admin.setCreatedBy("system");
        admin.setLastModifiedBy("system");

        userRepository.save(admin);

        logger.info("Admin account created successfully (username: admin, password: admin)");
    }
}
