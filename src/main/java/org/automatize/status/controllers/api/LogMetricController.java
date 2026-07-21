package org.automatize.status.controllers.api;

import org.automatize.status.api.response.LogMetricResponse;
import org.automatize.status.models.LogMetric;
import org.automatize.status.services.LogMetricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST API controller for aggregated log metrics.
 * <p>
 * Base route: {@code /api/log-metrics}. Exposes time-bucketed log counts grouped
 * by service and level, optionally scoped to a tenant and a starting time.
 * All endpoints require authentication.
 * </p>
 *
 * @see LogMetricService
 * @see LogMetricResponse
 */
@RestController
@RequestMapping("/api/log-metrics")
// MED-02: removed @CrossOrigin(origins = "*"); global CORS policy in SecurityConfig applies
@PreAuthorize("isAuthenticated()")
public class LogMetricController {

    @Autowired
    private LogMetricService logMetricService;

    /**
     * Retrieves aggregated log metrics.
     * <p>
     * Handles {@code GET /api/log-metrics}. When {@code since} is omitted it defaults
     * to the last 24 hours; when {@code tenantId} is provided results are scoped to
     * that tenant.
     * </p>
     *
     * @param tenantId optional tenant identifier to scope the metrics
     * @param since optional start of the time window (ISO-8601); defaults to 24 hours ago
     * @return ResponseEntity containing the list of log metric responses
     */
    @GetMapping
    public ResponseEntity<List<LogMetricResponse>> getMetrics(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime since) {

        ZonedDateTime from = since != null ? since : ZonedDateTime.now().minusHours(24);
        List<LogMetric> metrics = tenantId != null
                ? logMetricService.findByTenantSince(tenantId, from)
                : logMetricService.findSince(from);

        return ResponseEntity.ok(metrics.stream().map(this::mapToResponse).toList());
    }

    /**
     * Maps a {@link LogMetric} entity to its API response representation.
     *
     * @param m the log metric entity to map
     * @return the mapped log metric response
     */
    private LogMetricResponse mapToResponse(LogMetric m) {
        LogMetricResponse r = new LogMetricResponse();
        r.setId(m.getId());
        r.setTenantId(m.getTenant() != null ? m.getTenant().getId() : null);
        r.setService(m.getService());
        r.setLevel(m.getLevel());
        r.setBucket(m.getBucket());
        r.setBucketType(m.getBucketType());
        r.setCount(m.getCount());
        return r;
    }
}
