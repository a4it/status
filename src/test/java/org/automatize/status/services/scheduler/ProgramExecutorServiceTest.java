package org.automatize.status.services.scheduler;

import org.automatize.status.models.SchedulerJobRun;
import org.automatize.status.models.SchedulerProgramConfig;
import org.automatize.status.models.scheduler.JobRunStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProgramExecutorService}.
 *
 * <p>The happy-path {@code execute} launches a real OS process via
 * {@link ProcessBuilder} and therefore needs integration coverage. These tests
 * cover the null-config guard, OS command assembly ({@code buildCommand} for both
 * direct and shell-wrapped modes) and the bounded stream reader
 * ({@code readStream}) using in-memory streams only.</p>
 */
@ExtendWith(MockitoExtension.class)
class ProgramExecutorServiceTest {

    @InjectMocks
    private ProgramExecutorService programExecutorService;

    // -------------------------------------------------------------------------
    // execute() - null config guard
    // -------------------------------------------------------------------------

    /**
     * Verifies the null-config guard on {@code execute}.
     * Expected outcome: run status is FAILURE with a "configuration is missing" message.
     */
    @Test
    void execute_nullConfig_setsFailureWithMessage() {
        SchedulerJobRun run = new SchedulerJobRun();

        programExecutorService.execute(null, run);

        assertThat(run.getStatus()).isEqualTo(JobRunStatus.FAILURE);
        assertThat(run.getErrorMessage()).isEqualTo("Program configuration is missing");
    }

    // -------------------------------------------------------------------------
    // buildCommand()
    // -------------------------------------------------------------------------

    /**
     * Verifies command assembly without shell wrapping passes the command and args directly.
     * Expected outcome: the list contains exactly the command followed by its arguments.
     */
    @Test
    void buildCommand_noShellWrap_passesCommandAndArgsDirectly() {
        SchedulerProgramConfig config = new SchedulerProgramConfig();
        config.setShellWrap(false);
        config.setCommand("/usr/bin/ls");
        config.setArguments(List.of("-l", "/tmp"));

        List<String> command = ReflectionTestUtils.invokeMethod(
                programExecutorService, "buildCommand", config);

        assertThat(command).containsExactly("/usr/bin/ls", "-l", "/tmp");
    }

    /**
     * Verifies command assembly with no arguments and no shell wrapping.
     * Expected outcome: the list contains only the command.
     */
    @Test
    void buildCommand_noShellWrapNoArgs_returnsCommandOnly() {
        SchedulerProgramConfig config = new SchedulerProgramConfig();
        config.setShellWrap(false);
        config.setCommand("/usr/bin/uptime");

        List<String> command = ReflectionTestUtils.invokeMethod(
                programExecutorService, "buildCommand", config);

        assertThat(command).containsExactly("/usr/bin/uptime");
    }

    /**
     * Verifies shell wrapping with the default shell wraps the command in {@code /bin/bash -c}.
     * Expected outcome: the list is {@code [/bin/bash, -c, "<command> <args>"]}.
     */
    @Test
    void buildCommand_shellWrapDefaultShell_wrapsInBashDashC() {
        SchedulerProgramConfig config = new SchedulerProgramConfig();
        config.setShellWrap(true);
        config.setCommand("echo hi");
        config.setArguments(List.of("world"));

        List<String> command = ReflectionTestUtils.invokeMethod(
                programExecutorService, "buildCommand", config);

        assertThat(command).containsExactly("/bin/bash", "-c", "echo hi world");
    }

    /**
     * Verifies shell wrapping honours a configured custom shell path.
     * Expected outcome: the list uses the configured shell with {@code -c}.
     */
    @Test
    void buildCommand_shellWrapCustomShell_usesConfiguredShell() {
        SchedulerProgramConfig config = new SchedulerProgramConfig();
        config.setShellWrap(true);
        config.setShellPath("/bin/sh");
        config.setCommand("printf test");

        List<String> command = ReflectionTestUtils.invokeMethod(
                programExecutorService, "buildCommand", config);

        assertThat(command).containsExactly("/bin/sh", "-c", "printf test");
    }

    // -------------------------------------------------------------------------
    // readStream()
    // -------------------------------------------------------------------------

    /**
     * Verifies the bounded stream reader captures all lines when under the byte limit.
     * Expected outcome: the buffer contains the full stream content.
     */
    @Test
    void readStream_belowLimit_capturesAllLines() {
        InputStream in = new ByteArrayInputStream("line1\nline2\n".getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();

        ReflectionTestUtils.invokeMethod(programExecutorService, "readStream", in, sb, 1000);

        assertThat(sb.toString()).isEqualTo("line1\nline2\n");
    }

    /**
     * Verifies the bounded stream reader stops appending once the byte limit is exceeded.
     * Expected outcome: only the first line is retained; later lines are dropped.
     */
    @Test
    void readStream_aboveLimit_stopsAppending() {
        InputStream in = new ByteArrayInputStream("line1\nline2\n".getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();

        // maxBytes 3: after first line sb.length() (6) is no longer < 3, so line2 is dropped.
        ReflectionTestUtils.invokeMethod(programExecutorService, "readStream", in, sb, 3);

        assertThat(sb.toString()).isEqualTo("line1\n");
    }
}
