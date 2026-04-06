package org.automatize.status.models;

import jakarta.persistence.*;
import org.automatize.status.models.scheduler.JsonMapConverter;
import org.automatize.status.models.scheduler.StringListConverter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Configuration entity for scheduler jobs of type {@code PROGRAM}.
 *
 * <p>Stores the command, arguments, working directory, environment variables,
 * and optional shell-wrapping settings required to execute an OS-level program
 * or script.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Entity
@Table(name = "scheduler_program_configs")
public class SchedulerProgramConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private SchedulerJob job;

    @Column(name = "command", length = 2048)
    private String command;

    @Convert(converter = StringListConverter.class)
    @Column(name = "arguments", columnDefinition = "TEXT")
    private List<String> arguments;

    @Column(name = "working_directory", length = 1024)
    private String workingDirectory;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "environment_vars", columnDefinition = "TEXT")
    private Map<String, String> environmentVars;

    @Column(name = "shell_wrap", nullable = false)
    private Boolean shellWrap = false;

    @Column(name = "shell_path", length = 512)
    private String shellPath = "/bin/bash";

    @Column(name = "run_as_user", length = 255)
    private String runAsUser;

    public SchedulerProgramConfig() {
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

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public Map<String, String> getEnvironmentVars() {
        return environmentVars;
    }

    public void setEnvironmentVars(Map<String, String> environmentVars) {
        this.environmentVars = environmentVars;
    }

    public Boolean getShellWrap() {
        return shellWrap;
    }

    public void setShellWrap(Boolean shellWrap) {
        this.shellWrap = shellWrap;
    }

    public String getShellPath() {
        return shellPath;
    }

    public void setShellPath(String shellPath) {
        this.shellPath = shellPath;
    }

    public String getRunAsUser() {
        return runAsUser;
    }

    public void setRunAsUser(String runAsUser) {
        this.runAsUser = runAsUser;
    }
}
