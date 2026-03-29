package org.automatize.status.services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.automatize.status.api.response.LogViewerResponse;
import org.automatize.status.api.response.LoggerInfoResponse;
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

@Service
public class LogViewerService {

    private static final int MAX_LINES = 2000;

    @Value("${logging.file.name:}")
    private String configuredLogFile;

    public LogViewerResponse readAppLog(int lines, String search) {
        String filePath = resolveAppLogPath();
        return readLogFile(filePath, lines, search);
    }

    public LogViewerResponse readSyslog(int lines, String search) {
        String filePath = resolveSyslogPath();
        return readLogFile(filePath, lines, search);
    }

    public LogViewerResponse readLogFile(String filePath, int lines, String search) {
        LogViewerResponse response = new LogViewerResponse();
        response.setFilePath(filePath);

        if (filePath == null || filePath.isBlank()) {
            response.setLines(List.of("[No log file path configured]"));
            response.setTotalLines(1);
            response.setTruncated(false);
            response.setFileSizeBytes(0);
            return response;
        }

        Path path = Paths.get(filePath);
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
                if (hasSearch && !line.toLowerCase().contains(searchLower)) {
                    continue;
                }
                deque.addLast(line);
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
        } catch (IOException ignored) {
        }

        response.setLines(resultLines);
        response.setTotalLines(resultLines.size());
        response.setTruncated(truncated);
        response.setFileSizeBytes(fileSize);
        return response;
    }

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
            if (logger.getLevel() != null) {
                interestingNames.add(logger.getName());
            }
        }

        List<LoggerInfoResponse> result = new ArrayList<>();
        for (String name : interestingNames) {
            ch.qos.logback.classic.Logger logger = ctx.getLogger(name);
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

    public void setLogLevel(String loggerName, String level) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = ctx.getLogger(loggerName);
        if (logger == null) return;

        if (level == null || level.isBlank() || "DEFAULT".equalsIgnoreCase(level) || "null".equalsIgnoreCase(level)) {
            logger.setLevel(null);
        } else {
            logger.setLevel(Level.toLevel(level, null));
        }
    }

    private String resolveAppLogPath() {
        if (configuredLogFile != null && !configuredLogFile.isBlank()) {
            return configuredLogFile;
        }
        // Default Spring Boot log file location
        Path defaultPath = Paths.get("logs/spring.log");
        if (Files.exists(defaultPath)) {
            return defaultPath.toAbsolutePath().toString();
        }
        return defaultPath.toAbsolutePath().toString();
    }

    private String resolveSyslogPath() {
        Path syslog = Paths.get("/var/log/syslog");
        if (Files.exists(syslog)) {
            return syslog.toString();
        }
        Path messages = Paths.get("/var/log/messages");
        if (Files.exists(messages)) {
            return messages.toString();
        }
        return "/var/log/syslog";
    }
}
