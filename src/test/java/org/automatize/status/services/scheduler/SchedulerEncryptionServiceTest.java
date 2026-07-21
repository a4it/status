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

    /** Name of the {@code @Value} field holding the Base64 AES key. */
    private static final String ENCRYPTION_KEY_FIELD = "encryptionKeyBase64";

    /** Plaintext reused to assert IV randomness across encryptions. */
    private static final String REPEAT_PLAINTEXT = "repeat";

    /** Plaintext used to exercise the built-in default-key path. */
    private static final String DEV_SECRET = "dev-secret";

    private SchedulerEncryptionService service;

    /**
     * Creates a fresh service and injects the deterministic Base64 AES key via reflection before each test.
     */
    @BeforeEach
    void setUp() {
        service = new SchedulerEncryptionService();
        ReflectionTestUtils.setField(service, ENCRYPTION_KEY_FIELD, KEY_BASE64);
    }

    // ---- encrypt / decrypt round-trip ---------------------------------

    /**
     * Verifies a plaintext survives an encrypt-then-decrypt round-trip.
     * Expected outcome: the decrypted value equals the original plaintext.
     */
    @Test
    void encryptThenDecrypt_plaintext_returnsOriginal() {
        String plaintext = "super-secret-password";

        String encrypted = service.encrypt(plaintext);
        String decrypted = service.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    /**
     * Verifies an empty string survives the encrypt-then-decrypt round-trip.
     * Expected outcome: the decrypted value is empty.
     */
    @Test
    void encryptThenDecrypt_emptyString_returnsOriginal() {
        String encrypted = service.encrypt("");

        assertThat(service.decrypt(encrypted)).isEmpty();
    }

    /**
     * Verifies a Unicode string survives the encrypt-then-decrypt round-trip.
     * Expected outcome: the decrypted value equals the original Unicode plaintext.
     */
    @Test
    void encryptThenDecrypt_unicode_returnsOriginal() {
        String plaintext = "pä$$wörd-✓-码";

        assertThat(service.decrypt(service.encrypt(plaintext))).isEqualTo(plaintext);
    }

    /**
     * Verifies encrypting the same input twice yields different ciphertexts due to a random IV.
     * Expected outcome: the two ciphertexts differ but both decrypt to the original.
     */
    @Test
    void encrypt_sameInputTwice_producesDifferentCiphertext() {
        // A random IV is generated per call, so ciphertexts must differ.
        String a = service.encrypt(REPEAT_PLAINTEXT);
        String b = service.encrypt(REPEAT_PLAINTEXT);

        assertThat(a).isNotEqualTo(b);
        assertThat(service.decrypt(a)).isEqualTo(REPEAT_PLAINTEXT);
        assertThat(service.decrypt(b)).isEqualTo(REPEAT_PLAINTEXT);
    }

    /**
     * Verifies the ciphertext output is valid Base64.
     * Expected outcome: the output decodes to a non-empty byte array.
     */
    @Test
    void encrypt_producesValidBase64() {
        String encrypted = service.encrypt("value");

        // Must be decodable Base64 (throws if not).
        assertThat(java.util.Base64.getDecoder().decode(encrypted)).isNotEmpty();
    }

    // ---- null handling -------------------------------------------------

    /**
     * Verifies encrypting {@code null} returns {@code null}.
     * Expected outcome: {@code encrypt(null)} is {@code null}.
     */
    @Test
    void encrypt_null_returnsNull() {
        assertThat(service.encrypt(null)).isNull();
    }

    /**
     * Verifies decrypting {@code null} returns {@code null}.
     * Expected outcome: {@code decrypt(null)} is {@code null}.
     */
    @Test
    void decrypt_null_returnsNull() {
        assertThat(service.decrypt(null)).isNull();
    }

    // ---- default key (no property configured) -------------------------

    /**
     * Verifies the round-trip works with the built-in default key (empty key property).
     * Expected outcome: the decrypted value equals the original.
     */
    @Test
    void encryptThenDecrypt_withDefaultKey_returnsOriginal() {
        SchedulerEncryptionService defaultKeyService = new SchedulerEncryptionService();
        ReflectionTestUtils.setField(defaultKeyService, ENCRYPTION_KEY_FIELD, "");

        String encrypted = defaultKeyService.encrypt(DEV_SECRET);

        assertThat(defaultKeyService.decrypt(encrypted)).isEqualTo(DEV_SECRET);
    }

    /**
     * Verifies a {@code null} key property falls back to the default key for the round-trip.
     * Expected outcome: the decrypted value equals the original.
     */
    @Test
    void encryptThenDecrypt_withNullKeyProperty_usesDefaultKey() {
        SchedulerEncryptionService defaultKeyService = new SchedulerEncryptionService();
        ReflectionTestUtils.setField(defaultKeyService, ENCRYPTION_KEY_FIELD, null);

        String encrypted = defaultKeyService.encrypt(DEV_SECRET);

        assertThat(defaultKeyService.decrypt(encrypted)).isEqualTo(DEV_SECRET);
    }

    /**
     * Verifies decrypting a value encrypted under a different key fails GCM authentication.
     * Expected outcome: an {@link EncryptionException} is thrown.
     */
    @Test
    void decrypt_valueEncryptedWithDifferentKey_throwsEncryptionException() {
        SchedulerEncryptionService other = new SchedulerEncryptionService();
        ReflectionTestUtils.setField(other, ENCRYPTION_KEY_FIELD, "");
        String encryptedWithDefaultKey = other.encrypt("secret");

        // Decrypting with the configured (different) key fails GCM authentication.
        assertThatThrownBy(() -> service.decrypt(encryptedWithDefaultKey))
                .isInstanceOf(EncryptionException.class);
    }

    // ---- decrypt failure paths ----------------------------------------

    /**
     * Verifies decrypting non-Base64 garbage input fails.
     * Expected outcome: an {@link EncryptionException} is thrown.
     */
    @Test
    void decrypt_garbageBase64_throwsEncryptionException() {
        assertThatThrownBy(() -> service.decrypt("!!!not-base64!!!"))
                .isInstanceOf(EncryptionException.class);
    }

    /**
     * Verifies a single flipped bit in the ciphertext breaks GCM authentication.
     * Expected outcome: an {@link EncryptionException} is thrown.
     */
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

    /**
     * Verifies input shorter than the 12-byte IV length is rejected.
     * Expected outcome: an {@link EncryptionException} is thrown.
     */
    @Test
    void decrypt_tooShortForIv_throwsEncryptionException() {
        // Fewer bytes than the 12-byte IV length triggers a negative array size.
        String tooShort = java.util.Base64.getEncoder().encodeToString(new byte[4]);

        assertThatThrownBy(() -> service.decrypt(tooShort))
                .isInstanceOf(EncryptionException.class);
    }

    // ---- maskValue -----------------------------------------------------

    /**
     * Verifies a non-null value is replaced by the fixed mask string.
     * Expected outcome: {@code maskValue} returns the bullet mask.
     */
    @Test
    void maskValue_nonNull_returnsMask() {
        assertThat(service.maskValue("secret")).isEqualTo("••••••••");
    }

    /**
     * Verifies masking {@code null} returns {@code null}.
     * Expected outcome: {@code maskValue(null)} is {@code null}.
     */
    @Test
    void maskValue_null_returnsNull() {
        assertThat(service.maskValue(null)).isNull();
    }

    /**
     * Verifies an empty (but non-null) string is masked.
     * Expected outcome: {@code maskValue} returns the bullet mask.
     */
    @Test
    void maskValue_emptyString_returnsMask() {
        // Empty string is non-null, so it is masked.
        assertThat(service.maskValue("")).isEqualTo("••••••••");
    }
}
