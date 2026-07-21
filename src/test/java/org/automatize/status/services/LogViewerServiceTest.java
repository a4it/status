package org.automatize.status.services;

import org.automatize.status.api.response.LoggerInfoResponse;
import org.automatize.status.api.response.LogViewerResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LogViewerService}. File reading is tested against real
 * temp files (deterministic, controllable I/O); the logger management methods run
 * against the live logback context available under test.
 */
@ExtendWith(MockitoExtension.class)
class LogViewerServiceTest {

    private final LogViewerService service = new LogViewerService();

    /**
     * Helper that injects the service's {@code configuredLogFile} field via reflection,
     * simulating the value normally supplied by application configuration.
     *
     * @param value the log file path to configure on the service under test
     */
    private void setConfiguredLogFile(String value) {
        ReflectionTestUtils.setField(service, "configuredLogFile", value);
    }

    // ── readLogFile ───────────────────────────────────────────────────────────

    /**
     * Verifies that a {@code null} file path yields a single placeholder line, a total of
     * one line, no truncation, and a zero byte size.
     */
    @Test
    void readLogFile_nullPath_returnsPlaceholder() {
        LogViewerResponse resp = service.readLogFile(null, 100, null);

        assertThat(resp.getLines()).containsExactly("[No log file path configured]");
        assertThat(resp.getTotalLines()).isEqualTo(1);
        assertThat(resp.isTruncated()).isFalse();
        assertThat(resp.getFileSizeBytes()).isZero();
    }

    /**
     * Verifies that a blank (whitespace-only) file path is treated like an unconfigured
     * path and yields the "no log file path configured" placeholder line.
     */
    @Test
    void readLogFile_blankPath_returnsPlaceholder() {
        LogViewerResponse resp = service.readLogFile("   ", 100, null);

        assertThat(resp.getLines()).containsExactly("[No log file path configured]");
    }

    /**
     * Verifies that a path pointing to a non-existent file yields a single line whose
     * content indicates the log file was not found.
     */
    @Test
    void readLogFile_missingFile_returnsNotFoundPlaceholder() {
        LogViewerResponse resp = service.readLogFile("/no/such/file/here.log", 100, null);

        assertThat(resp.getLines()).hasSize(1);
        assertThat(resp.getLines().get(0)).contains("Log file not found");
    }

    /**
     * Verifies that when an existing file has fewer lines than requested, all lines are
     * returned in order with the correct total count, no truncation, and a positive file size.
     *
     * @param dir JUnit-provided temporary directory used to create the real log file
     * @throws IOException if writing the temporary log file fails
     */
    @Test
    void readLogFile_existingFile_returnsLastNLines(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("app.log");
        Files.write(file, List.of("line1", "line2", "line3"));

        LogViewerResponse resp = service.readLogFile(file.toString(), 100, null);

        assertThat(resp.getLines()).containsExactly("line1", "line2", "line3");
        assertThat(resp.getTotalLines()).isEqualTo(3);
        assertThat(resp.isTruncated()).isFalse();
        assertThat(resp.getFileSizeBytes()).isGreaterThan(0);
    }

    /**
     * Verifies that when a file has more lines than requested, only the last N lines are
     * returned and the response's truncated flag is set to {@code true}.
     *
     * @param dir JUnit-provided temporary directory used to create the real log file
     * @throws IOException if writing the temporary log file fails
     */
    @Test
    void readLogFile_moreLinesThanRequested_truncatesToLastNAndFlagsTruncated(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("app.log");
        List<String> content = IntStream.rangeClosed(1, 10).mapToObj(i -> "line" + i).toList();
        Files.write(file, content);

        LogViewerResponse resp = service.readLogFile(file.toString(), 3, null);

        assertThat(resp.getLines()).containsExactly("line8", "line9", "line10");
        assertThat(resp.isTruncated()).isTrue();
    }

    /**
     * Verifies that supplying a search term filters lines case-insensitively, returning only
     * matching lines, and that the truncated flag stays {@code false} when a filter is applied.
     *
     * @param dir JUnit-provided temporary directory used to create the real log file
     * @throws IOException if writing the temporary log file fails
     */
    @Test
    void readLogFile_withSearch_filtersMatchingLinesCaseInsensitiveAndNotTruncated(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("app.log");
        Files.write(file, List.of("INFO started", "ERROR boom", "info again", "DEBUG noise"));

        LogViewerResponse resp = service.readLogFile(file.toString(), 100, "info");

        assertThat(resp.getLines()).containsExactly("INFO started", "info again");
        // truncated is always false when a search filter is applied
        assertThat(resp.isTruncated()).isFalse();
    }

    // ── readAppLog / readSyslog delegation ────────────────────────────────────

    /**
     * Verifies that {@code readAppLog} resolves and reads from the configured log file path,
     * returning that path and its contents in the response.
     *
     * @param dir JUnit-provided temporary directory used to create the real log file
     * @throws IOException if writing the temporary log file fails
     */
    @Test
    void readAppLog_usesConfiguredLogFilePath(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("configured.log");
        Files.write(file, List.of("hello"));
        setConfiguredLogFile(file.toString());

        LogViewerResponse resp = service.readAppLog(100, null);

        assertThat(resp.getFilePath()).isEqualTo(file.toString());
        assertThat(resp.getLines()).containsExactly("hello");
    }

    /**
     * Verifies that {@code readSyslog} always produces a non-null response with a resolved,
     * non-blank file path regardless of whether the syslog file exists in the test environment.
     */
    @Test
    void readSyslog_returnsResponseWithResolvedPath() {
        setConfiguredLogFile("");

        LogViewerResponse resp = service.readSyslog(10, null);

        // syslog path may or may not exist in the test env; a response is always produced
        assertThat(resp).isNotNull();
        assertThat(resp.getFilePath()).isNotBlank();
    }

    // ── getLoggers / setLogLevel (live logback context) ───────────────────────

    /**
     * Verifies that {@code getLoggers} returns a non-empty list including the ROOT logger and
     * the application package, sorted by name, with every entry exposing an effective level.
     */
    @Test
    void getLoggers_includesRootAndKnownPackages_sortedByName() {
        List<LoggerInfoResponse> loggers = service.getLoggers();

        assertThat(loggers).isNotEmpty();
        assertThat(loggers).extracting(LoggerInfoResponse::getName).contains("ROOT");
        assertThat(loggers).extracting(LoggerInfoResponse::getName).contains("org.automatize.status");
        assertThat(loggers).isSortedAccordingTo(java.util.Comparator.comparing(LoggerInfoResponse::getName));
        assertThat(loggers).allSatisfy(l -> assertThat(l.getEffectiveLevel()).isNotNull());
    }

    /**
     * Verifies that setting an explicit level (DEBUG) on a named logger records that level as the
     * logger's configured level; a finally block restores the logger to DEFAULT afterwards.
     */
    @Test
    void setLogLevel_explicitLevel_setsConfiguredLevel() {
        String name = "org.automatize.status.services.LogViewerServiceTest.custom";
        try {
            service.setLogLevel(name, "DEBUG");

            LoggerInfoResponse info = service.getLoggers().stream()
                    .filter(l -> l.getName().equals(name))
                    .findFirst().orElseThrow();
            assertThat(info.getConfiguredLevel()).isEqualTo("DEBUG");
        } finally {
            service.setLogLevel(name, "DEFAULT");
        }
    }

    /**
     * Verifies that passing the DEFAULT keyword clears a previously configured level so the logger
     * either drops out of the configured set or reports a null configured level.
     */
    @Test
    void setLogLevel_defaultKeyword_clearsConfiguredLevel() {
        String name = "org.automatize.status.services.LogViewerServiceTest.reset";
        service.setLogLevel(name, "WARN");
        service.setLogLevel(name, "DEFAULT");

        LoggerInfoResponse info = service.getLoggers().stream()
                .filter(l -> l.getName().equals(name))
                .findFirst().orElse(null);
        // Once cleared, either it drops out of the "configured" set or reports null configured level
        // if the logger is still present, assert its configured level was cleared to null
        if (info != null) {
            assertThat(info.getConfiguredLevel()).isNull();
        }
    }
}
