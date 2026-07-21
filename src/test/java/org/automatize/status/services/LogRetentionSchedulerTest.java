package org.automatize.status.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link LogRetentionScheduler}.
 */
@ExtendWith(MockitoExtension.class)
class LogRetentionSchedulerTest {

    @Mock
    private LogIngestionService logIngestionService;

    @InjectMocks
    private LogRetentionScheduler scheduler;

    @Test
    void purge_delegatesToPurgeOldLogs() {
        scheduler.purge();

        verify(logIngestionService).purgeOldLogs();
    }
}
