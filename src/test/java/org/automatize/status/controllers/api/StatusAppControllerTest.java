package org.automatize.status.controllers.api;

import org.automatize.status.api.response.StatusAppResponse;
import org.automatize.status.exceptions.DuplicateResourceException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.services.StatusAppService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link StatusAppController}. Security filters are
 * disabled ({@code addFilters = false}); focus is request mapping, bean
 * validation (400), JSON contract, {@code @ResponseStatus} exception mapping
 * (404/409), and delegation to the (mocked) service layer.
 */
@WebMvcTest(controllers = StatusAppController.class)
class StatusAppControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private StatusAppService statusAppService;

    private StatusAppResponse sampleResponse(UUID id) {
        StatusAppResponse r = new StatusAppResponse();
        r.setId(id);
        r.setName("My App");
        r.setSlug("my-app");
        r.setStatus("OPERATIONAL");
        return r;
    }

    private String validBody() {
        return "{\"name\":\"My App\",\"slug\":\"my-app\"}";
    }

    @Test
    void getAllStatusApps_returnsOkPage() throws Exception {
        when(statusAppService.getAllStatusApps(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse(UUID.randomUUID()))));

        mockMvc.perform(get("/api/status-apps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("My App"));
    }

    @Test
    void getStatusAppById_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusAppService.getStatusAppById(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get("/api/status-apps/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("my-app"));
    }

    @Test
    void getStatusAppById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusAppService.getStatusAppById(id))
                .thenThrow(new ResourceNotFoundException("Status app not found with id: " + id));

        mockMvc.perform(get("/api/status-apps/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void createStatusApp_valid_returns201() throws Exception {
        when(statusAppService.createStatusApp(any())).thenReturn(sampleResponse(UUID.randomUUID()));

        mockMvc.perform(post("/api/status-apps").contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My App"));
    }

    @Test
    void createStatusApp_missingName_returns400() throws Exception {
        String body = "{\"slug\":\"my-app\"}";
        mockMvc.perform(post("/api/status-apps").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createStatusApp_invalidSlug_returns400() throws Exception {
        String body = "{\"name\":\"My App\",\"slug\":\"Invalid Slug!\"}";
        mockMvc.perform(post("/api/status-apps").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createStatusApp_duplicate_returns409() throws Exception {
        when(statusAppService.createStatusApp(any()))
                .thenThrow(new DuplicateResourceException("Status app with slug already exists: my-app"));

        mockMvc.perform(post("/api/status-apps").contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isConflict());
    }

    @Test
    void updateStatusApp_valid_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusAppService.updateStatusApp(eq(id), any())).thenReturn(sampleResponse(id));

        mockMvc.perform(put("/api/status-apps/{id}", id).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My App"));
    }

    @Test
    void updateStatusApp_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusAppService.updateStatusApp(eq(id), any()))
                .thenThrow(new ResourceNotFoundException("Status app not found with id: " + id));

        mockMvc.perform(put("/api/status-apps/{id}", id).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteStatusApp_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/status-apps/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(statusAppService).deleteStatusApp(id);
    }

    @Test
    void updateStatusAppStatus_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusAppService.updateStatus(eq(id), eq("MAJOR_OUTAGE"))).thenReturn(sampleResponse(id));

        mockMvc.perform(patch("/api/status-apps/{id}/status", id).param("status", "MAJOR_OUTAGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void updateStatusAppStatus_missingStatusParam_returns400() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/api/status-apps/{id}/status", id))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStatusAppsByTenant_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(statusAppService.getStatusAppsByTenant(tenantId))
                .thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/status-apps/tenant/{tenantId}", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("My App"));
    }

    @Test
    void getStatusAppsByOrganization_returnsOk() throws Exception {
        UUID orgId = UUID.randomUUID();
        when(statusAppService.getStatusAppsByOrganization(orgId))
                .thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/status-apps/organization/{organizationId}", orgId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("my-app"));
    }

    @Test
    void getStatusAppsByPlatform_returnsOk() throws Exception {
        UUID platformId = UUID.randomUUID();
        when(statusAppService.getStatusAppsByPlatform(platformId))
                .thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/status-apps/platform/{platformId}", platformId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("My App"));
    }
}
