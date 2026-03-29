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

    private SsrfValidator() {}

    /**
     * Validates that the hostname resolves to a publicly routable address.
     *
     * @param hostname the hostname or IP address to validate
     * @throws IllegalArgumentException if the host is private, loopback, link-local,
     *                                  multicast, or cannot be resolved
     */
    public static void validateHost(String hostname) {
        if (hostname == null || hostname.isBlank()) {
            throw new IllegalArgumentException("Host may not be blank");
        }
        InetAddress addr;
        try {
            addr = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot resolve host: " + hostname, e);
        }
        if (addr.isLoopbackAddress()) {
            throw new IllegalArgumentException("Loopback addresses are not allowed");
        }
        if (addr.isSiteLocalAddress()) {
            throw new IllegalArgumentException("Private/site-local addresses are not allowed");
        }
        if (addr.isLinkLocalAddress()) {
            throw new IllegalArgumentException("Link-local addresses are not allowed (e.g. 169.254.x.x)");
        }
        if (addr.isMulticastAddress()) {
            throw new IllegalArgumentException("Multicast addresses are not allowed");
        }
    }
}
