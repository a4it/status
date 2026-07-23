package org.automatize.status.models;

import jakarta.persistence.*;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.automatize.status.models.scheduler.JobStatus;
import org.automatize.status.models.scheduler.JobType;
import org.automatize.status.models.scheduler.StringListConverter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing a scheduled job definition.
 *
 * <p>A job defines what work to perform (via job_type), when to perform it
 * (cron_expression), and how to handle failures (retry/timeout settings).
 * The type-specific configuration lives in one of the four config tables
 * (program, sql, rest, soap), linked via one-to-one relationships.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Entity
@Table(name = "scheduler_jobs")
@EntityListeners(AuditTimestampListener.class)
public class SchedulerJob implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    @Getter
    @Setter
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
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
    @Column(name = "job_type", nullable = false, length = 50)
    @Getter
    @Setter
    private JobType jobType;

    @Column(name = "cron_expression", nullable = false, length = 255)
    @Getter
    @Setter
    private String cronExpression;

    @Column(name = "time_zone", nullable = false, length = 100)
    @Getter
    @Setter
    private String timeZone = "UTC";

    @Column(name = "enabled", nullable = false)
    @Getter
    @Setter
    private Boolean enabled = true;

    @Column(name = "allow_concurrent", nullable = false)
    @Getter
    @Setter
    private Boolean allowConcurrent = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Getter
    @Setter
    private JobStatus status = JobStatus.ACTIVE;

    @Column(name = "last_run_at")
    @Getter
    @Setter
    private ZonedDateTime lastRunAt;

    @Column(name = "next_run_at")
    @Getter
    @Setter
    private ZonedDateTime nextRunAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_run_status", length = 50)
    @Getter
    @Setter
    private JobRunStatus lastRunStatus;

    @Column(name = "consecutive_failures", nullable = false)
    @Getter
    @Setter
    private Integer consecutiveFailures = 0;

    @Column(name = "max_retry_attempts", nullable = false)
    @Getter
    @Setter
    private Integer maxRetryAttempts = 0;

    @Column(name = "retry_delay_seconds", nullable = false)
    @Getter
    @Setter
    private Integer retryDelaySeconds = 60;

    @Column(name = "timeout_seconds", nullable = false)
    @Getter
    @Setter
    private Integer timeoutSeconds = 300;

    @Column(name = "max_output_bytes", nullable = false)
    @Getter
    @Setter
    private Integer maxOutputBytes = 102400;

    @Convert(converter = StringListConverter.class)
    @Column(name = "tags", columnDefinition = "TEXT")
    @Getter
    @Setter
    private List<String> tags;

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

    // Config relationships
    @OneToOne(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Getter
    @Setter
    private SchedulerProgramConfig programConfig;

    @OneToOne(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Getter
    @Setter
    private SchedulerSqlConfig sqlConfig;

    @OneToOne(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Getter
    @Setter
    private SchedulerRestConfig restConfig;

    @OneToOne(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Getter
    @Setter
    private SchedulerSoapConfig soapConfig;

    /**
     * Default constructor required by JPA.
     */
    public SchedulerJob() {
    }
}
