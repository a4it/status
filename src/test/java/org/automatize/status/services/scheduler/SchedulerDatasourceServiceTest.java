package org.automatize.status.services.scheduler;

import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.SchedulerJdbcDatasource;
import org.automatize.status.models.Tenant;
import org.automatize.status.repositories.SchedulerJdbcDatasourceRepository;
import org.automatize.status.repositories.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerDatasourceServiceTest {

    @Mock private SchedulerJdbcDatasourceRepository datasourceRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private SchedulerEncryptionService encryptionService;
    @Mock private SqlExecutorService sqlExecutorService;

    @InjectMocks private SchedulerDatasourceService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID dsId = UUID.randomUUID();

    private SchedulerJdbcDatasource newDs() {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setId(dsId);
        ds.setName("primary");
        return ds;
    }

    @Test
    void list_delegatesToRepository() {
        List<SchedulerJdbcDatasource> list = List.of(newDs());
        when(datasourceRepository.findByTenantId(tenantId)).thenReturn(list);

        assertThat(service.list(tenantId)).isSameAs(list);
    }

    @Test
    void get_found_returnsDatasource() {
        SchedulerJdbcDatasource ds = newDs();
        when(datasourceRepository.findByIdAndTenantId(dsId, tenantId)).thenReturn(Optional.of(ds));

        assertThat(service.get(dsId, tenantId)).isSameAs(ds);
    }

    @Test
    void get_notFound_throwsResourceNotFound() {
        when(datasourceRepository.findByIdAndTenantId(dsId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(dsId, tenantId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Datasource not found");
    }

    @Test
    void create_withPassword_encryptsAndSaves() {
        SchedulerJdbcDatasource ds = newDs();
        ds.setPasswordEnc("secret");
        Tenant tenant = new Tenant();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(encryptionService.encrypt("secret")).thenReturn("ENC(secret)");
        when(datasourceRepository.save(any(SchedulerJdbcDatasource.class))).thenAnswer(inv -> inv.getArgument(0));

        SchedulerJdbcDatasource saved = service.create(ds, tenantId, "tim");

        assertThat(saved.getTenant()).isSameAs(tenant);
        assertThat(saved.getCreatedBy()).isEqualTo("tim");
        assertThat(saved.getPasswordEnc()).isEqualTo("ENC(secret)");
        verify(encryptionService).encrypt("secret");
    }

    @Test
    void create_blankPassword_doesNotEncrypt() {
        SchedulerJdbcDatasource ds = newDs();
        ds.setPasswordEnc("   ");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(new Tenant()));
        when(datasourceRepository.save(any(SchedulerJdbcDatasource.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(ds, tenantId, "tim");

        verify(encryptionService, never()).encrypt(any());
    }

    @Test
    void create_tenantNotFound_throwsResourceNotFound() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(newDs(), tenantId, "tim"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tenant not found");
    }

    @Test
    void update_notFound_throwsResourceNotFound() {
        when(datasourceRepository.findByIdAndTenantId(dsId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(dsId, newDs(), tenantId, "tim"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_withNewPassword_encryptsAndCopiesFields() {
        SchedulerJdbcDatasource existing = newDs();
        existing.setPasswordEnc("OLD");
        SchedulerJdbcDatasource data = newDs();
        data.setName("renamed");
        data.setPasswordEnc("newpw");
        when(datasourceRepository.findByIdAndTenantId(dsId, tenantId)).thenReturn(Optional.of(existing));
        when(encryptionService.encrypt("newpw")).thenReturn("ENC(newpw)");
        when(datasourceRepository.save(any(SchedulerJdbcDatasource.class))).thenAnswer(inv -> inv.getArgument(0));

        SchedulerJdbcDatasource saved = service.update(dsId, data, tenantId, "tim");

        assertThat(saved.getName()).isEqualTo("renamed");
        assertThat(saved.getPasswordEnc()).isEqualTo("ENC(newpw)");
        assertThat(saved.getLastModifiedBy()).isEqualTo("tim");
    }

    @Test
    void update_blankPassword_keepsExistingPassword() {
        SchedulerJdbcDatasource existing = newDs();
        existing.setPasswordEnc("OLD");
        SchedulerJdbcDatasource data = newDs();
        data.setPasswordEnc(null);
        when(datasourceRepository.findByIdAndTenantId(dsId, tenantId)).thenReturn(Optional.of(existing));
        when(datasourceRepository.save(any(SchedulerJdbcDatasource.class))).thenAnswer(inv -> inv.getArgument(0));

        SchedulerJdbcDatasource saved = service.update(dsId, data, tenantId, "tim");

        assertThat(saved.getPasswordEnc()).isEqualTo("OLD");
        verify(encryptionService, never()).encrypt(any());
    }

    @Test
    void delete_found_deletes() {
        SchedulerJdbcDatasource ds = newDs();
        when(datasourceRepository.findByIdAndTenantId(dsId, tenantId)).thenReturn(Optional.of(ds));

        service.delete(dsId, tenantId);

        verify(datasourceRepository).delete(ds);
    }

    @Test
    void delete_notFound_throwsResourceNotFound() {
        when(datasourceRepository.findByIdAndTenantId(dsId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(dsId, tenantId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(datasourceRepository, never()).delete(any());
    }

    @Test
    void testConnection_found_delegatesToSqlExecutor() {
        SchedulerJdbcDatasource ds = newDs();
        Map<String, Object> expected = Map.of("success", true, "latencyMs", 12L);
        when(datasourceRepository.findByIdAndTenantId(dsId, tenantId)).thenReturn(Optional.of(ds));
        when(sqlExecutorService.testConnection(ds)).thenReturn(expected);

        Map<String, Object> result = service.testConnection(dsId, tenantId);

        assertThat(result).isSameAs(expected);
        verify(sqlExecutorService).testConnection(ds);
    }
}
