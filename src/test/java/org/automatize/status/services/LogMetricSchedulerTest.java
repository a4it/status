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
 */
@ExtendWith(MockitoExtension.class)
class LogMetricSchedulerTest {

    @Mock
    private LogMetricService logMetricService;

    @InjectMocks
    private LogMetricScheduler scheduler;

    @Test
    void aggregate_delegatesToService() {
        scheduler.aggregate();

        verify(logMetricService).aggregateRecentLogs();
    }

    @Test
    void aggregate_serviceThrows_exceptionIsSwallowed() {
        doThrow(new RuntimeException("boom")).when(logMetricService).aggregateRecentLogs();

        assertThatCode(() -> scheduler.aggregate()).doesNotThrowAnyException();
        verify(logMetricService).aggregateRecentLogs();
    }
}
