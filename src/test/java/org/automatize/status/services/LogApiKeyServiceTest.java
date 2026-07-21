package org.automatize.status.services;

import org.automatize.status.models.LogApiKey;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.LogApiKeyRepository;
import org.automatize.status.repositories.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LogApiKeyService} — key generation, SHA-256 hashing,
 * prefix display, activation toggling and CRUD lookups.
 */
@ExtendWith(MockitoExtension.class)
class LogApiKeyServiceTest {

    @Mock
    private LogApiKeyRepository logApiKeyRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private LogApiKeyService service;

    // ── findAll / findById ────────────────────────────────────────────────────

    @Test
    void findAll_returnsRepositoryOrderedList() {
        LogApiKey k = new LogApiKey();
        when(logApiKeyRepository.findAllByOrderByCreatedDateTechnicalDesc()).thenReturn(List.of(k));

        assertThat(service.findAll()).containsExactly(k);
    }

    @Test
    void findById_present_returnsKey() {
        UUID id = UUID.randomUUID();
        LogApiKey k = new LogApiKey();
        when(logApiKeyRepository.findById(id)).thenReturn(Optional.of(k));

        assertThat(service.findById(id)).isSameAs(k);
    }

    @Test
    void findById_missing_throwsRuntimeException() {
        UUID id = UUID.randomUUID();
        when(logApiKeyRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Log API key not found");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_noTenant_generatesHashedKeyAndPrefixAndExposesRawOnce() {
        when(logApiKeyRepository.save(any(LogApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        LogApiKey result = service.create(null, "ci-key");

        assertThat(result.getName()).isEqualTo("ci-key");
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getKeyHash()).isNotNull().hasSize(64);           // SHA-256 hex
        assertThat(result.getKeyPrefix()).isNotNull().hasSize(8);
        assertThat(result.getRawKeyOnceOnly()).isNotBlank();
        // prefix is the first 8 chars of the raw key
        assertThat(result.getRawKeyOnceOnly()).startsWith(result.getKeyPrefix());
        // hash is not the raw key
        assertThat(result.getKeyHash()).isNotEqualTo(result.getRawKeyOnceOnly());
        assertThat(result.getTenant()).isNull();
        verify(tenantRepository, never()).findById(any());
    }

    @Test
    void create_withTenant_attachesResolvedTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(logApiKeyRepository.save(any(LogApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        LogApiKey result = service.create(tenantId, "scoped");

        assertThat(result.getTenant()).isSameAs(tenant);
    }

    @Test
    void create_generatesUniqueKeysAcrossInvocations() {
        when(logApiKeyRepository.save(any(LogApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        LogApiKey a = service.create(null, "a");
        LogApiKey b = service.create(null, "b");

        assertThat(a.getKeyHash()).isNotEqualTo(b.getKeyHash());
        assertThat(a.getRawKeyOnceOnly()).isNotEqualTo(b.getRawKeyOnceOnly());
    }

    // ── toggleActive ──────────────────────────────────────────────────────────

    @Test
    void toggleActive_activeKey_becomesInactive() {
        UUID id = UUID.randomUUID();
        LogApiKey k = new LogApiKey();
        k.setIsActive(true);
        when(logApiKeyRepository.findById(id)).thenReturn(Optional.of(k));
        when(logApiKeyRepository.save(any(LogApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        LogApiKey result = service.toggleActive(id);

        assertThat(result.getIsActive()).isFalse();
    }

    @Test
    void toggleActive_nullActive_becomesTrue() {
        UUID id = UUID.randomUUID();
        LogApiKey k = new LogApiKey();
        k.setIsActive(null);
        when(logApiKeyRepository.findById(id)).thenReturn(Optional.of(k));
        when(logApiKeyRepository.save(any(LogApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        LogApiKey result = service.toggleActive(id);

        assertThat(result.getIsActive()).isTrue();
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_existingKey_delegatesToRepository() {
        UUID id = UUID.randomUUID();
        LogApiKey k = new LogApiKey();
        when(logApiKeyRepository.findById(id)).thenReturn(Optional.of(k));

        service.delete(id);

        verify(logApiKeyRepository).delete(k);
    }

    @Test
    void delete_missingKey_throwsRuntimeException() {
        UUID id = UUID.randomUUID();
        when(logApiKeyRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(RuntimeException.class);
        verify(logApiKeyRepository, never()).delete(any());
    }
}
