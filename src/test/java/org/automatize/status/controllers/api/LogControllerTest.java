package org.automatize.status.controllers.api;

import org.automatize.status.models.Log;
import org.automatize.status.models.LogApiKey;
import org.automatize.status.services.LogApiKeyService;
import org.automatize.status.services.LogIngestionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link LogController}. Ingestion endpoints authenticate
 * via the {@code X-Log-Api-Key} header and translate validation failures to 401
 * in-controller; query endpoints delegate to {@link LogIngestionService}.
 */
@WebMvcTest(controllers = LogController.class)
class LogControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private LogIngestionService logIngestionService;

    @MockitoBean
    private LogApiKeyService logApiKeyService;

    private Log sampleLog(UUID id) {
        Log log = new Log();
        log.setId(id);
        log.setLevel("INFO");
        log.setService("orders");
        log.setMessage("order placed");
        log.setLogTimestamp(ZonedDateTime.now());
        return log;
    }

    // ─── POST /api/logs ────────────────────────────────────────────────────────

    @Test
    void ingest_validWithApiKey_returns201() throws Exception {
        when(logIngestionService.validateApiKey("k")).thenReturn(new LogApiKey());
        when(logIngestionService.ingest(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(sampleLog(UUID.randomUUID()));

        String body = "{\"level\":\"INFO\",\"service\":\"orders\",\"message\":\"order placed\"}";
        mockMvc.perform(post("/api/logs")
                        .header("X-Log-Api-Key", "k")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.level").value("INFO"))
                .andExpect(jsonPath("$.service").value("orders"));
    }

    @Test
    void ingest_droppedByRule_returnsOkMessage() throws Exception {
        when(logIngestionService.validateApiKey("k")).thenReturn(new LogApiKey());
        when(logIngestionService.ingest(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(null);

        String body = "{\"level\":\"INFO\",\"service\":\"orders\",\"message\":\"order placed\"}";
        mockMvc.perform(post("/api/logs")
                        .header("X-Log-Api-Key", "k")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void ingest_missingApiKeyHeader_returns401() throws Exception {
        String body = "{\"level\":\"INFO\",\"service\":\"orders\",\"message\":\"order placed\"}";
        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void ingest_invalidApiKey_returns401() throws Exception {
        when(logIngestionService.validateApiKey("bad"))
                .thenThrow(new RuntimeException("Invalid or inactive API key"));

        String body = "{\"level\":\"INFO\",\"service\":\"orders\",\"message\":\"order placed\"}";
        mockMvc.perform(post("/api/logs")
                        .header("X-Log-Api-Key", "bad")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void ingest_missingLevel_returns400() throws Exception {
        String body = "{\"service\":\"orders\",\"message\":\"order placed\"}";
        mockMvc.perform(post("/api/logs")
                        .header("X-Log-Api-Key", "k")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingest_invalidLevelValue_returns400() throws Exception {
        String body = "{\"level\":\"TRACE\",\"service\":\"orders\",\"message\":\"order placed\"}";
        mockMvc.perform(post("/api/logs")
                        .header("X-Log-Api-Key", "k")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /api/logs/batch ──────────────────────────────────────────────────

    @Test
    void ingestBatch_validWithApiKey_returns201() throws Exception {
        when(logIngestionService.validateApiKey("k")).thenReturn(new LogApiKey());
        when(logIngestionService.ingestBatch(any())).thenReturn(1);

        String body = "{\"logs\":[{\"level\":\"ERROR\",\"service\":\"orders\",\"message\":\"boom\"}]}";
        mockMvc.perform(post("/api/logs/batch")
                        .header("X-Log-Api-Key", "k")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stored").value(1))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void ingestBatch_missingApiKeyHeader_returns401() throws Exception {
        String body = "{\"logs\":[{\"level\":\"ERROR\",\"service\":\"orders\",\"message\":\"boom\"}]}";
        mockMvc.perform(post("/api/logs/batch")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ingestBatch_emptyLogs_returns400() throws Exception {
        String body = "{\"logs\":[]}";
        mockMvc.perform(post("/api/logs/batch")
                        .header("X-Log-Api-Key", "k")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // ─── GET endpoints ─────────────────────────────────────────────────────────

    @Test
    void getLogs_returnsOkPage() throws Exception {
        when(logIngestionService.searchLogs(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleLog(UUID.randomUUID()))));

        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].service").value("orders"));
    }

    @Test
    void getById_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(logIngestionService.getById(id)).thenReturn(sampleLog(id));

        mockMvc.perform(get("/api/logs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("INFO"));
    }

    @Test
    void getById_notFound_propagates() {
        UUID id = UUID.randomUUID();
        when(logIngestionService.getById(id)).thenThrow(new RuntimeException("Log not found: " + id));

        // Service throws a plain RuntimeException (no @ResponseStatus); MockMvc has no
        // resolver for it, so the exception propagates out of perform().
        Assertions.assertThrows(Exception.class,
                () -> mockMvc.perform(get("/api/logs/{id}", id)));
    }

    @Test
    void getServices_returnsOk() throws Exception {
        when(logIngestionService.getDistinctServices()).thenReturn(List.of("orders", "billing"));

        mockMvc.perform(get("/api/logs/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("orders"));
    }
}
