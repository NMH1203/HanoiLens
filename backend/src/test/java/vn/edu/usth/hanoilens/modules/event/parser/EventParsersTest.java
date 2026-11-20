package vn.edu.usth.hanoilens.modules.event.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import vn.edu.usth.hanoilens.modules.event.parser.EventParser.ParseException;

/** Fixed sample documents -> fixed results: the parsers are deterministic and need no network or database. */
class EventParsersTest {

    private final JsonLdEventParser jsonLd = new JsonLdEventParser(new ObjectMapper());
    private final RssEventParser rss = new RssEventParser();

    private static final String HTML = """
            <html><head><title>Lịch sự kiện</title>
            <script type="application/ld+json">
            {"@context":"https://schema.org","@type":"Event","name":"Triển lãm <b>Ảnh</b> Di sản",
             "description":"Ảnh   tư liệu &amp; hiện vật","startDate":"2031-05-01T09:00:00+07:00",
             "endDate":"2031-05-20","location":{"@type":"Place","name":"Bảo tàng Mỹ thuật"},
             "url":"https://events.example.org/e/1"}
            </script>
            <script type="application/ld+json">
            {"@graph":[{"@type":["Thing","MusicEvent"],"name":"Đêm nhạc Hồ Gươm","startDate":"2031-06-01",
              "location":"Phố đi bộ"},{"@type":"Organization","name":"Not an event"}]}
            </script></head><body></body></html>
            """;

    @Test
    void jsonLdEventsAreExtractedAndCleaned() {
        List<ParsedEvent> events = jsonLd.parse(HTML, Map.of(), 50);

        assertThat(events).hasSize(2);
        ParsedEvent first = events.get(0);
        assertThat(first.title()).isEqualTo("Triển lãm Ảnh Di sản");            // markup stripped
        assertThat(first.description()).isEqualTo("Ảnh tư liệu & hiện vật");     // spaces collapsed, entity decoded
        assertThat(first.startsAt()).isEqualTo(Instant.parse("2031-05-01T02:00:00Z"));
        assertThat(first.endsAt()).isEqualTo(Instant.parse("2031-05-20T16:59:59Z"));   // bare end date = 23:59:59 Hanoi
        assertThat(first.venueName()).isEqualTo("Bảo tàng Mỹ thuật");
        assertThat(first.url()).isEqualTo("https://events.example.org/e/1");

        ParsedEvent second = events.get(1);                                        // subtype inside @graph
        assertThat(second.title()).isEqualTo("Đêm nhạc Hồ Gươm");
        assertThat(second.startsAt()).isEqualTo(Instant.parse("2031-05-31T17:00:00Z"));   // 00:00 Hanoi
        assertThat(second.endsAt()).isEqualTo(Instant.parse("2031-06-01T16:59:59Z"));      // no endDate: end of that day
        assertThat(second.venueName()).isEqualTo("Phố đi bộ");
    }

    @Test
    void rawJsonDocumentsAndLimitsAreSupported() {
        String raw = "[{\"@type\":\"Event\",\"name\":\"A\",\"startDate\":\"2031-01-01T10:00:00\"},"
                + "{\"@type\":\"Event\",\"name\":\"B\",\"startDate\":\"2031-01-02T10:00:00\"}]";
        assertThat(jsonLd.parse(raw, Map.of(), 50)).hasSize(2);
        assertThat(jsonLd.parse(raw, Map.of(), 1)).hasSize(1);                      // maxItems is honoured
        assertThat(jsonLd.parse("<html>no data</html>", Map.of(), 50)).isEmpty();
    }

    @Test
    void malformedJsonLdIsAParseError() {
        assertThatThrownBy(() -> jsonLd.parse("<script type=\"application/ld+json\">{oops</script>", Map.of(), 50))
                .isInstanceOf(ParseException.class);
    }

    @Test
    void anUnreadableDateIsLeftNullForValidationToReject() {
        List<ParsedEvent> events = jsonLd.parse("{\"@type\":\"Event\",\"name\":\"X\",\"startDate\":\"next friday\"}",
                Map.of(), 50);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).startsAt()).isNull();
    }

    private static final String RSS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"><channel><title>Sự kiện</title>
              <item><title>Hội thảo Di sản</title><link>https://events.example.org/e/9</link>
                <description>Mô tả</description><startdate>2031-07-01T08:30:00+07:00</startdate>
                <enddate>2031-07-01T11:00:00+07:00</enddate><location>Hội trường A</location></item>
              <item><title>Không có ngày</title><link>https://events.example.org/e/10</link></item>
            </channel></rss>
            """;

    @Test
    void rssItemsUseTheConfiguredElementNames() {
        List<ParsedEvent> events = rss.parse(RSS, Map.of(), 50);
        assertThat(events).hasSize(2);
        assertThat(events.get(0).title()).isEqualTo("Hội thảo Di sản");
        assertThat(events.get(0).startsAt()).isEqualTo(Instant.parse("2031-07-01T01:30:00Z"));
        assertThat(events.get(0).venueName()).isEqualTo("Hội trường A");
        assertThat(events.get(0).url()).isEqualTo("https://events.example.org/e/9");
        assertThat(events.get(1).startsAt()).isNull();                              // rejected later by validation

        String custom = RSS.replace("startdate", "from").replace("enddate", "until").replace("location", "where");
        List<ParsedEvent> mapped = rss.parse(custom, Map.of("startElement", "from", "endElement", "until",
                "venueElement", "where"), 50);
        assertThat(mapped.get(0).startsAt()).isEqualTo(Instant.parse("2031-07-01T01:30:00Z"));
        assertThat(mapped.get(0).venueName()).isEqualTo("Hội trường A");
    }

    @Test
    void xmlWithADoctypeIsRefusedAgainstXxe() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
                + "<rss><channel><item><title>&x;</title></item></channel></rss>";
        assertThatThrownBy(() -> rss.parse(xxe, Map.of(), 50)).isInstanceOf(ParseException.class);
        assertThatThrownBy(() -> rss.parse("<rss><item>", Map.of(), 50)).isInstanceOf(ParseException.class);
    }
}
