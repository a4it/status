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

    /**
     * Default constructor required by JPA.
     */
    public SchedulerProgramConfig() {
    }

    /**
     * Gets the unique identifier of the program configuration.
     *
     * @return the UUID of the configuration
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the program configuration.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the job this configuration belongs to.
     *
     * @return the associated {@link SchedulerJob}
     */
    public SchedulerJob getJob() {
        return job;
    }

    /**
     * Sets the job this configuration belongs to.
     *
     * @param job the {@link SchedulerJob} to associate
     */
    public void setJob(SchedulerJob job) {
        this.job = job;
    }

    /**
     * Gets the command or executable path to run.
     *
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * Sets the command or executable path to run.
     *
     * @param command the command to set
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * Gets the list of command-line arguments.
     *
     * @return the argument list
     */
    public List<String> getArguments() {
        return arguments;
    }

    /**
     * Sets the list of command-line arguments.
     *
     * @param arguments the argument list to set
     */
    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    /**
     * Gets the working directory in which the command is executed.
     *
     * @return the working directory path
     */
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Sets the working directory in which the command is executed.
     *
     * @param workingDirectory the working directory path to set
     */
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * Gets the environment variables passed to the process.
     *
     * @return the map of environment variable names to values
     */
    public Map<String, String> getEnvironmentVars() {
        return environmentVars;
    }

    /**
     * Sets the environment variables passed to the process.
     *
     * @param environmentVars the map of environment variable names to values to set
     */
    public void setEnvironmentVars(Map<String, String> environmentVars) {
        this.environmentVars = environmentVars;
    }

    /**
     * Indicates whether the command is wrapped in a shell invocation.
     *
     * @return {@code true} if shell-wrapped, {@code false} otherwise
     */
    public Boolean getShellWrap() {
        return shellWrap;
    }

    /**
     * Sets whether the command is wrapped in a shell invocation.
     *
     * @param shellWrap the shell-wrap flag to set
     */
    public void setShellWrap(Boolean shellWrap) {
        this.shellWrap = shellWrap;
    }

    /**
     * Gets the path to the shell used when shell-wrapping is enabled.
     *
     * @return the shell path
     */
    public String getShellPath() {
        return shellPath;
    }

    /**
     * Sets the path to the shell used when shell-wrapping is enabled.
     *
     * @param shellPath the shell path to set
     */
    public void setShellPath(String shellPath) {
        this.shellPath = shellPath;
    }

    /**
     * Gets the OS user the process should run as, if specified.
     *
     * @return the run-as user, or {@code null} to use the default
     */
    public String getRunAsUser() {
        return runAsUser;
    }

    /**
     * Sets the OS user the process should run as.
     *
     * @param runAsUser the run-as user to set
     */
    public void setRunAsUser(String runAsUser) {
        this.runAsUser = runAsUser;
    }
}
