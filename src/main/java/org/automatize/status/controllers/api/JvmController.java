package org.automatize.status.controllers.api;

import org.automatize.status.api.request.GcScheduleRequest;
import org.automatize.status.api.response.JvmStatsResponse;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.services.JvmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jvm")
@PreAuthorize("isAuthenticated()")
public class JvmController {

    @Autowired
    private JvmService jvmService;

    @GetMapping("/stats")
    public ResponseEntity<JvmStatsResponse> getStats() {
        return ResponseEntity.ok(jvmService.getStats());
    }

    @GetMapping("/gc/schedule")
    public ResponseEntity<GcScheduleRequest> getGcSchedule() {
        return ResponseEntity.ok(jvmService.getScheduleConfig());
    }

    @PutMapping("/gc/schedule")
    public ResponseEntity<Void> updateGcSchedule(@RequestBody GcScheduleRequest request) {
        jvmService.updateSchedule(request.isEnabled(), request.getCron());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/gc/run")
    public ResponseEntity<MessageResponse> runGcNow() {
        jvmService.triggerGcNow();
        return ResponseEntity.ok(new MessageResponse("GC triggered", true));
    }
}
