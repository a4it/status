package org.automatize.status.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link LogRetentionScheduler}.
 *
 * <p>Testing approach: pure Mockito unit test. The collaborating
 * {@link LogIngestionService} is mocked and injected into the scheduler, and the
 * scheduled entry point is invoked directly to verify it delegates to the
 * ingestion service rather than exercising any real scheduling or persistence.
 */
@ExtendWith(MockitoExtension.class)
class LogRetentionSchedulerTest {

    @Mock
    private LogIngestionService logIngestionService;

    @InjectMocks
    private LogRetentionScheduler scheduler;

    /**
     * Verifies that invoking the scheduler's {@code purge} entry point delegates to
     * {@link LogIngestionService#purgeOldLogs()} exactly.
     */
    @Test
    void purge_delegatesToPurgeOldLogs() {
        scheduler.purge();

        verify(logIngestionService).purgeOldLogs();
    }
}
