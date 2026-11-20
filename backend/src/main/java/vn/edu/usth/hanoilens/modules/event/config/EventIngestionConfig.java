package vn.edu.usth.hanoilens.modules.event.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableScheduling
class EventIngestionConfig {

    /** Small, bounded pool: a crawl never competes with request threads for long (ARCHITECTURE §1). */
    @Bean(name = "eventImportExecutor")
    TaskExecutor eventImportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("event-import-");
        executor.initialize();
        return executor;
    }
}
