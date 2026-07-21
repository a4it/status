package org.automatize.status.services.scheduler;

import org.automatize.status.models.SchedulerJdbcDatasource;
import org.automatize.status.models.SchedulerJobRun;
import org.automatize.status.models.SchedulerSqlConfig;
import org.automatize.status.models.scheduler.DbType;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.automatize.status.models.scheduler.SqlType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SqlExecutorService}.
 *
 * <p>The happy-path SQL execution and a successful {@code testConnection} both
 * open a real JDBC connection ({@link java.sql.DriverManager}) and therefore need
 * integration coverage. These tests exercise the null-config guard, the
 * "no datasource / no inline config" validation, the connection-failure path
 * (using a JDBC URL that no registered driver accepts, so no network occurs),
 * and the JDBC-URL / credential resolution helpers.</p>
 */
@ExtendWith(MockitoExtension.class)
class SqlExecutorServiceTest {

    /** A JDBC URL whose scheme is accepted by no registered driver, so getConnection fails fast without any network. */
    private static final String UNROUTABLE_JDBC_URL = "jdbc:nosuchdriverxyz://localhost/db";

    @Mock
    private SchedulerEncryptionService encryptionService;

    @InjectMocks
    private SqlExecutorService sqlExecutorService;

    // -------------------------------------------------------------------------
    // execute() - null config guard
    // -------------------------------------------------------------------------

    @Test
    void execute_nullConfig_setsFailureWithMessage() {
        SchedulerJobRun run = new SchedulerJobRun();

        sqlExecutorService.execute(null, run);

        assertThat(run.getStatus()).isEqualTo(JobRunStatus.FAILURE);
        assertThat(run.getErrorMessage()).isEqualTo("SQL configuration is missing");
    }

    // -------------------------------------------------------------------------
    // execute() - configuration validation and connection failure
    // -------------------------------------------------------------------------

    @Test
    void execute_noDatasourceNoInlineConfig_throwsIllegalArgument() {
        // URL resolution happens before the guarded try block, so the exception propagates.
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        SchedulerJobRun run = new SchedulerJobRun();

        assertThatThrownBy(() -> sqlExecutorService.execute(config, run))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No datasource or inline JDBC config provided");
    }

    @Test
    void execute_unroutableJdbcUrl_setsFailureWithMessage() {
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setInlineJdbcUrl(UNROUTABLE_JDBC_URL);
        // inlineDbType left null so loadDriver performs no Class.forName
        config.setInlineUsername("u");
        config.setSqlType(SqlType.DML);
        config.setSqlStatement("UPDATE t SET x = 1");
        SchedulerJobRun run = new SchedulerJobRun();

        sqlExecutorService.execute(config, run);

        assertThat(run.getStatus()).isEqualTo(JobRunStatus.FAILURE);
        assertThat(run.getErrorMessage()).isNotBlank();
        assertThat(run.getRowsAffected()).isNull();
    }

    // -------------------------------------------------------------------------
    // testConnection()
    // -------------------------------------------------------------------------

    @Test
    void testConnection_unroutableUrl_returnsSuccessFalseWithError() {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setDbType(DbType.POSTGRESQL);
        ds.setJdbcUrlOverride(UNROUTABLE_JDBC_URL);
        ds.setUsername("u");
        // passwordEnc null -> encryptionService.decrypt not invoked

        Map<String, Object> result = sqlExecutorService.testConnection(ds);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("error")).isNotNull();
        assertThat(result.get("latencyMs")).isInstanceOf(Long.class);
    }

    // -------------------------------------------------------------------------
    // resolveJdbcUrl()
    // -------------------------------------------------------------------------

    @Test
    void resolveJdbcUrl_withDatasourceOverride_usesOverride() {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setDbType(DbType.POSTGRESQL);
        ds.setJdbcUrlOverride("jdbc:postgresql://db.host:5432/custom");
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setDatasource(ds);

        String url = ReflectionTestUtils.invokeMethod(sqlExecutorService, "resolveJdbcUrl", config);

        assertThat(url).isEqualTo("jdbc:postgresql://db.host:5432/custom");
    }

    @Test
    void resolveJdbcUrl_withDatasourceHostPortDb_buildsUrl() {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setDbType(DbType.POSTGRESQL);
        ds.setHost("myhost");
        ds.setPort(6543);
        ds.setDatabaseName("mydb");
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setDatasource(ds);

        String url = ReflectionTestUtils.invokeMethod(sqlExecutorService, "resolveJdbcUrl", config);

        assertThat(url).isEqualTo("jdbc:postgresql://myhost:6543/mydb");
    }

    @Test
    void resolveJdbcUrl_withDatasourceNoPort_usesDefaultPort() {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setDbType(DbType.MYSQL);
        ds.setHost("myhost");
        ds.setDatabaseName("mydb");
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setDatasource(ds);

        String url = ReflectionTestUtils.invokeMethod(sqlExecutorService, "resolveJdbcUrl", config);

        assertThat(url).isEqualTo("jdbc:mysql://myhost:3306/mydb");
    }

    @Test
    void resolveJdbcUrl_inlineJdbcUrl_usesInline() {
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setInlineJdbcUrl("jdbc:postgresql://inline:5432/db");

        String url = ReflectionTestUtils.invokeMethod(sqlExecutorService, "resolveJdbcUrl", config);

        assertThat(url).isEqualTo("jdbc:postgresql://inline:5432/db");
    }

    @Test
    void resolveJdbcUrl_inlineDbTypeOnly_buildsDefaultLocalhostUrl() {
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setInlineDbType(DbType.POSTGRESQL);

        String url = ReflectionTestUtils.invokeMethod(sqlExecutorService, "resolveJdbcUrl", config);

        assertThat(url).isEqualTo("jdbc:postgresql://localhost:5432/");
    }

    // -------------------------------------------------------------------------
    // resolveUsername() / resolvePassword()
    // -------------------------------------------------------------------------

    @Test
    void resolveUsername_withDatasource_usesDatasourceUsername() {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setUsername("ds-user");
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setDatasource(ds);

        String username = ReflectionTestUtils.invokeMethod(sqlExecutorService, "resolveUsername", config);

        assertThat(username).isEqualTo("ds-user");
    }

    @Test
    void resolveUsername_inline_usesInlineUsername() {
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setInlineUsername("inline-user");

        String username = ReflectionTestUtils.invokeMethod(sqlExecutorService, "resolveUsername", config);

        assertThat(username).isEqualTo("inline-user");
    }

    @Test
    void resolvePassword_withDatasourceEncrypted_decryptsPassword() {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setPasswordEnc("enc-pw");
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setDatasource(ds);
        when(encryptionService.decrypt("enc-pw")).thenReturn("plain-pw");

        String password = ReflectionTestUtils.invokeMethod(sqlExecutorService, "resolvePassword", config);

        assertThat(password).isEqualTo("plain-pw");
    }

    @Test
    void resolvePassword_inlineEncrypted_decryptsPassword() {
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setInlinePasswordEnc("enc-inline");
        when(encryptionService.decrypt("enc-inline")).thenReturn("plain-inline");

        String password = ReflectionTestUtils.invokeMethod(sqlExecutorService, "resolvePassword", config);

        assertThat(password).isEqualTo("plain-inline");
    }

    @Test
    void resolvePassword_inlineNoPassword_returnsNull() {
        SchedulerSqlConfig config = new SchedulerSqlConfig();

        String password = ReflectionTestUtils.invokeMethod(sqlExecutorService, "resolvePassword", config);

        assertThat(password).isNull();
    }

    // -------------------------------------------------------------------------
    // getDriverClass()
    // -------------------------------------------------------------------------

    @Test
    void getDriverClass_nullDbType_returnsPostgresDefault() {
        String driver = ReflectionTestUtils.invokeMethod(
                sqlExecutorService, "getDriverClass", new Object[]{null});

        assertThat(driver).isEqualTo("org.postgresql.Driver");
    }

    @Test
    void getDriverClass_mysql_returnsMysqlDriver() {
        String driver = ReflectionTestUtils.invokeMethod(
                sqlExecutorService, "getDriverClass", DbType.MYSQL);

        assertThat(driver).isEqualTo("com.mysql.cj.jdbc.Driver");
    }

    // -------------------------------------------------------------------------
    // buildJdbcUrlFromDatasource()
    // -------------------------------------------------------------------------

    @Test
    void buildJdbcUrlFromDatasource_override_usesOverride() {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setDbType(DbType.MARIADB);
        ds.setJdbcUrlOverride("jdbc:mariadb://override/db");

        String url = ReflectionTestUtils.invokeMethod(
                sqlExecutorService, "buildJdbcUrlFromDatasource", ds);

        assertThat(url).isEqualTo("jdbc:mariadb://override/db");
    }

    @Test
    void buildJdbcUrlFromDatasource_noOverride_buildsFromParts() {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setDbType(DbType.MARIADB);
        ds.setHost("mariahost");
        ds.setPort(3307);
        ds.setDatabaseName("mariadb");

        String url = ReflectionTestUtils.invokeMethod(
                sqlExecutorService, "buildJdbcUrlFromDatasource", ds);

        assertThat(url).isEqualTo("jdbc:mariadb://mariahost:3307/mariadb");
    }
}
