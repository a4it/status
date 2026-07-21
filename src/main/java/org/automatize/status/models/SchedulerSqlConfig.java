package org.automatize.status.models;

import jakarta.persistence.*;
import org.automatize.status.models.scheduler.DbType;
import org.automatize.status.models.scheduler.SqlType;

import java.util.UUID;

/**
 * Configuration entity for scheduler jobs of type {@code SQL}.
 *
 * <p>A SQL job can connect either via a shared {@link SchedulerJdbcDatasource}
 * (referenced by {@code datasource}) or via inline credentials stored directly
 * on this entity. If both are provided the shared datasource takes precedence
 * (enforced by the service layer).</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Entity
@Table(name = "scheduler_sql_configs")
public class SchedulerSqlConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private SchedulerJob job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datasource_id")
    private SchedulerJdbcDatasource datasource;

    @Enumerated(EnumType.STRING)
    @Column(name = "inline_db_type", length = 50)
    private DbType inlineDbType;

    @Column(name = "inline_jdbc_url", length = 2048)
    private String inlineJdbcUrl;

    @Column(name = "inline_username", length = 255)
    private String inlineUsername;

    @Column(name = "inline_password_enc", length = 2048)
    private String inlinePasswordEnc;

    @Column(name = "sql_statement", columnDefinition = "TEXT")
    private String sqlStatement;

    @Enumerated(EnumType.STRING)
    @Column(name = "sql_type", nullable = false, length = 50)
    private SqlType sqlType = SqlType.DML;

    @Column(name = "capture_result_set", nullable = false)
    private Boolean captureResultSet = false;

    @Column(name = "max_result_rows", nullable = false)
    private Integer maxResultRows = 100;

    @Column(name = "connection_timeout_ms", nullable = false)
    private Integer connectionTimeoutMs = 5000;

    @Column(name = "query_timeout_seconds", nullable = false)
    private Integer queryTimeoutSeconds = 60;

    /**
     * Default constructor required by JPA.
     */
    public SchedulerSqlConfig() {
    }

    /**
     * Gets the unique identifier of this SQL configuration.
     *
     * @return the UUID of the configuration
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of this SQL configuration.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the scheduler job that owns this configuration.
     *
     * @return the associated SchedulerJob
     */
    public SchedulerJob getJob() {
        return job;
    }

    /**
     * Sets the scheduler job that owns this configuration.
     *
     * @param job the SchedulerJob to set
     */
    public void setJob(SchedulerJob job) {
        this.job = job;
    }

    /**
     * Gets the shared JDBC datasource used for the connection.
     *
     * @return the associated SchedulerJdbcDatasource, or null when inline credentials are used
     */
    public SchedulerJdbcDatasource getDatasource() {
        return datasource;
    }

    /**
     * Sets the shared JDBC datasource used for the connection.
     *
     * @param datasource the SchedulerJdbcDatasource to set
     */
    public void setDatasource(SchedulerJdbcDatasource datasource) {
        this.datasource = datasource;
    }

    /**
     * Gets the database type for the inline connection.
     *
     * @return the inline database type
     */
    public DbType getInlineDbType() {
        return inlineDbType;
    }

    /**
     * Sets the database type for the inline connection.
     *
     * @param inlineDbType the inline database type to set
     */
    public void setInlineDbType(DbType inlineDbType) {
        this.inlineDbType = inlineDbType;
    }

    /**
     * Gets the JDBC URL for the inline connection.
     *
     * @return the inline JDBC URL
     */
    public String getInlineJdbcUrl() {
        return inlineJdbcUrl;
    }

    /**
     * Sets the JDBC URL for the inline connection.
     *
     * @param inlineJdbcUrl the inline JDBC URL to set
     */
    public void setInlineJdbcUrl(String inlineJdbcUrl) {
        this.inlineJdbcUrl = inlineJdbcUrl;
    }

    /**
     * Gets the username for the inline connection.
     *
     * @return the inline username
     */
    public String getInlineUsername() {
        return inlineUsername;
    }

    /**
     * Sets the username for the inline connection.
     *
     * @param inlineUsername the inline username to set
     */
    public void setInlineUsername(String inlineUsername) {
        this.inlineUsername = inlineUsername;
    }

    /**
     * Gets the encrypted password for the inline connection.
     *
     * @return the encrypted inline password
     */
    public String getInlinePasswordEnc() {
        return inlinePasswordEnc;
    }

    /**
     * Sets the encrypted password for the inline connection.
     *
     * @param inlinePasswordEnc the encrypted inline password to set
     */
    public void setInlinePasswordEnc(String inlinePasswordEnc) {
        this.inlinePasswordEnc = inlinePasswordEnc;
    }

    /**
     * Gets the SQL statement to execute.
     *
     * @return the SQL statement
     */
    public String getSqlStatement() {
        return sqlStatement;
    }

    /**
     * Sets the SQL statement to execute.
     *
     * @param sqlStatement the SQL statement to set
     */
    public void setSqlStatement(String sqlStatement) {
        this.sqlStatement = sqlStatement;
    }

    /**
     * Gets the type of SQL statement (e.g., DML or query).
     *
     * @return the SQL type
     */
    public SqlType getSqlType() {
        return sqlType;
    }

    /**
     * Sets the type of SQL statement (e.g., DML or query).
     *
     * @param sqlType the SQL type to set
     */
    public void setSqlType(SqlType sqlType) {
        this.sqlType = sqlType;
    }

    /**
     * Checks whether the result set is captured after execution.
     *
     * @return true if the result set is captured, false otherwise
     */
    public Boolean getCaptureResultSet() {
        return captureResultSet;
    }

    /**
     * Sets whether the result set is captured after execution.
     *
     * @param captureResultSet the capture-result-set flag to set
     */
    public void setCaptureResultSet(Boolean captureResultSet) {
        this.captureResultSet = captureResultSet;
    }

    /**
     * Gets the maximum number of result rows to capture.
     *
     * @return the maximum result row count
     */
    public Integer getMaxResultRows() {
        return maxResultRows;
    }

    /**
     * Sets the maximum number of result rows to capture.
     *
     * @param maxResultRows the maximum result row count to set
     */
    public void setMaxResultRows(Integer maxResultRows) {
        this.maxResultRows = maxResultRows;
    }

    /**
     * Gets the connection timeout in milliseconds.
     *
     * @return the connection timeout in milliseconds
     */
    public Integer getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    /**
     * Sets the connection timeout in milliseconds.
     *
     * @param connectionTimeoutMs the connection timeout in milliseconds to set
     */
    public void setConnectionTimeoutMs(Integer connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    /**
     * Gets the query execution timeout in seconds.
     *
     * @return the query timeout in seconds
     */
    public Integer getQueryTimeoutSeconds() {
        return queryTimeoutSeconds;
    }

    /**
     * Sets the query execution timeout in seconds.
     *
     * @param queryTimeoutSeconds the query timeout in seconds to set
     */
    public void setQueryTimeoutSeconds(Integer queryTimeoutSeconds) {
        this.queryTimeoutSeconds = queryTimeoutSeconds;
    }
}
