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

    private static final String TITLE_DB_UPGRADE = "DB upgrade";
    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String JSON_APP_ID_PREFIX = "{\"appId\":\"";
    private static final String BASE_PATH = "/api/maintenance";
    private static final String ID_PATH = "/api/maintenance/{id}";

    @MockitoBean
    private StatusMaintenanceService statusMaintenanceService;

    /**
     * Builds a sample {@link StatusMaintenanceResponse} for stubbing service returns.
     *
     * @param id the identifier to assign to the response
     * @return a sample maintenance response with representative field values
     */
    private StatusMaintenanceResponse sampleResponse(UUID id) {
        StatusMaintenanceResponse r = new StatusMaintenanceResponse();
        r.setId(id);
        r.setTitle(TITLE_DB_UPGRADE);
        r.setStatus(STATUS_SCHEDULED);
        return r;
    }

    /**
     * Provides a minimal valid JSON request body that satisfies bean validation for maintenance creation.
     *
     * @return a JSON string with the required maintenance fields
     */
    private String validBody() {
        return JSON_APP_ID_PREFIX + UUID.randomUUID() + "\",\"title\":\"DB upgrade\","
                + "\"status\":\"SCHEDULED\",\"startsAt\":\"2026-01-01T10:00:00Z\","
                + "\"endsAt\":\"2026-01-01T12:00:00Z\"}";
    }

    @Test
    /**
     * Verifies GET /api/maintenance returns 200 OK with a paged JSON body of maintenance windows.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getAllMaintenance_returnsOkPage() throws Exception {
        when(statusMaintenanceService.getAllMaintenance(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse(UUID.randomUUID()))));

        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value(TITLE_DB_UPGRADE));
    }

    @Test
    /**
     * Verifies GET /api/maintenance/{id} returns 200 OK with the maintenance window when it exists.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getMaintenanceById_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusMaintenanceService.getMaintenanceById(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get(ID_PATH, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(STATUS_SCHEDULED));
    }

    @Test
    /**
     * Verifies GET /api/maintenance/{id} maps {@link ResourceNotFoundException} to 404 Not Found.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getMaintenanceById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusMaintenanceService.getMaintenanceById(id))
                .thenThrow(new ResourceNotFoundException("Maintenance not found with id: " + id));

        mockMvc.perform(get(ID_PATH, id))
                .andExpect(status().isNotFound());
    }

    @Test
    /**
     * Verifies POST /api/maintenance with a valid body returns 201 Created and echoes the maintenance window.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void createMaintenance_valid_returns201() throws Exception {
        when(statusMaintenanceService.createMaintenance(any())).thenReturn(sampleResponse(UUID.randomUUID()));

        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(TITLE_DB_UPGRADE));
    }

    @Test
    /**
     * Verifies POST /api/maintenance with a missing title fails bean validation with 400 Bad Request.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void createMaintenance_missingTitle_returns400() throws Exception {
        String body = JSON_APP_ID_PREFIX + UUID.randomUUID() + "\",\"status\":\"SCHEDULED\","
                + "\"startsAt\":\"2026-01-01T10:00:00Z\",\"endsAt\":\"2026-01-01T12:00:00Z\"}";
        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    /**
     * Verifies POST /api/maintenance with a missing endsAt fails bean validation with 400 Bad Request.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void createMaintenance_missingEndsAt_returns400() throws Exception {
        String body = JSON_APP_ID_PREFIX + UUID.randomUUID() + "\",\"title\":\"DB upgrade\","
                + "\"status\":\"SCHEDULED\",\"startsAt\":\"2026-01-01T10:00:00Z\"}";
        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    /**
     * Verifies PUT /api/maintenance/{id} with a valid body returns 200 OK with the updated maintenance window.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void updateMaintenance_valid_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusMaintenanceService.updateMaintenance(eq(id), any())).thenReturn(sampleResponse(id));

        mockMvc.perform(put(ID_PATH, id).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    /**
     * Verifies DELETE /api/maintenance/{id} returns 200 OK with a success message and delegates to the service.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void deleteMaintenance_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete(ID_PATH, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(statusMaintenanceService).deleteMaintenance(id);
    }

    @Test
    /**
     * Verifies PATCH /api/maintenance/{id}/status with a status param returns 200 OK with the updated maintenance window.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void updateMaintenanceStatus_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusMaintenanceService.updateStatus(eq(id), eq("COMPLETED"))).thenReturn(sampleResponse(id));

        mockMvc.perform(patch("/api/maintenance/{id}/status", id).param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(TITLE_DB_UPGRADE));
    }

    @Test
    /**
     * Verifies GET /api/maintenance/upcoming returns 200 OK with upcoming maintenance windows.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getUpcomingMaintenance_returnsOk() throws Exception {
        when(statusMaintenanceService.getUpcomingMaintenance(any(), any(), eq(30)))
                .thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/maintenance/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value(TITLE_DB_UPGRADE));
    }

    @Test
    /**
     * Verifies GET /api/maintenance/active returns 200 OK with the currently active maintenance windows.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getActiveMaintenance_returnsOk() throws Exception {
        when(statusMaintenanceService.getActiveMaintenance(any(), any()))
                .thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/maintenance/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value(STATUS_SCHEDULED));
    }

    @Test
    /**
     * Verifies POST /api/maintenance/{id}/start returns 200 OK with the started maintenance window.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void startMaintenance_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusMaintenanceService.startMaintenance(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(post("/api/maintenance/{id}/start", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    /**
     * Verifies POST /api/maintenance/{id}/start maps {@link BusinessRuleException} for a non-startable state to 409 Conflict.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void startMaintenance_invalidState_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusMaintenanceService.startMaintenance(id))
                .thenThrow(new BusinessRuleException("Maintenance is not in a startable state"));

        mockMvc.perform(post("/api/maintenance/{id}/start", id))
                .andExpect(status().isConflict());
    }

    @Test
    /**
     * Verifies POST /api/maintenance/{id}/complete returns 200 OK with the completed maintenance window.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void completeMaintenance_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusMaintenanceService.completeMaintenance(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(post("/api/maintenance/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }
}
