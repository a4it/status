package org.automatize.status.services;

import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.models.StatusIncident;
import org.automatize.status.models.StatusIncidentComponent;
import org.automatize.status.models.StatusUptimeHistory;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.automatize.status.repositories.StatusIncidentComponentRepository;
import org.automatize.status.repositories.StatusIncidentRepository;
import org.automatize.status.repositories.StatusUptimeHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * Service responsible for calculating and storing daily uptime history for apps and components.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Run daily scheduled job to calculate previous day's uptime</li>
 *   <li>Calculate uptime based on public incidents only</li>
 *   <li>Store daily uptime records in StatusUptimeHistory table</li>
 *   <li>Support backfilling historical uptime data</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see StatusUptimeHistory
 * @see StatusIncident
 */
@Service
public class UptimeHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(UptimeHistoryService.class);
    private static final int MINUTES_IN_DAY = 1440;

    private final StatusAppRepository statusAppRepository;
    private final StatusComponentRepository statusComponentRepository;
    private final StatusIncidentRepository statusIncidentRepository;
    private final StatusIncidentComponentRepository statusIncidentComponentRepository;
    private final StatusUptimeHistoryRepository statusUptimeHistoryRepository;

    @Value("${uptime-history.enabled:true}")
    private boolean enabled;

    /**
     * Constructs a new UptimeHistoryService with the required dependencies.
     *
     * @param statusAppRepository repository for status app data access
     * @param statusComponentRepository repository for status component data access
     * @param statusIncidentRepository repository for status incident data access
     * @param statusIncidentComponentRepository repository for incident-component relationship data access
     * @param statusUptimeHistoryRepository repository for uptime history data access
     */
    public UptimeHistoryService(StatusAppRepository statusAppRepository,
                                 StatusComponentRepository statusComponentRepository,
                                 StatusIncidentRepository statusIncidentRepository,
                                 StatusIncidentComponentRepository statusIncidentComponentRepository,
                                 StatusUptimeHistoryRepository statusUptimeHistoryRepository) {
        this.statusAppRepository = statusAppRepository;
        this.statusComponentRepository = statusComponentRepository;
        this.statusIncidentRepository = statusIncidentRepository;
        this.statusIncidentComponentRepository = statusIncidentComponentRepository;
        this.statusUptimeHistoryRepository = statusUptimeHistoryRepository;
    }

    /**
     * Scheduled job that runs daily at 00:05 to calculate the previous day's uptime.
     * This ensures all incidents for the previous day are accounted for.
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void calculateDailyUptime() {
        if (!enabled) {
            logger.debug("Uptime history calculation is disabled");
            return;
        }

        LocalDate yesterday = LocalDate.now().minusDays(1);
        logger.info("Starting daily uptime calculation for date: {}", yesterday);

        try {
            calculateUptimeForDate(yesterday);
            logger.info("Completed daily uptime calculation for date: {}", yesterday);
        } catch (Exception e) {
            logger.error("Error calculating daily uptime for date {}: {}", yesterday, e.getMessage(), e);
        }
    }

    /**
     * Calculate and store uptime for a specific date.
     * For each app and component, queries public incidents that overlap with the date,
     * calculates outage/degraded minutes, and stores the uptime record.
     *
     * @param date the date to calculate uptime for
     */
    @Transactional
    public void calculateUptimeForDate(LocalDate date) {
        logger.debug("Calculating uptime for date: {}", date);

        List<StatusApp> apps = statusAppRepository.findAll();
        int appsProcessed = 0;
        int componentsProcessed = 0;

        for (StatusApp app : apps) {
            try {
                calculateAppUptime(app, date);
                appsProcessed++;

                List<StatusComponent> components = statusComponentRepository.findByAppId(app.getId());
                for (StatusComponent component : components) {
                    try {
                        calculateComponentUptime(app, component, date);
                        componentsProcessed++;
                    } catch (Exception e) {
                        logger.error("Error calculating uptime for component {} on {}: {}",
                                component.getName(), date, e.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.error("Error calculating uptime for app {} on {}: {}",
                        app.getName(), date, e.getMessage());
            }
        }

        logger.debug("Uptime calculation completed for {} apps and {} components on {}",
                appsProcessed, componentsProcessed, date);
    }

    /**
     * Backfill uptime history for a range of days up to today.
     *
     * @param days the number of days to backfill (counting backwards from today)
     * @return the number of days processed
     */
    @Transactional
    public int backfillUptimeHistory(int days) {
        logger.info("Starting uptime history backfill for {} days", days);

        int processed = 0;
        for (int i = days; i > 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            try {
                calculateUptimeForDate(date);
                processed++;
            } catch (Exception e) {
                logger.error("Error backfilling uptime for date {}: {}", date, e.getMessage());
            }
        }

        logger.info("Completed uptime history backfill: {} days processed", processed);
        return processed;
    }

    /**
     * Calculate uptime for a specific app on a specific date.
     *
     * @param app the app to calculate uptime for
     * @param date the date to calculate uptime for
     */
    private void calculateAppUptime(StatusApp app, LocalDate date) {
        ZonedDateTime dayStart = date.atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);

        List<StatusIncident> incidents = statusIncidentRepository
                .findPublicIncidentsAffectingDate(app.getId(), dayStart, dayEnd);

        int outageMinutes = 0;
        int degradedMinutes = 0;

        for (StatusIncident incident : incidents) {
            int minutes = calculateIncidentMinutesForDay(incident, dayStart, dayEnd);
            if (isSevereIncident(incident)) {
                outageMinutes += minutes;
            } else {
                degradedMinutes += minutes;
            }
        }

        saveUptimeRecord(app, null, date, outageMinutes, degradedMinutes, incidents.size());
    }

    /**
     * Calculate uptime for a specific component on a specific date.
     *
     * @param app the parent app of the component
     * @param component the component to calculate uptime for
     * @param date the date to calculate uptime for
     */
    private void calculateComponentUptime(StatusApp app, StatusComponent component, LocalDate date) {
        ZonedDateTime dayStart = date.atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);

        List<StatusIncidentComponent> incidentComponents = statusIncidentComponentRepository
                .findPublicIncidentsAffectingComponentOnDate(component.getId(), dayStart, dayEnd);

        int outageMinutes = 0;
        int degradedMinutes = 0;

        for (StatusIncidentComponent ic : incidentComponents) {
            StatusIncident incident = ic.getIncident();
            int minutes = calculateIncidentMinutesForDay(incident, dayStart, dayEnd);
            if (isSevereIncident(incident)) {
                outageMinutes += minutes;
            } else {
                degradedMinutes += minutes;
            }
        }

        saveUptimeRecord(app, component, date, outageMinutes, degradedMinutes, incidentComponents.size());
    }

    /**
     * Calculate how many minutes an incident affected a specific day.
     * Clamps the incident duration to the day boundaries.
     *
     * @param incident the incident to calculate duration for
     * @param dayStart the start of the day
     * @param dayEnd the end of the day
     * @return the number of minutes the incident affected the day
     */
    private int calculateIncidentMinutesForDay(StatusIncident incident, ZonedDateTime dayStart, ZonedDateTime dayEnd) {
        ZonedDateTime effectiveStart = incident.getStartedAt().isBefore(dayStart)
                ? dayStart : incident.getStartedAt();
        ZonedDateTime effectiveEnd = incident.getResolvedAt() != null && incident.getResolvedAt().isBefore(dayEnd)
                ? incident.getResolvedAt() : dayEnd;

        if (effectiveEnd.isBefore(effectiveStart)) {
            return 0;
        }

        long minutes = ChronoUnit.MINUTES.between(effectiveStart, effectiveEnd);
        return Math.max(0, (int) Math.min(minutes, MINUTES_IN_DAY));
    }

    /**
     * Determine if an incident is severe (CRITICAL or MAJOR).
     * Severe incidents count as outage minutes, others count as degraded minutes.
     *
     * @param incident the incident to check
     * @return true if the incident is severe
     */
    private boolean isSevereIncident(StatusIncident incident) {
        String severity = incident.getSeverity();
        return "CRITICAL".equals(severity) || "MAJOR".equals(severity);
    }

    /**
     * Save or update an uptime history record.
     *
     * @param app the app the record belongs to
     * @param component the component the record belongs to (null for app-level records)
     * @param date the date of the record
     * @param outageMinutes the number of outage minutes
     * @param degradedMinutes the number of degraded minutes
     * @param incidentCount the number of incidents
     */
    private void saveUptimeRecord(StatusApp app, StatusComponent component, LocalDate date,
                                   int outageMinutes, int degradedMinutes, int incidentCount) {

        Optional<StatusUptimeHistory> existing = component != null
                ? statusUptimeHistoryRepository.findComponentUptimeByDate(component.getId(), date)
                : statusUptimeHistoryRepository.findAppUptimeByDate(app.getId(), date);

        StatusUptimeHistory record = existing.orElse(new StatusUptimeHistory());
        record.setApp(app);
        record.setComponent(component);
        record.setRecordDate(date);
        record.setTotalMinutes(MINUTES_IN_DAY);
        record.setOutageMinutes(outageMinutes);
        record.setDegradedMinutes(degradedMinutes);
        record.setOperationalMinutes(MINUTES_IN_DAY - outageMinutes - degradedMinutes);
        record.setIncidentCount(incidentCount);

        BigDecimal uptime = BigDecimal.valueOf(MINUTES_IN_DAY - outageMinutes - degradedMinutes)
                .divide(BigDecimal.valueOf(MINUTES_IN_DAY), 5, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(3, RoundingMode.HALF_UP);
        record.setUptimePercentage(uptime);

        if (outageMinutes > 0) {
            record.setStatus("MAJOR_OUTAGE");
        } else if (degradedMinutes > 0) {
            record.setStatus("DEGRADED");
        } else {
            record.setStatus("OPERATIONAL");
        }

        statusUptimeHistoryRepository.save(record);

        logger.debug("Saved uptime record for {} {} on {}: {}% (outage: {}min, degraded: {}min)",
                component != null ? "component" : "app",
                component != null ? component.getName() : app.getName(),
                date,
                uptime,
                outageMinutes,
                degradedMinutes);
    }
}
