package org.automatize.status.services.scheduler;

import org.automatize.status.models.SchedulerJobRun;
import org.automatize.status.models.SchedulerProgramConfig;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Executor service for scheduler jobs of type {@code PROGRAM}.
 *
 * <p>Launches an OS-level process, streams stdout/stderr with an optional
 * byte cap, waits for the process to exit (honouring the per-job timeout),
 * and writes the outcome back to the supplied {@link SchedulerJobRun}.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Service
public class ProgramExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(ProgramExecutorService.class);

    @Value("${scheduler.program.max-output-bytes:102400}")
    private int maxOutputBytes;

    /**
     * Executes the program defined by {@code config} and writes the result into {@code run}.
     *
     * @param config the program configuration; if {@code null} the run is marked as FAILURE
     * @param run    the run record to populate with outcome details
     */
    public void execute(SchedulerProgramConfig config, SchedulerJobRun run) {
        if (config == null) {
            run.setStatus(JobRunStatus.FAILURE);
            run.setErrorMessage("Program configuration is missing");
            return;
        }

        int timeoutSeconds = run.getJob().getTimeoutSeconds() != null ? run.getJob().getTimeoutSeconds() : 300;

        try {
            List<String> command = buildCommand(config);
            ProcessBuilder pb = new ProcessBuilder(command);

            if (config.getWorkingDirectory() != null && !config.getWorkingDirectory().isBlank()) {
                pb.directory(new java.io.File(config.getWorkingDirectory()));
            }

            if (config.getEnvironmentVars() != null && !config.getEnvironmentVars().isEmpty()) {
                pb.environment().putAll(config.getEnvironmentVars());
            }

            pb.redirectErrorStream(false);
            Process process = pb.start();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread stdoutThread = new Thread(() -> readStream(process.getInputStream(), stdout, maxOutputBytes));
            Thread stderrThread = new Thread(() -> readStream(process.getErrorStream(), stderr, maxOutputBytes));
            stdoutThread.start();
            stderrThread.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            stdoutThread.join(5000);
            stderrThread.join(5000);

            if (!finished) {
                process.destroyForcibly();
                run.setStatus(JobRunStatus.TIMEOUT);
                run.setErrorMessage("Process timed out after " + timeoutSeconds + " seconds");
                run.setStdoutOutput(stdout.toString());
                run.setStderrOutput(stderr.toString());
                return;
            }

            int exitCode = process.exitValue();
            run.setExitCode(exitCode);
            run.setStdoutOutput(stdout.toString());
            run.setStderrOutput(stderr.toString());
            run.setStatus(exitCode == 0 ? JobRunStatus.SUCCESS : JobRunStatus.FAILURE);
            if (exitCode != 0) {
                run.setErrorMessage("Process exited with code " + exitCode);
            }

        } catch (Exception e) {
            logger.error("Program execution failed", e);
            run.setStatus(JobRunStatus.FAILURE);
            run.setErrorMessage(e.getMessage());
        }
    }

    /**
     * Builds the OS command list from the program configuration.
     * When {@code shellWrap} is {@code true} the command is wrapped in the configured shell
     * (defaulting to {@code /bin/bash -c}). Otherwise the command and arguments are passed
     * directly to {@link ProcessBuilder}.
     */
    private List<String> buildCommand(SchedulerProgramConfig config) {
        List<String> command = new ArrayList<>();
        if (Boolean.TRUE.equals(config.getShellWrap())) {
            String shell = config.getShellPath() != null ? config.getShellPath() : "/bin/bash";
            command.add(shell);
            command.add("-c");
            StringBuilder cmd = new StringBuilder(config.getCommand());
            if (config.getArguments() != null) {
                for (String arg : config.getArguments()) {
                    cmd.append(" ").append(arg);
                }
            }
            command.add(cmd.toString());
        } else {
            command.add(config.getCommand());
            if (config.getArguments() != null) {
                command.addAll(config.getArguments());
            }
        }
        return command;
    }

    /**
     * Reads lines from {@code stream} into {@code sb} up to {@code maxBytes} total characters.
     */
    private void readStream(InputStream stream, StringBuilder sb, int maxBytes) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() < maxBytes) {
                    sb.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            logger.debug("Error reading process stream", e);
        }
    }
}
