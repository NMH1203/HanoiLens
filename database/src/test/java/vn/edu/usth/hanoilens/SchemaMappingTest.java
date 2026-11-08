package vn.edu.usth.hanoilens;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Loading the context already proves Flyway V1..V7 applied and {@code ddl-auto=validate} accepts every entity;
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
                "destinations", "landmarks", "arrival_checks", "journeys", "journey_landmark_visits", "stamps",
                "user_stamps", "rewards", "user_rewards", "cultural_events", "event_sources", "event_imports",
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
    void journeysStoreNoCoordinates() {
        List<String> columns = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'journeys'", String.class);
        assertThat(columns).noneMatch(c -> c.contains("lat") || c.contains("lng") || c.contains("lon"));
    }
}
