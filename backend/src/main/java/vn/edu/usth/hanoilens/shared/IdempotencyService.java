package vn.edu.usth.hanoilens.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the {@code Idempotency-Key} contract (docs/API.md §2) on top of the {@code idempotency_keys}
 * table. The key row and the action run in ONE transaction, so a failed action leaves no key behind and a
 * retry runs again; a successful action stores its response atomically with its side effects.
 */
@Service
public class IdempotencyService {

    private static final Pattern KEY_FORMAT = Pattern.compile("^[A-Za-z0-9._:-]{8,128}$");

    /** What an action produced: HTTP status plus the full response envelope. */
    public record Result(int status, Object body) {
    }

    public record StoredResponse(int status, JsonNode body) {
    }

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public IdempotencyService(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Transactional
    public StoredResponse execute(UUID userId, String key, String method, String path, Object requestBody,
            Supplier<Result> action) {
        if (key == null || key.isBlank()) {
            throw ApiException.badRequest("IDEMPOTENCY_KEY_REQUIRED", "The Idempotency-Key header is required.");
        }
        if (!KEY_FORMAT.matcher(key).matches()) {
            throw ApiException.badRequest("IDEMPOTENCY_KEY_INVALID",
                    "Idempotency-Key must be 8-128 characters of letters, digits, '.', '_', ':' or '-'.");
        }
        String requestHash = hash(method, path, requestBody);

        jdbc.update("DELETE FROM idempotency_keys WHERE user_id = ? AND idempotency_key = ? AND expires_at < now()",
                userId, key);
        int inserted = jdbc.update("""
                INSERT INTO idempotency_keys
                    (user_id, idempotency_key, request_method, request_path, request_hash, status, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?, 'IN_PROGRESS', now(), now() + interval '24 hours')
                ON CONFLICT (user_id, idempotency_key) DO NOTHING
                """, userId, key, method, path, requestHash);

        if (inserted == 0) {
            return replayOrReject(userId, key, requestHash);
        }

        Result result = action.get();
        JsonNode body = mapper.valueToTree(result.body());
        jdbc.update("""
                UPDATE idempotency_keys
                   SET status = 'COMPLETED', response_status = ?, response_body = ?::jsonb
                 WHERE user_id = ? AND idempotency_key = ?
                """, result.status(), body.toString(), userId, key);
        return new StoredResponse(result.status(), body);
    }

    private StoredResponse replayOrReject(UUID userId, String key, String requestHash) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT request_hash, status, response_status, response_body::text AS response_body
                  FROM idempotency_keys WHERE user_id = ? AND idempotency_key = ?
                """, userId, key);
        if (rows.isEmpty()) {
            // The competing transaction rolled back between our insert and select: ask the client to retry.
            throw ApiException.conflict("REQUEST_IN_PROGRESS", "An identical request is already in progress.");
        }
        Map<String, Object> row = rows.get(0);
        if (!requestHash.equals(row.get("request_hash"))) {
            throw ApiException.unprocessable("IDEMPOTENCY_KEY_REUSED",
                    "This Idempotency-Key was already used with a different request.");
        }
        if (!"COMPLETED".equals(row.get("status"))) {
            throw ApiException.conflict("REQUEST_IN_PROGRESS", "An identical request is already in progress.");
        }
        try {
            return new StoredResponse(((Number) row.get("response_status")).intValue(),
                    mapper.readTree((String) row.get("response_body")));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Stored idempotent response is not valid JSON", ex);
        }
    }

    private String hash(String method, String path, Object body) {
        try {
            String canonical = method + " " + path + "\n" + (body == null ? "" : mapper.writeValueAsString(body));
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Cannot hash request", ex);
        }
    }
}
