package io.cockroachdb.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AsyncConfiguration {
    /**
     * An unbounded virtual thread executor to be used for I/O bound tasks.
     */
    @Bean
    public AsyncTaskExecutor asyncTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setThreadNamePrefix("virtual-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(-1);
        return executor;
    }
}


