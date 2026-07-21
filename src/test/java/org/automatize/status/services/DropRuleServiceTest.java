package org.automatize.status.services;

import org.automatize.status.models.DropRule;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.DropRuleRepository;
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
 * Unit tests for {@link DropRuleService} (CRUD + activation toggling).
 * Note: the actual drop-rule *matching* logic lives in {@link LogIngestionService}
 * and is covered thoroughly in {@code LogIngestionServiceTest}.
 *
 * <p>Testing approach: pure Mockito unit tests. The {@link DropRuleRepository} and
 * {@link TenantRepository} are mocked so each service operation is verified in
 * isolation, asserting the persisted field values (often via {@link ArgumentCaptor})
 * and the not-found error paths without touching a database.</p>
 */
@ExtendWith(MockitoExtension.class)
class DropRuleServiceTest {

    @Mock
    private DropRuleRepository dropRuleRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private DropRuleService service;

    /**
     * Verifies {@code findAll()} returns the repository's ordered result set unchanged.
     * Expects the single stubbed rule to be passed straight through.
     */
    @Test
    void findAll_returnsOrderedRepositoryList() {
        DropRule r = new DropRule();
        when(dropRuleRepository.findAllByOrderByCreatedDateTechnicalDesc()).thenReturn(List.of(r));

        assertThat(service.findAll()).containsExactly(r);
    }

    /**
     * Verifies {@code findById} returns the rule when the repository finds it.
     * Expects the exact stubbed instance to be returned.
     */
    @Test
    void findById_present_returnsRule() {
        UUID id = UUID.randomUUID();
        DropRule r = new DropRule();
        when(dropRuleRepository.findById(id)).thenReturn(Optional.of(r));

        assertThat(service.findById(id)).isSameAs(r);
    }

    /**
     * Verifies {@code findById} throws when the repository has no match.
     * Expects a {@link RuntimeException} whose message contains "Drop rule not found".
     */
    @Test
    void findById_missing_throwsRuntimeException() {
        UUID id = UUID.randomUUID();
        when(dropRuleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Drop rule not found");
    }

    /**
     * Verifies {@code create} with a null tenant id populates all fields, leaves the
     * tenant null, saves the rule, and never performs a tenant lookup.
     */
    @Test
    void create_noTenant_setsAllFieldsAndSaves() {
        when(dropRuleRepository.save(any(DropRule.class))).thenAnswer(inv -> inv.getArgument(0));

        DropRule result = service.create(null, "rule-1", "ERROR", "svc", "boom", true);

        ArgumentCaptor<DropRule> captor = ArgumentCaptor.forClass(DropRule.class);
        verify(dropRuleRepository).save(captor.capture());
        DropRule saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("rule-1");
        assertThat(saved.getLevel()).isEqualTo("ERROR");
        assertThat(saved.getService()).isEqualTo("svc");
        assertThat(saved.getMessagePattern()).isEqualTo("boom");
        assertThat(saved.getIsActive()).isTrue();
        assertThat(saved.getTenant()).isNull();
        assertThat(result).isSameAs(saved);
        verify(tenantRepository, never()).findById(any());
    }

    /**
     * Verifies {@code create} with a tenant id resolves and attaches that tenant to
     * the new rule. Expects the resolved tenant to be set and the inactive flag honoured.
     */
    @Test
    void create_withTenant_attachesResolvedTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(dropRuleRepository.save(any(DropRule.class))).thenAnswer(inv -> inv.getArgument(0));

        DropRule result = service.create(tenantId, "r", null, null, null, false);

        assertThat(result.getTenant()).isSameAs(tenant);
        assertThat(result.getIsActive()).isFalse();
    }

    /**
     * Verifies {@code update} on an existing rule overwrites every mutable field.
     * Expects name, level, service, message pattern and active flag to reflect the new values.
     */
    @Test
    void update_existing_overwritesFields() {
        UUID id = UUID.randomUUID();
        DropRule existing = new DropRule();
        existing.setName("old");
        existing.setIsActive(false);
        when(dropRuleRepository.findById(id)).thenReturn(Optional.of(existing));
        when(dropRuleRepository.save(any(DropRule.class))).thenAnswer(inv -> inv.getArgument(0));

        DropRule result = service.update(id, "new", "WARN", "svc2", "pat", true);

        assertThat(result.getName()).isEqualTo("new");
        assertThat(result.getLevel()).isEqualTo("WARN");
        assertThat(result.getService()).isEqualTo("svc2");
        assertThat(result.getMessagePattern()).isEqualTo("pat");
        assertThat(result.getIsActive()).isTrue();
    }

    /**
     * Verifies {@code update} on a missing rule throws and never persists.
     * Expects a {@link RuntimeException} and no repository save.
     */
    @Test
    void update_missing_throwsRuntimeException() {
        UUID id = UUID.randomUUID();
        when(dropRuleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, "n", null, null, null, true))
                .isInstanceOf(RuntimeException.class);
        verify(dropRuleRepository, never()).save(any());
    }

    /**
     * Verifies {@code delete} on an existing rule delegates removal to the repository.
     * Expects {@code repository.delete(rule)} to be invoked with the resolved entity.
     */
    @Test
    void delete_existing_delegatesToRepository() {
        UUID id = UUID.randomUUID();
        DropRule r = new DropRule();
        when(dropRuleRepository.findById(id)).thenReturn(Optional.of(r));

        service.delete(id);

        verify(dropRuleRepository).delete(r);
    }

    /**
     * Verifies {@code delete} on a missing rule throws and never deletes.
     * Expects a {@link RuntimeException} and no repository delete call.
     */
    @Test
    void delete_missing_throwsRuntimeException() {
        UUID id = UUID.randomUUID();
        when(dropRuleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(RuntimeException.class);
        verify(dropRuleRepository, never()).delete(any());
    }

    /**
     * Verifies {@code toggleActive} flips an active rule to inactive.
     * Expects the returned rule's active flag to be false.
     */
    @Test
    void toggleActive_activeRule_becomesInactive() {
        UUID id = UUID.randomUUID();
        DropRule r = new DropRule();
        r.setIsActive(true);
        when(dropRuleRepository.findById(id)).thenReturn(Optional.of(r));
        when(dropRuleRepository.save(any(DropRule.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.toggleActive(id).getIsActive()).isFalse();
    }

    /**
     * Verifies {@code toggleActive} treats a null active flag as inactive and turns it on.
     * Expects the returned rule's active flag to be true.
     */
    @Test
    void toggleActive_nullActive_becomesTrue() {
        UUID id = UUID.randomUUID();
        DropRule r = new DropRule();
        r.setIsActive(null);
        when(dropRuleRepository.findById(id)).thenReturn(Optional.of(r));
        when(dropRuleRepository.save(any(DropRule.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.toggleActive(id).getIsActive()).isTrue();
    }
}
