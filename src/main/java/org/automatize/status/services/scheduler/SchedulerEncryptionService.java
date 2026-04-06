package org.automatize.status.services.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive scheduler configuration values
 * (passwords, tokens, API keys) using AES-256-GCM.
 *
 * <p>The encryption key is loaded from the {@code scheduler.encryption.key} property
 * (Base64-encoded 32-byte value). A hardcoded development key is used when no
 * property is configured.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Service
public class SchedulerEncryptionService {

    /** GCM authentication tag length in bits. */
    private static final int GCM_TAG_LENGTH = 128;

    /** GCM initialisation vector length in bytes. */
    private static final int GCM_IV_LENGTH = 12;

    @Value("${scheduler.encryption.key:}")
    private String encryptionKeyBase64;

    private SecretKey getSecretKey() {
        if (encryptionKeyBase64 == null || encryptionKeyBase64.isBlank()) {
            // 32 bytes = 256-bit AES key — development only
            byte[] defaultKey = "status-scheduler-default-key-32b".getBytes();
            return new SecretKeySpec(defaultKey, "AES");
        }
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64);
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts a plaintext string using AES/GCM/NoPadding.
     * The result is Base64-encoded with the IV prepended to the ciphertext.
     *
     * @param plaintext the value to encrypt; {@code null} returns {@code null}
     * @return Base64-encoded {@code IV || ciphertext}
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            SecretKey key = getSecretKey();
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt value", e);
        }
    }

    /**
     * Decrypts a Base64-encoded {@code IV || ciphertext} string produced by {@link #encrypt}.
     *
     * @param ciphertext the encrypted value; {@code null} returns {@code null}
     * @return the original plaintext string
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted));
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt value", e);
        }
    }

    /**
     * Returns a masked placeholder if the value is non-null, or {@code null} otherwise.
     * Used when returning sensitive config to API callers.
     *
     * @param value the sensitive value to mask
     * @return {@code "••••••••"} when non-null, otherwise {@code null}
     */
    public String maskValue(String value) {
        return value != null ? "••••••••" : null;
    }
}
