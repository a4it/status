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

    private static final String PLATFORM_NAME = "Cloud";
    private static final String PLATFORM_SLUG = "cloud";
    private static final String BASE_PATH = "/api/status-platforms";
    private static final String ID_PATH = "/api/status-platforms/{id}";
    private static final String JSON_PATH_NAME = "$.name";

    @MockitoBean
    private StatusPlatformService statusPlatformService;

    /**
     * Builds a sample {@link StatusPlatformResponse} for stubbing service returns.
     *
     * @param id the identifier to assign to the response
     * @return a sample platform response with representative field values
     */
    private StatusPlatformResponse sampleResponse(UUID id) {
        StatusPlatformResponse r = new StatusPlatformResponse();
        r.setId(id);
        r.setName(PLATFORM_NAME);
        r.setSlug(PLATFORM_SLUG);
        r.setStatus("OPERATIONAL");
        return r;
    }

    /**
     * Provides a minimal valid JSON request body that satisfies bean validation for platform creation.
     *
     * @return a JSON string with the required name and slug fields
     */
    private String validBody() {
        return "{\"name\":\"Cloud\",\"slug\":\"cloud\"}";
    }

    @Test
    /**
     * Verifies GET /api/status-platforms returns 200 OK with a paged JSON body of platforms.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getAllPlatforms_returnsOkPage() throws Exception {
        when(statusPlatformService.getAllPlatforms(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse(UUID.randomUUID()))));

        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value(PLATFORM_NAME));
    }

    @Test
    /**
     * Verifies GET /api/status-platforms/all returns 200 OK with all platforms in display order.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getAllPlatformsOrdered_returnsOk() throws Exception {
        when(statusPlatformService.getAllPlatformsOrdered())
                .thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/status-platforms/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value(PLATFORM_SLUG));
    }

    @Test
    /**
     * Verifies GET /api/status-platforms/{id} returns 200 OK with the platform when it exists.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getPlatformById_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusPlatformService.getPlatformById(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get(ID_PATH, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_NAME).value(PLATFORM_NAME));
    }

    @Test
    /**
     * Verifies GET /api/status-platforms/{id} maps {@link ResourceNotFoundException} to 404 Not Found.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getPlatformById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusPlatformService.getPlatformById(id))
                .thenThrow(new ResourceNotFoundException("Platform not found with id: " + id));

        mockMvc.perform(get(ID_PATH, id))
                .andExpect(status().isNotFound());
    }

    @Test
    /**
     * Verifies GET /api/status-platforms/slug/{slug} returns 200 OK with the matching platform.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getPlatformBySlug_found_returnsOk() throws Exception {
        when(statusPlatformService.getPlatformBySlug(PLATFORM_SLUG)).thenReturn(sampleResponse(UUID.randomUUID()));

        mockMvc.perform(get("/api/status-platforms/slug/{slug}", PLATFORM_SLUG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(PLATFORM_SLUG));
    }

    @Test
    /**
     * Verifies POST /api/status-platforms with a valid body returns 201 Created and echoes the platform.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void createPlatform_valid_returns201() throws Exception {
        when(statusPlatformService.createPlatform(any(), any(), any()))
                .thenReturn(sampleResponse(UUID.randomUUID()));

        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_PATH_NAME).value(PLATFORM_NAME));
    }

    @Test
    /**
     * Verifies POST /api/status-platforms with a missing name fails bean validation with 400 Bad Request.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void createPlatform_missingName_returns400() throws Exception {
        String body = "{\"slug\":\"cloud\"}";
        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    /**
     * Verifies POST /api/status-platforms maps {@link DuplicateResourceException} to 409 Conflict.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void createPlatform_duplicate_returns409() throws Exception {
        when(statusPlatformService.createPlatform(any(), any(), any()))
                .thenThrow(new DuplicateResourceException("Platform with slug already exists: cloud"));

        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isConflict());
    }

    @Test
    /**
     * Verifies PUT /api/status-platforms/{id} with a valid body returns 200 OK with the updated platform.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void updatePlatform_valid_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusPlatformService.updatePlatform(eq(id), any(), any(), any())).thenReturn(sampleResponse(id));

        mockMvc.perform(put(ID_PATH, id).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    /**
     * Verifies PUT /api/status-platforms/{id} maps {@link ResourceNotFoundException} to 404 Not Found.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void updatePlatform_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusPlatformService.updatePlatform(eq(id), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Platform not found with id: " + id));

        mockMvc.perform(put(ID_PATH, id).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isNotFound());
    }

    @Test
    /**
     * Verifies DELETE /api/status-platforms/{id} returns 200 OK with a success message and delegates to the service.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void deletePlatform_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete(ID_PATH, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(statusPlatformService).deletePlatform(id);
    }

    @Test
    /**
     * Verifies PATCH /api/status-platforms/{id}/status with a status param returns 200 OK with the updated platform.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void updatePlatformStatus_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusPlatformService.updateStatus(eq(id), eq("DEGRADED"))).thenReturn(sampleResponse(id));

        mockMvc.perform(patch("/api/status-platforms/{id}/status", id).param("status", "DEGRADED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_NAME).value(PLATFORM_NAME));
    }

    @Test
    /**
     * Verifies GET /api/status-platforms/tenant/{tenantId} returns 200 OK with the tenant's platforms.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getPlatformsByTenant_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(statusPlatformService.getPlatformsByTenant(tenantId))
                .thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/status-platforms/tenant/{tenantId}", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value(PLATFORM_SLUG));
    }
}
