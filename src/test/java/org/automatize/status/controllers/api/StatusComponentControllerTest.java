package org.automatize.status.controllers.api;

import org.automatize.status.api.response.StatusComponentResponse;
import org.automatize.status.exceptions.BusinessRuleException;
import org.automatize.status.exceptions.DuplicateResourceException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.services.StatusComponentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class StatusComponentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatusComponentService statusComponentService;

    private StatusComponentResponse sampleResponse(UUID id) {
        StatusComponentResponse r = new StatusComponentResponse();
        r.setId(id);
        r.setName("API");
        r.setStatus("OPERATIONAL");
        return r;
    }

    @Test
    void getAllComponents_returnsOkPage() throws Exception {
        when(statusComponentService.getAllComponents(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse(UUID.randomUUID()))));

        mockMvc.perform(get("/api/components"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("API"));
    }

    @Test
    void getComponentById_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusComponentService.getComponentById(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get("/api/components/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPERATIONAL"));
    }

    @Test
    void getComponentById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusComponentService.getComponentById(id))
                .thenThrow(new ResourceNotFoundException("Component not found with id: " + id));

        mockMvc.perform(get("/api/components/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void createComponent_valid_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(statusComponentService.createComponent(any())).thenReturn(sampleResponse(id));

        String body = "{\"appId\":\"" + UUID.randomUUID() + "\",\"name\":\"API\"}";
        mockMvc.perform(post("/api/components").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("API"));
    }

    @Test
    void createComponent_missingName_returns400() throws Exception {
        String body = "{\"appId\":\"" + UUID.randomUUID() + "\"}";
        mockMvc.perform(post("/api/components").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createComponent_duplicate_returns409() throws Exception {
        when(statusComponentService.createComponent(any()))
                .thenThrow(new DuplicateResourceException("Component with name already exists in this app: API"));

        String body = "{\"appId\":\"" + UUID.randomUUID() + "\",\"name\":\"API\"}";
        mockMvc.perform(post("/api/components").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteComponent_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/components/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(statusComponentService).deleteComponent(id);
    }

    @Test
    void deleteComponent_hasActiveIncidents_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new BusinessRuleException("Cannot delete component with active incidents"))
                .when(statusComponentService).deleteComponent(eq(id));

        mockMvc.perform(delete("/api/components/{id}", id))
                .andExpect(status().isConflict());
    }
}
