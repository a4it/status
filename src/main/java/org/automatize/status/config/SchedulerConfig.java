package org.automatize.status.config;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Spring configuration for the scheduler engine's {@link TaskScheduler} bean.
 *
 * <p>{@code @EnableScheduling} is already declared on {@link org.automatize.status.StatusApplication}
 * and is therefore omitted here to prevent duplicate registration.</p>
 *
 * <p>Thread pool size is configurable via {@code scheduler.thread-pool-size} (default: 10).
 * All threads are named {@code scheduler-N} for easy identification in thread dumps and logs.</p>
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Configuration
public class SchedulerConfig {

    @Value("${scheduler.thread-pool-size:10}")
    private int threadPoolSize;

    /**
     * Provides the {@link TaskScheduler} used by the scheduler engine to register and
     * fire cron-triggered job tasks.
     *
     * @return a configured {@link ThreadPoolTaskScheduler}
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(threadPoolSize);
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.setErrorHandler(throwable ->
                LoggerFactory.getLogger(SchedulerConfig.class)
                        .error("Unhandled error in scheduled task", throwable));
        scheduler.initialize();
        return scheduler;
    }
}
