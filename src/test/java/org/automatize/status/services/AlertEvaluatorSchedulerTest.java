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
 *
 * <p>Exercises the scheduled entry point that periodically triggers alert-rule
 * evaluation. Uses Mockito to mock the collaborating {@link AlertRuleService} and
 * verifies both the delegation contract and the scheduler's exception-swallowing
 * behaviour so that a failing evaluation never breaks the scheduling loop.</p>
 */
@ExtendWith(MockitoExtension.class)
class AlertEvaluatorSchedulerTest {

    @Mock
    private AlertRuleService alertRuleService;

    @InjectMocks
    private AlertEvaluatorScheduler scheduler;

    /**
     * Verifies the scheduled {@code evaluate()} tick delegates to the service.
     * Expects {@link AlertRuleService#evaluateAll()} to be invoked exactly once.
     */
    @Test
    void evaluate_delegatesToAlertRuleService() {
        scheduler.evaluate();

        verify(alertRuleService).evaluateAll();
    }

    /**
     * Verifies that when the underlying service throws during evaluation, the
     * scheduler swallows the exception rather than propagating it (so the schedule
     * keeps running). Expects no exception and that the delegate was still called.
     */
    @Test
    void evaluate_whenServiceThrows_doesNotPropagate() {
        doThrow(new RuntimeException("boom")).when(alertRuleService).evaluateAll();

        assertThatCode(() -> scheduler.evaluate()).doesNotThrowAnyException();

        verify(alertRuleService).evaluateAll();
    }
}
