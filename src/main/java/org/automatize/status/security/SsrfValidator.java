package org.automatize.status.security;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Validates that a target hostname or IP address is not a private, loopback,
 * link-local, or multicast address before making an outbound connection.
 *
 * Used by HealthCheckService to prevent Server-Side Request Forgery (SSRF).
 */
public final class SsrfValidator {

    /**
     * Private constructor to prevent instantiation of this static utility class.
     */
    private SsrfValidator() {}

    /**
     * Validates that the hostname resolves to a publicly routable address.
     *
     * @param hostname the hostname or IP address to validate
     * @throws IllegalArgumentException if the host is private, loopback, link-local,
     *                                  multicast, or cannot be resolved
     */
    public static void validateHost(String hostname) {
        // Reject null or blank hostnames outright
        if (hostname == null || hostname.isBlank()) {
            throw new IllegalArgumentException("Host may not be blank");
        }
        InetAddress addr;
        try {
            addr = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot resolve host: " + hostname, e);
        }
        // Block loopback addresses (e.g. 127.0.0.1, ::1)
        if (addr.isLoopbackAddress()) {
            throw new IllegalArgumentException("Loopback addresses are not allowed");
        }
        // Block private/site-local addresses (e.g. 10.x, 172.16-31.x, 192.168.x)
        if (addr.isSiteLocalAddress()) {
            throw new IllegalArgumentException("Private/site-local addresses are not allowed");
        }
        // Block link-local addresses (e.g. 169.254.x.x)
        if (addr.isLinkLocalAddress()) {
            throw new IllegalArgumentException("Link-local addresses are not allowed (e.g. 169.254.x.x)");
        }
        // Block multicast addresses
        if (addr.isMulticastAddress()) {
            throw new IllegalArgumentException("Multicast addresses are not allowed");
        }
    }
}
