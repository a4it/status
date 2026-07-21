package org.automatize.status.services.scheduler;

import org.automatize.status.exceptions.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SchedulerEncryptionService}.
 *
 * <p>The AES-256 key is a deterministic Base64-encoded 32-byte value injected
 * into the {@code encryptionKeyBase64} {@code @Value} field via reflection.</p>
 */
@ExtendWith(MockitoExtension.class)
class SchedulerEncryptionServiceTest {

    /** Base64 encoding of a deterministic 32-byte (256-bit) AES key. */
    private static final String KEY_BASE64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private SchedulerEncryptionService service;

    @BeforeEach
    void setUp() {
        service = new SchedulerEncryptionService();
        ReflectionTestUtils.setField(service, "encryptionKeyBase64", KEY_BASE64);
    }

    // ---- encrypt / decrypt round-trip ---------------------------------

    @Test
    void encryptThenDecrypt_plaintext_returnsOriginal() {
        String plaintext = "super-secret-password";

        String encrypted = service.encrypt(plaintext);
        String decrypted = service.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encryptThenDecrypt_emptyString_returnsOriginal() {
        String encrypted = service.encrypt("");

        assertThat(service.decrypt(encrypted)).isEmpty();
    }

    @Test
    void encryptThenDecrypt_unicode_returnsOriginal() {
        String plaintext = "pä$$wörd-✓-码";

        assertThat(service.decrypt(service.encrypt(plaintext))).isEqualTo(plaintext);
    }

    @Test
    void encrypt_sameInputTwice_producesDifferentCiphertext() {
        // A random IV is generated per call, so ciphertexts must differ.
        String a = service.encrypt("repeat");
        String b = service.encrypt("repeat");

        assertThat(a).isNotEqualTo(b);
        assertThat(service.decrypt(a)).isEqualTo("repeat");
        assertThat(service.decrypt(b)).isEqualTo("repeat");
    }

    @Test
    void encrypt_producesValidBase64() {
        String encrypted = service.encrypt("value");

        // Must be decodable Base64 (throws if not).
        assertThat(java.util.Base64.getDecoder().decode(encrypted)).isNotEmpty();
    }

    // ---- null handling -------------------------------------------------

    @Test
    void encrypt_null_returnsNull() {
        assertThat(service.encrypt(null)).isNull();
    }

    @Test
    void decrypt_null_returnsNull() {
        assertThat(service.decrypt(null)).isNull();
    }

    // ---- default key (no property configured) -------------------------

    @Test
    void encryptThenDecrypt_withDefaultKey_returnsOriginal() {
        SchedulerEncryptionService defaultKeyService = new SchedulerEncryptionService();
        ReflectionTestUtils.setField(defaultKeyService, "encryptionKeyBase64", "");

        String encrypted = defaultKeyService.encrypt("dev-secret");

        assertThat(defaultKeyService.decrypt(encrypted)).isEqualTo("dev-secret");
    }

    @Test
    void encryptThenDecrypt_withNullKeyProperty_usesDefaultKey() {
        SchedulerEncryptionService defaultKeyService = new SchedulerEncryptionService();
        ReflectionTestUtils.setField(defaultKeyService, "encryptionKeyBase64", null);

        String encrypted = defaultKeyService.encrypt("dev-secret");

        assertThat(defaultKeyService.decrypt(encrypted)).isEqualTo("dev-secret");
    }

    @Test
    void decrypt_valueEncryptedWithDifferentKey_throwsEncryptionException() {
        SchedulerEncryptionService other = new SchedulerEncryptionService();
        ReflectionTestUtils.setField(other, "encryptionKeyBase64", "");
        String encryptedWithDefaultKey = other.encrypt("secret");

        // Decrypting with the configured (different) key fails GCM authentication.
        assertThatThrownBy(() -> service.decrypt(encryptedWithDefaultKey))
                .isInstanceOf(EncryptionException.class);
    }

    // ---- decrypt failure paths ----------------------------------------

    @Test
    void decrypt_garbageBase64_throwsEncryptionException() {
        assertThatThrownBy(() -> service.decrypt("!!!not-base64!!!"))
                .isInstanceOf(EncryptionException.class);
    }

    @Test
    void decrypt_tamperedCiphertext_throwsEncryptionException() {
        String encrypted = service.encrypt("payload");
        byte[] raw = java.util.Base64.getDecoder().decode(encrypted);
        // Flip a bit in the ciphertext body to break GCM authentication.
        raw[raw.length - 1] ^= 0x01;
        String tampered = java.util.Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> service.decrypt(tampered))
                .isInstanceOf(EncryptionException.class);
    }

    @Test
    void decrypt_tooShortForIv_throwsEncryptionException() {
        // Fewer bytes than the 12-byte IV length triggers a negative array size.
        String tooShort = java.util.Base64.getEncoder().encodeToString(new byte[4]);

        assertThatThrownBy(() -> service.decrypt(tooShort))
                .isInstanceOf(EncryptionException.class);
    }

    // ---- maskValue -----------------------------------------------------

    @Test
    void maskValue_nonNull_returnsMask() {
        assertThat(service.maskValue("secret")).isEqualTo("••••••••");
    }

    @Test
    void maskValue_null_returnsNull() {
        assertThat(service.maskValue(null)).isNull();
    }

    @Test
    void maskValue_emptyString_returnsMask() {
        // Empty string is non-null, so it is masked.
        assertThat(service.maskValue("")).isEqualTo("••••••••");
    }
}
