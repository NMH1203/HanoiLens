package vn.edu.usth.hanoilens.modules.event.parser;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * RSS 2.0 / Atom-like feeds where each {@code <item>} (or {@code <entry>}) carries the event dates in elements
 * whose local names come from the source's parser_config (defaults: startdate, enddate, location, link).
 * An item without a usable start date is passed on and rejected by validation. DOCTYPE is forbidden (XXE).
 */
@Component
public class RssEventParser implements EventParser {

    @Override
    public List<ParsedEvent> parse(String body, Map<String, Object> config, int maxItems) {
        Document doc = read(body);
        String startName = name(config, "startElement", "startdate");
        String endName = name(config, "endElement", "enddate");
        String venueName = name(config, "venueElement", "location");
        String linkName = name(config, "linkElement", "link");

        List<ParsedEvent> events = new ArrayList<>();
        for (Element item : elements(doc, "item", "entry")) {
            if (events.size() >= maxItems) {
                break;
            }
            String start = child(item, startName);
            String end = child(item, endName);
            events.add(new ParsedEvent(
                    ParseSupport.clean(child(item, "title")),
                    ParseSupport.clean(child(item, "description") != null ? child(item, "description")
                            : child(item, "summary")),
                    ParseSupport.parseDate(start, false),
                    end == null ? ParseSupport.parseDate(start, ParseSupport.isDateOnly(start))
                            : ParseSupport.parseDate(end, true),
                    ParseSupport.clean(child(item, venueName)),
                    linkOf(item, linkName)));
        }
        return events;
    }

    private static Document read(String body) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            f.setNamespaceAware(true);
            return f.newDocumentBuilder().parse(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ParseException("Malformed feed.");
        }
    }

    private static String name(Map<String, Object> config, String key, String fallback) {
        Object v = config.get(key);
        return v instanceof String s && !s.isBlank() ? s : fallback;
    }

    private static List<Element> elements(Document doc, String... localNames) {
        List<Element> found = new ArrayList<>();
        NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element e = (Element) all.item(i);
            for (String n : localNames) {
                if (n.equals(localName(e))) {
                    found.add(e);
                }
            }
        }
        return found;
    }

    private static String localName(Element e) {
        return e.getLocalName() != null ? e.getLocalName() : e.getTagName();
    }

    /** Text of the first direct child element with this local name (case-insensitive), or null. */
    private static String child(Element parent, String localName) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && localName(e).equalsIgnoreCase(localName)) {
                String text = e.getTextContent();
                return text == null || text.isBlank() ? null : text.trim();
            }
        }
        return null;
    }

    /** RSS: {@code <link>text</link>}; Atom: {@code <link href="..."/>}. */
    private static String linkOf(Element item, String linkName) {
        for (Node n = item.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && localName(e).equalsIgnoreCase(linkName)) {
                String href = e.getAttribute("href");
                if (!href.isBlank()) {
                    return href.trim();
                }
                String text = e.getTextContent();
                return text == null || text.isBlank() ? null : text.trim();
            }
        }
        return null;
    }
}
