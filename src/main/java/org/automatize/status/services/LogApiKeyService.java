package org.automatize.status.services;

import org.automatize.status.models.LogApiKey;
import org.automatize.status.repositories.LogApiKeyRepository;
import org.automatize.status.repositories.TenantRepository;
import org.automatize.status.util.ApiKeyGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
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

    @Transactional(readOnly = true)
    public List<LogApiKey> findAll() {
        return logApiKeyRepository.findAllByOrderByCreatedDateTechnicalDesc();
    }

    @Transactional(readOnly = true)
    public LogApiKey findById(UUID id) {
        return logApiKeyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log API key not found: " + id));
    }

    public LogApiKey create(UUID tenantId, String name) {
        String rawKey = ApiKeyGenerator.generateApiKey();
        LogApiKey key = new LogApiKey();
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

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public LogApiKey toggleActive(UUID id) {
        LogApiKey key = findById(id);
        key.setIsActive(!Boolean.TRUE.equals(key.getIsActive()));
        return logApiKeyRepository.save(key);
    }

    public void delete(UUID id) {
        logApiKeyRepository.delete(findById(id));
    }
}
