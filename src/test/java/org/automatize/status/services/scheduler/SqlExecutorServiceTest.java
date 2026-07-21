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

    /** Name of the private {@code resolveJdbcUrl} method under reflective test. */
    private static final String METHOD_RESOLVE_JDBC_URL = "resolveJdbcUrl";

    /** Name of the private {@code resolvePassword} method under reflective test. */
    private static final String METHOD_RESOLVE_PASSWORD = "resolvePassword";

    @Mock
    private SchedulerEncryptionService encryptionService;

    @InjectMocks
    private SqlExecutorService sqlExecutorService;

    // -------------------------------------------------------------------------
    // execute() - null config guard
    // -------------------------------------------------------------------------

    /**
     * Verifies the null-config guard on {@code execute}.
     * Expected outcome: run status is FAILURE with a "SQL configuration is missing" message.
     */
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

    /**
     * Verifies that a config with neither a datasource nor inline JDBC settings is rejected.
     * Expected outcome: {@link IllegalArgumentException} propagates before the guarded try block.
     */
    @Test
    void execute_noDatasourceNoInlineConfig_throwsIllegalArgument() {
        // URL resolution happens before the guarded try block, so the exception propagates.
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        SchedulerJobRun run = new SchedulerJobRun();

        assertThatThrownBy(() -> sqlExecutorService.execute(config, run))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No datasource or inline JDBC config provided");
    }

    /**
     * Verifies a JDBC URL that no driver accepts causes a connection failure recorded on the run.
     * Expected outcome: run is FAILURE with a non-blank error and no rows affected.
     */
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

    /**
     * Verifies {@code testConnection} against an unreachable URL reports failure with diagnostics.
     * Expected outcome: result has success=false, a non-null error and a numeric latency.
     */
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

    /**
     * Verifies URL resolution prefers a datasource's explicit JDBC URL override.
     * Expected outcome: the override URL is returned verbatim.
     */
    @Test
    void resolveJdbcUrl_withDatasourceOverride_usesOverride() {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setDbType(DbType.POSTGRESQL);
        ds.setJdbcUrlOverride("jdbc:postgresql://db.host:5432/custom");
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setDatasource(ds);

        String url = ReflectionTestUtils.invokeMethod(sqlExecutorService, METHOD_RESOLVE_JDBC_URL, config);

        assertThat(url).isEqualTo("jdbc:postgresql://db.host:5432/custom");
    }

    /**
     * Verifies URL resolution builds a URL from a datasource's host, port and database.
     * Expected outcome: a well-formed {@code jdbc:postgresql://host:port/db} URL.
     */
    @Test
    void resolveJdbcUrl_withDatasourceHostPortDb_buildsUrl() {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setDbType(DbType.POSTGRESQL);
        ds.setHost("myhost");
        ds.setPort(6543);
        ds.setDatabaseName("mydb");
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setDatasource(ds);

        String url = ReflectionTestUtils.invokeMethod(sqlExecutorService, METHOD_RESOLVE_JDBC_URL, config);

        assertThat(url).isEqualTo("jdbc:postgresql://myhost:6543/mydb");
    }

    /**
     * Verifies URL resolution substitutes the DB type's default port when none is set.
     * Expected outcome: the MySQL default port 3306 is used.
     */
    @Test
    void resolveJdbcUrl_withDatasourceNoPort_usesDefaultPort() {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setDbType(DbType.MYSQL);
        ds.setHost("myhost");
        ds.setDatabaseName("mydb");
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setDatasource(ds);

        String url = ReflectionTestUtils.invokeMethod(sqlExecutorService, METHOD_RESOLVE_JDBC_URL, config);

        assertThat(url).isEqualTo("jdbc:mysql://myhost:3306/mydb");
    }

    /**
     * Verifies URL resolution uses an inline JDBC URL when no datasource is present.
     * Expected outcome: the inline URL is returned verbatim.
     */
    @Test
    void resolveJdbcUrl_inlineJdbcUrl_usesInline() {
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setInlineJdbcUrl("jdbc:postgresql://inline:5432/db");

        String url = ReflectionTestUtils.invokeMethod(sqlExecutorService, METHOD_RESOLVE_JDBC_URL, config);

        assertThat(url).isEqualTo("jdbc:postgresql://inline:5432/db");
    }

    /**
     * Verifies URL resolution builds a default localhost URL from only an inline DB type.
     * Expected outcome: a {@code jdbc:postgresql://localhost:5432/} URL.
     */
    @Test
    void resolveJdbcUrl_inlineDbTypeOnly_buildsDefaultLocalhostUrl() {
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setInlineDbType(DbType.POSTGRESQL);

        String url = ReflectionTestUtils.invokeMethod(sqlExecutorService, METHOD_RESOLVE_JDBC_URL, config);

        assertThat(url).isEqualTo("jdbc:postgresql://localhost:5432/");
    }

    // -------------------------------------------------------------------------
    // resolveUsername() / resolvePassword()
    // -------------------------------------------------------------------------

    /**
     * Verifies username resolution prefers the datasource's username.
     * Expected outcome: the datasource username is returned.
     */
    @Test
    void resolveUsername_withDatasource_usesDatasourceUsername() {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setUsername("ds-user");
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setDatasource(ds);

        String username = ReflectionTestUtils.invokeMethod(sqlExecutorService, "resolveUsername", config);

        assertThat(username).isEqualTo("ds-user");
    }

    /**
     * Verifies username resolution falls back to the inline username when no datasource is set.
     * Expected outcome: the inline username is returned.
     */
    @Test
    void resolveUsername_inline_usesInlineUsername() {
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setInlineUsername("inline-user");

        String username = ReflectionTestUtils.invokeMethod(sqlExecutorService, "resolveUsername", config);

        assertThat(username).isEqualTo("inline-user");
    }

    /**
     * Verifies password resolution decrypts the datasource's encrypted password.
     * Expected outcome: the decrypted plaintext is returned.
     */
    @Test
    void resolvePassword_withDatasourceEncrypted_decryptsPassword() {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setPasswordEnc("enc-pw");
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setDatasource(ds);
        when(encryptionService.decrypt("enc-pw")).thenReturn("plain-pw");

        String password = ReflectionTestUtils.invokeMethod(sqlExecutorService, METHOD_RESOLVE_PASSWORD, config);

        assertThat(password).isEqualTo("plain-pw");
    }

    /**
     * Verifies password resolution decrypts the inline encrypted password when no datasource is set.
     * Expected outcome: the decrypted plaintext is returned.
     */
    @Test
    void resolvePassword_inlineEncrypted_decryptsPassword() {
        SchedulerSqlConfig config = new SchedulerSqlConfig();
        config.setInlinePasswordEnc("enc-inline");
        when(encryptionService.decrypt("enc-inline")).thenReturn("plain-inline");

        String password = ReflectionTestUtils.invokeMethod(sqlExecutorService, METHOD_RESOLVE_PASSWORD, config);

        assertThat(password).isEqualTo("plain-inline");
    }

    /**
     * Verifies password resolution returns {@code null} when no password is configured anywhere.
     * Expected outcome: {@code null} is returned.
     */
    @Test
    void resolvePassword_inlineNoPassword_returnsNull() {
        SchedulerSqlConfig config = new SchedulerSqlConfig();

        String password = ReflectionTestUtils.invokeMethod(sqlExecutorService, METHOD_RESOLVE_PASSWORD, config);

        assertThat(password).isNull();
    }

    // -------------------------------------------------------------------------
    // getDriverClass()
    // -------------------------------------------------------------------------

    /**
     * Verifies driver-class resolution defaults to the PostgreSQL driver for a {@code null} DB type.
     * Expected outcome: {@code org.postgresql.Driver} is returned.
     */
    @Test
    void getDriverClass_nullDbType_returnsPostgresDefault() {
        String driver = ReflectionTestUtils.invokeMethod(
                sqlExecutorService, "getDriverClass", new Object[]{null});

        assertThat(driver).isEqualTo("org.postgresql.Driver");
    }

    /**
     * Verifies driver-class resolution returns the MySQL driver for the MYSQL DB type.
     * Expected outcome: {@code com.mysql.cj.jdbc.Driver} is returned.
     */
    @Test
    void getDriverClass_mysql_returnsMysqlDriver() {
        String driver = ReflectionTestUtils.invokeMethod(
                sqlExecutorService, "getDriverClass", DbType.MYSQL);

        assertThat(driver).isEqualTo("com.mysql.cj.jdbc.Driver");
    }

    // -------------------------------------------------------------------------
    // buildJdbcUrlFromDatasource()
    // -------------------------------------------------------------------------

    /**
     * Verifies datasource URL building prefers an explicit JDBC URL override.
     * Expected outcome: the override URL is returned verbatim.
     */
    @Test
    void buildJdbcUrlFromDatasource_override_usesOverride() {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setDbType(DbType.MARIADB);
        ds.setJdbcUrlOverride("jdbc:mariadb://override/db");

        String url = ReflectionTestUtils.invokeMethod(
                sqlExecutorService, "buildJdbcUrlFromDatasource", ds);

        assertThat(url).isEqualTo("jdbc:mariadb://override/db");
    }

    /**
     * Verifies datasource URL building assembles the URL from host/port/database when no override is set.
     * Expected outcome: a well-formed {@code jdbc:mariadb://host:port/db} URL.
     */
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
