package vn.edu.usth.hanoilens.modules.event.parser;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** Date and text rules shared by the parsers. All rules are fixed and documented here. */
public final class ParseSupport {

    public static final ZoneId HANOI = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Pattern TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private ParseSupport() {
    }

    /**
     * ISO-8601 with an offset is used as is; a local date-time or a bare date is read in Asia/Ho_Chi_Minh.
     *
     * @param endOfDay for a bare date: true = 23:59:59 that day (used for an end date), false = 00:00
     * @return null when the text is missing or not an ISO-8601 date/time
     */
    public static Instant parseDate(String text, boolean endOfDay) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        try {
            return OffsetDateTime.parse(t).toInstant();
        } catch (DateTimeParseException ignored) {
            // try the next form
        }
        try {
            return LocalDateTime.parse(t).atZone(HANOI).toInstant();
        } catch (DateTimeParseException ignored) {
            // try the next form
        }
        try {
            LocalDate date = LocalDate.parse(t);
            return (endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay()).atZone(HANOI).toInstant();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    /** True when the text is a bare date (no time part). */
    public static boolean isDateOnly(String text) {
        return text != null && text.trim().length() == 10;
    }

    /** Strips markup, collapses whitespace, normalises Unicode (NFC). Clients render the result as plain text. */
    public static String clean(String text) {
        if (text == null) {
            return null;
        }
        String noTags = TAGS.matcher(text).replaceAll(" ")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ");
        String s = SPACES.matcher(Normalizer.normalize(noTags, Normalizer.Form.NFC)).replaceAll(" ").trim();
        return s.isEmpty() ? null : s;
    }
}
