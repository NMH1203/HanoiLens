package vn.edu.usth.hanoilens.modules.event.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code hanoilens.events.ingestion.*}. The scheduled crawl is off by default; imports started through the
 * admin API run in the background unless {@code run-async} is false (tests run them synchronously).
 */
@ConfigurationProperties("hanoilens.events.ingestion")
public record EventIngestionProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("true") boolean runAsync,
        @DefaultValue("0 0 */6 * * *") String cron,
        @DefaultValue("1000000") int maxBodyBytes,
        @DefaultValue("5") int connectTimeoutSeconds,
        @DefaultValue("10") int requestTimeoutSeconds,
        @DefaultValue("200") int maxItems) {
}
