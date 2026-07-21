package org.automatize.status.controllers.api;

import org.automatize.status.models.LogApiKey;
import org.automatize.status.services.LogApiKeyService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link LogApiKeyController}. The raw key is surfaced only
 * in the creation response (transient {@code rawKeyOnceOnly} field).
 */
@WebMvcTest(controllers = LogApiKeyController.class)
class LogApiKeyControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private LogApiKeyService logApiKeyService;

    private LogApiKey sampleKey(UUID id, String name) {
        LogApiKey k = new LogApiKey();
        k.setId(id);
        k.setName(name);
        k.setKeyPrefix("abcd1234");
        k.setIsActive(true);
        return k;
    }

    @Test
    void findAll_returnsOk() throws Exception {
        when(logApiKeyService.findAll())
                .thenReturn(List.of(sampleKey(UUID.randomUUID(), "prod-ingest")));

        mockMvc.perform(get("/api/log-api-keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("prod-ingest"))
                .andExpect(jsonPath("$[0].keyPrefix").value("abcd1234"));
    }

    @Test
    void create_valid_returns201WithRawKey() throws Exception {
        LogApiKey key = sampleKey(UUID.randomUUID(), "ci-key");
        key.setRawKeyOnceOnly("sk_live_rawsecret");
        when(logApiKeyService.create(any(), eq("ci-key"))).thenReturn(key);

        String body = "{\"name\":\"ci-key\"}";
        mockMvc.perform(post("/api/log-api-keys")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("ci-key"))
                .andExpect(jsonPath("$.rawKey").value("sk_live_rawsecret"));
    }

    @Test
    void create_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/log-api-keys")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void toggle_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        LogApiKey key = sampleKey(id, "prod-ingest");
        key.setIsActive(false);
        when(logApiKeyService.toggleActive(id)).thenReturn(key);

        mockMvc.perform(post("/api/log-api-keys/{id}/toggle", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void delete_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/log-api-keys/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(logApiKeyService).delete(id);
    }
}
