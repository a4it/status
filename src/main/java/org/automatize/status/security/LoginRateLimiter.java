package org.automatize.status.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for login attempts.
 * Allows up to MAX_ATTEMPTS per IP within a sliding WINDOW_MS window.
 *
 * <p>The per-IP map is bounded in two ways so that an unauthenticated scan of the login
 * endpoint cannot grow it without limit: a scheduled sweep removes buckets whose window
 * has elapsed, and a hard cap sheds entries inline when a burst outruns that sweep.</p>
 */
@Component
public class LoginRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(LoginRateLimiter.class);

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 60_000L; // 1 minute

    /** Interval of the background sweep that removes buckets with an elapsed window. */
    private static final long EVICTION_INTERVAL_MS = 300_000L; // 5 minutes

    /** Maximum number of client IPs tracked at once before entries are shed inline. */
    private static final int MAX_TRACKED_IPS = 10_000;

    /**
     * Holds the attempt count and the start time of the current sliding window for a single client IP.
     *
     * @param count       the number of attempts recorded in the current window
     * @param windowStart the epoch milliseconds timestamp marking the start of the window
     */
    private record Bucket(AtomicInteger count, long windowStart) {}

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Returns true if the IP is within its allowed attempt budget, false if it should be blocked.
     */
    public boolean isAllowed(String clientIp) {
        long now = System.currentTimeMillis();
        enforceCapacity(clientIp, now);
        Bucket bucket = buckets.compute(clientIp, (ip, existing) -> {
            // No existing bucket or the current window has elapsed: start a fresh window
            if (existing == null || (now - existing.windowStart()) >= WINDOW_MS) {
                return new Bucket(new AtomicInteger(1), now);
            }
            existing.count().incrementAndGet();
            return existing;
        });
        return bucket.count().get() <= MAX_ATTEMPTS;
    }

    /**
     * Removes buckets whose window has elapsed. Runs on a fixed delay so that IPs which
     * never return are not retained for the lifetime of the process.
     */
    @Scheduled(fixedDelay = EVICTION_INTERVAL_MS)
    void evictExpired() {
        evictExpired(System.currentTimeMillis());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Keeps the tracked-IP map under {@link #MAX_TRACKED_IPS}. Only a new IP can grow the
     * map, so already-tracked IPs skip this entirely and never lose their running count to
     * an unrelated burst.
     *
     * @param clientIp the IP about to be recorded
     * @param now      the current epoch milliseconds
     */
    private void enforceCapacity(String clientIp, long now) {
        // Below the cap, or this IP is already tracked — the map cannot grow
        if (buckets.size() < MAX_TRACKED_IPS || buckets.containsKey(clientIp)) {
            return;
        }
        evictExpired(now);
        // Eviction freed nothing: this many distinct IPs are inside one window, so drop
        // everything rather than let the map keep growing
        if (buckets.size() >= MAX_TRACKED_IPS) {
            logger.warn("Login rate limiter reached {} tracked IPs within one window; clearing all buckets",
                    MAX_TRACKED_IPS);
            buckets.clear();
        }
    }

    /**
     * Removes every bucket whose window started before the current window.
     *
     * @param now the current epoch milliseconds
     */
    private void evictExpired(long now) {
        long cutoff = now - WINDOW_MS;
        buckets.entrySet().removeIf(entry -> entry.getValue().windowStart() < cutoff);
    }
}
