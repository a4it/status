package org.automatize.status.controllers.api;

import org.automatize.status.models.DropRule;
import org.automatize.status.services.DropRuleService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link DropRuleController} (CRUD over log drop rules).
 */
@WebMvcTest(controllers = DropRuleController.class)
class DropRuleControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private DropRuleService dropRuleService;

    /**
     * Builds a fully populated sample {@link DropRule} for use as service stub
     * output, with fixed level, service, message pattern, and active flag.
     *
     * @param id   the identifier to assign to the rule
     * @param name the name to assign to the rule
     * @return a populated {@link DropRule} instance
     */
    private DropRule sampleRule(UUID id, String name) {
        DropRule r = new DropRule();
        r.setId(id);
        r.setName(name);
        r.setLevel("DEBUG");
        r.setService("orders");
        r.setMessagePattern("noise");
        r.setIsActive(true);
        return r;
    }

    /**
     * Verifies that listing drop rules returns {@code 200 OK} with the rules
     * from the mocked service serialized into the JSON array.
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void findAll_returnsOk() throws Exception {
        when(dropRuleService.findAll()).thenReturn(List.of(sampleRule(UUID.randomUUID(), "drop-debug")));

        mockMvc.perform(get("/api/drop-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("drop-debug"));
    }

    /**
     * Verifies that fetching an existing drop rule by id returns {@code 200 OK}
     * with the rule's fields (e.g. {@code level}) in the JSON body.
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void findById_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(dropRuleService.findById(id)).thenReturn(sampleRule(id, "drop-debug"));

        mockMvc.perform(get("/api/drop-rules/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("DEBUG"));
    }

    /**
     * Verifies that when the service throws a plain {@link RuntimeException} for a
     * missing rule, the exception propagates out of {@code perform()} (there is no
     * {@code @ResponseStatus} or resolver to map it to an HTTP status).
     */
    @Test
    void findById_notFound_propagates() {
        UUID id = UUID.randomUUID();
        when(dropRuleService.findById(id)).thenThrow(new RuntimeException("Drop rule not found: " + id));

        // DropRuleService throws a plain RuntimeException (no @ResponseStatus); with no
        // matching resolver the exception propagates out of perform().
        Assertions.assertThrows(Exception.class,
                () -> mockMvc.perform(get("/api/drop-rules/{id}", id)));
    }

    /**
     * Verifies that creating a valid drop rule returns {@code 201 Created} with
     * the created rule's {@code name} in the JSON body.
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void create_valid_returns201() throws Exception {
        DropRule rule = sampleRule(UUID.randomUUID(), "drop-debug");
        when(dropRuleService.create(any(), any(), any(), any(), any(), anyBoolean())).thenReturn(rule);

        String body = "{\"name\":\"drop-debug\",\"level\":\"DEBUG\",\"service\":\"orders\",\"active\":true}";
        mockMvc.perform(post("/api/drop-rules")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("drop-debug"));
    }

    /**
     * Verifies that creating a drop rule without the required {@code name} field
     * fails validation and returns {@code 400 Bad Request}.
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void create_missingName_returns400() throws Exception {
        String body = "{\"level\":\"DEBUG\"}";
        mockMvc.perform(post("/api/drop-rules")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that updating a drop rule returns {@code 200 OK} with the updated
     * {@code name} reflected in the JSON body.
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void update_valid_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(dropRuleService.update(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(sampleRule(id, "drop-debug-updated"));

        String body = "{\"name\":\"drop-debug-updated\",\"active\":false}";
        mockMvc.perform(put("/api/drop-rules/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("drop-debug-updated"));
    }

    /**
     * Verifies that toggling a drop rule's active state returns {@code 200 OK}
     * with the new {@code isActive} value in the JSON body.
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void toggle_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        DropRule rule = sampleRule(id, "drop-debug");
        rule.setIsActive(false);
        when(dropRuleService.toggleActive(id)).thenReturn(rule);

        mockMvc.perform(post("/api/drop-rules/{id}/toggle", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    /**
     * Verifies that deleting a drop rule returns {@code 200 OK} with
     * {@code success=true} and that the service's {@code delete} method is invoked
     * with the requested id.
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void delete_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/drop-rules/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(dropRuleService).delete(id);
    }
}
