package org.automatize.status.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for login attempts.
 * Allows up to MAX_ATTEMPTS per IP within a sliding WINDOW_MS window.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 60_000L; // 1 minute

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
}
