package org.automatize.status.config;

import org.automatize.status.models.*;
import org.automatize.status.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Secondary data initializer that seeds realistic process mining demo data.
 * <p>
 * Creates a demo platform with services and correlated log traces to populate
 * the process mining view with meaningful workflow patterns, including alternative
 * paths that skip certain events (cache hits, guest checkout, fraud blocks, etc.).
 * <p>
 * Can be disabled via {@code data.demo.enabled=false}.
 */
@Component
@Order(2)
public class ProcessMiningDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProcessMiningDataInitializer.class);

    private static final String DEMO_PLATFORM_SLUG = "demo-platform";

    // Service indices
    private static final int SVC_GATEWAY   = 0;
    private static final int SVC_AUTH      = 1;
    private static final int SVC_ORDER     = 2;
    private static final int SVC_PAYMENT   = 3;
    private static final int SVC_NOTIFY    = 4;
    private static final int SVC_INVENTORY = 5;
    private static final int SVC_FRAUD     = 6;
    private static final int SVC_CACHE     = 7;
    private static final int SVC_SEARCH    = 8;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${data.initializer.enabled:false}")
    private boolean enabled;

    @Value("${app.setup.completed:false}")
    private boolean setupCompleted;

    private final TenantRepository tenantRepository;
    private final OrganizationRepository organizationRepository;
    private final StatusPlatformRepository statusPlatformRepository;
    private final StatusAppRepository statusAppRepository;
    private final LogRepository logRepository;

    public ProcessMiningDataInitializer(TenantRepository tenantRepository,
                                        OrganizationRepository organizationRepository,
                                        StatusPlatformRepository statusPlatformRepository,
                                        StatusAppRepository statusAppRepository,
                                        LogRepository logRepository) {
        this.tenantRepository = tenantRepository;
        this.organizationRepository = organizationRepository;
        this.statusPlatformRepository = statusPlatformRepository;
        this.statusAppRepository = statusAppRepository;
        this.logRepository = logRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            logger.info("Process mining demo data initializer is disabled (data.initializer.enabled=false)");
            return;
        }

        if (!setupCompleted) {
            logger.info("Skipping process mining data initializer: setup wizard not yet complete (app.setup.completed=false)");
            return;
        }

        Tenant tenant = tenantRepository.findByName("Default Tenant").orElse(null);
        if (tenant == null) {
            logger.warn("Default Tenant not found — skipping process mining data initialization");
            return;
        }

        Organization org = organizationRepository.findByName("Default Organization").orElse(null);
        if (org == null) {
            logger.warn("Default Organization not found — skipping process mining data initialization");
            return;
        }

        // Wipe existing demo data so we always start fresh
        statusPlatformRepository.findBySlug(DEMO_PLATFORM_SLUG).ifPresent(existing -> {
            logger.info("Removing existing demo data before re-seeding...");
            List<StatusApp> existingApps = statusAppRepository.findByPlatformId(existing.getId());
            List<String> serviceNames = existingApps.stream().map(StatusApp::getName).toList();
            if (!serviceNames.isEmpty()) {
                logRepository.deleteByServiceIn(serviceNames);
            }
            statusAppRepository.deleteAll(existingApps);
            statusPlatformRepository.delete(existing);
        });
        entityManager.flush();

        logger.info("Seeding process mining demo data...");

        StatusPlatform platform = createPlatform(tenant, org);

        List<StatusApp> apps = new ArrayList<>();
        for (String name : List.of(
                "API Gateway",
                "Auth Service",
                "Order Service",
                "Payment Service",
                "Notification Service",
                "Inventory Service",
                "Fraud Detection",
                "Cache Service",
                "Search Service")) {
            apps.add(createApp(name, platform, tenant, org));
        }

        generateLogs(tenant, apps);

        logger.info("Process mining demo data seeded: 1 platform, {} apps, logs generated", apps.size());
    }

    private StatusPlatform createPlatform(Tenant tenant, Organization org) {
        StatusPlatform p = new StatusPlatform();
        p.setName("Demo Platform");
        p.setSlug(DEMO_PLATFORM_SLUG);
        p.setDescription("Demo platform for process mining visualization");
        p.setStatus("OPERATIONAL");
        p.setIsPublic(true);
        p.setTenant(tenant);
        p.setOrganization(org);
        p.setCreatedBy("system");
        p.setLastModifiedBy("system");
        return statusPlatformRepository.save(p);
    }

    private StatusApp createApp(String name, StatusPlatform platform, Tenant tenant, Organization org) {
        StatusApp app = new StatusApp();
        app.setName(name);
        app.setSlug(name.toLowerCase().replace(" ", "-") + "-demo");
        app.setDescription("Demo service: " + name);
        app.setStatus("OPERATIONAL");
        app.setIsPublic(true);
        app.setPlatform(platform);
        app.setTenant(tenant);
        app.setOrganization(org);
        app.setCreatedBy("system");
        app.setLastModifiedBy("system");
        return statusAppRepository.save(app);
    }

    private void generateLogs(Tenant tenant, List<StatusApp> apps) {
        Random rng = new Random(42);
        ZonedDateTime now = ZonedDateTime.now();

        List<Log> logs = new ArrayList<>();

        // 300 traces spread over the last 30 days
        for (int i = 0; i < 300; i++) {
            String traceId = UUID.randomUUID().toString();
            ZonedDateTime base = now.minusSeconds(rng.nextInt(30 * 24 * 3600));

            int pattern = pickPattern(rng);
            logs.addAll(buildTrace(tenant, apps, traceId, base, pattern, rng));
        }

        logRepository.saveAll(logs);
        logger.info("Saved {} log entries for process mining", logs.size());
    }

    /**
     * Weighted pattern selection (out of 100):
     *  0 = happy path            (20%) — Gateway → Auth → Order → Payment → Notify
     *  1 = auth failure          (10%) — Gateway → Auth (fail) → blocked
     *  2 = payment failure       (10%) — full flow but Payment declines, Order rolls back
     *  3 = pre-paid / notify     ( 8%) — Gateway → Auth → Order → Notify (skips Payment)
     *  4 = timeout / critical    ( 7%) — Gateway → Auth → Order hangs → 504
     *  5 = cache-hit checkout    ( 8%) — Gateway → Auth → Cache → Payment → Notify (skips Order Service)
     *  6 = guest checkout        ( 7%) — Gateway → Order → Payment → Notify (skips Auth)
     *  7 = fraud detected        ( 7%) — Gateway → Auth → Fraud Detection → blocked
     *  8 = scheduled batch job   ( 7%) — Order → Inventory → Payment → Notify (no Gateway / Auth)
     *  9 = search-driven order   ( 8%) — Search → Gateway → Auth → Order → Inventory → Payment → Notify
     * 10 = payment retry success ( 8%) — full flow with Payment fail → retry → succeed
     */
    private int pickPattern(Random rng) {
        int r = rng.nextInt(100);
        if (r < 20) return 0;
        if (r < 30) return 1;
        if (r < 40) return 2;
        if (r < 48) return 3;
        if (r < 55) return 4;
        if (r < 63) return 5;
        if (r < 70) return 6;
        if (r < 77) return 7;
        if (r < 84) return 8;
        if (r < 92) return 9;
        return 10;
    }

    private List<Log> buildTrace(Tenant tenant, List<StatusApp> apps, String traceId,
                                 ZonedDateTime base, int pattern, Random rng) {
        return switch (pattern) {
            case 0  -> happyPath(tenant, apps, traceId, base, rng);
            case 1  -> authFailure(tenant, apps, traceId, base, rng);
            case 2  -> paymentFailure(tenant, apps, traceId, base, rng);
            case 3  -> prePaidOrder(tenant, apps, traceId, base, rng);
            case 4  -> timeoutCritical(tenant, apps, traceId, base, rng);
            case 5  -> cacheHitCheckout(tenant, apps, traceId, base, rng);
            case 6  -> guestCheckout(tenant, apps, traceId, base, rng);
            case 7  -> fraudDetected(tenant, apps, traceId, base, rng);
            case 8  -> scheduledBatchJob(tenant, apps, traceId, base, rng);
            case 9  -> searchDrivenOrder(tenant, apps, traceId, base, rng);
            default -> paymentRetrySuccess(tenant, apps, traceId, base, rng);
        };
    }

    // ── Pattern 0 ──────────────────────────────────────────────────────────────
    // Gateway → Auth → Order → Payment → Notification  (all INFO)
    private List<Log> happyPath(Tenant tenant, List<StatusApp> apps, String traceId,
                                ZonedDateTime base, Random rng) {
        String orderId = orderId(rng);
        int userId = rng.nextInt(9000) + 1000;
        return List.of(
            log(tenant, apps.get(SVC_GATEWAY),   traceId, base,                  "INFO", "Incoming POST /api/orders — " + orderId + " from user " + userId),
            log(tenant, apps.get(SVC_AUTH),       traceId, base.plusSeconds(1),  "INFO", "JWT validated for user " + userId + ", role=CUSTOMER"),
            log(tenant, apps.get(SVC_ORDER),      traceId, base.plusSeconds(3),  "INFO", "Order " + orderId + " created and persisted to DB"),
            log(tenant, apps.get(SVC_INVENTORY),  traceId, base.plusSeconds(4),  "INFO", "Stock reserved: 1x SKU-" + (rng.nextInt(900) + 100) + " for " + orderId),
            log(tenant, apps.get(SVC_PAYMENT),    traceId, base.plusSeconds(6),  "INFO", "Payment authorised: " + currency(rng) + " for " + orderId),
            log(tenant, apps.get(SVC_NOTIFY),     traceId, base.plusSeconds(8),  "INFO", "Confirmation email queued for " + orderId + " → user " + userId)
        );
    }

    // ── Pattern 1 ──────────────────────────────────────────────────────────────
    // Gateway → Auth (token expired → rejected)  — Order/Payment/Notify never reached
    private List<Log> authFailure(Tenant tenant, List<StatusApp> apps, String traceId,
                                  ZonedDateTime base, Random rng) {
        int userId = rng.nextInt(9000) + 1000;
        return List.of(
            log(tenant, apps.get(SVC_GATEWAY), traceId, base,                  "INFO",    "Incoming POST /api/orders from IP " + randomIp(rng)),
            log(tenant, apps.get(SVC_AUTH),    traceId, base.plusSeconds(1),   "WARNING", "Token expired for user " + userId + " (exp=" + (System.currentTimeMillis() / 1000 - 3600) + ")"),
            log(tenant, apps.get(SVC_AUTH),    traceId, base.plusSeconds(2),   "ERROR",   "Authentication failed: JWT signature invalid for user " + userId),
            log(tenant, apps.get(SVC_GATEWAY), traceId, base.plusSeconds(3),   "WARNING", "Request rejected: 401 Unauthorized — downstream services not called")
        );
    }

    // ── Pattern 2 ──────────────────────────────────────────────────────────────
    // Full flow but Payment declines, Inventory rolls back, Notification skipped
    private List<Log> paymentFailure(Tenant tenant, List<StatusApp> apps, String traceId,
                                     ZonedDateTime base, Random rng) {
        String orderId = orderId(rng);
        int userId = rng.nextInt(9000) + 1000;
        return List.of(
            log(tenant, apps.get(SVC_GATEWAY),   traceId, base,                  "INFO",    "Incoming POST /api/orders — " + orderId),
            log(tenant, apps.get(SVC_AUTH),       traceId, base.plusSeconds(1),  "INFO",    "JWT validated for user " + userId),
            log(tenant, apps.get(SVC_ORDER),      traceId, base.plusSeconds(3),  "INFO",    "Order " + orderId + " reserved in DB"),
            log(tenant, apps.get(SVC_INVENTORY),  traceId, base.plusSeconds(4),  "INFO",    "Stock reserved for " + orderId),
            log(tenant, apps.get(SVC_PAYMENT),    traceId, base.plusSeconds(6),  "WARNING", "Payment gateway timeout for " + orderId + " (5200ms)"),
            log(tenant, apps.get(SVC_PAYMENT),    traceId, base.plusSeconds(9),  "ERROR",   "Payment declined: card issuer refused " + orderId),
            log(tenant, apps.get(SVC_INVENTORY),  traceId, base.plusSeconds(10), "WARNING", "Stock reservation rolled back for " + orderId),
            log(tenant, apps.get(SVC_ORDER),      traceId, base.plusSeconds(11), "WARNING", "Order " + orderId + " cancelled due to payment failure")
        );
    }

    // ── Pattern 3 ──────────────────────────────────────────────────────────────
    // Pre-paid / wallet order — Payment Service skipped entirely
    // Gateway → Auth → Order → Inventory → Notification
    private List<Log> prePaidOrder(Tenant tenant, List<StatusApp> apps, String traceId,
                                   ZonedDateTime base, Random rng) {
        String orderId = orderId(rng);
        int userId = rng.nextInt(9000) + 1000;
        double balance = 20 + rng.nextInt(480);
        return List.of(
            log(tenant, apps.get(SVC_GATEWAY),  traceId, base,                 "INFO", "Incoming POST /api/orders — " + orderId + " (wallet payment)"),
            log(tenant, apps.get(SVC_AUTH),      traceId, base.plusSeconds(1), "INFO", "JWT validated for user " + userId + ", wallet balance=" + balance),
            log(tenant, apps.get(SVC_ORDER),     traceId, base.plusSeconds(3), "INFO", "Order " + orderId + " created, deducted from wallet balance"),
            log(tenant, apps.get(SVC_INVENTORY), traceId, base.plusSeconds(4), "INFO", "Stock reserved for " + orderId),
            log(tenant, apps.get(SVC_NOTIFY),    traceId, base.plusSeconds(6), "INFO", "Wallet order confirmation sent for " + orderId + " → user " + userId)
        );
    }

    // ── Pattern 4 ──────────────────────────────────────────────────────────────
    // Gateway → Auth → Order hangs → CRITICAL timeout, 504 returned
    private List<Log> timeoutCritical(Tenant tenant, List<StatusApp> apps, String traceId,
                                      ZonedDateTime base, Random rng) {
        String orderId = orderId(rng);
        int dbMs = 4000 + rng.nextInt(6000);
        return List.of(
            log(tenant, apps.get(SVC_GATEWAY), traceId, base,                  "INFO",     "Incoming POST /api/checkout — " + orderId),
            log(tenant, apps.get(SVC_AUTH),    traceId, base.plusSeconds(1),   "INFO",     "JWT validated"),
            log(tenant, apps.get(SVC_ORDER),   traceId, base.plusSeconds(3),   "WARNING",  "DB query slow: " + dbMs + "ms for " + orderId + " (threshold=2000ms)"),
            log(tenant, apps.get(SVC_ORDER),   traceId, base.plusSeconds(33),  "CRITICAL", "Request timeout exceeded for " + orderId + " after 30s"),
            log(tenant, apps.get(SVC_GATEWAY), traceId, base.plusSeconds(34),  "ERROR",    "504 Gateway Timeout returned for " + orderId + " — Payment and Notify not reached")
        );
    }

    // ── Pattern 5 ──────────────────────────────────────────────────────────────
    // Cache-hit checkout — Order Service skipped entirely (items already in cart cache)
    // Gateway → Auth → Cache → Payment → Notification
    private List<Log> cacheHitCheckout(Tenant tenant, List<StatusApp> apps, String traceId,
                                       ZonedDateTime base, Random rng) {
        String orderId = orderId(rng);
        int userId = rng.nextInt(9000) + 1000;
        String cacheKey = "cart:" + userId + ":" + (rng.nextInt(9000) + 1000);
        return List.of(
            log(tenant, apps.get(SVC_GATEWAY), traceId, base,                 "INFO", "Incoming POST /api/checkout — " + orderId + " (express checkout)"),
            log(tenant, apps.get(SVC_AUTH),    traceId, base.plusSeconds(1),  "INFO", "JWT validated for user " + userId),
            log(tenant, apps.get(SVC_CACHE),   traceId, base.plusSeconds(1),  "INFO", "Cache HIT for key " + cacheKey + " — 3 items, skipping Order Service"),
            log(tenant, apps.get(SVC_PAYMENT), traceId, base.plusSeconds(3),  "INFO", "Payment authorised: " + currency(rng) + " for " + orderId),
            log(tenant, apps.get(SVC_NOTIFY),  traceId, base.plusSeconds(5),  "INFO", "Express checkout confirmation queued for " + orderId)
        );
    }

    // ── Pattern 6 ──────────────────────────────────────────────────────────────
    // Guest checkout — Auth Service skipped entirely
    // Gateway → Order → Payment → Notification
    private List<Log> guestCheckout(Tenant tenant, List<StatusApp> apps, String traceId,
                                    ZonedDateTime base, Random rng) {
        String orderId = orderId(rng);
        String sessionId = "guest-" + Long.toHexString(rng.nextLong() & 0xFFFFFFL);
        return List.of(
            log(tenant, apps.get(SVC_GATEWAY),  traceId, base,                 "INFO", "Incoming POST /api/guest/checkout — session=" + sessionId),
            log(tenant, apps.get(SVC_ORDER),    traceId, base.plusSeconds(2),  "INFO", "Guest order " + orderId + " created (no auth required)"),
            log(tenant, apps.get(SVC_INVENTORY),traceId, base.plusSeconds(3),  "INFO", "Stock reserved for guest order " + orderId),
            log(tenant, apps.get(SVC_PAYMENT),  traceId, base.plusSeconds(5),  "INFO", "Payment processed for guest order " + orderId),
            log(tenant, apps.get(SVC_NOTIFY),   traceId, base.plusSeconds(7),  "INFO", "Guest confirmation email sent to address provided in " + orderId)
        );
    }

    // ── Pattern 7 ──────────────────────────────────────────────────────────────
    // Fraud detected — stops after Fraud Detection, Order/Payment/Notify never called
    // Gateway → Auth → Fraud Detection → blocked
    private List<Log> fraudDetected(Tenant tenant, List<StatusApp> apps, String traceId,
                                    ZonedDateTime base, Random rng) {
        int userId = rng.nextInt(9000) + 1000;
        int riskScore = 75 + rng.nextInt(25);
        return List.of(
            log(tenant, apps.get(SVC_GATEWAY), traceId, base,                  "INFO",    "Incoming POST /api/orders from IP " + randomIp(rng) + " — user " + userId),
            log(tenant, apps.get(SVC_AUTH),    traceId, base.plusSeconds(1),   "INFO",    "JWT validated for user " + userId),
            log(tenant, apps.get(SVC_FRAUD),   traceId, base.plusSeconds(2),   "WARNING", "Evaluating risk for user " + userId + ": velocity=" + (rng.nextInt(20) + 5) + " orders/hr"),
            log(tenant, apps.get(SVC_FRAUD),   traceId, base.plusSeconds(3),   "ERROR",   "Risk score " + riskScore + "/100 exceeds threshold (70) for user " + userId + " — blocking order"),
            log(tenant, apps.get(SVC_GATEWAY), traceId, base.plusSeconds(4),   "WARNING", "Request rejected: 403 Fraud detected — Order, Payment, Notify skipped")
        );
    }

    // ── Pattern 8 ──────────────────────────────────────────────────────────────
    // Internal scheduled batch job — no API Gateway, no Auth Service
    // Order → Inventory → Payment → Notification
    private List<Log> scheduledBatchJob(Tenant tenant, List<StatusApp> apps, String traceId,
                                        ZonedDateTime base, Random rng) {
        int batchSize = 10 + rng.nextInt(90);
        String batchId = "BATCH-" + (rng.nextInt(9000) + 1000);
        return List.of(
            log(tenant, apps.get(SVC_ORDER),     traceId, base,                  "INFO", "Scheduled job started: processing " + batchSize + " pending orders — " + batchId),
            log(tenant, apps.get(SVC_INVENTORY), traceId, base.plusSeconds(2),   "INFO", "Inventory sync: " + batchSize + " reservations confirmed for " + batchId),
            log(tenant, apps.get(SVC_PAYMENT),   traceId, base.plusSeconds(5),   "INFO", "Batch payment run completed: " + batchSize + " transactions — " + batchId),
            log(tenant, apps.get(SVC_NOTIFY),    traceId, base.plusSeconds(7),   "INFO", "Batch digest sent: " + batchSize + " confirmation emails queued — " + batchId)
        );
    }

    // ── Pattern 9 ──────────────────────────────────────────────────────────────
    // Search-driven full order — includes Search Service at head of trace
    // Search → Gateway → Auth → Order → Inventory → Payment → Notification
    private List<Log> searchDrivenOrder(Tenant tenant, List<StatusApp> apps, String traceId,
                                        ZonedDateTime base, Random rng) {
        String orderId = orderId(rng);
        int userId = rng.nextInt(9000) + 1000;
        String[] queries = {"laptop", "headphones", "monitor", "keyboard", "webcam", "desk lamp", "usb hub"};
        String query = queries[rng.nextInt(queries.length)];
        int results = 5 + rng.nextInt(95);
        return List.of(
            log(tenant, apps.get(SVC_SEARCH),    traceId, base,                  "INFO", "Search query '" + query + "' — " + results + " results returned to user " + userId),
            log(tenant, apps.get(SVC_GATEWAY),   traceId, base.plusSeconds(4),   "INFO", "Incoming POST /api/orders — " + orderId + " (originated from search click)"),
            log(tenant, apps.get(SVC_AUTH),       traceId, base.plusSeconds(5),  "INFO", "JWT validated for user " + userId),
            log(tenant, apps.get(SVC_ORDER),      traceId, base.plusSeconds(7),  "INFO", "Order " + orderId + " created from search result — query='" + query + "'"),
            log(tenant, apps.get(SVC_INVENTORY),  traceId, base.plusSeconds(8),  "INFO", "Stock reserved for " + orderId),
            log(tenant, apps.get(SVC_PAYMENT),    traceId, base.plusSeconds(10), "INFO", "Payment authorised: " + currency(rng) + " for " + orderId),
            log(tenant, apps.get(SVC_NOTIFY),     traceId, base.plusSeconds(12), "INFO", "Order confirmation queued for " + orderId + " → user " + userId)
        );
    }

    // ── Pattern 10 ─────────────────────────────────────────────────────────────
    // Payment fails once, retries and succeeds — Notification sent after retry
    // Gateway → Auth → Order → Inventory → Payment(fail) → Payment(retry) → Notification
    private List<Log> paymentRetrySuccess(Tenant tenant, List<StatusApp> apps, String traceId,
                                          ZonedDateTime base, Random rng) {
        String orderId = orderId(rng);
        int userId = rng.nextInt(9000) + 1000;
        return List.of(
            log(tenant, apps.get(SVC_GATEWAY),   traceId, base,                   "INFO",    "Incoming POST /api/orders — " + orderId),
            log(tenant, apps.get(SVC_AUTH),       traceId, base.plusSeconds(1),   "INFO",    "JWT validated for user " + userId),
            log(tenant, apps.get(SVC_ORDER),      traceId, base.plusSeconds(3),   "INFO",    "Order " + orderId + " created and reserved"),
            log(tenant, apps.get(SVC_INVENTORY),  traceId, base.plusSeconds(4),   "INFO",    "Stock reserved for " + orderId),
            log(tenant, apps.get(SVC_PAYMENT),    traceId, base.plusSeconds(6),   "WARNING", "Payment attempt 1 failed for " + orderId + ": upstream network timeout (3100ms)"),
            log(tenant, apps.get(SVC_PAYMENT),    traceId, base.plusSeconds(12),  "INFO",    "Retrying payment for " + orderId + " (attempt 2 of 3)"),
            log(tenant, apps.get(SVC_PAYMENT),    traceId, base.plusSeconds(15),  "INFO",    "Payment authorised on retry: " + currency(rng) + " for " + orderId),
            log(tenant, apps.get(SVC_NOTIFY),     traceId, base.plusSeconds(17),  "INFO",    "Confirmation email queued for " + orderId + " (after payment retry)")
        );
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Log log(Tenant tenant, StatusApp app, String traceId, ZonedDateTime timestamp,
                    String level, String message) {
        Log log = new Log();
        log.setTenant(tenant);
        log.setService(app.getName());
        log.setTraceId(traceId);
        log.setLogTimestamp(timestamp);
        log.setLevel(level);
        log.setMessage(message);
        log.setRequestId(UUID.randomUUID().toString().substring(0, 8));
        return log;
    }

    private String orderId(Random rng) {
        return "ORD-" + (10000 + rng.nextInt(90000));
    }

    private String currency(Random rng) {
        int cents = 500 + rng.nextInt(29500);
        return "$" + (cents / 100) + "." + String.format("%02d", cents % 100);
    }

    private String randomIp(Random rng) {
        return rng.nextInt(255) + "." + rng.nextInt(255) + "." + rng.nextInt(255) + "." + rng.nextInt(255);
    }
}
