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
 */
@ExtendWith(MockitoExtension.class)
class DropRuleServiceTest {

    @Mock
    private DropRuleRepository dropRuleRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private DropRuleService service;

    @Test
    void findAll_returnsOrderedRepositoryList() {
        DropRule r = new DropRule();
        when(dropRuleRepository.findAllByOrderByCreatedDateTechnicalDesc()).thenReturn(List.of(r));

        assertThat(service.findAll()).containsExactly(r);
    }

    @Test
    void findById_present_returnsRule() {
        UUID id = UUID.randomUUID();
        DropRule r = new DropRule();
        when(dropRuleRepository.findById(id)).thenReturn(Optional.of(r));

        assertThat(service.findById(id)).isSameAs(r);
    }

    @Test
    void findById_missing_throwsRuntimeException() {
        UUID id = UUID.randomUUID();
        when(dropRuleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Drop rule not found");
    }

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

    @Test
    void update_missing_throwsRuntimeException() {
        UUID id = UUID.randomUUID();
        when(dropRuleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, "n", null, null, null, true))
                .isInstanceOf(RuntimeException.class);
        verify(dropRuleRepository, never()).save(any());
    }

    @Test
    void delete_existing_delegatesToRepository() {
        UUID id = UUID.randomUUID();
        DropRule r = new DropRule();
        when(dropRuleRepository.findById(id)).thenReturn(Optional.of(r));

        service.delete(id);

        verify(dropRuleRepository).delete(r);
    }

    @Test
    void delete_missing_throwsRuntimeException() {
        UUID id = UUID.randomUUID();
        when(dropRuleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(RuntimeException.class);
        verify(dropRuleRepository, never()).delete(any());
    }

    @Test
    void toggleActive_activeRule_becomesInactive() {
        UUID id = UUID.randomUUID();
        DropRule r = new DropRule();
        r.setIsActive(true);
        when(dropRuleRepository.findById(id)).thenReturn(Optional.of(r));
        when(dropRuleRepository.save(any(DropRule.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.toggleActive(id).getIsActive()).isFalse();
    }

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
