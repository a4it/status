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

    public SchedulerSqlConfig() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SchedulerJob getJob() {
        return job;
    }

    public void setJob(SchedulerJob job) {
        this.job = job;
    }

    public SchedulerJdbcDatasource getDatasource() {
        return datasource;
    }

    public void setDatasource(SchedulerJdbcDatasource datasource) {
        this.datasource = datasource;
    }

    public DbType getInlineDbType() {
        return inlineDbType;
    }

    public void setInlineDbType(DbType inlineDbType) {
        this.inlineDbType = inlineDbType;
    }

    public String getInlineJdbcUrl() {
        return inlineJdbcUrl;
    }

    public void setInlineJdbcUrl(String inlineJdbcUrl) {
        this.inlineJdbcUrl = inlineJdbcUrl;
    }

    public String getInlineUsername() {
        return inlineUsername;
    }

    public void setInlineUsername(String inlineUsername) {
        this.inlineUsername = inlineUsername;
    }

    public String getInlinePasswordEnc() {
        return inlinePasswordEnc;
    }

    public void setInlinePasswordEnc(String inlinePasswordEnc) {
        this.inlinePasswordEnc = inlinePasswordEnc;
    }

    public String getSqlStatement() {
        return sqlStatement;
    }

    public void setSqlStatement(String sqlStatement) {
        this.sqlStatement = sqlStatement;
    }

    public SqlType getSqlType() {
        return sqlType;
    }

    public void setSqlType(SqlType sqlType) {
        this.sqlType = sqlType;
    }

    public Boolean getCaptureResultSet() {
        return captureResultSet;
    }

    public void setCaptureResultSet(Boolean captureResultSet) {
        this.captureResultSet = captureResultSet;
    }

    public Integer getMaxResultRows() {
        return maxResultRows;
    }

    public void setMaxResultRows(Integer maxResultRows) {
        this.maxResultRows = maxResultRows;
    }

    public Integer getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(Integer connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public Integer getQueryTimeoutSeconds() {
        return queryTimeoutSeconds;
    }

    public void setQueryTimeoutSeconds(Integer queryTimeoutSeconds) {
        this.queryTimeoutSeconds = queryTimeoutSeconds;
    }
}
