package org.automatize.status.controllers.api;

import org.automatize.status.api.response.LogViewerResponse;
import org.automatize.status.api.response.LoggerInfoResponse;
import org.automatize.status.services.LogViewerService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link LogViewerController} (admin log file/logger tools).
 */
@WebMvcTest(controllers = LogViewerController.class)
class LogViewerControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private LogViewerService logViewerService;

    private LogViewerResponse sampleViewerResponse() {
        LogViewerResponse r = new LogViewerResponse();
        r.setLines(List.of("line one", "line two"));
        r.setTotalLines(2);
        r.setTruncated(false);
        r.setFilePath("/var/log/app.log");
        r.setFileSizeBytes(1024L);
        return r;
    }

    @Test
    void getAppLog_returnsOk() throws Exception {
        when(logViewerService.readAppLog(anyInt(), anyString())).thenReturn(sampleViewerResponse());

        mockMvc.perform(get("/api/log-viewer/app-log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLines").value(2))
                .andExpect(jsonPath("$.lines[0]").value("line one"));
    }

    @Test
    void getSyslog_returnsOk() throws Exception {
        when(logViewerService.readSyslog(anyInt(), anyString())).thenReturn(sampleViewerResponse());

        mockMvc.perform(get("/api/log-viewer/syslog").param("lines", "100").param("search", "err"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filePath").value("/var/log/app.log"));
    }

    @Test
    void getLoggers_returnsOk() throws Exception {
        LoggerInfoResponse info = new LoggerInfoResponse();
        info.setName("org.automatize.status");
        info.setEffectiveLevel("INFO");
        info.setConfiguredLevel("DEBUG");
        when(logViewerService.getLoggers()).thenReturn(List.of(info));

        mockMvc.perform(get("/api/log-viewer/loggers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("org.automatize.status"))
                .andExpect(jsonPath("$[0].effectiveLevel").value("INFO"));
    }

    @Test
    void setLogLevel_returnsOk() throws Exception {
        String body = "{\"level\":\"DEBUG\"}";
        mockMvc.perform(put("/api/log-viewer/loggers/{name}", "org.automatize.status")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        verify(logViewerService).setLogLevel(eq("org.automatize.status"), eq("DEBUG"));
    }
}
