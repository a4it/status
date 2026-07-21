package org.automatize.status.controllers.api;

import org.automatize.status.api.request.SetupAdminRequest;
import org.automatize.status.api.request.SetupOrganizationRequest;
import org.automatize.status.api.request.SetupTenantRequest;
import org.automatize.status.api.request.SetupTestConnectionRequest;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.api.response.SetupStatusResponse;
import org.automatize.status.models.Organization;
import org.automatize.status.models.Tenant;
import org.automatize.status.services.SetupService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link SetupApiController}, the setup-wizard REST driver.
 * Covers status, DB connection test, tenant/organization/admin creation, and
 * property read/save — including the "setup already complete" 403 guard, bean
 * validation (400), and service-failure branches, delegating to a mocked
 * {@link SetupService}.
 */
@WebMvcTest(controllers = SetupApiController.class)
class SetupApiControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private SetupService setupService;

    // ---- status ----------------------------------------------------------

    /**
     * Verifies GET /api/setup/status returns 200 with the current setup status (completion + DB connectivity).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getStatus_returnsOk() throws Exception {
        SetupStatusResponse resp = new SetupStatusResponse();
        resp.setSetupCompleted(false);
        resp.setDbConnected(true);
        when(setupService.getStatus()).thenReturn(resp);

        mockMvc.perform(get("/api/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dbConnected").value(true))
                .andExpect(jsonPath("$.setupCompleted").value(false));
    }

    // ---- test-connection -------------------------------------------------

    /**
     * Verifies POST /api/setup/test-connection returns 200 with success=true when setup is incomplete
     * and the service reports a successful connection.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void testConnection_valid_returnsOk() throws Exception {
        when(setupService.isSetupAlreadyComplete()).thenReturn(false);
        when(setupService.testConnection(any(SetupTestConnectionRequest.class)))
                .thenReturn(new MessageResponse("Connection successful.", true));

        String body = "{\"url\":\"jdbc:postgresql://localhost/db\",\"username\":\"u\",\"password\":\"p\"}";
        mockMvc.perform(post("/api/setup/test-connection").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * Verifies POST /api/setup/test-connection returns 400 with success=false when the service reports
     * a failed connection.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void testConnection_failed_returns400() throws Exception {
        when(setupService.isSetupAlreadyComplete()).thenReturn(false);
        when(setupService.testConnection(any(SetupTestConnectionRequest.class)))
                .thenReturn(new MessageResponse("Connection failed: timeout", false));

        String body = "{\"url\":\"jdbc:postgresql://localhost/db\",\"username\":\"u\"}";
        mockMvc.perform(post("/api/setup/test-connection").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * Verifies POST /api/setup/test-connection returns 403 when setup is already complete (guard).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void testConnection_alreadyComplete_returns403() throws Exception {
        when(setupService.isSetupAlreadyComplete()).thenReturn(true);

        String body = "{\"url\":\"jdbc:postgresql://localhost/db\",\"username\":\"u\"}";
        mockMvc.perform(post("/api/setup/test-connection").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    /**
     * Verifies POST /api/setup/test-connection returns 400 when required {@code @NotBlank} fields
     * (url + username) are missing (bean validation failure).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void testConnection_missingFields_returns400() throws Exception {
        String body = "{\"password\":\"p\"}"; // url + username are @NotBlank
        mockMvc.perform(post("/api/setup/test-connection").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // ---- tenant ----------------------------------------------------------

    /**
     * Verifies POST /api/setup/tenant returns 200 with the created tenant when setup is incomplete.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void createTenant_valid_returnsOk() throws Exception {
        when(setupService.isSetupAlreadyComplete()).thenReturn(false);
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("Acme");
        when(setupService.createTenant(any(SetupTenantRequest.class))).thenReturn(tenant);

        String body = "{\"name\":\"Acme\"}";
        mockMvc.perform(post("/api/setup/tenant").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme"));
    }

    /**
     * Verifies POST /api/setup/tenant returns 403 when setup is already complete (guard).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void createTenant_alreadyComplete_returns403() throws Exception {
        when(setupService.isSetupAlreadyComplete()).thenReturn(true);

        mockMvc.perform(post("/api/setup/tenant").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Acme\"}"))
                .andExpect(status().isForbidden());
    }

    /**
     * Verifies POST /api/setup/tenant returns 400 when the required name is missing (bean validation failure).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void createTenant_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/setup/tenant").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies POST /api/setup/tenant returns 400 with success=false when the service throws during
     * tenant creation.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void createTenant_serviceThrows_returns400() throws Exception {
        when(setupService.isSetupAlreadyComplete()).thenReturn(false);
        when(setupService.createTenant(any(SetupTenantRequest.class)))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/setup/tenant").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Acme\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ---- organization ----------------------------------------------------

    /**
     * Verifies POST /api/setup/organization returns 200 with the created organization when setup is incomplete.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void createOrganization_valid_returnsOk() throws Exception {
        when(setupService.isSetupAlreadyComplete()).thenReturn(false);
        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        org.setName("Acme Org");
        when(setupService.createOrganization(any(SetupOrganizationRequest.class))).thenReturn(org);

        String body = "{\"name\":\"Acme Org\",\"organizationType\":\"CUSTOMER\",\"tenantId\":\""
                + UUID.randomUUID() + "\"}";
        mockMvc.perform(post("/api/setup/organization").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Org"));
    }

    /**
     * Verifies POST /api/setup/organization returns 403 when setup is already complete (guard).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void createOrganization_alreadyComplete_returns403() throws Exception {
        when(setupService.isSetupAlreadyComplete()).thenReturn(true);

        String body = "{\"name\":\"Acme Org\",\"organizationType\":\"CUSTOMER\",\"tenantId\":\""
                + UUID.randomUUID() + "\"}";
        mockMvc.perform(post("/api/setup/organization").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    /**
     * Verifies POST /api/setup/organization returns 400 when required fields (organizationType + tenantId)
     * are missing (bean validation failure).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void createOrganization_missingFields_returns400() throws Exception {
        String body = "{\"name\":\"Acme Org\"}"; // organizationType + tenantId missing
        mockMvc.perform(post("/api/setup/organization").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // ---- admin -----------------------------------------------------------

    /**
     * Provides a valid JSON admin-creation request body satisfying all bean-validation constraints.
     *
     * @return a JSON string with username, password, email, fullName and organizationId populated
     */
    private String adminBody() {
        return "{\"username\":\"admin\",\"password\":\"password123\",\"email\":\"admin@example.com\","
                + "\"fullName\":\"Admin User\",\"organizationId\":\"" + UUID.randomUUID() + "\"}";
    }

    /**
     * Verifies POST /api/setup/admin returns 200 with success=true and marks setup complete when the
     * admin is registered successfully.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void createAdmin_valid_returnsOk() throws Exception {
        when(setupService.isSetupAlreadyComplete()).thenReturn(false);
        when(setupService.createAdmin(any(SetupAdminRequest.class)))
                .thenReturn(new MessageResponse("User registered successfully!", true));

        mockMvc.perform(post("/api/setup/admin").contentType(MediaType.APPLICATION_JSON).content(adminBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(setupService).markSetupComplete();
    }

    /**
     * Verifies POST /api/setup/admin returns 400 with success=false when the service reports a failure
     * (e.g. username already taken).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void createAdmin_serviceFailure_returns400() throws Exception {
        when(setupService.isSetupAlreadyComplete()).thenReturn(false);
        when(setupService.createAdmin(any(SetupAdminRequest.class)))
                .thenReturn(new MessageResponse("Username is already taken!", false));

        mockMvc.perform(post("/api/setup/admin").contentType(MediaType.APPLICATION_JSON).content(adminBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * Verifies POST /api/setup/admin returns 403 when setup is already complete (guard).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void createAdmin_alreadyComplete_returns403() throws Exception {
        when(setupService.isSetupAlreadyComplete()).thenReturn(true);

        mockMvc.perform(post("/api/setup/admin").contentType(MediaType.APPLICATION_JSON).content(adminBody()))
                .andExpect(status().isForbidden());
    }

    /**
     * Verifies POST /api/setup/admin returns 400 when the username is too short and required fields are
     * missing (bean validation failure).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void createAdmin_missingFields_returns400() throws Exception {
        String body = "{\"username\":\"ad\"}"; // too short + missing required fields
        mockMvc.perform(post("/api/setup/admin").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // ---- properties ------------------------------------------------------

    /**
     * Verifies GET /api/setup/properties returns 200 with the grouped application properties.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getProperties_returnsOk() throws Exception {
        Map<String, List<SetupService.PropertyEntry>> groups = Map.of(
                "Critical", List.of(new SetupService.PropertyEntry(
                        "server.port", "8080", "HTTP port", false)));
        when(setupService.getGroupedProperties()).thenReturn(groups);

        mockMvc.perform(get("/api/setup/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Critical[0].key").value("server.port"));
    }

    /**
     * Verifies GET /api/setup/properties returns 400 with success=false when reading the properties
     * throws an {@code IOException}.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getProperties_serviceThrows_returns400() throws Exception {
        when(setupService.getGroupedProperties()).thenThrow(new java.io.IOException("no file"));

        mockMvc.perform(get("/api/setup/properties"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * Verifies POST /api/setup/properties returns 200 with success=true and delegates persistence to
     * the service.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void saveProperties_returnsOk() throws Exception {
        String body = "{\"properties\":{\"server.port\":\"9090\"}}";
        mockMvc.perform(post("/api/setup/properties").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(setupService).saveProperties(any());
    }

    /**
     * Verifies POST /api/setup/properties returns 400 with success=false when saving throws an
     * {@code IOException}.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void saveProperties_serviceThrows_returns400() throws Exception {
        doThrow(new java.io.IOException("write failed")).when(setupService).saveProperties(any());

        String body = "{\"properties\":{\"server.port\":\"9090\"}}";
        mockMvc.perform(post("/api/setup/properties").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
