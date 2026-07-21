package org.automatize.status.controllers.api;

import org.automatize.status.exceptions.DuplicateResourceException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.Tenant;
import org.automatize.status.services.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link TenantController}. Security filters and method
 * security are disabled ({@code addFilters = false}); focus is request mapping,
 * bean validation (400), JSON contract, {@code @ResponseStatus} exception
 * mapping (404/409), and delegation to the (mocked) service layer.
 */
@WebMvcTest(controllers = TenantController.class)
class TenantControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private TenantService tenantService;

    /**
     * Builds a minimal active {@link Tenant} fixture for use in stubbed service
     * responses.
     *
     * @param id the identifier to assign to the tenant
     * @return a populated sample {@link Tenant}
     */
    private Tenant sampleTenant(UUID id) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setName("Globex");
        t.setIsActive(true);
        return t;
    }

    /**
     * Verifies that GET {@code /api/tenants} returns 200 with a paged JSON body
     * whose content is populated from the service.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void getAllTenants_returnsOkPage() throws Exception {
        when(tenantService.getAllTenants(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleTenant(UUID.randomUUID()))));

        mockMvc.perform(get("/api/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Globex"));
    }

    /**
     * Verifies that GET {@code /api/tenants/{id}} returns 200 with the tenant
     * when the service resolves it.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void getTenantById_found_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(tenantService.getTenantById(id)).thenReturn(sampleTenant(id));

        mockMvc.perform(get("/api/tenants/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Globex"));
    }

    /**
     * Verifies that GET {@code /api/tenants/{id}} returns 404 when the service
     * raises {@link ResourceNotFoundException}.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void getTenantById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(tenantService.getTenantById(id))
                .thenThrow(new ResourceNotFoundException("Tenant not found with id: " + id));

        mockMvc.perform(get("/api/tenants/{id}", id))
                .andExpect(status().isNotFound());
    }

    /**
     * Verifies that POST {@code /api/tenants} with a valid body returns 201 and
     * the created tenant.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void createTenant_valid_returns201() throws Exception {
        when(tenantService.createTenant(any())).thenReturn(sampleTenant(UUID.randomUUID()));

        String body = "{\"name\":\"Globex\"}";
        mockMvc.perform(post("/api/tenants").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Globex"));
    }

    /**
     * Verifies that POST {@code /api/tenants} with a body missing the required
     * name fails bean validation and returns 400.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void createTenant_missingName_returns400() throws Exception {
        String body = "{}";
        mockMvc.perform(post("/api/tenants").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that POST {@code /api/tenants} returns 409 when the service
     * raises {@link DuplicateResourceException} for an existing tenant name.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void createTenant_duplicate_returns409() throws Exception {
        when(tenantService.createTenant(any()))
                .thenThrow(new DuplicateResourceException("Tenant with name already exists: Globex"));

        String body = "{\"name\":\"Globex\"}";
        mockMvc.perform(post("/api/tenants").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    /**
     * Verifies that PUT {@code /api/tenants/{id}} with a valid body returns 200
     * and the updated tenant.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void updateTenant_valid_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(tenantService.updateTenant(eq(id), any())).thenReturn(sampleTenant(id));

        String body = "{\"name\":\"Globex\"}";
        mockMvc.perform(put("/api/tenants/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Globex"));
    }

    /**
     * Verifies that PUT {@code /api/tenants/{id}} with a body missing the
     * required name fails bean validation and returns 400.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void updateTenant_missingName_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        String body = "{}";
        mockMvc.perform(put("/api/tenants/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that DELETE {@code /api/tenants/{id}} returns 200 with a success
     * message and delegates deletion to the service.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void deleteTenant_returns200Message() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/tenants/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(tenantService).deleteTenant(id);
    }

    /**
     * Verifies that GET {@code /api/tenants/name/{name}} returns 200 with the
     * tenant when the service resolves it by name.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void getTenantByName_found_returns200() throws Exception {
        when(tenantService.getTenantByName("Globex")).thenReturn(sampleTenant(UUID.randomUUID()));

        mockMvc.perform(get("/api/tenants/name/{name}", "Globex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Globex"));
    }

    /**
     * Verifies that GET {@code /api/tenants/name/{name}} returns 404 when the
     * service raises {@link ResourceNotFoundException} for an unknown name.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void getTenantByName_notFound_returns404() throws Exception {
        when(tenantService.getTenantByName("Missing"))
                .thenThrow(new ResourceNotFoundException("Tenant not found with name: Missing"));

        mockMvc.perform(get("/api/tenants/name/{name}", "Missing"))
                .andExpect(status().isNotFound());
    }
}
