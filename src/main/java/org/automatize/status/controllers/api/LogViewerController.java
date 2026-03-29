package org.automatize.status.controllers.api;

import org.automatize.status.api.request.LogLevelRequest;
import org.automatize.status.api.response.LogViewerResponse;
import org.automatize.status.api.response.LoggerInfoResponse;
import org.automatize.status.services.LogViewerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/log-viewer")
// MED-05: log viewer restricted to admin roles only (reads sensitive system logs)
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
public class LogViewerController {

    @Autowired
    private LogViewerService logViewerService;

    @GetMapping("/app-log")
    public ResponseEntity<LogViewerResponse> getAppLog(
            @RequestParam(defaultValue = "500") int lines,
            @RequestParam(defaultValue = "") String search) {
        return ResponseEntity.ok(logViewerService.readAppLog(lines, search));
    }

    @GetMapping("/syslog")
    public ResponseEntity<LogViewerResponse> getSyslog(
            @RequestParam(defaultValue = "500") int lines,
            @RequestParam(defaultValue = "") String search) {
        return ResponseEntity.ok(logViewerService.readSyslog(lines, search));
    }

    @GetMapping("/loggers")
    public ResponseEntity<List<LoggerInfoResponse>> getLoggers() {
        return ResponseEntity.ok(logViewerService.getLoggers());
    }

    @PutMapping("/loggers/{name}")
    public ResponseEntity<?> setLogLevel(
            @PathVariable String name,
            @RequestBody LogLevelRequest request) {
        logViewerService.setLogLevel(name, request.getLevel());
        return ResponseEntity.ok().build();
    }
}
