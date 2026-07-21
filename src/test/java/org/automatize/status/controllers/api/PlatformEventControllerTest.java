package org.automatize.status.controllers.api;

import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.exceptions.UnauthorizedException;
import org.automatize.status.models.PlatformEvent;
import org.automatize.status.services.PlatformEventService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link PlatformEventController}. Security filters are
 * disabled ({@code addFilters = false}); focus is request mapping, bean
 * validation (400), JSON contract, and the controller's own try/catch mapping
 * of service {@code RuntimeException}s to 401/400, plus {@code @ResponseStatus}
 * exception mapping (404) and unhandled {@code RuntimeException} (500).
 */
@WebMvcTest(controllers = PlatformEventController.class)
class PlatformEventControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private PlatformEventService platformEventService;

    /**
     * Builds a representative {@link PlatformEvent} used to stub the service.
     *
     * @param id the event id to assign
     * @return a populated {@link PlatformEvent} with {@code ERROR} severity
     */
    private PlatformEvent sampleEvent(UUID id) {
        PlatformEvent e = new PlatformEvent();
        e.setId(id);
        e.setSeverity("ERROR");
        e.setMessage("Something broke");
        e.setSource("monitor");
        return e;
    }

    // ---- logEventWithApiKey ----

    /**
     * Verifies {@code POST /api/events/log} with a valid API key and body returns
     * 201 with the created event's message.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    void logEventWithApiKey_valid_returns201() throws Exception {
        when(platformEventService.createEventWithApiKey(any(), any(), any(), any(), any(), any()))
                .thenReturn(sampleEvent(UUID.randomUUID()));

        String body = "{\"severity\":\"ERROR\",\"message\":\"Something broke\"}";
        mockMvc.perform(post("/api/events/log")
                        .header("X-API-Key", "secret-key")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Something broke"));
    }

    /**
     * Verifies {@code POST /api/events/log} without the {@code X-API-Key} header
     * returns 401 with {@code success=false}.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    void logEventWithApiKey_missingApiKey_returns401() throws Exception {
        String body = "{\"severity\":\"ERROR\",\"message\":\"Something broke\"}";
        mockMvc.perform(post("/api/events/log")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * Verifies {@code POST /api/events/log} maps a service
     * {@link UnauthorizedException} (invalid key) to HTTP 401 with
     * {@code success=false}.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    void logEventWithApiKey_invalidApiKey_returns401() throws Exception {
        when(platformEventService.createEventWithApiKey(any(), any(), any(), any(), any(), any()))
                .thenThrow(new UnauthorizedException("Invalid API key"));

        String body = "{\"severity\":\"ERROR\",\"message\":\"Something broke\"}";
        mockMvc.perform(post("/api/events/log")
                        .header("X-API-Key", "bad-key")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * Verifies {@code POST /api/events/log} with a missing message fails bean
     * validation and returns 400.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    void logEventWithApiKey_missingMessage_returns400() throws Exception {
        String body = "{\"severity\":\"ERROR\"}";
        mockMvc.perform(post("/api/events/log")
                        .header("X-API-Key", "secret-key")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies {@code POST /api/events/log} with an invalid severity value fails
     * bean validation and returns 400.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    void logEventWithApiKey_invalidSeverity_returns400() throws Exception {
        String body = "{\"severity\":\"NOPE\",\"message\":\"Something broke\"}";
        mockMvc.perform(post("/api/events/log")
                        .header("X-API-Key", "secret-key")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // ---- getEvents ----

    /**
     * Verifies {@code GET /api/events} returns 200 with a paged result whose first
     * content entry has the expected severity.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    void getEvents_returnsOkPage() throws Exception {
        when(platformEventService.searchEvents(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleEvent(UUID.randomUUID()))));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].severity").value("ERROR"));
    }

    // ---- getEventById ----

    /**
     * Verifies {@code GET /api/events/{id}} for an existing event returns 200 with
     * the matching {@code id}.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    void getEventById_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(platformEventService.getEventById(id)).thenReturn(sampleEvent(id));

        mockMvc.perform(get("/api/events/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    /**
     * Verifies {@code GET /api/events/{id}} maps a service
     * {@link ResourceNotFoundException} to HTTP 404.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    void getEventById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(platformEventService.getEventById(id))
                .thenThrow(new ResourceNotFoundException("Event not found with id: " + id));

        mockMvc.perform(get("/api/events/{id}", id))
                .andExpect(status().isNotFound());
    }

    // ---- createEvent ----

    /**
     * Verifies {@code POST /api/events} with a valid body returns 201 with the
     * created event's severity.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    void createEvent_valid_returns201() throws Exception {
        when(platformEventService.createEvent(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(sampleEvent(UUID.randomUUID()));

        String body = "{\"appId\":\"" + UUID.randomUUID() + "\",\"severity\":\"ERROR\",\"message\":\"Something broke\"}";
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.severity").value("ERROR"));
    }

    /**
     * Verifies {@code POST /api/events} with a missing appId fails bean validation
     * and returns 400.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    void createEvent_missingAppId_returns400() throws Exception {
        String body = "{\"severity\":\"ERROR\",\"message\":\"Something broke\"}";
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies {@code POST /api/events} with an invalid severity value fails bean
     * validation and returns 400.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    void createEvent_invalidSeverity_returns400() throws Exception {
        String body = "{\"appId\":\"" + UUID.randomUUID() + "\",\"severity\":\"NOPE\",\"message\":\"Something broke\"}";
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that a plain (unannotated) {@link RuntimeException} thrown by the
     * service on {@code POST /api/events} is not mapped to a status and instead
     * propagates out of the servlet (asserted via {@code assertThrows}).
     */
    @Test
    void createEvent_serviceRuntimeException_propagates() {
        when(platformEventService.createEvent(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Status app not found"));

        // No @ControllerAdvice and a plain (unannotated) RuntimeException: the
        // exception is not mapped to a status and propagates out of the servlet.
        String body = "{\"appId\":\"" + UUID.randomUUID() + "\",\"severity\":\"ERROR\",\"message\":\"Something broke\"}";
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(body)));
    }

    // ---- deleteEvent ----

    /**
     * Verifies {@code DELETE /api/events/{id}} returns 200 with {@code success=true}
     * and delegates to {@link PlatformEventService#deleteEvent}.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    void deleteEvent_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/events/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(platformEventService).deleteEvent(id);
    }

    // ---- regenerate key ----

    /**
     * Verifies {@code POST /api/events/regenerate-key/component/{componentId}}
     * returns 200 with the new {@code apiKey} and {@code success=true}.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    void regenerateComponentApiKey_returnsOk() throws Exception {
        UUID componentId = UUID.randomUUID();
        when(platformEventService.regenerateComponentApiKey(componentId)).thenReturn("new-key-123");

        mockMvc.perform(post("/api/events/regenerate-key/component/{componentId}", componentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKey").value("new-key-123"))
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * Verifies {@code POST /api/events/regenerate-key/component/{componentId}}
     * maps a service {@link RuntimeException} to HTTP 400 with
     * {@code success=false}.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    void regenerateComponentApiKey_serviceFails_returns400() throws Exception {
        UUID componentId = UUID.randomUUID();
        when(platformEventService.regenerateComponentApiKey(componentId))
                .thenThrow(new RuntimeException("Component not found"));

        mockMvc.perform(post("/api/events/regenerate-key/component/{componentId}", componentId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * Verifies {@code POST /api/events/regenerate-key/app/{appId}} returns 200 with
     * the new {@code apiKey} and {@code success=true}.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    void regenerateAppApiKey_returnsOk() throws Exception {
        UUID appId = UUID.randomUUID();
        when(platformEventService.regenerateAppApiKey(appId)).thenReturn("app-key-456");

        mockMvc.perform(post("/api/events/regenerate-key/app/{appId}", appId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKey").value("app-key-456"))
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * Verifies {@code POST /api/events/regenerate-key/app/{appId}} maps a service
     * {@link RuntimeException} to HTTP 400 with {@code success=false}.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    void regenerateAppApiKey_serviceFails_returns400() throws Exception {
        UUID appId = UUID.randomUUID();
        when(platformEventService.regenerateAppApiKey(appId))
                .thenThrow(new RuntimeException("App not found"));

        mockMvc.perform(post("/api/events/regenerate-key/app/{appId}", appId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
