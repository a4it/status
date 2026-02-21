package org.automatize.status.services;

import org.automatize.status.models.LogApiKey;
import org.automatize.status.repositories.LogApiKeyRepository;
import org.automatize.status.repositories.TenantRepository;
import org.automatize.status.util.ApiKeyGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        LogApiKey key = new LogApiKey();
        if (tenantId != null) {
            tenantRepository.findById(tenantId).ifPresent(key::setTenant);
        }
        key.setName(name);
        key.setApiKey(ApiKeyGenerator.generateApiKey());
        key.setIsActive(true);
        return logApiKeyRepository.save(key);
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
