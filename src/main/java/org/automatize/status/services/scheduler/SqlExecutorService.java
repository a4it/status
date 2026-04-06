package org.automatize.status.services.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.automatize.status.models.SchedulerJdbcDatasource;
import org.automatize.status.models.SchedulerJobRun;
import org.automatize.status.models.SchedulerSqlConfig;
import org.automatize.status.models.scheduler.DbType;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.automatize.status.models.scheduler.SqlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

/**
 * Executor service for scheduler jobs of type {@code SQL}.
 *
 * <p>Opens a JDBC connection (via a shared {@link SchedulerJdbcDatasource} or
 * inline credentials), runs the configured SQL statement, and writes the outcome
 * to the supplied {@link SchedulerJobRun}. Also exposes a
 * {@link #testConnection(SchedulerJdbcDatasource)} utility used by the datasource
 * management API.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Service
public class SqlExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(SqlExecutorService.class);

    @Autowired
    private SchedulerEncryptionService encryptionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Executes the SQL job defined by {@code config} and writes the result into {@code run}.
     *
     * @param config the SQL configuration; if {@code null} the run is marked as FAILURE
     * @param run    the run record to populate with outcome details
     */
    public void execute(SchedulerSqlConfig config, SchedulerJobRun run) {
        if (config == null) {
            run.setStatus(JobRunStatus.FAILURE);
            run.setErrorMessage("SQL configuration is missing");
            return;
        }

        String jdbcUrl = resolveJdbcUrl(config);
        String username = resolveUsername(config);
        String password = resolvePassword(config);
        int queryTimeout = config.getQueryTimeoutSeconds() != null ? config.getQueryTimeoutSeconds() : 60;
        int maxRows = config.getMaxResultRows() != null ? config.getMaxResultRows() : 100;

        try {
            loadDriver(config);

            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
                 Statement stmt = conn.createStatement()) {

                stmt.setQueryTimeout(queryTimeout);
                SqlType sqlType = config.getSqlType() != null ? config.getSqlType() : SqlType.DML;

                if (sqlType == SqlType.QUERY) {
                    stmt.setMaxRows(maxRows);
                    ResultSet rs = stmt.executeQuery(config.getSqlStatement());
                    String json = resultSetToJson(rs, maxRows);
                    run.setResponseBody(json);
                    long rowCount = 0;
                    try {
                        List<?> rows = objectMapper.readValue(json, List.class);
                        rowCount = rows.size();
                    } catch (Exception ignored) {
                    }
                    run.setRowsAffected(rowCount);
                    run.setStatus(JobRunStatus.SUCCESS);

                } else if (sqlType == SqlType.DML) {
                    long rows = stmt.executeLargeUpdate(config.getSqlStatement());
                    run.setRowsAffected(rows);
                    run.setStatus(JobRunStatus.SUCCESS);

                } else {
                    // DDL
                    stmt.execute(config.getSqlStatement());
                    run.setStatus(JobRunStatus.SUCCESS);
                }
            }
        } catch (SQLTimeoutException e) {
            run.setStatus(JobRunStatus.TIMEOUT);
            run.setErrorMessage("Query timed out: " + e.getMessage());
        } catch (Exception e) {
            logger.error("SQL execution failed", e);
            run.setStatus(JobRunStatus.FAILURE);
            run.setErrorMessage(e.getMessage());
        }
    }

    /**
     * Tests connectivity to the given datasource and returns a result map with
     * {@code success} (boolean), optional {@code error} (string), and {@code latencyMs} (long).
     *
     * @param datasource the datasource to test
     * @return result map
     */
    public Map<String, Object> testConnection(SchedulerJdbcDatasource datasource) {
        Map<String, Object> result = new HashMap<>();
        long start = System.currentTimeMillis();
        String jdbcUrl = buildJdbcUrlFromDatasource(datasource);
        String password = datasource.getPasswordEnc() != null
                ? encryptionService.decrypt(datasource.getPasswordEnc())
                : null;

        try {
            Class.forName(getDriverClass(datasource.getDbType()));
            try (Connection conn = DriverManager.getConnection(jdbcUrl, datasource.getUsername(), password)) {
                conn.isValid(5);
                result.put("success", true);
                result.put("latencyMs", System.currentTimeMillis() - start);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("latencyMs", System.currentTimeMillis() - start);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String resolveJdbcUrl(SchedulerSqlConfig config) {
        if (config.getDatasource() != null) {
            return buildJdbcUrlFromDatasource(config.getDatasource());
        }
        if (config.getInlineJdbcUrl() != null && !config.getInlineJdbcUrl().isBlank()) {
            return config.getInlineJdbcUrl();
        }
        DbType dbType = config.getInlineDbType();
        if (dbType == null) throw new RuntimeException("No datasource or inline JDBC config provided");
        return dbType.buildJdbcUrl("localhost", dbType.getDefaultPort(), "");
    }

    private String resolveUsername(SchedulerSqlConfig config) {
        if (config.getDatasource() != null) return config.getDatasource().getUsername();
        return config.getInlineUsername();
    }

    private String resolvePassword(SchedulerSqlConfig config) {
        if (config.getDatasource() != null) {
            String enc = config.getDatasource().getPasswordEnc();
            return enc != null ? encryptionService.decrypt(enc) : null;
        }
        String enc = config.getInlinePasswordEnc();
        return enc != null ? encryptionService.decrypt(enc) : null;
    }

    private String buildJdbcUrlFromDatasource(SchedulerJdbcDatasource ds) {
        if (ds.getJdbcUrlOverride() != null && !ds.getJdbcUrlOverride().isBlank()) {
            return ds.getJdbcUrlOverride();
        }
        int port = ds.getPort() != null ? ds.getPort() : ds.getDbType().getDefaultPort();
        return ds.getDbType().buildJdbcUrl(ds.getHost(), port, ds.getDatabaseName());
    }

    private void loadDriver(SchedulerSqlConfig config) throws ClassNotFoundException {
        DbType dbType = config.getDatasource() != null
                ? config.getDatasource().getDbType()
                : config.getInlineDbType();
        if (dbType != null) {
            Class.forName(getDriverClass(dbType));
        }
    }

    private String getDriverClass(DbType dbType) {
        if (dbType == null) return "org.postgresql.Driver";
        return dbType.getDriverClass();
    }

    private String resultSetToJson(ResultSet rs, int maxRows) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();
        int count = 0;
        while (rs.next() && count < maxRows) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= colCount; i++) {
                row.put(meta.getColumnName(i), rs.getObject(i));
            }
            rows.add(row);
            count++;
        }
        return objectMapper.writeValueAsString(rows);
    }
}
