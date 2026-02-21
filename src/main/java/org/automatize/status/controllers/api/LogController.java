package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.LogBatchRequest;
import org.automatize.status.api.request.LogRequest;
import org.automatize.status.api.response.LogResponse;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.models.Log;
import org.automatize.status.models.LogApiKey;
import org.automatize.status.services.LogApiKeyService;
import org.automatize.status.services.LogIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for log ingestion and querying.
 * POST /api/logs and POST /api/logs/batch accept an X-Log-Api-Key header for auth.
 * GET endpoints require JWT authentication.
 */
@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogController {

    @Autowired
    private LogIngestionService logIngestionService;

    @Autowired
    private LogApiKeyService logApiKeyService;

    /**
     * Ingests a single log entry. Authenticated via X-Log-Api-Key header.
     */
    @PostMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> ingest(
            @RequestHeader(value = "X-Log-Api-Key", required = false) String apiKey,
            @Valid @RequestBody LogRequest request) {

        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("X-Log-Api-Key header is required", false));
        }
        try {
            LogApiKey key = logIngestionService.validateApiKey(apiKey);
            UUID tenantId = key.getTenant() != null ? key.getTenant().getId() : request.getTenantId();

            Log log = logIngestionService.ingest(
                    tenantId,
                    request.getTimestamp(),
                    request.getLevel(),
                    request.getService(),
                    request.getMessage(),
                    request.getMetadata(),
                    request.getTraceId(),
                    request.getRequestId()
            );

            if (log == null) {
                return ResponseEntity.ok(new MessageResponse("Log dropped by drop rule", true));
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(log));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * Ingests a batch of log entries. Authenticated via X-Log-Api-Key header.
     */
    @PostMapping("/batch")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> ingestBatch(
            @RequestHeader(value = "X-Log-Api-Key", required = false) String apiKey,
            @Valid @RequestBody LogBatchRequest request) {

        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("X-Log-Api-Key header is required", false));
        }
        try {
            LogApiKey key = logIngestionService.validateApiKey(apiKey);
            UUID tenantId = key.getTenant() != null ? key.getTenant().getId() : null;

            List<LogIngestionService.LogEntry> entries = request.getLogs().stream()
                    .map(r -> new LogIngestionService.LogEntry(
                            tenantId != null ? tenantId : r.getTenantId(),
                            r.getTimestamp(),
                            r.getLevel(),
                            r.getService(),
                            r.getMessage(),
                            r.getMetadata(),
                            r.getTraceId(),
                            r.getRequestId()
                    )).toList();

            int stored = logIngestionService.ingestBatch(entries);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("stored", stored, "submitted", request.getLogs().size(), "success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * Queries logs with optional filtering. Requires JWT auth.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<LogResponse>> getLogs(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<Log> page = logIngestionService.searchLogs(tenantId, level, service, startDate, endDate, search, pageable);
        return ResponseEntity.ok(page.map(this::mapToResponse));
    }

    /**
     * Returns a single log entry by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LogResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapToResponse(logIngestionService.getById(id)));
    }

    /**
     * Returns all distinct service names across ingested logs.
     */
    @GetMapping("/services")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getServices() {
        return ResponseEntity.ok(logIngestionService.getDistinctServices());
    }

    // -------------------------------------------------------------------------

    private LogResponse mapToResponse(Log log) {
        LogResponse r = new LogResponse();
        r.setId(log.getId());
        r.setTenantId(log.getTenant() != null ? log.getTenant().getId() : null);
        r.setLogTimestamp(log.getLogTimestamp());
        r.setLevel(log.getLevel());
        r.setService(log.getService());
        r.setMessage(log.getMessage());
        r.setMetadata(log.getMetadata());
        r.setTraceId(log.getTraceId());
        r.setRequestId(log.getRequestId());
        return r;
    }
}
