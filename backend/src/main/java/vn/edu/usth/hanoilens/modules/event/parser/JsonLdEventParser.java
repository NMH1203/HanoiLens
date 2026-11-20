package vn.edu.usth.hanoilens.modules.event.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * schema.org {@code Event} objects from {@code <script type="application/ld+json">} blocks (or a raw JSON
 * document). Rules: name -> title, description, startDate/endDate, location.name -> venue, url -> source url.
 * A missing endDate means the event ends when it starts (a bare start date: at the end of that day).
 */
@Component
public class JsonLdEventParser implements EventParser {

    private static final Pattern SCRIPT = Pattern.compile(
            "<script[^>]*type\\s*=\\s*[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Set<String> EVENT_TYPES = Set.of("Event", "ExhibitionEvent", "Festival", "MusicEvent",
            "TheaterEvent", "DanceEvent", "VisualArtsEvent", "EducationEvent", "ScreeningEvent", "LiteraryEvent");

    private final ObjectMapper json;

    JsonLdEventParser(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public List<ParsedEvent> parse(String body, Map<String, Object> config, int maxItems) {
        List<JsonNode> roots = new ArrayList<>();
        String trimmed = body == null ? "" : body.trim();
        try {
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                roots.add(json.readTree(trimmed));
            } else {
                Matcher m = SCRIPT.matcher(trimmed);
                while (m.find()) {
                    roots.add(json.readTree(m.group(1).trim()));
                }
            }
        } catch (JsonProcessingException ex) {
            throw new ParseException("Malformed JSON-LD.");
        }
        List<ParsedEvent> events = new ArrayList<>();
        for (JsonNode root : roots) {
            collect(root, events, maxItems, 0);
        }
        return events;
    }

    private void collect(JsonNode node, List<ParsedEvent> out, int maxItems, int depth) {
        if (node == null || depth > 6 || out.size() >= maxItems) {
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collect(child, out, maxItems, depth + 1));
        } else if (node.isObject()) {
            if (isEvent(node)) {
                out.add(toEvent(node));
            }
            collect(node.get("@graph"), out, maxItems, depth + 1);
        }
    }

    private static boolean isEvent(JsonNode node) {
        JsonNode type = node.get("@type");
        if (type == null) {
            return false;
        }
        if (type.isTextual()) {
            return EVENT_TYPES.contains(type.asText());
        }
        for (JsonNode t : type) {
            if (t.isTextual() && EVENT_TYPES.contains(t.asText())) {
                return true;
            }
        }
        return false;
    }

    private static ParsedEvent toEvent(JsonNode n) {
        String startText = text(n, "startDate");
        String endText = text(n, "endDate");
        Instant start = ParseSupport.parseDate(startText, false);
        Instant end = endText == null ? ParseSupport.parseDate(startText, ParseSupport.isDateOnly(startText))
                : ParseSupport.parseDate(endText, true);
        return new ParsedEvent(ParseSupport.clean(text(n, "name")), ParseSupport.clean(text(n, "description")),
                start, end, ParseSupport.clean(venue(n.get("location"))), text(n, "url"));
    }

    private static String venue(JsonNode location) {
        if (location == null) {
            return null;
        }
        if (location.isTextual()) {
            return location.asText();
        }
        if (location.isArray() && !location.isEmpty()) {
            return venue(location.get(0));
        }
        return text(location, "name");
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || !v.isValueNode() || v.isNull() ? null : v.asText();
    }
}
