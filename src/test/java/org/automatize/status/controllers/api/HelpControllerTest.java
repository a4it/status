package org.automatize.status.controllers.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link HelpController}. The controller holds no injected
 * services; it loads bundled markdown from {@code classpath:help/*.md} in a
 * {@code @PostConstruct} hook, so tests exercise the real content endpoints,
 * slug validation (400), unknown slug (404), and search query handling.
 */
@WebMvcTest(controllers = HelpController.class)
class HelpControllerTest extends AbstractApiControllerTest {

    @Test
    void listFiles_returnsOkArray() throws Exception {
        mockMvc.perform(get("/api/help"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getFile_knownSlug_returnsOk() throws Exception {
        // 01-architecture.md is bundled under src/main/resources/help
        mockMvc.perform(get("/api/help/{slug}", "01-architecture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("01-architecture"))
                .andExpect(jsonPath("$.html").exists());
    }

    @Test
    void getFile_invalidSlug_returns400() throws Exception {
        // Uppercase fails the [a-z0-9\-]+ slug pattern.
        mockMvc.perform(get("/api/help/{slug}", "Invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getFile_unknownSlug_returns404() throws Exception {
        mockMvc.perform(get("/api/help/{slug}", "zzz-nonexistent-help-page"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_validQuery_returnsOkArray() throws Exception {
        mockMvc.perform(get("/api/help/search").param("q", "architecture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void search_shortQuery_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/help/search").param("q", "a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void search_missingQueryParam_returns400() throws Exception {
        mockMvc.perform(get("/api/help/search"))
                .andExpect(status().isBadRequest());
    }
}
