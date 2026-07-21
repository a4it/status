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

    private static final String SERVICE_ORDERS = "orders";
    private static final String LOG_JSON = "{\"level\":\"INFO\",\"service\":\"orders\",\"message\":\"order placed\"}";
    private static final String LOGS_PATH = "/api/logs";
    private static final String LOGS_BATCH_PATH = "/api/logs/batch";
    private static final String API_KEY_HEADER = "X-Log-Api-Key";
    private static final String JSON_PATH_SUCCESS = "$.success";

    @MockitoBean
    private LogIngestionService logIngestionService;

    @MockitoBean
    private LogApiKeyService logApiKeyService;

    /**
     * Builds a sample {@link Log} entity populated with representative field values
     * for use as service-mock return data.
     *
     * @param id the identifier to assign to the log
     * @return a fully populated {@link Log} instance
     */
    private Log sampleLog(UUID id) {
        Log log = new Log();
        log.setId(id);
        log.setLevel("INFO");
        log.setService(SERVICE_ORDERS);
        log.setMessage("order placed");
        log.setLogTimestamp(ZonedDateTime.now());
        return log;
    }

    // ─── POST /api/logs ────────────────────────────────────────────────────────

    /**
     * Verifies that a well-formed log with a valid {@code X-Log-Api-Key} header is
     * ingested and returns {@code 201 Created} with the persisted log's fields in the body.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void ingest_validWithApiKey_returns201() throws Exception {
        when(logIngestionService.validateApiKey("k")).thenReturn(new LogApiKey());
        when(logIngestionService.ingest(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(sampleLog(UUID.randomUUID()));

        String body = LOG_JSON;
        mockMvc.perform(post(LOGS_PATH)
                        .header(API_KEY_HEADER,"k")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.level").value("INFO"))
                .andExpect(jsonPath("$.service").value(SERVICE_ORDERS));
    }

    /**
     * Verifies that when the ingestion service drops the log (returns {@code null}),
     * the endpoint responds {@code 200 OK} with {@code success=true} rather than 201.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void ingest_droppedByRule_returnsOkMessage() throws Exception {
        when(logIngestionService.validateApiKey("k")).thenReturn(new LogApiKey());
        when(logIngestionService.ingest(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(null);

        String body = LOG_JSON;
        mockMvc.perform(post(LOGS_PATH)
                        .header(API_KEY_HEADER,"k")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_SUCCESS).value(true));
    }

    /**
     * Verifies that an ingestion request lacking the {@code X-Log-Api-Key} header is
     * rejected with {@code 401 Unauthorized} and {@code success=false}.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void ingest_missingApiKeyHeader_returns401() throws Exception {
        String body = LOG_JSON;
        mockMvc.perform(post(LOGS_PATH)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(JSON_PATH_SUCCESS).value(false));
    }

    /**
     * Verifies that when API key validation throws (invalid or inactive key), the
     * endpoint returns {@code 401 Unauthorized} with {@code success=false}.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void ingest_invalidApiKey_returns401() throws Exception {
        when(logIngestionService.validateApiKey("bad"))
                .thenThrow(new RuntimeException("Invalid or inactive API key"));

        String body = LOG_JSON;
        mockMvc.perform(post(LOGS_PATH)
                        .header(API_KEY_HEADER,"bad")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(JSON_PATH_SUCCESS).value(false));
    }

    /**
     * Verifies that a request body omitting the required {@code level} field fails
     * bean validation and returns {@code 400 Bad Request}.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void ingest_missingLevel_returns400() throws Exception {
        String body = "{\"service\":\"orders\",\"message\":\"order placed\"}";
        mockMvc.perform(post(LOGS_PATH)
                        .header(API_KEY_HEADER,"k")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that a request body carrying an unsupported {@code level} value
     * (e.g. {@code TRACE}) fails validation and returns {@code 400 Bad Request}.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void ingest_invalidLevelValue_returns400() throws Exception {
        String body = "{\"level\":\"TRACE\",\"service\":\"orders\",\"message\":\"order placed\"}";
        mockMvc.perform(post(LOGS_PATH)
                        .header(API_KEY_HEADER,"k")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /api/logs/batch ──────────────────────────────────────────────────

    /**
     * Verifies that a valid batch of logs with a valid API key returns {@code 201 Created}
     * reporting the number of stored logs and {@code success=true}.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void ingestBatch_validWithApiKey_returns201() throws Exception {
        when(logIngestionService.validateApiKey("k")).thenReturn(new LogApiKey());
        when(logIngestionService.ingestBatch(any())).thenReturn(1);

        String body = "{\"logs\":[{\"level\":\"ERROR\",\"service\":\"orders\",\"message\":\"boom\"}]}";
        mockMvc.perform(post(LOGS_BATCH_PATH)
                        .header(API_KEY_HEADER,"k")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stored").value(1))
                .andExpect(jsonPath(JSON_PATH_SUCCESS).value(true));
    }

    /**
     * Verifies that a batch ingestion request without the {@code X-Log-Api-Key} header
     * is rejected with {@code 401 Unauthorized}.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void ingestBatch_missingApiKeyHeader_returns401() throws Exception {
        String body = "{\"logs\":[{\"level\":\"ERROR\",\"service\":\"orders\",\"message\":\"boom\"}]}";
        mockMvc.perform(post(LOGS_BATCH_PATH)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Verifies that a batch request with an empty {@code logs} array fails validation
     * and returns {@code 400 Bad Request}.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void ingestBatch_emptyLogs_returns400() throws Exception {
        String body = "{\"logs\":[]}";
        mockMvc.perform(post(LOGS_BATCH_PATH)
                        .header(API_KEY_HEADER,"k")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // ─── GET endpoints ─────────────────────────────────────────────────────────

    /**
     * Verifies that {@code GET /api/logs} returns {@code 200 OK} with a paged result
     * whose first entry reflects the mocked log's service.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void getLogs_returnsOkPage() throws Exception {
        when(logIngestionService.searchLogs(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleLog(UUID.randomUUID()))));

        mockMvc.perform(get(LOGS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].service").value(SERVICE_ORDERS));
    }

    /**
     * Verifies that {@code GET /api/logs/{id}} for an existing log returns
     * {@code 200 OK} with the log's fields in the body.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void getById_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(logIngestionService.getById(id)).thenReturn(sampleLog(id));

        mockMvc.perform(get("/api/logs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("INFO"));
    }

    /**
     * Verifies that when the service throws a plain {@link RuntimeException} for an
     * unknown id (no {@code @ResponseStatus} mapping), the exception propagates out of
     * {@code perform()} rather than being resolved to an HTTP status.
     */
    @Test
    void getById_notFound_propagates() {
        UUID id = UUID.randomUUID();
        when(logIngestionService.getById(id)).thenThrow(new RuntimeException("Log not found: " + id));

        // Service throws a plain RuntimeException (no @ResponseStatus); MockMvc has no
        // resolver for it, so the exception propagates out of perform().
        Assertions.assertThrows(Exception.class,
                () -> mockMvc.perform(get("/api/logs/{id}", id)));
    }

    /**
     * Verifies that {@code GET /api/logs/services} returns {@code 200 OK} with the
     * distinct service names supplied by the service.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void getServices_returnsOk() throws Exception {
        when(logIngestionService.getDistinctServices()).thenReturn(List.of(SERVICE_ORDERS, "billing"));

        mockMvc.perform(get("/api/logs/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(SERVICE_ORDERS));
    }
}
