package org.automatize.status.services;

import org.automatize.status.exceptions.HashingException;
import org.automatize.status.models.LogApiKey;
import org.automatize.status.repositories.LogApiKeyRepository;
import org.automatize.status.repositories.TenantRepository;
import org.automatize.status.util.ApiKeyGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Service for managing API keys used to authenticate log ingestion.
 */
@Service
@Transactional
public class LogApiKeyService {

    @Autowired
    private LogApiKeyRepository logApiKeyRepository;

    @Autowired
    private TenantRepository tenantRepository;

    /**
     * Returns a page of log API keys ordered by creation date, newest first.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of {@link LogApiKey} entities
     */
    @Transactional(readOnly = true)
    public Page<LogApiKey> findAll(Pageable pageable) {
        return logApiKeyRepository.findAllByOrderByCreatedDateTechnicalDesc(pageable);
    }

    /**
     * Finds a log API key by its identifier.
     *
     * @param id the API key identifier
     * @return the matching {@link LogApiKey}
     * @throws RuntimeException if no key exists with the given id
     */
    @Transactional(readOnly = true)
    public LogApiKey findById(UUID id) {
        return logApiKeyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log API key not found: " + id));
    }

    /**
     * Creates and persists a new log API key. A raw key is generated and only
     * its SHA-256 hash and an 8-character display prefix are stored; the
     * plaintext key is returned once via a transient field for the caller to
     * surface in the creation response.
     *
     * @param tenantId optional tenant to associate the key with; may be null
     * @param name     a human-readable name for the key
     * @return the persisted {@link LogApiKey}, carrying the raw key once via
     *         its transient field
     */
    public LogApiKey create(UUID tenantId, String name) {
        String rawKey = ApiKeyGenerator.generateApiKey();
        LogApiKey key = new LogApiKey();
        // Associate the key with a tenant only when a tenant id was supplied
        if (tenantId != null) {
            tenantRepository.findById(tenantId).ifPresent(key::setTenant);
        }
        key.setName(name);
        // MED-03: store SHA-256 hash + 8-char display prefix; never store the plaintext key
        key.setKeyHash(sha256Hex(rawKey));
        key.setKeyPrefix(rawKey.substring(0, 8));
        key.setIsActive(true);
        LogApiKey saved = logApiKeyRepository.save(key);
        // Pass the raw key via transient field so the controller can include it
        // in the creation response exactly once.
        saved.setRawKeyOnceOnly(rawKey);
        return saved;
    }

    /**
     * Computes the lowercase hexadecimal SHA-256 hash of the given input.
     *
     * @param input the string to hash
     * @return the hex-encoded SHA-256 digest
     * @throws HashingException if the SHA-256 algorithm is unavailable
     */
    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new HashingException("SHA-256 not available", e);
        }
    }

    /**
     * Toggles the active state of the identified API key and persists the change.
     *
     * @param id the API key identifier
     * @return the updated {@link LogApiKey}
     * @throws RuntimeException if no key exists with the given id
     */
    public LogApiKey toggleActive(UUID id) {
        LogApiKey key = findById(id);
        key.setIsActive(!Boolean.TRUE.equals(key.getIsActive()));
        return logApiKeyRepository.save(key);
    }

    /**
     * Deletes the identified API key.
     *
     * @param id the API key identifier
     * @throws RuntimeException if no key exists with the given id
     */
    public void delete(UUID id) {
        logApiKeyRepository.delete(findById(id));
    }
}
