package org.automatize.status.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SsrfValidator}.
 *
 * <p>All test hosts are IP literals (no DNS lookup) except the unresolvable-host case,
 * which uses the reserved {@code .invalid} TLD guaranteed never to resolve.</p>
 */
class SsrfValidatorTest {

    @Test
    void validateHost_null_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> SsrfValidator.validateHost(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void validateHost_blank_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> SsrfValidator.validateHost("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void validateHost_unresolvableHost_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> SsrfValidator.validateHost("no-such-host.invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot resolve host");
    }

    @Test
    void validateHost_loopback_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> SsrfValidator.validateHost("127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Loopback");
    }

    @Test
    void validateHost_privateClassA_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> SsrfValidator.validateHost("10.0.0.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Private/site-local");
    }

    @Test
    void validateHost_privateClassC_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> SsrfValidator.validateHost("192.168.1.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Private/site-local");
    }

    @Test
    void validateHost_linkLocal_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> SsrfValidator.validateHost("169.254.1.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Link-local");
    }

    @Test
    void validateHost_multicast_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> SsrfValidator.validateHost("224.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Multicast");
    }

    @Test
    void validateHost_publicIp_doesNotThrow() {
        assertThatCode(() -> SsrfValidator.validateHost("8.8.8.8"))
                .doesNotThrowAnyException();
    }
}
