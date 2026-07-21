package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.*;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.api.response.SetupStatusResponse;
import org.automatize.status.models.Organization;
import org.automatize.status.models.Tenant;
import org.automatize.status.services.SetupService;
import org.automatize.status.services.SetupService.PropertyEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * REST API controller for the first-run application setup wizard.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Report setup completion status</li>
 *   <li>Test the database connection during setup</li>
 *   <li>Create the initial tenant, organization, and admin account</li>
 *   <li>Read and persist grouped configuration properties</li>
 * </ul>
 * </p>
 *
 * <p>
 * Base route: {@code /api/setup}. These endpoints are intended to be used before
 * setup is complete; mutating operations reject requests once setup has finished.
 * </p>
 *
 * @see SetupService
 */
@RestController
@RequestMapping("/api/setup")
public class SetupApiController {

    private static final Logger logger = LoggerFactory.getLogger(SetupApiController.class);

    private static final String SETUP_ALREADY_COMPLETE = "Setup is already complete.";

    @Autowired
    private SetupService setupService;

    /**
     * Returns the current setup status of the application.
     * <p>
     * HTTP GET {@code /api/setup/status}
     * </p>
     *
     * @return ResponseEntity containing the setup status
     */
    @GetMapping("/status")
    public ResponseEntity<SetupStatusResponse> getStatus() {
        return ResponseEntity.ok(setupService.getStatus());
    }

    /**
     * Tests the supplied database connection details during setup.
     * <p>
     * HTTP POST {@code /api/setup/test-connection}
     * </p>
     *
     * @param request the validated connection test request
     * @return ResponseEntity with the connection result, HTTP 403 if setup is already
     *         complete, or HTTP 400 if the connection failed
     */
    @PostMapping("/test-connection")
    public ResponseEntity<MessageResponse> testConnection(@Valid @RequestBody SetupTestConnectionRequest request) {
        // Reject the operation when setup has already been completed
        if (setupService.isSetupAlreadyComplete()) {
            return ResponseEntity.status(403).body(new MessageResponse(SETUP_ALREADY_COMPLETE, false));
        }
        MessageResponse result = setupService.testConnection(request);
        return result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    /**
     * Creates the initial tenant during setup.
     * <p>
     * HTTP POST {@code /api/setup/tenant}
     * </p>
     *
     * @param request the validated tenant creation request
     * @return ResponseEntity containing the created tenant, HTTP 403 if setup is already
     *         complete, or HTTP 400 with an error message on failure
     */
    @PostMapping("/tenant")
    public ResponseEntity<?> createTenant(@Valid @RequestBody SetupTenantRequest request) {
        // Reject the operation when setup has already been completed
        if (setupService.isSetupAlreadyComplete()) {
            return ResponseEntity.status(403).body(new MessageResponse(SETUP_ALREADY_COMPLETE, false));
        }
        try {
            Tenant tenant = setupService.createTenant(request);
            return ResponseEntity.ok(tenant);
        } catch (Exception e) {
            logger.warn("Failed to create tenant during setup", e);
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * Creates the initial organization during setup.
     * <p>
     * HTTP POST {@code /api/setup/organization}
     * </p>
     *
     * @param request the validated organization creation request
     * @return ResponseEntity containing the created organization, HTTP 403 if setup is
     *         already complete, or HTTP 400 with an error message on failure
     */
    @PostMapping("/organization")
    public ResponseEntity<?> createOrganization(@Valid @RequestBody SetupOrganizationRequest request) {
        // Reject the operation when setup has already been completed
        if (setupService.isSetupAlreadyComplete()) {
            return ResponseEntity.status(403).body(new MessageResponse(SETUP_ALREADY_COMPLETE, false));
        }
        try {
            Organization org = setupService.createOrganization(request);
            return ResponseEntity.ok(org);
        } catch (Exception e) {
            logger.warn("Failed to create organization during setup", e);
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * Creates the initial administrator account and marks setup as complete.
     * <p>
     * HTTP POST {@code /api/setup/admin}
     * </p>
     *
     * @param request the validated admin creation request
     * @return ResponseEntity confirming completion, HTTP 403 if setup is already complete,
     *         or HTTP 400 with an error message on failure
     */
    @PostMapping("/admin")
    public ResponseEntity<MessageResponse> createAdmin(@Valid @RequestBody SetupAdminRequest request) {
        // Reject the operation when setup has already been completed
        if (setupService.isSetupAlreadyComplete()) {
            return ResponseEntity.status(403).body(new MessageResponse(SETUP_ALREADY_COMPLETE, false));
        }
        try {
            MessageResponse result = setupService.createAdmin(request);
            // Abort and surface the error when admin creation did not succeed
            if (!result.isSuccess()) {
                return ResponseEntity.badRequest().body(result);
            }
            setupService.markSetupComplete();
            return ResponseEntity.ok(new MessageResponse("Admin account created and setup complete!", true));
        } catch (Exception e) {
            logger.warn("Failed to create admin during setup", e);
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * Retrieves configurable application properties grouped by category.
     * <p>
     * HTTP GET {@code /api/setup/properties}
     * </p>
     *
     * @return ResponseEntity containing grouped property entries, or HTTP 400 with an
     *         error message on failure
     */
    @GetMapping("/properties")
    public ResponseEntity<?> getProperties() {
        try {
            Map<String, List<PropertyEntry>> groups = setupService.getGroupedProperties();
            return ResponseEntity.ok(groups);
        } catch (Exception e) {
            logger.warn("Failed to load setup properties", e);
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * Persists updated application configuration properties.
     * <p>
     * HTTP POST {@code /api/setup/properties}
     * </p>
     *
     * @param request the properties to save
     * @return ResponseEntity with a success message, or HTTP 400 with an error message
     *         on failure
     */
    @PostMapping("/properties")
    public ResponseEntity<MessageResponse> saveProperties(@RequestBody SetupPropertiesRequest request) {
        try {
            setupService.saveProperties(request);
            return ResponseEntity.ok(new MessageResponse("Configuration saved successfully.", true));
        } catch (Exception e) {
            logger.warn("Failed to save setup properties", e);
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }
}
