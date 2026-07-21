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

@RestController
@RequestMapping("/api/setup")
public class SetupApiController {

    private static final Logger logger = LoggerFactory.getLogger(SetupApiController.class);

    private static final String SETUP_ALREADY_COMPLETE = "Setup is already complete.";

    @Autowired
    private SetupService setupService;

    @GetMapping("/status")
    public ResponseEntity<SetupStatusResponse> getStatus() {
        return ResponseEntity.ok(setupService.getStatus());
    }

    @PostMapping("/test-connection")
    public ResponseEntity<MessageResponse> testConnection(@Valid @RequestBody SetupTestConnectionRequest request) {
        if (setupService.isSetupAlreadyComplete()) {
            return ResponseEntity.status(403).body(new MessageResponse(SETUP_ALREADY_COMPLETE, false));
        }
        MessageResponse result = setupService.testConnection(request);
        return result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/tenant")
    public ResponseEntity<?> createTenant(@Valid @RequestBody SetupTenantRequest request) {
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

    @PostMapping("/organization")
    public ResponseEntity<?> createOrganization(@Valid @RequestBody SetupOrganizationRequest request) {
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

    @PostMapping("/admin")
    public ResponseEntity<MessageResponse> createAdmin(@Valid @RequestBody SetupAdminRequest request) {
        if (setupService.isSetupAlreadyComplete()) {
            return ResponseEntity.status(403).body(new MessageResponse(SETUP_ALREADY_COMPLETE, false));
        }
        try {
            MessageResponse result = setupService.createAdmin(request);
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
