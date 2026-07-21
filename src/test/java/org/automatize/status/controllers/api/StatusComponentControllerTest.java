package org.automatize.status.controllers.api;

import org.automatize.status.api.response.StatusComponentResponse;
import org.automatize.status.exceptions.BusinessRuleException;
import org.automatize.status.exceptions.DuplicateResourceException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.services.StatusComponentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link StatusComponentController}. Security filters are
 * disabled ({@code addFilters = false}); focus is request mapping, bean
 * validation (400), JSON contract, {@code @ResponseStatus} exception mapping
 * (404/409), and delegation to the (mocked) service layer.
 */
@WebMvcTest(controllers = StatusComponentController.class)
class StatusComponentControllerTest extends AbstractApiControllerTest {

    private static final String COMPONENTS_PATH = "/api/components";
    private static final String COMPONENT_BY_ID_PATH = "/api/components/{id}";
    private static final String APP_ID_JSON_PREFIX = "{\"appId\":\"";

    @MockitoBean
    private StatusComponentService statusComponentService;

    /**
     * Builds a sample {@link StatusComponentResponse} for stubbing service returns.
     *
     * @param id the identifier to assign to the response
     * @return a sample component response with representative field values
     */
    private StatusComponentResponse sampleResponse(UUID id) {
        StatusComponentResponse r = new StatusComponentResponse();
        r.setId(id);
        r.setName("API");
        r.setStatus("OPERATIONAL");
        return r;
    }

    @Test
    /**
     * Verifies GET /api/components returns 200 OK with a paged JSON body of components.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getAllComponents_returnsOkPage() throws Exception {
        when(statusComponentService.getAllComponents(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse(UUID.randomUUID()))));

        mockMvc.perform(get(COMPONENTS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("API"));
    }

    @Test
    /**
     * Verifies GET /api/components/{id} returns 200 OK with the component when it exists.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getComponentById_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusComponentService.getComponentById(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get(COMPONENT_BY_ID_PATH, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPERATIONAL"));
    }

    @Test
    /**
     * Verifies GET /api/components/{id} maps {@link ResourceNotFoundException} to 404 Not Found.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void getComponentById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusComponentService.getComponentById(id))
                .thenThrow(new ResourceNotFoundException("Component not found with id: " + id));

        mockMvc.perform(get(COMPONENT_BY_ID_PATH, id))
                .andExpect(status().isNotFound());
    }

    @Test
    /**
     * Verifies POST /api/components with a valid body returns 201 Created and echoes the component.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void createComponent_valid_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusComponentService.createComponent(any())).thenReturn(sampleResponse(id));

        String body = APP_ID_JSON_PREFIX + UUID.randomUUID() + "\",\"name\":\"API\"}";
        mockMvc.perform(post(COMPONENTS_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("API"));
    }

    @Test
    /**
     * Verifies POST /api/components with a missing name fails bean validation with 400 Bad Request.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void createComponent_missingName_returns400() throws Exception {
        String body = APP_ID_JSON_PREFIX + UUID.randomUUID() + "\"}";
        mockMvc.perform(post(COMPONENTS_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    /**
     * Verifies POST /api/components maps {@link DuplicateResourceException} to 409 Conflict.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void createComponent_duplicate_returns409() throws Exception {
        when(statusComponentService.createComponent(any()))
                .thenThrow(new DuplicateResourceException("Component with name already exists in this app: API"));

        String body = APP_ID_JSON_PREFIX + UUID.randomUUID() + "\",\"name\":\"API\"}";
        mockMvc.perform(post(COMPONENTS_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    /**
     * Verifies DELETE /api/components/{id} returns 200 OK with a success message and delegates to the service.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void deleteComponent_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete(COMPONENT_BY_ID_PATH, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(statusComponentService).deleteComponent(id);
    }

    @Test
    /**
     * Verifies DELETE /api/components/{id} maps {@link BusinessRuleException} for active incidents to 409 Conflict.
     *
     * @throws Exception if the mock request cannot be performed
     */
    void deleteComponent_hasActiveIncidents_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new BusinessRuleException("Cannot delete component with active incidents"))
                .when(statusComponentService).deleteComponent(eq(id));

        mockMvc.perform(delete(COMPONENT_BY_ID_PATH, id))
                .andExpect(status().isConflict());
    }
}
