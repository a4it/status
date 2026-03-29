package org.automatize.status.services;

import org.automatize.status.api.response.ProcessMiningResponse;
import org.automatize.status.models.Log;
import org.automatize.status.repositories.LogRepository;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProcessMiningService {

    private static final Map<String, String> LEVEL_ICONS = Map.of(
        "CRITICAL", "\uD83D\uDEA8",
        "ERROR", "\u274C",
        "WARNING", "\u26A0\uFE0F",
        "INFO", "\u2139\uFE0F",
        "DEBUG", "\uD83D\uDD0D"
    );

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private StatusAppRepository statusAppRepository;

    @Autowired
    private StatusComponentRepository statusComponentRepository;

    public ProcessMiningResponse buildCases(String scope, UUID scopeId, UUID tenantId,
                                            ZonedDateTime from, ZonedDateTime to,
                                            int maxCases, int minEvents) {
        List<String> serviceNames = resolveServiceNames(scope, scopeId);
        if (serviceNames.isEmpty()) {
            return new ProcessMiningResponse(List.of(), 0, false);
        }

        List<String> traceIds = logRepository.findDistinctTraceIdsForServices(
            tenantId, from, to, serviceNames, PageRequest.of(0, maxCases));

        boolean truncated = traceIds.size() == maxCases;

        if (traceIds.isEmpty()) {
            return new ProcessMiningResponse(List.of(), 0, false);
        }

        List<Log> logs = logRepository.findByTraceIdIn(traceIds);

        Map<String, List<Log>> grouped = logs.stream()
            .collect(Collectors.groupingBy(Log::getTraceId, LinkedHashMap::new, Collectors.toList()));

        List<ProcessMiningResponse.ProcessCase> cases = grouped.entrySet().stream()
            .filter(e -> e.getValue().size() >= minEvents)
            .map(e -> {
                List<ProcessMiningResponse.ProcessEvent> events = e.getValue().stream()
                    .sorted(Comparator.comparing(Log::getLogTimestamp))
                    .map(log -> new ProcessMiningResponse.ProcessEvent(
                        log.getService() != null ? log.getService() : "unknown",
                        log.getLogTimestamp().toString(),
                        LEVEL_ICONS.getOrDefault(log.getLevel(), "\uD83D\uDCCB"),
                        log.getLevel(),
                        log.getMessage()
                    ))
                    .collect(Collectors.toList());
                return new ProcessMiningResponse.ProcessCase(e.getKey(), events);
            })
            .collect(Collectors.toList());

        return new ProcessMiningResponse(cases, cases.size(), truncated);
    }

    private List<String> resolveServiceNames(String scope, UUID scopeId) {
        if ("platform".equals(scope)) {
            return statusAppRepository.findByPlatformId(scopeId).stream()
                .map(app -> app.getName())
                .collect(Collectors.toList());
        } else if ("application".equals(scope)) {
            return statusComponentRepository.findByAppId(scopeId).stream()
                .map(component -> component.getName())
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
