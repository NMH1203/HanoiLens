package vn.edu.usth.hanoilens.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Append-only audit trail of every {@code /admin/**} action (SECURITY TM-4.4). Written in the caller's
 * transaction, so an action and its audit row commit or roll back together. {@code details} must never contain
 * raw coordinates, tokens or signed URLs.
 */
@Component
public class AdminAuditLog {

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    AdminAuditLog(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void record(UUID adminUserId, String action, String targetType, UUID targetId, Map<String, ?> details) {
        String detailsJson;
        try {
            detailsJson = details == null ? null : json.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Audit details are not serialisable.", ex);
        }
        jdbc.update("""
                INSERT INTO admin_audit_logs (id, admin_user_id, action, target_type, target_id, details)
                VALUES (?, ?, ?, ?, ?, ?::jsonb)
                """, Ids.newId(), adminUserId, action, targetType, targetId, detailsJson);
    }
}
