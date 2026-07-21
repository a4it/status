package org.automatize.status.controllers.api;

import org.automatize.status.api.request.GcScheduleRequest;
import org.automatize.status.api.response.JvmStatsResponse;
import org.automatize.status.services.JvmService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link JvmController}: JVM stats retrieval, GC schedule
 * read/update, and manual GC trigger, delegating to the mocked {@link JvmService}.
 */
@WebMvcTest(controllers = JvmController.class)
class JvmControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private JvmService jvmService;

    @Test
    void getStats_returnsOk() throws Exception {
        JvmStatsResponse stats = new JvmStatsResponse();
        stats.setHeapUsed(1024L);
        stats.setThreadCount(42);
        when(jvmService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/jvm/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heapUsed").value(1024))
                .andExpect(jsonPath("$.threadCount").value(42));
    }

    @Test
    void getGcSchedule_returnsOk() throws Exception {
        GcScheduleRequest cfg = new GcScheduleRequest();
        cfg.setEnabled(true);
        cfg.setCron("0 0 * * * *");
        when(jvmService.getScheduleConfig()).thenReturn(cfg);

        mockMvc.perform(get("/api/jvm/gc/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.cron").value("0 0 * * * *"));
    }

    @Test
    void updateGcSchedule_returnsOk() throws Exception {
        String body = "{\"enabled\":true,\"cron\":\"0 0 * * * *\"}";

        mockMvc.perform(put("/api/jvm/gc/schedule").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        verify(jvmService).updateSchedule(anyBoolean(), any());
    }

    @Test
    void runGcNow_returnsOkMessage() throws Exception {
        mockMvc.perform(post("/api/jvm/gc/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("GC triggered"));

        verify(jvmService).triggerGcNow();
    }
}
