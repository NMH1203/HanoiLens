package vn.edu.usth.hanoilens;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Loading the context already proves Flyway V1..V10 applied and {@code ddl-auto=validate} accepts every entity;
 * this adds explicit checks on the extensions and on the guarantees the application relies on.
 */
class SchemaMappingTest extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void extensionsAreInstalled() {
        List<String> ext = jdbc.queryForList("SELECT extname FROM pg_extension", String.class);
        assertThat(ext).contains("postgis", "vector");
    }

    @Test
    void coreTablesExist() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);
        assertThat(tables).contains("users", "refresh_tokens", "idempotency_keys", "admin_audit_logs",
                "destinations", "landmarks", "arrival_checks", "visits", "visit_landmarks", "stamps",
                "user_stamps", "cultural_events", "event_sources", "event_imports",
                "photos", "upload_sessions", "photo_exports", "recaps", "recap_photos", "recognition_results",
                "knowledge_chunks", "qa_conversations", "qa_messages");
    }

    @Test
    void arrivalChecksStoreNoCoordinates() {
        List<String> columns = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'arrival_checks'", String.class);
        assertThat(columns).noneMatch(c -> c.contains("lat") || c.contains("lng") || c.contains("lon"));
    }

    @Test
    void visitsStoreNoCoordinates() {
        List<String> columns = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'visits'", String.class);
        assertThat(columns).noneMatch(c -> c.contains("lat") || c.contains("lng") || c.contains("lon"));
    }

    /** ADR 011: no rewards and no "journey" entity; every visit reference is called visit_id (ARCHITECTURE §10). */
    @Test
    void noRewardOrJourneyNamesRemainInTheSchema() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);
        assertThat(tables).noneMatch(t -> t.contains("reward") || t.contains("journey"));
        assertThat(jdbc.queryForList("""
                SELECT table_name || '.' || column_name FROM information_schema.columns
                 WHERE table_schema = 'public' AND column_name LIKE '%journey%'
                """, String.class)).isEmpty();
        assertThat(jdbc.queryForList("""
                SELECT table_name || '.' || column_name FROM information_schema.columns
                 WHERE table_schema = 'public' AND column_name = 'visit_id' ORDER BY 1
                """, String.class)).contains("visit_landmarks.visit_id", "user_stamps.visit_id", "photos.visit_id",
                "upload_sessions.visit_id", "recaps.visit_id", "qa_conversations.visit_id", "arrival_checks.visit_id");
        assertThat(jdbc.queryForList("""
                SELECT conname FROM pg_constraint WHERE connamespace = 'public'::regnamespace
                   AND (conname LIKE '%journey%' OR conname LIKE '%jlv%' OR conname LIKE '%reward%')
                """, String.class)).isEmpty();
        assertThat(jdbc.queryForList("""
                SELECT indexname FROM pg_indexes WHERE schemaname = 'public'
                   AND (indexname LIKE '%journey%' OR indexname LIKE '%jlv%' OR indexname LIKE '%reward%')
                """, String.class)).isEmpty();
    }

    @Test
    void oneOpenVisitPerUserIsEnforcedByTheDatabase() {
        assertThat(jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'visits'", String.class))
                .contains("uq_visits_one_open_per_user");
    }
}
