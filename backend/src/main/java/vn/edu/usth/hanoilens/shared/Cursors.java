package vn.edu.usth.hanoilens.shared;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/** Opaque cursor helpers. Clients must never build or parse these (docs/API.md §2 Pagination). */
public final class Cursors {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 50;

    private Cursors() {
    }

    public static int limit(Integer requested) {
        if (requested == null) {
            return DEFAULT_LIMIT;
        }
        if (requested < 1 || requested > MAX_LIMIT) {
            throw ApiException.badRequest("INVALID_LIMIT", "limit must be between 1 and " + MAX_LIMIT + ".");
        }
        return requested;
    }

    // ---- offset cursor (static catalogue lists) -------------------------------------------------

    public static String encodeOffset(int offset) {
        return encode("o:" + offset);
    }

    public static int decodeOffset(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            String raw = decode(cursor);
            if (!raw.startsWith("o:")) {
                throw new IllegalArgumentException();
            }
            int offset = Integer.parseInt(raw.substring(2));
            if (offset < 0) {
                throw new IllegalArgumentException();
            }
            return offset;
        } catch (IllegalArgumentException ex) {
            throw invalid();
        }
    }

    // ---- keyset cursor (time-ordered private lists) ---------------------------------------------

    public record Keyset(Instant at, UUID id) {
    }

    public static String encodeKeyset(Instant at, UUID id) {
        return encode("k:" + at.toString() + "|" + id);
    }

    /** Returns null for a missing cursor (first page). */
    public static Keyset decodeKeyset(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String raw = decode(cursor);
            if (!raw.startsWith("k:")) {
                throw new IllegalArgumentException();
            }
            String[] parts = raw.substring(2).split("\\|", 2);
            return new Keyset(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (RuntimeException ex) {
            throw invalid();
        }
    }

    private static ApiException invalid() {
        return ApiException.badRequest("INVALID_CURSOR", "The cursor is not valid.");
    }

    private static String encode(String raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String cursor) {
        return new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
    }
}
