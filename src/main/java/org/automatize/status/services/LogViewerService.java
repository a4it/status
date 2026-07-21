package org.automatize.status.services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.automatize.status.api.response.LogViewerResponse;
import org.automatize.status.api.response.LoggerInfoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Service that exposes read access to application and system log files and
 * runtime control over Logback logger levels.
 * <p>
 * Supports tailing the last N lines of a log file with optional case-insensitive
 * search filtering, listing the currently interesting loggers with their effective
 * and configured levels, and dynamically adjusting a logger's level at runtime.
 * </p>
 */
@Service
public class LogViewerService {

    private static final Logger logger = LoggerFactory.getLogger(LogViewerService.class);

    private static final int MAX_LINES = 2000;

    @Value("${logging.file.name:}")
    private String configuredLogFile;

    /**
     * Reads the tail of the application log file.
     *
     * @param lines  the maximum number of lines to return (capped at {@link #MAX_LINES})
     * @param search optional case-insensitive filter; only matching lines are returned
     * @return a response containing the matching log lines and file metadata
     */
    public LogViewerResponse readAppLog(int lines, String search) {
        String filePath = resolveAppLogPath();
        return readLogFile(filePath, lines, search);
    }

    /**
     * Reads the tail of the system log file (syslog/messages).
     *
     * @param lines  the maximum number of lines to return (capped at {@link #MAX_LINES})
     * @param search optional case-insensitive filter; only matching lines are returned
     * @return a response containing the matching log lines and file metadata
     */
    public LogViewerResponse readSyslog(int lines, String search) {
        String filePath = resolveSyslogPath();
        return readLogFile(filePath, lines, search);
    }

    /**
     * Reads the last matching lines of the given log file.
     * <p>
     * Streams the file line by line, applying the optional search filter, and keeps
     * only the last {@code lines} matching entries in memory. Missing paths, absent
     * files, and I/O errors are reported gracefully within the response rather than
     * thrown.
     * </p>
     *
     * @param filePath the absolute path of the log file to read
     * @param lines    the maximum number of lines to return (capped at {@link #MAX_LINES})
     * @param search   optional case-insensitive filter; only matching lines are returned
     * @return a response containing the resulting log lines, truncation flag and file size
     */
    public LogViewerResponse readLogFile(String filePath, int lines, String search) {
        LogViewerResponse response = new LogViewerResponse();
        response.setFilePath(filePath);

        // No log file path configured: return a placeholder response
        if (filePath == null || filePath.isBlank()) {
            response.setLines(List.of("[No log file path configured]"));
            response.setTotalLines(1);
            response.setTruncated(false);
            response.setFileSizeBytes(0);
            return response;
        }

        Path path = Paths.get(filePath);
        // Configured file does not exist on disk: return a not-found placeholder
        if (!Files.exists(path)) {
            response.setLines(List.of("[Log file not found: " + filePath + "]"));
            response.setTotalLines(1);
            response.setTruncated(false);
            response.setFileSizeBytes(0);
            return response;
        }

        int requestedLines = Math.min(lines, MAX_LINES);
        boolean hasSearch = search != null && !search.isBlank();
        String searchLower = hasSearch ? search.toLowerCase() : null;

        // Use a deque to keep only the last N matching lines (memory efficient)
        ArrayDeque<String> deque = new ArrayDeque<>(requestedLines + 1);
        int totalRead = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalRead++;
                // Skip lines that do not match the active search filter
                if (hasSearch && !line.toLowerCase().contains(searchLower)) {
                    continue;
                }
                deque.addLast(line);
                // Drop the oldest retained line once the window exceeds the requested size
                if (deque.size() > requestedLines) {
                    deque.removeFirst();
                }
            }
        } catch (IOException e) {
            response.setLines(List.of("[Error reading log file: " + e.getMessage() + "]"));
            response.setTotalLines(1);
            response.setTruncated(false);
            response.setFileSizeBytes(0);
            return response;
        }

        List<String> resultLines = new ArrayList<>(deque);
        boolean truncated = totalRead > requestedLines && !hasSearch;

        long fileSize = 0;
        try {
            fileSize = Files.size(path);
        } catch (IOException e) {
            logger.debug("Unable to determine size of log file: {}", filePath, e);
        }

        response.setLines(resultLines);
        response.setTotalLines(resultLines.size());
        response.setTruncated(truncated);
        response.setFileSizeBytes(fileSize);
        return response;
    }

    /**
     * Lists the loggers of interest together with their level information.
     * <p>
     * The set always includes a fixed group of well-known loggers (ROOT, Spring,
     * Hibernate, the application package and Hikari) plus any logger that has an
     * explicitly configured level. Results are sorted alphabetically by name.
     * </p>
     *
     * @return a sorted list of logger information (name, effective and configured level)
     */
    public List<LoggerInfoResponse> getLoggers() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();

        Set<String> interestingNames = new LinkedHashSet<>();
        interestingNames.add("ROOT");
        interestingNames.add("org.springframework");
        interestingNames.add("org.hibernate");
        interestingNames.add("org.automatize.status");
        interestingNames.add("com.zaxxer.hikari");

        // Also add any logger that has an explicitly configured level
        for (ch.qos.logback.classic.Logger logger : ctx.getLoggerList()) {
            // Include any logger that has an explicitly configured (non-inherited) level
            if (logger.getLevel() != null) {
                interestingNames.add(logger.getName());
            }
        }

        List<LoggerInfoResponse> result = new ArrayList<>();
        for (String name : interestingNames) {
            ch.qos.logback.classic.Logger logger = ctx.getLogger(name);
            // Skip names that do not resolve to a logger instance
            if (logger == null) continue;

            LoggerInfoResponse info = new LoggerInfoResponse();
            info.setName(logger.getName());
            info.setEffectiveLevel(logger.getEffectiveLevel() != null ? logger.getEffectiveLevel().toString() : "INHERITED");
            info.setConfiguredLevel(logger.getLevel() != null ? logger.getLevel().toString() : null);
            result.add(info);
        }

        result.sort(Comparator.comparing(LoggerInfoResponse::getName));
        return result;
    }

    /**
     * Adjusts the level of a named logger at runtime.
     * <p>
     * Passing a blank, {@code "DEFAULT"} or {@code "null"} level resets the logger to
     * inherit its parent's level; any other value sets the explicit level parsed by
     * Logback.
     * </p>
     *
     * @param loggerName the name of the logger to modify
     * @param level      the level to apply, or a reset sentinel to inherit the parent level
     */
    public void setLogLevel(String loggerName, String level) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = ctx.getLogger(loggerName);
        // Unknown logger name: nothing to change
        if (logger == null) return;

        // Reset sentinel provided: clear the explicit level so the parent's level is inherited
        if (level == null || level.isBlank() || "DEFAULT".equalsIgnoreCase(level) || "null".equalsIgnoreCase(level)) {
            logger.setLevel(null);
        } else {
            // Explicit level requested: parse and apply it
            logger.setLevel(Level.toLevel(level, null));
        }
    }

    /**
     * Resolves the path of the application log file.
     * <p>
     * Prefers the {@code logging.file.name} configured value; otherwise falls back to
     * the default Spring Boot location ({@code logs/spring.log}), returned as an
     * absolute path.
     * </p>
     *
     * @return the resolved application log file path
     */
    private String resolveAppLogPath() {
        // Use the explicitly configured log file when one is set
        if (configuredLogFile != null && !configuredLogFile.isBlank()) {
            return configuredLogFile;
        }
        // Default Spring Boot log file location
        Path defaultPath = Paths.get("logs/spring.log");
        // Prefer the default file when it exists on disk
        if (Files.exists(defaultPath)) {
            return defaultPath.toAbsolutePath().toString();
        }
        return defaultPath.toAbsolutePath().toString();
    }

    /**
     * Resolves the path of the system log file.
     * <p>
     * Probes the common locations ({@code /var/log/syslog} then {@code /var/log/messages})
     * and returns the first that exists, defaulting to {@code /var/log/syslog}.
     * </p>
     *
     * @return the resolved system log file path
     */
    private String resolveSyslogPath() {
        Path syslog = Paths.get("/var/log/syslog");
        // Debian-style syslog present: use it
        if (Files.exists(syslog)) {
            return syslog.toString();
        }
        Path messages = Paths.get("/var/log/messages");
        // RHEL-style messages log present: use it
        if (Files.exists(messages)) {
            return messages.toString();
        }
        return "/var/log/syslog";
    }
}
