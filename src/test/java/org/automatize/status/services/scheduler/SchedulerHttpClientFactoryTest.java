package org.automatize.status.services.scheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SchedulerHttpClientFactory}.
 *
 * <p>The factory exists so scheduler executions reuse clients instead of building (and
 * leaking) one per run, so these tests focus on cache identity, the settings that make two
 * configurations distinct, and deterministic shutdown.</p>
 */
class SchedulerHttpClientFactoryTest {

    private SchedulerHttpClientFactory factory;

    /** Creates a fresh factory so cached clients do not carry over between tests. */
    @BeforeEach
    void setUp() {
        factory = new SchedulerHttpClientFactory();
    }

    /** Closes any clients a test left behind. */
    @AfterEach
    void tearDown() {
        factory.shutdown();
    }

    /**
     * Verifies the core fix: repeated executions of the same job configuration reuse one
     * client rather than allocating a new selector thread each time.
     */
    @Test
    void getClient_sameSettings_returnsCachedInstance() {
        // Act
        HttpClient first = factory.getClient(5000, true, true);
        HttpClient second = factory.getClient(5000, true, true);

        // Assert
        assertThat(second).isSameAs(first);
        assertThat(clients()).hasSize(1);
    }

    /**
     * Verifies that each setting that shapes a client — connect timeout, redirect policy
     * and certificate verification — yields a distinct cached instance.
     */
    @Test
    void getClient_differingSettings_returnsDistinctInstances() {
        // Act
        HttpClient base = factory.getClient(5000, true, true);
        HttpClient otherTimeout = factory.getClient(9000, true, true);
        HttpClient noRedirects = factory.getClient(5000, false, true);
        HttpClient noSslVerify = factory.getClient(5000, true, false);

        // Assert
        assertThat(otherTimeout).isNotSameAs(base);
        assertThat(noRedirects).isNotSameAs(base);
        assertThat(noSslVerify).isNotSameAs(base);
        assertThat(clients()).hasSize(4);
    }

    /**
     * Verifies that the requested connect timeout and redirect policy actually reach the
     * built client, so caching did not silently drop job configuration.
     */
    @Test
    void getClient_appliesRequestedSettings() {
        // Act
        HttpClient client = factory.getClient(7500, false, true);

        // Assert
        assertThat(client.connectTimeout()).contains(Duration.ofMillis(7500));
        assertThat(client.followRedirects()).isEqualTo(HttpClient.Redirect.NEVER);
    }

    /**
     * Verifies that disabling certificate verification produces a usable client with a
     * trust-all SSL context installed, and that the context is shared rather than rebuilt
     * per client.
     */
    @Test
    void getClient_withSslVerifyDisabled_sharesTrustAllContext() {
        // Act
        HttpClient first = factory.getClient(5000, true, false);
        HttpClient second = factory.getClient(9000, true, false);

        // Assert
        assertThat(first.sslContext()).isNotNull();
        assertThat(second.sslContext()).isSameAs(first.sslContext());
    }

    /**
     * Verifies that shutdown drops every cached client, releasing their selector threads
     * deterministically instead of waiting on garbage collection.
     */
    @Test
    void shutdown_closesAndClearsCachedClients() {
        // Arrange
        factory.getClient(5000, true, true);
        factory.getClient(9000, false, true);
        assertThat(clients()).hasSize(2);

        // Act
        factory.shutdown();

        // Assert
        assertThat(clients()).isEmpty();
    }

    /**
     * Verifies the guard against an unbounded spread of configurations: once the cache is
     * full it is shed rather than grown past its cap.
     */
    @Test
    void getClient_beyondCacheCap_shedsCachedClients() {
        // Arrange: fill the cache to its 64-entry cap with distinct timeouts
        for (int i = 0; i < 64; i++) {
            factory.getClient(1000 + i, true, true);
        }
        assertThat(clients()).hasSize(64);

        // Act: a 65th distinct configuration
        HttpClient overflow = factory.getClient(99_000, true, true);

        // Assert
        assertThat(overflow).isNotNull();
        assertThat(clients()).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // Test helpers
    // -------------------------------------------------------------------------

    /** Returns the factory's internal client cache. */
    @SuppressWarnings("unchecked")
    private Map<Object, HttpClient> clients() {
        return (Map<Object, HttpClient>) ReflectionTestUtils.getField(factory, "clients");
    }
}
