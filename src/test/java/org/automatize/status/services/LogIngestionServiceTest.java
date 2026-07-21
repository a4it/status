package org.automatize.status.services;

import org.automatize.status.models.DropRule;
import org.automatize.status.models.Log;
import org.automatize.status.models.LogApiKey;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.DropRuleRepository;
import org.automatize.status.repositories.LogApiKeyRepository;
import org.automatize.status.repositories.LogRepository;
import org.automatize.status.repositories.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LogIngestionService}. Also exercises the drop-rule
 * matching logic (private helpers) through the public ingest / ingestBatch methods.
 */
@ExtendWith(MockitoExtension.class)
class LogIngestionServiceTest {

    @Mock
    private LogRepository logRepository;

    @Mock
    private LogApiKeyRepository logApiKeyRepository;

    @Mock
    private DropRuleRepository dropRuleRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private LogIngestionService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "retentionDays", 30);
    }

    private DropRule rule(String level, String svc, String messagePattern) {
        DropRule r = new DropRule();
        r.setLevel(level);
        r.setService(svc);
        r.setMessagePattern(messagePattern);
        r.setIsActive(true);
        return r;
    }

    // ── validateApiKey ────────────────────────────────────────────────────────

    @Test
    void validateApiKey_activeKeyExists_returnsKey() {
        LogApiKey key = new LogApiKey();
        key.setName("ci");
        when(logApiKeyRepository.findByKeyHashAndIsActiveTrue(anyString()))
                .thenReturn(Optional.of(key));

        LogApiKey result = service.validateApiKey("raw-key-value");

        assertThat(result).isSameAs(key);
    }

    @Test
    void validateApiKey_noMatch_throwsRuntimeException() {
        when(logApiKeyRepository.findByKeyHashAndIsActiveTrue(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateApiKey("bad"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid or inactive");
    }

    @Test
    void validateApiKey_hashesInputConsistently_sameKeyProducesSameLookup() {
        when(logApiKeyRepository.findByKeyHashAndIsActiveTrue(anyString()))
                .thenReturn(Optional.of(new LogApiKey()));
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);

        service.validateApiKey("abc");
        service.validateApiKey("abc");

        verify(logApiKeyRepository, org.mockito.Mockito.times(2))
                .findByKeyHashAndIsActiveTrue(hashCaptor.capture());
        // Deterministic SHA-256 hex: 64 chars, identical for identical input
        assertThat(hashCaptor.getAllValues().get(0))
                .hasSize(64)
                .isEqualTo(hashCaptor.getAllValues().get(1));
    }

    // ── ingest ────────────────────────────────────────────────────────────────

    @Test
    void ingest_noMatchingRule_persistsLogWithUppercasedLevel() {
        when(dropRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc())
                .thenReturn(List.of());
        when(logRepository.save(any(Log.class))).thenAnswer(inv -> inv.getArgument(0));

        Log result = service.ingest(null, null, "info", "billing", "hello", null, null, null);

        ArgumentCaptor<Log> captor = ArgumentCaptor.forClass(Log.class);
        verify(logRepository).save(captor.capture());
        assertThat(result).isNotNull();
        assertThat(captor.getValue().getLevel()).isEqualTo("INFO");
        assertThat(captor.getValue().getService()).isEqualTo("billing");
        assertThat(captor.getValue().getLogTimestamp()).isNotNull();
    }

    @Test
    void ingest_matchingRule_dropsLogAndReturnsNull() {
        when(dropRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc())
                .thenReturn(List.of(rule("ERROR", null, null)));

        Log result = service.ingest(null, null, "ERROR", "svc", "boom", null, null, null);

        assertThat(result).isNull();
        verify(logRepository, never()).save(any());
    }

    @Test
    void ingest_ruleLevelDiffersFromEntry_notDropped() {
        when(dropRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc())
                .thenReturn(List.of(rule("ERROR", null, null)));
        when(logRepository.save(any(Log.class))).thenAnswer(inv -> inv.getArgument(0));

        Log result = service.ingest(null, null, "INFO", "svc", "msg", null, null, null);

        assertThat(result).isNotNull();
    }

    @Test
    void ingest_ruleMessagePatternMatchesSubstring_dropped() {
        when(dropRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc())
                .thenReturn(List.of(rule(null, null, "TIMEOUT")));

        Log result = service.ingest(null, null, "INFO", "svc", "Request timeout occurred", null, null, null);

        assertThat(result).isNull();
    }

    @Test
    void ingest_ruleMessagePatternButNullMessage_matchesAndDrops() {
        // messagePattern set, but incoming message null → the pattern guard is skipped, rule matches
        when(dropRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc())
                .thenReturn(List.of(rule(null, null, "TIMEOUT")));

        Log result = service.ingest(null, null, "INFO", "svc", null, null, null, null);

        assertThat(result).isNull();
    }

    @Test
    void ingest_ruleServiceMismatch_notDropped() {
        when(dropRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc())
                .thenReturn(List.of(rule(null, "other-svc", null)));
        when(logRepository.save(any(Log.class))).thenAnswer(inv -> inv.getArgument(0));

        Log result = service.ingest(null, null, "INFO", "svc", "msg", null, null, null);

        assertThat(result).isNotNull();
    }

    @Test
    void ingest_withTenantId_attachesResolvedTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        when(dropRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc())
                .thenReturn(List.of());
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(logRepository.save(any(Log.class))).thenAnswer(inv -> inv.getArgument(0));

        service.ingest(tenantId, ZonedDateTime.now(), "INFO", "svc", "msg", null, null, null);

        ArgumentCaptor<Log> captor = ArgumentCaptor.forClass(Log.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().getTenant()).isSameAs(tenant);
    }

    // ── ingestBatch ───────────────────────────────────────────────────────────

    @Test
    void ingestBatch_mixOfDroppedAndKept_returnsKeptCount() {
        when(dropRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc())
                .thenReturn(List.of(rule("DEBUG", null, null)));

        List<LogIngestionService.LogEntry> entries = List.of(
                new LogIngestionService.LogEntry(null, null, "DEBUG", "svc", "drop me", null, null, null),
                new LogIngestionService.LogEntry(null, null, "INFO", "svc", "keep me", null, null, null),
                new LogIngestionService.LogEntry(null, null, "ERROR", "svc", "keep me too", null, null, null)
        );

        int stored = service.ingestBatch(entries);

        assertThat(stored).isEqualTo(2);
        ArgumentCaptor<List<Log>> captor = ArgumentCaptor.forClass(List.class);
        verify(logRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void ingestBatch_preloadsTenantsOnce_attachesToMatchingEntries() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        when(dropRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc())
                .thenReturn(List.of());
        when(tenantRepository.findAllById(any())).thenReturn(List.of(tenant));

        List<LogIngestionService.LogEntry> entries = List.of(
                new LogIngestionService.LogEntry(tenantId, null, "INFO", "svc", "a", null, null, null),
                new LogIngestionService.LogEntry(tenantId, null, "INFO", "svc", "b", null, null, null)
        );

        int stored = service.ingestBatch(entries);

        assertThat(stored).isEqualTo(2);
        verify(tenantRepository).findAllById(any());
        verify(tenantRepository, never()).findById(any());
    }

    @Test
    void ingestBatch_noTenantIds_skipsTenantLookup() {
        when(dropRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc())
                .thenReturn(List.of());

        List<LogIngestionService.LogEntry> entries = List.of(
                new LogIngestionService.LogEntry(null, null, "INFO", "svc", "a", null, null, null)
        );

        int stored = service.ingestBatch(entries);

        assertThat(stored).isEqualTo(1);
        verify(tenantRepository, never()).findAllById(any());
    }

    // ── searchLogs / getById / getDistinctServices ────────────────────────────

    @Test
    void searchLogs_delegatesToRepositoryWithUnsortedPageable() {
        Page<Log> page = new PageImpl<>(List.of(new Log()));
        when(logRepository.searchLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<Log> result = service.searchLogs(null, "INFO", "svc", null, null, "q", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(logRepository).searchLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void getById_present_returnsLog() {
        UUID id = UUID.randomUUID();
        Log log = new Log();
        when(logRepository.findById(id)).thenReturn(Optional.of(log));

        assertThat(service.getById(id)).isSameAs(log);
    }

    @Test
    void getById_missing_throwsRuntimeException() {
        UUID id = UUID.randomUUID();
        when(logRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Log not found");
    }

    @Test
    void getDistinctServices_returnsRepositoryList() {
        when(logRepository.findDistinctServices()).thenReturn(List.of("a", "b"));

        assertThat(service.getDistinctServices()).containsExactly("a", "b");
    }

    // ── purgeOldLogs ──────────────────────────────────────────────────────────

    @Test
    void purgeOldLogs_deletesUsingConfiguredCutoff() {
        ReflectionTestUtils.setField(service, "retentionDays", 7);
        ArgumentCaptor<ZonedDateTime> cutoff = ArgumentCaptor.forClass(ZonedDateTime.class);
        when(logRepository.deleteOlderThan(cutoff.capture())).thenReturn(42);

        service.purgeOldLogs();

        verify(logRepository).deleteOlderThan(any(ZonedDateTime.class));
        // cutoff should be ~7 days ago, comfortably before now and after 8 days ago
        assertThat(cutoff.getValue()).isBefore(ZonedDateTime.now().minusDays(6));
        assertThat(cutoff.getValue()).isAfter(ZonedDateTime.now().minusDays(8));
    }
}
