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

@RestController
@RequestMapping("/api/help")
public class HelpController {

    private final List<HelpFileResponse> fileList = new ArrayList<>();
    private final Map<String, String> contentCache = new LinkedHashMap<>();

    private final Parser parser;
    private final HtmlRenderer renderer;

    public HelpController() {
        List<org.commonmark.Extension> extensions = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder().extensions(extensions).build();
    }

    @PostConstruct
    public void loadFiles() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:help/*.md");

        Arrays.sort(resources, Comparator.comparing(Resource::getFilename));

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null || !filename.matches("\\d.*\\.md")) continue;

            String slug = filename.replace(".md", "");
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            String title = extractTitle(content);

            fileList.add(new HelpFileResponse(slug, title));
            contentCache.put(slug, content);
        }
    }

    @GetMapping
    public List<HelpFileResponse> listFiles() {
        return fileList;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<HelpContentResponse> getFile(@PathVariable String slug) {
        if (!slug.matches("[a-z0-9\\-]+")) {
            return ResponseEntity.badRequest().build();
        }
        String markdown = contentCache.get(slug);
        if (markdown == null) {
            return ResponseEntity.notFound().build();
        }
        String html = markdownToHtml(markdown);
        String title = extractTitle(markdown);
        return ResponseEntity.ok(new HelpContentResponse(slug, title, html));
    }

    @GetMapping("/search")
    public List<HelpSearchResult> search(@RequestParam String q) {
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
            if (count == 0) continue;

            String title = extractTitle(content);
            String excerpt = buildExcerpt(content, q, 200);
            results.add(new HelpSearchResult(slug, title, excerpt, count));
        }

        results.sort(Comparator.comparingInt(HelpSearchResult::matchCount).reversed());
        return results;
    }

    private String markdownToHtml(String markdown) {
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    private String extractTitle(String markdown) {
        return Arrays.stream(markdown.split("\n"))
                .filter(line -> line.startsWith("# "))
                .map(line -> line.substring(2).trim())
                .findFirst()
                .orElse("Documentation");
    }

    private int countOccurrences(String text, String term) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(term, idx)) != -1) {
            count++;
            idx += term.length();
        }
        return count;
    }

    private String buildExcerpt(String content, String query, int maxLength) {
        String lower = content.toLowerCase();
        int pos = lower.indexOf(query.toLowerCase());
        if (pos == -1) return content.substring(0, Math.min(maxLength, content.length()));

        int start = Math.max(0, pos - 60);
        int end = Math.min(content.length(), pos + query.length() + 140);
        String excerpt = content.substring(start, end).replaceAll("[#*`>]", "").trim();
        if (start > 0) excerpt = "..." + excerpt;
        if (end < content.length()) excerpt = excerpt + "...";
        return excerpt;
    }
}
