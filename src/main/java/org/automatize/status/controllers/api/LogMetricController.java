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

@RestController
@RequestMapping("/api/log-metrics")
@CrossOrigin(origins = "*")
@PreAuthorize("isAuthenticated()")
public class LogMetricController {

    @Autowired
    private LogMetricService logMetricService;

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
