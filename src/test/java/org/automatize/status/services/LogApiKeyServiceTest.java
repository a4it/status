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
 *
 * <p>Testing approach: the service is exercised in isolation using Mockito. The
 * {@link LogApiKeyRepository} and {@link TenantRepository} collaborators are
 * {@code @Mock}s injected via {@code @InjectMocks}, and repository {@code save}
 * calls are stubbed to echo their argument so the service's in-memory mutations
 * can be asserted directly.</p>
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

    /**
     * Verifies that {@link LogApiKeyService#findAll()} returns exactly the list
     * supplied by the repository's ordered-by-creation query.
     */
    @Test
    void findAll_returnsRepositoryOrderedList() {
        LogApiKey k = new LogApiKey();
        when(logApiKeyRepository.findAllByOrderByCreatedDateTechnicalDesc()).thenReturn(List.of(k));

        assertThat(service.findAll()).containsExactly(k);
    }

    /**
     * Verifies that {@link LogApiKeyService#findById(UUID)} returns the key when the
     * repository finds a matching record.
     */
    @Test
    void findById_present_returnsKey() {
        UUID id = UUID.randomUUID();
        LogApiKey k = new LogApiKey();
        when(logApiKeyRepository.findById(id)).thenReturn(Optional.of(k));

        assertThat(service.findById(id)).isSameAs(k);
    }

    /**
     * Verifies that {@link LogApiKeyService#findById(UUID)} throws a
     * {@link RuntimeException} mentioning "Log API key not found" when no record exists.
     */
    @Test
    void findById_missing_throwsRuntimeException() {
        UUID id = UUID.randomUUID();
        when(logApiKeyRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Log API key not found");
    }

    // ── create ────────────────────────────────────────────────────────────────

    /**
     * Verifies that creating a key without a tenant generates an active key with a
     * 64-char SHA-256 hash, an 8-char prefix matching the start of the raw key, exposes
     * the raw key once, and never consults the tenant repository.
     */
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

    /**
     * Verifies that creating a key with a tenant id resolves the tenant via the
     * repository and attaches it to the new key.
     */
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

    /**
     * Verifies that successive create calls generate distinct raw keys and distinct
     * hashes.
     */
    @Test
    void create_generatesUniqueKeysAcrossInvocations() {
        when(logApiKeyRepository.save(any(LogApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        LogApiKey a = service.create(null, "a");
        LogApiKey b = service.create(null, "b");

        assertThat(a.getKeyHash()).isNotEqualTo(b.getKeyHash());
        assertThat(a.getRawKeyOnceOnly()).isNotEqualTo(b.getRawKeyOnceOnly());
    }

    // ── toggleActive ──────────────────────────────────────────────────────────

    /**
     * Verifies that toggling an active key flips it to inactive.
     */
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

    /**
     * Verifies that toggling a key whose active flag is null treats it as inactive and
     * sets it to active.
     */
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

    /**
     * Verifies that deleting an existing key looks it up and delegates removal to the
     * repository.
     */
    @Test
    void delete_existingKey_delegatesToRepository() {
        UUID id = UUID.randomUUID();
        LogApiKey k = new LogApiKey();
        when(logApiKeyRepository.findById(id)).thenReturn(Optional.of(k));

        service.delete(id);

        verify(logApiKeyRepository).delete(k);
    }

    /**
     * Verifies that deleting a missing key throws a {@link RuntimeException} and never
     * calls the repository's delete method.
     */
    @Test
    void delete_missingKey_throwsRuntimeException() {
        UUID id = UUID.randomUUID();
        when(logApiKeyRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(RuntimeException.class);
        verify(logApiKeyRepository, never()).delete(any());
    }
}
