package vn.edu.usth.hanoilens.modules.recap.service;

import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.edu.usth.hanoilens.modules.visit.VisitEnded;

/**
 * Runs synchronously inside the transaction that ended the visit: the visit and its PENDING recap are
 * committed together or not at all. A later worker moves the recap PENDING -> PROCESSING -> READY.
 */
@Component
class RecapOnVisitEnded {

    private final JdbcTemplate jdbc;

    RecapOnVisitEnded(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener
    void on(VisitEnded event) {
        jdbc.update("""
                INSERT INTO recaps (id, user_id, journey_id, recap_type, status, created_at)
                VALUES (?, ?, ?, 'JOURNEY', 'PENDING', now())
                """, event.recapId(), event.userId(), event.visitId());
    }
}
