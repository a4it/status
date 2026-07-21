package org.automatize.status.controllers.api;

import org.automatize.status.api.response.StatusPlatformResponse;
import org.automatize.status.exceptions.DuplicateResourceException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.services.StatusPlatformService;
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
 * WebMvc slice tests for {@link StatusPlatformController}. Security filters are
 * disabled ({@code addFilters = false}); focus is request mapping, bean
 * validation (400), JSON contract, {@code @ResponseStatus} exception mapping
 * (404/409), and delegation to the (mocked) service layer.
 */
@WebMvcTest(controllers = StatusPlatformController.class)
class StatusPlatformControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private StatusPlatformService statusPlatformService;

    private StatusPlatformResponse sampleResponse(UUID id) {
        StatusPlatformResponse r = new StatusPlatformResponse();
        r.setId(id);
        r.setName("Cloud");
        r.setSlug("cloud");
        r.setStatus("OPERATIONAL");
        return r;
    }

    private String validBody() {
        return "{\"name\":\"Cloud\",\"slug\":\"cloud\"}";
    }

    @Test
    void getAllPlatforms_returnsOkPage() throws Exception {
        when(statusPlatformService.getAllPlatforms(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse(UUID.randomUUID()))));

        mockMvc.perform(get("/api/status-platforms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Cloud"));
    }

    @Test
    void getAllPlatformsOrdered_returnsOk() throws Exception {
        when(statusPlatformService.getAllPlatformsOrdered())
                .thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/status-platforms/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("cloud"));
    }

    @Test
    void getPlatformById_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusPlatformService.getPlatformById(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get("/api/status-platforms/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cloud"));
    }

    @Test
    void getPlatformById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusPlatformService.getPlatformById(id))
                .thenThrow(new ResourceNotFoundException("Platform not found with id: " + id));

        mockMvc.perform(get("/api/status-platforms/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPlatformBySlug_found_returnsOk() throws Exception {
        when(statusPlatformService.getPlatformBySlug("cloud")).thenReturn(sampleResponse(UUID.randomUUID()));

        mockMvc.perform(get("/api/status-platforms/slug/{slug}", "cloud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("cloud"));
    }

    @Test
    void createPlatform_valid_returns201() throws Exception {
        when(statusPlatformService.createPlatform(any(), any(), any()))
                .thenReturn(sampleResponse(UUID.randomUUID()));

        mockMvc.perform(post("/api/status-platforms").contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cloud"));
    }

    @Test
    void createPlatform_missingName_returns400() throws Exception {
        String body = "{\"slug\":\"cloud\"}";
        mockMvc.perform(post("/api/status-platforms").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPlatform_duplicate_returns409() throws Exception {
        when(statusPlatformService.createPlatform(any(), any(), any()))
                .thenThrow(new DuplicateResourceException("Platform with slug already exists: cloud"));

        mockMvc.perform(post("/api/status-platforms").contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isConflict());
    }

    @Test
    void updatePlatform_valid_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusPlatformService.updatePlatform(eq(id), any(), any(), any())).thenReturn(sampleResponse(id));

        mockMvc.perform(put("/api/status-platforms/{id}", id).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void updatePlatform_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusPlatformService.updatePlatform(eq(id), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Platform not found with id: " + id));

        mockMvc.perform(put("/api/status-platforms/{id}", id).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePlatform_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/status-platforms/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(statusPlatformService).deletePlatform(id);
    }

    @Test
    void updatePlatformStatus_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusPlatformService.updateStatus(eq(id), eq("DEGRADED"))).thenReturn(sampleResponse(id));

        mockMvc.perform(patch("/api/status-platforms/{id}/status", id).param("status", "DEGRADED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cloud"));
    }

    @Test
    void getPlatformsByTenant_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(statusPlatformService.getPlatformsByTenant(tenantId))
                .thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/status-platforms/tenant/{tenantId}", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("cloud"));
    }
}
