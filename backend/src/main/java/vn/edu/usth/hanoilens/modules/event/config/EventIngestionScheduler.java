package vn.edu.usth.hanoilens.modules.event.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.usth.hanoilens.modules.event.service.EventImportService;

/** Periodic crawl of every active source. Off unless {@code hanoilens.events.ingestion.enabled=true}. */
@Component
@ConditionalOnProperty(name = "hanoilens.events.ingestion.enabled", havingValue = "true")
class EventIngestionScheduler {

    private final EventImportService imports;

    EventIngestionScheduler(EventImportService imports) {
        this.imports = imports;
    }

    @Scheduled(cron = "${hanoilens.events.ingestion.cron:0 0 */6 * * *}")
    void crawl() {
        imports.crawlAllSources();
    }
}
