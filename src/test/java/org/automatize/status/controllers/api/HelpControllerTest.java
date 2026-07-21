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

    private static final String HELP_SLUG_PATH = "/api/help/{slug}";
    private static final String HELP_SEARCH_PATH = "/api/help/search";

    /**
     * Verifies {@code GET /api/help} returns 200 with a JSON array of help files.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void listFiles_returnsOkArray() throws Exception {
        mockMvc.perform(get("/api/help"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    /**
     * Verifies {@code GET /api/help/{slug}} for a bundled slug returns 200 with the
     * matching {@code slug} and rendered {@code html} in the JSON body.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void getFile_knownSlug_returnsOk() throws Exception {
        // 01-architecture.md is bundled under src/main/resources/help
        mockMvc.perform(get(HELP_SLUG_PATH, "01-architecture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("01-architecture"))
                .andExpect(jsonPath("$.html").exists());
    }

    /**
     * Verifies {@code GET /api/help/{slug}} with a slug that violates the
     * {@code [a-z0-9\-]+} pattern returns 400 Bad Request.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void getFile_invalidSlug_returns400() throws Exception {
        // Uppercase fails the [a-z0-9\-]+ slug pattern.
        mockMvc.perform(get(HELP_SLUG_PATH, "Invalid"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies {@code GET /api/help/{slug}} with a well-formed but unknown slug
     * returns 404 Not Found.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void getFile_unknownSlug_returns404() throws Exception {
        mockMvc.perform(get(HELP_SLUG_PATH, "zzz-nonexistent-help-page"))
                .andExpect(status().isNotFound());
    }

    /**
     * Verifies {@code GET /api/help/search} with a sufficiently long query returns
     * 200 with a JSON array of matches.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void search_validQuery_returnsOkArray() throws Exception {
        mockMvc.perform(get(HELP_SEARCH_PATH).param("q", "architecture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    /**
     * Verifies {@code GET /api/help/search} with a too-short query returns 200 with
     * an empty JSON array (length 0).
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void search_shortQuery_returnsEmptyArray() throws Exception {
        mockMvc.perform(get(HELP_SEARCH_PATH).param("q", "a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * Verifies {@code GET /api/help/search} without the required {@code q} parameter
     * returns 400 Bad Request.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void search_missingQueryParam_returns400() throws Exception {
        mockMvc.perform(get(HELP_SEARCH_PATH))
                .andExpect(status().isBadRequest());
    }
}
