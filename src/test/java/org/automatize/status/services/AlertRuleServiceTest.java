package org.automatize.status.services;

import org.automatize.status.models.AlertRule;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.AlertRuleRepository;
import org.automatize.status.repositories.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AlertRuleService}.
 *
 * <p>SLACK and WEBHOOK delivery paths use a static {@code HttpClient} that opens real
 * network connections and cannot be mocked without refactoring, so only the EMAIL and
 * unknown-notification-type branches of {@code fireAlert} are exercised here.</p>
 */
@ExtendWith(MockitoExtension.class)
class AlertRuleServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private LogMetricService logMetricService;

    @InjectMocks
    private AlertRuleService alertRuleService;

    private AlertRule rule() {
        AlertRule rule = new AlertRule();
        rule.setName("rule");
        rule.setService("svc");
        rule.setLevel("ERROR");
        rule.setThresholdCount(5L);
        rule.setWindowMinutes(10);
        rule.setCooldownMinutes(5);
        rule.setNotificationType("EMAIL");
        rule.setNotificationTarget("alert@x.com");
        rule.setIsActive(true);
        return rule;
    }

    // ------------------------------------------------------------------- CRUD

    @Test
    void findAll_returnsRepositoryResults() {
        List<AlertRule> rules = List.of(rule());
        when(alertRuleRepository.findAllByOrderByCreatedDateTechnicalDesc()).thenReturn(rules);

        assertThat(alertRuleService.findAll()).isEqualTo(rules);
    }

    @Test
    void findById_whenPresent_returnsRule() {
        UUID id = UUID.randomUUID();
        AlertRule rule = rule();
        when(alertRuleRepository.findById(id)).thenReturn(Optional.of(rule));

        assertThat(alertRuleService.findById(id)).isSameAs(rule);
    }

    @Test
    void findById_whenMissing_throws() {
        UUID id = UUID.randomUUID();
        when(alertRuleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertRuleService.findById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Alert rule not found");
    }

    @Test
    void create_withTenant_setsFieldsAndSaves() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(alertRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AlertRule created = alertRuleService.create(tenantId, "n", "s", "ERROR",
                7L, 15, 3, "EMAIL", "t@x.com", true);

        assertThat(created.getTenant()).isSameAs(tenant);
        assertThat(created.getName()).isEqualTo("n");
        assertThat(created.getThresholdCount()).isEqualTo(7L);
        assertThat(created.getWindowMinutes()).isEqualTo(15);
        assertThat(created.getIsActive()).isTrue();
        verify(alertRuleRepository).save(created);
    }

    @Test
    void create_withNullTenant_doesNotLookupTenant() {
        when(alertRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AlertRule created = alertRuleService.create(null, "n", "s", "ERROR",
                1L, 5, 1, "EMAIL", "t@x.com", false);

        assertThat(created.getTenant()).isNull();
        assertThat(created.getIsActive()).isFalse();
        verify(tenantRepository, never()).findById(any());
    }

    @Test
    void update_whenPresent_updatesAndSaves() {
        UUID id = UUID.randomUUID();
        AlertRule existing = rule();
        when(alertRuleRepository.findById(id)).thenReturn(Optional.of(existing));
        when(alertRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AlertRule updated = alertRuleService.update(id, "newName", "newSvc", "WARN",
                9L, 20, 4, "SLACK", "target", false);

        assertThat(updated.getName()).isEqualTo("newName");
        assertThat(updated.getLevel()).isEqualTo("WARN");
        assertThat(updated.getNotificationType()).isEqualTo("SLACK");
        assertThat(updated.getIsActive()).isFalse();
        verify(alertRuleRepository).save(existing);
    }

    @Test
    void update_whenMissing_throws() {
        UUID id = UUID.randomUUID();
        when(alertRuleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertRuleService.update(id, "n", "s", "l",
                1L, 1, 1, "EMAIL", "t", true))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void delete_removesResolvedRule() {
        UUID id = UUID.randomUUID();
        AlertRule existing = rule();
        when(alertRuleRepository.findById(id)).thenReturn(Optional.of(existing));

        alertRuleService.delete(id);

        verify(alertRuleRepository).delete(existing);
    }

    @Test
    void toggleActive_flipsActiveFlag() {
        UUID id = UUID.randomUUID();
        AlertRule existing = rule();
        existing.setIsActive(true);
        when(alertRuleRepository.findById(id)).thenReturn(Optional.of(existing));
        when(alertRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AlertRule result = alertRuleService.toggleActive(id);

        assertThat(result.getIsActive()).isFalse();
    }

    // ----------------------------------------------------------- evaluateAll

    @Test
    void evaluateAll_whenThresholdBreached_firesEmailAndRecordsFire() {
        AlertRule r = rule();
        r.setLastFiredAt(null);
        when(alertRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc()).thenReturn(List.of(r));
        when(logMetricService.sumCountSince(eq("svc"), eq("ERROR"), any(ZonedDateTime.class))).thenReturn(10L);

        alertRuleService.evaluateAll();

        verify(emailService).sendSimpleEmail(eq("alert@x.com"), anyString(), anyString());
        assertThat(r.getLastFiredAt()).isNotNull();
        verify(alertRuleRepository).save(r);
    }

    @Test
    void evaluateAll_whenBelowThreshold_doesNotFire() {
        AlertRule r = rule();
        r.setLastFiredAt(null);
        when(alertRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc()).thenReturn(List.of(r));
        when(logMetricService.sumCountSince(any(), any(), any())).thenReturn(1L);

        alertRuleService.evaluateAll();

        verify(emailService, never()).sendSimpleEmail(any(), any(), any());
        verify(alertRuleRepository, never()).save(any());
    }

    @Test
    void evaluateAll_whenInCooldown_skipsEvaluation() {
        AlertRule r = rule();
        r.setLastFiredAt(ZonedDateTime.now().minusMinutes(1));
        r.setCooldownMinutes(60);
        when(alertRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc()).thenReturn(List.of(r));

        alertRuleService.evaluateAll();

        verify(logMetricService, never()).sumCountSince(any(), any(), any());
        verify(emailService, never()).sendSimpleEmail(any(), any(), any());
    }

    @Test
    void evaluateAll_whenUnknownNotificationType_recordsFireButSendsNoEmail() {
        AlertRule r = rule();
        r.setNotificationType("TEAMS");
        r.setLastFiredAt(null);
        when(alertRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc()).thenReturn(List.of(r));
        when(logMetricService.sumCountSince(any(), any(), any())).thenReturn(99L);

        alertRuleService.evaluateAll();

        verify(emailService, never()).sendSimpleEmail(any(), any(), any());
        assertThat(r.getLastFiredAt()).isNotNull();
        verify(alertRuleRepository).save(r);
    }

    @Test
    void evaluateAll_whenRuleEvaluationThrows_continuesWithoutPropagating() {
        AlertRule r = rule();
        r.setLastFiredAt(null);
        when(alertRuleRepository.findByIsActiveTrueOrderByCreatedDateTechnicalDesc()).thenReturn(List.of(r));
        when(logMetricService.sumCountSince(any(), any(), any())).thenThrow(new RuntimeException("db error"));

        // should swallow the exception internally
        alertRuleService.evaluateAll();

        verify(emailService, never()).sendSimpleEmail(any(), any(), any());
    }
}
