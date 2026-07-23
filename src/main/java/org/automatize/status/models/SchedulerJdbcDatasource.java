package org.automatize.status.models;

import jakarta.persistence.*;
import org.automatize.status.models.scheduler.DbType;

import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing a reusable JDBC datasource configuration for scheduler SQL jobs.
 *
 * <p>Datasources are tenant-scoped and optionally scoped to an organisation.
 * Sensitive credentials (passwords) are stored in encrypted form via the
 * {@code password_enc} column and must be decrypted by the service layer
 * before use.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Entity
@Table(name = "scheduler_jdbc_datasources")
@EntityListeners(AuditTimestampListener.class)
public class SchedulerJdbcDatasource implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    @Getter
    @Setter
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @Getter
    @Setter
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    @Getter
    @Setter
    private Organization organization;

    @Column(name = "name", nullable = false, length = 255)
    @Getter
    @Setter
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    @Getter
    @Setter
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "db_type", nullable = false, length = 50)
    @Getter
    @Setter
    private DbType dbType;

    @Column(name = "host", length = 1024)
    @Getter
    @Setter
    private String host;

    @Column(name = "port")
    @Getter
    @Setter
    private Integer port;

    @Column(name = "database_name", length = 255)
    @Getter
    @Setter
    private String databaseName;

    @Column(name = "schema_name", length = 255)
    @Getter
    @Setter
    private String schemaName;

    @Column(name = "jdbc_url_override", length = 2048)
    @Getter
    @Setter
    private String jdbcUrlOverride;

    @Column(name = "username", length = 255)
    @Getter
    @Setter
    private String username;

    @Column(name = "password_enc", length = 2048)
    @Getter
    @Setter
    private String passwordEnc;

    @Column(name = "min_pool_size", nullable = false)
    @Getter
    @Setter
    private Integer minPoolSize = 1;

    @Column(name = "max_pool_size", nullable = false)
    @Getter
    @Setter
    private Integer maxPoolSize = 5;

    @Column(name = "connection_timeout_ms", nullable = false)
    @Getter
    @Setter
    private Integer connectionTimeoutMs = 5000;

    @Column(name = "extra_properties", columnDefinition = "TEXT")
    @Getter
    @Setter
    private String extraProperties;

    @Column(name = "enabled", nullable = false)
    @Getter
    @Setter
    private Boolean enabled = true;

    // Audit fields
    @Column(name = "created_by", length = 255)
    @Getter
    @Setter
    private String createdBy;

    @Column(name = "created_date")
    @Getter
    @Setter
    private ZonedDateTime createdDate;

    @Column(name = "created_date_technical")
    @Getter
    @Setter
    private Long createdDateTechnical;

    @Column(name = "last_modified_by", length = 255)
    @Getter
    @Setter
    private String lastModifiedBy;

    @Column(name = "last_modified_date")
    @Getter
    @Setter
    private ZonedDateTime lastModifiedDate;

    @Column(name = "last_modified_date_technical")
    @Getter
    @Setter
    private Long lastModifiedDateTechnical;

    /**
     * Default constructor required by JPA.
     */
    public SchedulerJdbcDatasource() {
    }
}
