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

/**
 * <p>
 * Service responsible for building process mining cases from correlated log events.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Resolve the set of service names in scope (by platform or application)</li>
 *   <li>Group log events by trace id into ordered process cases</li>
 *   <li>Decorate events with severity-level icons for visualisation</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
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

    /**
     * Builds process mining cases by correlating log events by trace id within the given scope and time window.
     * <p>
     * Only traces with at least {@code minEvents} events are returned, and the number of cases is
     * capped at {@code maxCases} (the response is flagged as truncated when the cap is reached).
     * </p>
     *
     * @param scope the scope type ("platform" or "application")
     * @param scopeId the UUID of the platform or application in scope
     * @param tenantId the tenant to restrict logs to
     * @param from the start of the time window
     * @param to the end of the time window
     * @param maxCases the maximum number of cases to return
     * @param minEvents the minimum number of events a trace must have to be included
     * @return a ProcessMiningResponse containing the assembled cases, case count, and truncation flag
     */
    public ProcessMiningResponse buildCases(String scope, UUID scopeId, UUID tenantId,
                                            ZonedDateTime from, ZonedDateTime to,
                                            int maxCases, int minEvents) {
        List<String> serviceNames = resolveServiceNames(scope, scopeId);
        // No services in scope means there is nothing to mine
        if (serviceNames.isEmpty()) {
            return new ProcessMiningResponse(List.of(), 0, false);
        }

        List<String> traceIds = logRepository.findDistinctTraceIdsForServices(
            tenantId, from, to, serviceNames, PageRequest.of(0, maxCases));

        boolean truncated = traceIds.size() == maxCases;

        // No matching traces means there are no cases to build
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

    /**
     * Resolves the set of service names in scope for mining based on the scope type.
     *
     * @param scope the scope type ("platform" or "application")
     * @param scopeId the UUID of the platform or application
     * @return the service names to mine, or an empty list for an unrecognised scope
     */
    private List<String> resolveServiceNames(String scope, UUID scopeId) {
        // Platform scope: service names are the apps belonging to the platform
        if ("platform".equals(scope)) {
            return statusAppRepository.findByPlatformId(scopeId).stream()
                .map(app -> app.getName())
                .collect(Collectors.toList());
        // Application scope: service names are the components belonging to the app
        } else if ("application".equals(scope)) {
            return statusComponentRepository.findByAppId(scopeId).stream()
                .map(component -> component.getName())
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
