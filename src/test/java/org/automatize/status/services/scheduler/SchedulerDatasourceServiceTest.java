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

/**
 * Unit tests for {@link SchedulerDatasourceService}, which manages tenant-scoped
 * JDBC datasource CRUD, password encryption on create/update, and delegates
 * connection testing to {@link SqlExecutorService}.
 *
 * <p>Repositories and collaborator services are Mockito mocks injected into the
 * service under test.</p>
 */
@ExtendWith(MockitoExtension.class)
class SchedulerDatasourceServiceTest {

    private static final String SECRET = "secret";

    @Mock private SchedulerJdbcDatasourceRepository datasourceRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private SchedulerEncryptionService encryptionService;
    @Mock private SqlExecutorService sqlExecutorService;

    @InjectMocks private SchedulerDatasourceService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID dsId = UUID.randomUUID();

    /**
     * Builds a minimal named datasource fixture with the shared test id.
     *
     * @return a new {@link SchedulerJdbcDatasource}
     */
    private SchedulerJdbcDatasource newDs() {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setId(dsId);
        ds.setName("primary");
        return ds;
    }

    /**
     * Verifies {@code list} delegates directly to the repository's tenant lookup.
     * Expected outcome: the repository's list is returned unchanged.
     */
    @Test
    void list_delegatesToRepository() {
        List<SchedulerJdbcDatasource> list = List.of(newDs());
        when(datasourceRepository.findByTenantId(tenantId)).thenReturn(list);

        assertThat(service.list(tenantId)).isSameAs(list);
    }

    /**
     * Verifies {@code get} returns the datasource when found for the tenant.
     * Expected outcome: the found datasource is returned.
     */
    @Test
    void get_found_returnsDatasource() {
        SchedulerJdbcDatasource ds = newDs();
        when(datasourceRepository.findByIdAndTenantId(dsId, tenantId)).thenReturn(Optional.of(ds));

        assertThat(service.get(dsId, tenantId)).isSameAs(ds);
    }

    /**
     * Verifies {@code get} throws when the datasource is not found.
     * Expected outcome: {@link ResourceNotFoundException} with "Datasource not found".
     */
    @Test
    void get_notFound_throwsResourceNotFound() {
        when(datasourceRepository.findByIdAndTenantId(dsId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(dsId, tenantId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Datasource not found");
    }

    /**
     * Verifies {@code create} with a password encrypts it and stamps audit fields before saving.
     * Expected outcome: tenant, createdBy and encrypted password are set on the saved entity.
     */
    @Test
    void create_withPassword_encryptsAndSaves() {
        SchedulerJdbcDatasource ds = newDs();
        ds.setPasswordEnc(SECRET);
        Tenant tenant = new Tenant();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(encryptionService.encrypt(SECRET)).thenReturn("ENC(secret)");
        when(datasourceRepository.save(any(SchedulerJdbcDatasource.class))).thenAnswer(inv -> inv.getArgument(0));

        SchedulerJdbcDatasource saved = service.create(ds, tenantId, "tim");

        assertThat(saved.getTenant()).isSameAs(tenant);
        assertThat(saved.getCreatedBy()).isEqualTo("tim");
        assertThat(saved.getPasswordEnc()).isEqualTo("ENC(secret)");
        verify(encryptionService).encrypt(SECRET);
    }

    /**
     * Verifies {@code create} with a blank password does not invoke encryption.
     * Expected outcome: the encryption service is never called.
     */
    @Test
    void create_blankPassword_doesNotEncrypt() {
        SchedulerJdbcDatasource ds = newDs();
        ds.setPasswordEnc("   ");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(new Tenant()));
        when(datasourceRepository.save(any(SchedulerJdbcDatasource.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(ds, tenantId, "tim");

        verify(encryptionService, never()).encrypt(any());
    }

    /**
     * Verifies {@code create} fails when the owning tenant does not exist.
     * Expected outcome: {@link ResourceNotFoundException} with "Tenant not found".
     */
    @Test
    void create_tenantNotFound_throwsResourceNotFound() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(newDs(), tenantId, "tim"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tenant not found");
    }

    /**
     * Verifies {@code update} fails when the target datasource is not found for the tenant.
     * Expected outcome: {@link ResourceNotFoundException} is thrown.
     */
    @Test
    void update_notFound_throwsResourceNotFound() {
        when(datasourceRepository.findByIdAndTenantId(dsId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(dsId, newDs(), tenantId, "tim"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies {@code update} with a new password re-encrypts it and copies editable fields.
     * Expected outcome: name, encrypted password and lastModifiedBy reflect the update.
     */
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

    /**
     * Verifies {@code update} with a blank/null password preserves the existing stored password.
     * Expected outcome: the old encrypted password is retained and encryption is not invoked.
     */
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

    /**
     * Verifies {@code delete} removes the datasource when found.
     * Expected outcome: the repository delete is invoked with the found entity.
     */
    @Test
    void delete_found_deletes() {
        SchedulerJdbcDatasource ds = newDs();
        when(datasourceRepository.findByIdAndTenantId(dsId, tenantId)).thenReturn(Optional.of(ds));

        service.delete(dsId, tenantId);

        verify(datasourceRepository).delete(ds);
    }

    /**
     * Verifies {@code delete} fails and deletes nothing when the datasource is not found.
     * Expected outcome: {@link ResourceNotFoundException} is thrown and no delete occurs.
     */
    @Test
    void delete_notFound_throwsResourceNotFound() {
        when(datasourceRepository.findByIdAndTenantId(dsId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(dsId, tenantId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(datasourceRepository, never()).delete(any());
    }

    /**
     * Verifies {@code testConnection} delegates to the SQL executor for a found datasource.
     * Expected outcome: the executor's result map is returned unchanged.
     */
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
