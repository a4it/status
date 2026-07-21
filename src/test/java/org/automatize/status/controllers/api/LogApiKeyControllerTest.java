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

    private static final String BASE_URL = "/api/log-api-keys";
    private static final String PROD_INGEST_NAME = "prod-ingest";
    private static final String CI_KEY_NAME = "ci-key";

    @MockitoBean
    private LogApiKeyService logApiKeyService;

    /**
     * Builds a sample active {@link LogApiKey} with a fixed key prefix for use in
     * mocked service responses.
     *
     * @param id   the key identifier to assign
     * @param name the display name to assign
     * @return a populated, active {@link LogApiKey}
     */
    private LogApiKey sampleKey(UUID id, String name) {
        LogApiKey k = new LogApiKey();
        k.setId(id);
        k.setName(name);
        k.setKeyPrefix("abcd1234");
        k.setIsActive(true);
        return k;
    }

    /**
     * Verifies {@code GET /api/log-api-keys} returns 200 with the mocked keys'
     * {@code name} and {@code keyPrefix} JSON fields.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void findAll_returnsOk() throws Exception {
        when(logApiKeyService.findAll())
                .thenReturn(List.of(sampleKey(UUID.randomUUID(), PROD_INGEST_NAME)));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(PROD_INGEST_NAME))
                .andExpect(jsonPath("$[0].keyPrefix").value("abcd1234"));
    }

    /**
     * Verifies {@code POST /api/log-api-keys} with a valid name returns 201 with the
     * created key's {@code name} and the one-time {@code rawKey} in the JSON body.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void create_valid_returns201WithRawKey() throws Exception {
        LogApiKey key = sampleKey(UUID.randomUUID(), CI_KEY_NAME);
        key.setRawKeyOnceOnly("sk_live_rawsecret");
        when(logApiKeyService.create(any(), eq(CI_KEY_NAME))).thenReturn(key);

        String body = "{\"name\":\"ci-key\"}";
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(CI_KEY_NAME))
                .andExpect(jsonPath("$.rawKey").value("sk_live_rawsecret"));
    }

    /**
     * Verifies {@code POST /api/log-api-keys} with a body missing the required name
     * returns 400 Bad Request.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void create_missingName_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies {@code POST /api/log-api-keys/{id}/toggle} returns 200 with the
     * updated {@code isActive} state from the mocked service.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void toggle_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        LogApiKey key = sampleKey(id, PROD_INGEST_NAME);
        key.setIsActive(false);
        when(logApiKeyService.toggleActive(id)).thenReturn(key);

        mockMvc.perform(post("/api/log-api-keys/{id}/toggle", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    /**
     * Verifies {@code DELETE /api/log-api-keys/{id}} returns 200 with a success
     * message and delegates to {@link LogApiKeyService#delete}.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void delete_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/log-api-keys/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(logApiKeyService).delete(id);
    }
}
