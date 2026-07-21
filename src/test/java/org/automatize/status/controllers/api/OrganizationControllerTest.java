package org.automatize.status.controllers.api;

import org.automatize.status.exceptions.BusinessRuleException;
import org.automatize.status.exceptions.DuplicateResourceException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.Organization;
import org.automatize.status.services.OrganizationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link OrganizationController}. Security filters and
 * method security are disabled ({@code addFilters = false}); focus is request
 * mapping, bean validation (400), JSON contract, {@code @ResponseStatus}
 * exception mapping (404/409), and delegation to the (mocked) service layer.
 */
@WebMvcTest(controllers = OrganizationController.class)
class OrganizationControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private OrganizationService organizationService;

    private Organization sampleOrg(UUID id) {
        Organization o = new Organization();
        o.setId(id);
        o.setName("Acme");
        o.setStatus("ACTIVE");
        return o;
    }

    private String validOrgJson() {
        return "{\"name\":\"Acme\",\"organizationType\":\"CUSTOMER\"}";
    }

    @Test
    void getAllOrganizations_returnsOkPage() throws Exception {
        when(organizationService.getAllOrganizations(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleOrg(UUID.randomUUID()))));

        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Acme"));
    }

    @Test
    void getOrganizationById_found_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(organizationService.getOrganizationById(id)).thenReturn(sampleOrg(id));

        mockMvc.perform(get("/api/organizations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme"));
    }

    @Test
    void getOrganizationById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(organizationService.getOrganizationById(id))
                .thenThrow(new ResourceNotFoundException("Organization not found with id: " + id));

        mockMvc.perform(get("/api/organizations/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrganization_valid_returns201() throws Exception {
        when(organizationService.createOrganization(any())).thenReturn(sampleOrg(UUID.randomUUID()));

        mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON).content(validOrgJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Acme"));
    }

    @Test
    void createOrganization_missingName_returns400() throws Exception {
        String body = "{\"organizationType\":\"CUSTOMER\"}";
        mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrganization_missingType_returns400() throws Exception {
        String body = "{\"name\":\"Acme\"}";
        mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrganization_duplicate_returns409() throws Exception {
        when(organizationService.createOrganization(any()))
                .thenThrow(new DuplicateResourceException("Organization with name already exists: Acme"));

        mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON).content(validOrgJson()))
                .andExpect(status().isConflict());
    }

    @Test
    void updateOrganization_valid_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(organizationService.updateOrganization(eq(id), any())).thenReturn(sampleOrg(id));

        mockMvc.perform(put("/api/organizations/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content(validOrgJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme"));
    }

    @Test
    void updateOrganization_missingType_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        String body = "{\"name\":\"Acme\"}";
        mockMvc.perform(put("/api/organizations/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteOrganization_returns200Message() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/organizations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(organizationService).deleteOrganization(id);
    }

    @Test
    void deleteOrganization_hasActiveUsers_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new BusinessRuleException("Cannot delete organization with active users"))
                .when(organizationService).deleteOrganization(eq(id));

        mockMvc.perform(delete("/api/organizations/{id}", id))
                .andExpect(status().isConflict());
    }

    @Test
    void getOrganizationsByTenant_returns200List() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(organizationService.getOrganizationsByTenant(tenantId))
                .thenReturn(List.of(sampleOrg(UUID.randomUUID())));

        mockMvc.perform(get("/api/organizations/tenant/{tenantId}", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Acme"));
    }

    @Test
    void getCurrentUserOrganization_returns200() throws Exception {
        when(organizationService.getCurrentUserOrganization()).thenReturn(sampleOrg(UUID.randomUUID()));

        mockMvc.perform(get("/api/organizations/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme"));
    }

    @Test
    void updateOrganizationStatus_valid_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(organizationService.updateStatus(eq(id), eq("INACTIVE"))).thenReturn(sampleOrg(id));

        mockMvc.perform(patch("/api/organizations/{id}/status", id).param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme"));
    }

    @Test
    void updateOrganizationStatus_missingStatusParam_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/api/organizations/{id}/status", id))
                .andExpect(status().isBadRequest());
    }
}
