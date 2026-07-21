package org.automatize.status.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.automatize.status.api.request.ProcessMiningRetentionRequest;
import org.automatize.status.api.response.ProcessMiningRetentionResponse;
import org.automatize.status.models.ProcessMiningRetentionRule;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusPlatform;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.ProcessMiningRetentionRuleRepository;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusPlatformRepository;
import org.automatize.status.repositories.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProcessMiningRetentionService} — CRUD, request mapping,
 * response mapping and the retention cleanup branches (platform / tenant / neither).
 */
@ExtendWith(MockitoExtension.class)
class ProcessMiningRetentionServiceTest {

    @Mock
    private ProcessMiningRetentionRuleRepository retentionRuleRepository;

    @Mock
    private StatusAppRepository statusAppRepository;

    @Mock
    private StatusPlatformRepository statusPlatformRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    @InjectMocks
    private ProcessMiningRetentionService service;

    /**
     * Builds an enabled {@link ProcessMiningRetentionRule} with the given id and retention window.
     *
     * @param id            the rule identifier
     * @param retentionDays the number of days data is retained
     * @return a new enabled retention rule
     */
    private ProcessMiningRetentionRule rule(UUID id, int retentionDays) {
        ProcessMiningRetentionRule r = new ProcessMiningRetentionRule();
        r.setId(id);
        r.setRetentionDays(retentionDays);
        r.setEnabled(true);
        return r;
    }

    /**
     * Builds a {@link StatusApp} (service) with the given name.
     *
     * @param name the service name
     * @return a new status app
     */
    private StatusApp app(String name) {
        StatusApp a = new StatusApp();
        a.setName(name);
        return a;
    }

    /**
     * Stubs the {@link EntityManager} JPQL delete chain so that executing the query reports
     * the given number of affected rows.
     *
     * @param deleted the row count returned by {@code executeUpdate()}
     */
    private void stubQueryChain(int deleted) {
        when(entityManager.createQuery(anyString())).thenReturn(query);
        lenient().when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(deleted);
    }

    // ── findAll / findById ────────────────────────────────────────────────────

    /**
     * Verifies that {@code findAll} maps every persisted rule to a response, and that a rule
     * without a platform is reported with the "All Platforms" label.
     */
    @Test
    void findAll_mapsAllRulesToResponses() {
        ProcessMiningRetentionRule r = rule(UUID.randomUUID(), 30);
        when(retentionRuleRepository.findAll()).thenReturn(List.of(r));

        List<ProcessMiningRetentionResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRetentionDays()).isEqualTo(30);
        // no platform → mapped to "All Platforms"
        assertThat(result.get(0).getPlatformName()).isEqualTo("All Platforms");
    }

    /**
     * Verifies that {@code findById} returns the mapped response when the rule exists.
     */
    @Test
    void findById_present_returnsResponse() {
        UUID id = UUID.randomUUID();
        when(retentionRuleRepository.findById(id)).thenReturn(Optional.of(rule(id, 15)));

        ProcessMiningRetentionResponse resp = service.findById(id);

        assertThat(resp.getId()).isEqualTo(id);
        assertThat(resp.getRetentionDays()).isEqualTo(15);
    }

    /**
     * Verifies that {@code findById} throws {@link NoSuchElementException} when the rule is absent.
     */
    @Test
    void findById_missing_throwsNoSuchElement() {
        UUID id = UUID.randomUUID();
        when(retentionRuleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Retention rule not found");
    }

    // ── create / update ───────────────────────────────────────────────────────

    /**
     * Verifies that creating a rule with neither tenant nor platform saves it with null
     * associations, and never looks up a tenant or platform.
     */
    @Test
    void create_noTenantNoPlatform_savesRuleWithNulls() {
        ProcessMiningRetentionRequest req = new ProcessMiningRetentionRequest();
        req.setRetentionDays(45);
        req.setEnabled(true);
        when(retentionRuleRepository.save(any(ProcessMiningRetentionRule.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProcessMiningRetentionResponse resp = service.create(req);

        assertThat(resp.getRetentionDays()).isEqualTo(45);
        assertThat(resp.isEnabled()).isTrue();
        assertThat(resp.getPlatformName()).isEqualTo("All Platforms");
        verify(tenantRepository, never()).findById(any());
        verify(statusPlatformRepository, never()).findById(any());
    }

    /**
     * Verifies that creating a rule with a tenant and platform id resolves both entities and
     * reflects their ids and names in the response.
     */
    @Test
    void create_withTenantAndPlatform_resolvesBoth() {
        UUID tenantId = UUID.randomUUID();
        UUID platformId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Acme");
        StatusPlatform platform = new StatusPlatform();
        platform.setId(platformId);
        platform.setName("Prod");

        ProcessMiningRetentionRequest req = new ProcessMiningRetentionRequest();
        req.setRetentionDays(10);
        req.setEnabled(false);
        req.setTenantId(tenantId);
        req.setPlatformId(platformId);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(statusPlatformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(retentionRuleRepository.save(any(ProcessMiningRetentionRule.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProcessMiningRetentionResponse resp = service.create(req);

        assertThat(resp.getTenantId()).isEqualTo(tenantId);
        assertThat(resp.getTenantName()).isEqualTo("Acme");
        assertThat(resp.getPlatformId()).isEqualTo(platformId);
        assertThat(resp.getPlatformName()).isEqualTo("Prod");
    }

    /**
     * Verifies that creating a rule with an unknown tenant id throws {@link NoSuchElementException}.
     */
    @Test
    void create_tenantNotFound_throwsNoSuchElement() {
        UUID tenantId = UUID.randomUUID();
        ProcessMiningRetentionRequest req = new ProcessMiningRetentionRequest();
        req.setTenantId(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Tenant not found");
    }

    /**
     * Verifies that creating a rule with an unknown platform id throws {@link NoSuchElementException}.
     */
    @Test
    void create_platformNotFound_throwsNoSuchElement() {
        UUID platformId = UUID.randomUUID();
        ProcessMiningRetentionRequest req = new ProcessMiningRetentionRequest();
        req.setPlatformId(platformId);
        when(statusPlatformRepository.findById(platformId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Platform not found");
    }

    /**
     * Verifies that updating an existing rule applies the request fields (retention days, enabled)
     * and persists the change.
     */
    @Test
    void update_existing_appliesRequestAndSaves() {
        UUID id = UUID.randomUUID();
        ProcessMiningRetentionRule existing = rule(id, 30);
        ProcessMiningRetentionRequest req = new ProcessMiningRetentionRequest();
        req.setRetentionDays(90);
        req.setEnabled(false);

        when(retentionRuleRepository.findById(id)).thenReturn(Optional.of(existing));
        when(retentionRuleRepository.save(any(ProcessMiningRetentionRule.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProcessMiningRetentionResponse resp = service.update(id, req);

        assertThat(resp.getRetentionDays()).isEqualTo(90);
        assertThat(resp.isEnabled()).isFalse();
    }

    /**
     * Verifies that updating a non-existent rule throws {@link NoSuchElementException}.
     */
    @Test
    void update_missing_throwsNoSuchElement() {
        UUID id = UUID.randomUUID();
        when(retentionRuleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new ProcessMiningRetentionRequest()))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    /**
     * Verifies that deleting an existing rule delegates to the repository's {@code deleteById}.
     */
    @Test
    void delete_existing_deletesById() {
        UUID id = UUID.randomUUID();
        when(retentionRuleRepository.existsById(id)).thenReturn(true);

        service.delete(id);

        verify(retentionRuleRepository).deleteById(id);
    }

    /**
     * Verifies that deleting a non-existent rule throws {@link NoSuchElementException} and does
     * not attempt a repository delete.
     */
    @Test
    void delete_missing_throwsNoSuchElement() {
        UUID id = UUID.randomUUID();
        when(retentionRuleRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(NoSuchElementException.class);
        verify(retentionRuleRepository, never()).deleteById(any());
    }

    // ── runRetentionNow (cleanup branches) ────────────────────────────────────

    /**
     * Verifies that a platform-scoped rule deletes process mining data for the platform's services,
     * records the run timestamp and deleted count, and persists the rule.
     */
    @Test
    void runRetentionNow_platformRule_deletesByServicesAndRecordsRun() {
        UUID platformId = UUID.randomUUID();
        StatusPlatform platform = new StatusPlatform();
        platform.setId(platformId);
        platform.setName("Prod");

        ProcessMiningRetentionRule r = rule(UUID.randomUUID(), 30);
        r.setPlatform(platform);

        when(retentionRuleRepository.findByEnabledTrue()).thenReturn(List.of(r));
        when(statusAppRepository.findByPlatformId(platformId)).thenReturn(List.of(app("svc-a"), app("svc-b")));
        stubQueryChain(7);

        Map<String, Object> result = service.runRetentionNow();

        assertThat(result.get("rulesProcessed")).isEqualTo(1);
        assertThat(result.get("totalDeleted")).isEqualTo(7);
        assertThat(r.getLastRunDeletedCount()).isEqualTo(7);
        assertThat(r.getLastRunAt()).isNotNull();
        verify(retentionRuleRepository).save(r);
        verify(query).executeUpdate();
    }

    /**
     * Verifies that a platform-scoped rule whose platform has no services deletes nothing and
     * never issues a delete query.
     */
    @Test
    void runRetentionNow_platformRuleWithNoServices_deletesNothing() {
        UUID platformId = UUID.randomUUID();
        StatusPlatform platform = new StatusPlatform();
        platform.setId(platformId);
        platform.setName("Empty");

        ProcessMiningRetentionRule r = rule(UUID.randomUUID(), 30);
        r.setPlatform(platform);

        when(retentionRuleRepository.findByEnabledTrue()).thenReturn(List.of(r));
        when(statusAppRepository.findByPlatformId(platformId)).thenReturn(List.of());

        Map<String, Object> result = service.runRetentionNow();

        assertThat(result.get("totalDeleted")).isEqualTo(0);
        assertThat(r.getLastRunDeletedCount()).isEqualTo(0);
        verify(entityManager, never()).createQuery(anyString());
    }

    /**
     * Verifies that a tenant-only rule deletes data scoped by tenant and never resolves services
     * by platform.
     */
    @Test
    void runRetentionNow_tenantOnlyRule_deletesByTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Acme");

        ProcessMiningRetentionRule r = rule(UUID.randomUUID(), 30);
        r.setTenant(tenant);

        when(retentionRuleRepository.findByEnabledTrue()).thenReturn(List.of(r));
        stubQueryChain(3);

        Map<String, Object> result = service.runRetentionNow();

        assertThat(result.get("totalDeleted")).isEqualTo(3);
        verify(query).executeUpdate();
        verify(statusAppRepository, never()).findByPlatformId(any());
    }

    /**
     * Verifies that a rule scoped to neither platform nor tenant deletes nothing, issues no delete
     * query, yet still saves the rule to record the run.
     */
    @Test
    void runRetentionNow_ruleWithNeitherPlatformNorTenant_deletesNothing() {
        ProcessMiningRetentionRule r = rule(UUID.randomUUID(), 30);

        when(retentionRuleRepository.findByEnabledTrue()).thenReturn(List.of(r));

        Map<String, Object> result = service.runRetentionNow();

        assertThat(result.get("totalDeleted")).isEqualTo(0);
        verify(entityManager, never()).createQuery(anyString());
        verify(retentionRuleRepository).save(r);
    }

    /**
     * Verifies that running retention with no enabled rules returns zero totals and empty details.
     */
    @Test
    void runRetentionNow_noRules_returnsZeroTotals() {
        when(retentionRuleRepository.findByEnabledTrue()).thenReturn(List.of());

        Map<String, Object> result = service.runRetentionNow();

        assertThat(result.get("rulesProcessed")).isEqualTo(0);
        assertThat(result.get("totalDeleted")).isEqualTo(0);
        assertThat((List<?>) result.get("details")).isEmpty();
    }

    // ── scheduledRun ──────────────────────────────────────────────────────────

    /**
     * Verifies that the scheduled entry point delegates to the retention run logic by querying
     * enabled rules.
     */
    @Test
    void scheduledRun_delegatesToRunRetentionNow() {
        when(retentionRuleRepository.findByEnabledTrue()).thenReturn(List.of());

        service.scheduledRun();

        verify(retentionRuleRepository).findByEnabledTrue();
    }
}
