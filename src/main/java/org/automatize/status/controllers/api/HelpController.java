package org.automatize.status.controllers.api;

import jakarta.annotation.PostConstruct;
import org.automatize.status.api.response.HelpContentResponse;
import org.automatize.status.api.response.HelpFileResponse;
import org.automatize.status.api.response.HelpSearchResult;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST API controller serving the in-application help documentation.
 * <p>
 * Base route: {@code /api/help}. Markdown help files bundled on the classpath under
 * {@code help/} are loaded once at startup, rendered to HTML on demand, and made
 * searchable. Endpoints list the available files, fetch a rendered file by slug, and
 * perform full-text search across the documentation.
 * </p>
 *
 * @see HelpContentResponse
 * @see HelpSearchResult
 */
@RestController
@RequestMapping("/api/help")
public class HelpController {

    private final List<HelpFileResponse> fileList = new ArrayList<>();
    private final Map<String, String> contentCache = new LinkedHashMap<>();

    private final Parser parser;
    private final HtmlRenderer renderer;

    /**
     * Constructs the controller, initialising the CommonMark parser and HTML renderer
     * with GitHub-flavoured table support.
     */
    public HelpController() {
        List<org.commonmark.Extension> extensions = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder().extensions(extensions).build();
    }

    /**
     * Loads and caches all bundled help markdown files at application startup.
     * <p>
     * Scans {@code classpath:help/*.md}, sorts the files by name, and caches the
     * content and extracted titles of files whose names begin with a digit.
     * </p>
     *
     * @throws IOException if the help resources cannot be read
     */
    @PostConstruct
    public void loadFiles() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:help/*.md");

        Arrays.sort(resources, Comparator.comparing(Resource::getFilename));

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            // Skip resources without a name or whose name does not start with a digit (ordering prefix)
            if (filename == null || !filename.matches("\\d.*\\.md")) continue;

            String slug = filename.replace(".md", "");
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            String title = extractTitle(content);

            fileList.add(new HelpFileResponse(slug, title));
            contentCache.put(slug, content);
        }
    }

    /**
     * Lists all available help files.
     * <p>
     * Handles {@code GET /api/help}.
     * </p>
     *
     * @return the list of help file summaries (slug and title)
     */
    @GetMapping
    public List<HelpFileResponse> listFiles() {
        return fileList;
    }

    /**
     * Fetches a single help file rendered to HTML.
     * <p>
     * Handles {@code GET /api/help/{slug}}.
     * </p>
     *
     * @param slug the slug identifying the help file
     * @return ResponseEntity with the rendered content, HTTP 400 for an invalid slug,
     *         or HTTP 404 when no file matches the slug
     */
    @GetMapping("/{slug}")
    public ResponseEntity<HelpContentResponse> getFile(@PathVariable String slug) {
        // Reject slugs containing anything other than lowercase letters, digits, and hyphens
        if (!slug.matches("[a-z0-9\\-]+")) {
            return ResponseEntity.badRequest().build();
        }
        String markdown = contentCache.get(slug);
        // No cached content for this slug: report not found
        if (markdown == null) {
            return ResponseEntity.notFound().build();
        }
        String html = markdownToHtml(markdown);
        String title = extractTitle(markdown);
        return ResponseEntity.ok(new HelpContentResponse(slug, title, html));
    }

    /**
     * Performs a full-text search across all help files.
     * <p>
     * Handles {@code GET /api/help/search}. Results are ordered by match count descending.
     * </p>
     *
     * @param q the search query; must be at least two non-blank characters
     * @return the list of matching help files with excerpts and match counts,
     *         or an empty list when the query is too short
     */
    @GetMapping("/search")
    public List<HelpSearchResult> search(@RequestParam String q) {
        // Ignore null, blank, or too-short queries
        if (q == null || q.isBlank() || q.length() < 2) {
            return List.of();
        }
        String lower = q.toLowerCase();
        List<HelpSearchResult> results = new ArrayList<>();

        for (Map.Entry<String, String> entry : contentCache.entrySet()) {
            String slug = entry.getKey();
            String content = entry.getValue();
            String contentLower = content.toLowerCase();

            int count = countOccurrences(contentLower, lower);
            // Skip files that do not contain the search term
            if (count == 0) continue;

            String title = extractTitle(content);
            String excerpt = buildExcerpt(content, q, 200);
            results.add(new HelpSearchResult(slug, title, excerpt, count));
        }

        results.sort(Comparator.comparingInt(HelpSearchResult::matchCount).reversed());
        return results;
    }

    /**
     * Renders markdown source to HTML.
     *
     * @param markdown the markdown source
     * @return the rendered HTML
     */
    private String markdownToHtml(String markdown) {
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    /**
     * Extracts the document title from the first level-one heading.
     *
     * @param markdown the markdown source
     * @return the heading text, or {@code "Documentation"} when no heading is present
     */
    private String extractTitle(String markdown) {
        return Arrays.stream(markdown.split("\n"))
                .filter(line -> line.startsWith("# "))
                .map(line -> line.substring(2).trim())
                .findFirst()
                .orElse("Documentation");
    }

    /**
     * Counts the non-overlapping occurrences of a term within a text.
     *
     * @param text the text to search
     * @param term the term to count
     * @return the number of occurrences
     */
    private int countOccurrences(String text, String term) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(term, idx)) != -1) {
            count++;
            idx += term.length();
        }
        return count;
    }

    /**
     * Builds a short excerpt of content centred on the first occurrence of a query.
     *
     * @param content the full content to excerpt from
     * @param query the query whose match anchors the excerpt
     * @param maxLength the fallback length used when the query is not found
     * @return the trimmed excerpt, with leading/trailing ellipses where truncated
     */
    private String buildExcerpt(String content, String query, int maxLength) {
        String lower = content.toLowerCase();
        int pos = lower.indexOf(query.toLowerCase());
        // Query not present: fall back to the leading slice of the content
        if (pos == -1) return content.substring(0, Math.min(maxLength, content.length()));

        int start = Math.max(0, pos - 60);
        int end = Math.min(content.length(), pos + query.length() + 140);
        String excerpt = content.substring(start, end).replaceAll("[#*`>]", "").trim();
        // Prepend an ellipsis when the excerpt does not start at the beginning of the content
        if (start > 0) excerpt = "..." + excerpt;
        // Append an ellipsis when the excerpt does not reach the end of the content
        if (end < content.length()) excerpt = excerpt + "...";
        return excerpt;
    }
}
