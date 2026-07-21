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
 * Unit tests for {@link AlertEvaluatorScheduler}.
 */
@ExtendWith(MockitoExtension.class)
class AlertEvaluatorSchedulerTest {

    @Mock
    private AlertRuleService alertRuleService;

    @InjectMocks
    private AlertEvaluatorScheduler scheduler;

    @Test
    void evaluate_delegatesToAlertRuleService() {
        scheduler.evaluate();

        verify(alertRuleService).evaluateAll();
    }

    @Test
    void evaluate_whenServiceThrows_doesNotPropagate() {
        doThrow(new RuntimeException("boom")).when(alertRuleService).evaluateAll();

        assertThatCode(() -> scheduler.evaluate()).doesNotThrowAnyException();

        verify(alertRuleService).evaluateAll();
    }
}
