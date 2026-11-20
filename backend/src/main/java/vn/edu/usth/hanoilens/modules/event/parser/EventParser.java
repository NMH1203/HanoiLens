package vn.edu.usth.hanoilens.modules.event.parser;

import java.util.List;
import java.util.Map;

/** Deterministic, source-specific extraction rules (no NLP model, ARCHITECTURE §9.3). */
public interface EventParser {

    /** Raised when the document cannot be read at all (malformed XML/JSON, forbidden constructs). */
    class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }

    /** @param config the source's validated {@code parser_config}, never null */
    List<ParsedEvent> parse(String body, Map<String, Object> config, int maxItems);
}
