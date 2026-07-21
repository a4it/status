package org.automatize.status.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link LogMetricScheduler}.
 *
 * <p>Testing approach: the scheduler is tested in isolation with Mockito. Its
 * {@link LogMetricService} collaborator is a {@code @Mock} injected via
 * {@code @InjectMocks}, and the tests verify that the scheduled trigger delegates to
 * the service and that failures from the service are swallowed rather than propagated.</p>
 */
@ExtendWith(MockitoExtension.class)
class LogMetricSchedulerTest {

    @Mock
    private LogMetricService logMetricService;

    @InjectMocks
    private LogMetricScheduler scheduler;

    /**
     * Verifies that the scheduled {@code aggregate} trigger delegates to
     * {@link LogMetricService#aggregateRecentLogs()}.
     */
    @Test
    void aggregate_delegatesToService() {
        scheduler.aggregate();

        verify(logMetricService).aggregateRecentLogs();
    }

    /**
     * Verifies that if the underlying service throws, the scheduled {@code aggregate}
     * call swallows the exception (does not propagate) while still invoking the service.
     */
    @Test
    void aggregate_serviceThrows_exceptionIsSwallowed() {
        doThrow(new RuntimeException("boom")).when(logMetricService).aggregateRecentLogs();

        assertThatCode(() -> scheduler.aggregate()).doesNotThrowAnyException();
        verify(logMetricService).aggregateRecentLogs();
    }
}
