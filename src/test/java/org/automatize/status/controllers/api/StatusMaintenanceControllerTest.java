package org.automatize.status.controllers.api;

import org.automatize.status.api.response.StatusMaintenanceResponse;
import org.automatize.status.exceptions.BusinessRuleException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.services.StatusMaintenanceService;
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
 * WebMvc slice tests for {@link StatusMaintenanceController}. Security filters are
 * disabled ({@code addFilters = false}); focus is request mapping, bean
 * validation (400), JSON contract, {@code @ResponseStatus} exception mapping
 * (404/409), and delegation to the (mocked) service layer.
 */
@WebMvcTest(controllers = StatusMaintenanceController.class)
class StatusMaintenanceControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private StatusMaintenanceService statusMaintenanceService;

    private StatusMaintenanceResponse sampleResponse(UUID id) {
        StatusMaintenanceResponse r = new StatusMaintenanceResponse();
        r.setId(id);
        r.setTitle("DB upgrade");
        r.setStatus("SCHEDULED");
        return r;
    }

    private String validBody() {
        return "{\"appId\":\"" + UUID.randomUUID() + "\",\"title\":\"DB upgrade\","
                + "\"status\":\"SCHEDULED\",\"startsAt\":\"2026-01-01T10:00:00Z\","
                + "\"endsAt\":\"2026-01-01T12:00:00Z\"}";
    }

    @Test
    void getAllMaintenance_returnsOkPage() throws Exception {
        when(statusMaintenanceService.getAllMaintenance(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse(UUID.randomUUID()))));

        mockMvc.perform(get("/api/maintenance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("DB upgrade"));
    }

    @Test
    void getMaintenanceById_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusMaintenanceService.getMaintenanceById(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get("/api/maintenance/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void getMaintenanceById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusMaintenanceService.getMaintenanceById(id))
                .thenThrow(new ResourceNotFoundException("Maintenance not found with id: " + id));

        mockMvc.perform(get("/api/maintenance/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void createMaintenance_valid_returns201() throws Exception {
        when(statusMaintenanceService.createMaintenance(any())).thenReturn(sampleResponse(UUID.randomUUID()));

        mockMvc.perform(post("/api/maintenance").contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("DB upgrade"));
    }

    @Test
    void createMaintenance_missingTitle_returns400() throws Exception {
        String body = "{\"appId\":\"" + UUID.randomUUID() + "\",\"status\":\"SCHEDULED\","
                + "\"startsAt\":\"2026-01-01T10:00:00Z\",\"endsAt\":\"2026-01-01T12:00:00Z\"}";
        mockMvc.perform(post("/api/maintenance").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMaintenance_missingEndsAt_returns400() throws Exception {
        String body = "{\"appId\":\"" + UUID.randomUUID() + "\",\"title\":\"DB upgrade\","
                + "\"status\":\"SCHEDULED\",\"startsAt\":\"2026-01-01T10:00:00Z\"}";
        mockMvc.perform(post("/api/maintenance").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMaintenance_valid_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusMaintenanceService.updateMaintenance(eq(id), any())).thenReturn(sampleResponse(id));

        mockMvc.perform(put("/api/maintenance/{id}", id).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void deleteMaintenance_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/maintenance/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(statusMaintenanceService).deleteMaintenance(id);
    }

    @Test
    void updateMaintenanceStatus_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusMaintenanceService.updateStatus(eq(id), eq("COMPLETED"))).thenReturn(sampleResponse(id));

        mockMvc.perform(patch("/api/maintenance/{id}/status", id).param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("DB upgrade"));
    }

    @Test
    void getUpcomingMaintenance_returnsOk() throws Exception {
        when(statusMaintenanceService.getUpcomingMaintenance(any(), any(), eq(30)))
                .thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/maintenance/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("DB upgrade"));
    }

    @Test
    void getActiveMaintenance_returnsOk() throws Exception {
        when(statusMaintenanceService.getActiveMaintenance(any(), any()))
                .thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/maintenance/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));
    }

    @Test
    void startMaintenance_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusMaintenanceService.startMaintenance(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(post("/api/maintenance/{id}/start", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void startMaintenance_invalidState_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusMaintenanceService.startMaintenance(id))
                .thenThrow(new BusinessRuleException("Maintenance is not in a startable state"));

        mockMvc.perform(post("/api/maintenance/{id}/start", id))
                .andExpect(status().isConflict());
    }

    @Test
    void completeMaintenance_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusMaintenanceService.completeMaintenance(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(post("/api/maintenance/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }
}
