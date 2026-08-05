package org.automatize.status.services.scheduler;

import jakarta.annotation.PreDestroy;
import org.automatize.status.exceptions.SchedulerExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches {@link HttpClient} instances shared by the REST and SOAP scheduler executors.
 *
 * <p>Each {@code HttpClient} owns a selector thread and an executor, so building one per
 * job execution leaks those resources until garbage collection. Clients are therefore
 * cached by the settings that actually shape them — connect timeout, redirect policy and
 * whether certificate verification is enabled — and reused across executions. Per-request
 * read timeouts stay on the {@link java.net.http.HttpRequest} and do not require a
 * distinct client.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Component
public class SchedulerHttpClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerHttpClientFactory.class);

    /**
     * Upper bound on distinct cached clients. Keys come from admin-defined job settings, so
     * this is only a guard against an unexpectedly wide spread of configurations.
     */
    private static final int MAX_CACHED_CLIENTS = 64;

    /**
     * Identifies a client configuration. Two jobs sharing these settings share one client.
     *
     * @param connectTimeoutMs the connect timeout in milliseconds
     * @param followRedirects  whether redirects are followed
     * @param sslVerify        whether server certificates are verified
     */
    private record ClientKey(long connectTimeoutMs, boolean followRedirects, boolean sslVerify) {}

    private final ConcurrentHashMap<ClientKey, HttpClient> clients = new ConcurrentHashMap<>();

    /** Lazily built trust-all context, shared by every client that disables verification. */
    private volatile SSLContext trustAllSslContext;

    /**
     * Returns a cached client for the given settings, building one on first use.
     *
     * @param connectTimeoutMs the connect timeout in milliseconds
     * @param followRedirects  whether redirects should be followed
     * @param sslVerify        whether server certificates should be verified
     * @return a reusable HTTP client for those settings
     */
    public HttpClient getClient(long connectTimeoutMs, boolean followRedirects, boolean sslVerify) {
        ClientKey key = new ClientKey(connectTimeoutMs, followRedirects, sslVerify);
        HttpClient cached = clients.get(key);
        // Fast path: an existing client already matches this configuration
        if (cached != null) {
            return cached;
        }
        // Guard against an unbounded spread of configurations before adding a new entry
        if (clients.size() >= MAX_CACHED_CLIENTS) {
            evictAll();
        }
        return clients.computeIfAbsent(key, this::buildClient);
    }

    /**
     * Closes every cached client on shutdown so their selector threads and executors are
     * released deterministically.
     */
    @PreDestroy
    void shutdown() {
        evictAll();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a client for the given key, installing a trust-all SSL context when
     * certificate verification is disabled.
     *
     * @param key the configuration to build for
     * @return the newly built client
     */
    private HttpClient buildClient(ClientKey key) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(key.connectTimeoutMs()))
                .followRedirects(key.followRedirects()
                        ? HttpClient.Redirect.NORMAL
                        : HttpClient.Redirect.NEVER);
        // SSL verification disabled — install a trust-all context
        if (!key.sslVerify()) {
            builder.sslContext(trustAllSslContext());
        }
        return builder.build();
    }

    /** Closes and removes every cached client. */
    private void evictAll() {
        clients.values().removeIf(client -> {
            closeQuietly(client);
            return true;
        });
    }

    /**
     * Closes a client, logging rather than propagating any failure so shutdown and eviction
     * are never derailed by a single client.
     *
     * @param client the client to close
     */
    private void closeQuietly(HttpClient client) {
        try {
            client.close();
        } catch (Exception e) {
            logger.warn("Failed to close scheduler HTTP client", e);
        }
    }

    /**
     * Returns the shared trust-all {@link SSLContext}, building it on first use.
     * Intended only for development/self-signed environments.
     *
     * @return a trust-all SSL context
     * @throws SchedulerExecutionException when the context cannot be constructed
     */
    private SSLContext trustAllSslContext() {
        SSLContext context = trustAllSslContext;
        // Already built — reuse it rather than attaching a fresh object graph per client
        if (context != null) {
            return context;
        }
        synchronized (this) {
            // Re-check inside the lock: another thread may have built it meanwhile
            if (trustAllSslContext == null) {
                trustAllSslContext = buildTrustAllSslContext();
            }
            return trustAllSslContext;
        }
    }

    /**
     * Builds an {@link SSLContext} whose trust manager accepts all certificates.
     *
     * @return a trust-all SSL context
     * @throws SchedulerExecutionException when the context cannot be constructed
     */
    private SSLContext buildTrustAllSslContext() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
            }}, null);
            return ctx;
        } catch (Exception e) {
            throw new SchedulerExecutionException("Failed to build trust-all SSL context", e);
        }
    }
}
