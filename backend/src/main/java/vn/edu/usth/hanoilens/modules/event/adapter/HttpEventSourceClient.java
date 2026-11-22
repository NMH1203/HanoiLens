package vn.edu.usth.hanoilens.modules.event.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import org.springframework.stereotype.Component;
import vn.edu.usth.hanoilens.modules.event.config.EventIngestionProperties;

/**
 * Fetches allow-listed HTTPS sources with strict limits (SECURITY TM-4.4): public addresses only (checked right
 * before connecting), no redirects, connect + request timeouts, a hard body-size cap and a content-type allow-list.
 * Residual risk: the JDK resolves the name again when it connects (DNS rebinding window).
 */
@Component
class HttpEventSourceClient implements EventSourceClient {

    private static final String[] ALLOWED_TYPES = {"text/html", "application/json", "application/ld+json",
            "application/rss+xml", "application/atom+xml", "application/xml", "text/xml"};

    private final EventIngestionProperties props;
    private final HttpClient http;

    HttpEventSourceClient(EventIngestionProperties props) {
        this.props = props;
        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)          // a redirect could point at an internal host
                .connectTimeout(Duration.ofSeconds(props.connectTimeoutSeconds()))
                .build();
    }

    @Override
    public String fetch(URI uri) {
        try {
            OutboundUrlPolicy.requireAllowedUrl(uri.toString());
            OutboundUrlPolicy.requirePublicAddresses(uri);
        } catch (IllegalArgumentException ex) {
            throw new FetchException("SOURCE_NOT_ALLOWED");
        } catch (UnknownHostException ex) {
            throw new FetchException("SOURCE_UNREACHABLE");
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(props.requestTimeoutSeconds()))
                .header("User-Agent", "HanoiLensBot/1.0 (event ingestion; contact: project team)")
                .header("Accept", "application/ld+json, application/json, application/rss+xml, text/html;q=0.8")
                .GET().build();
        try {
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() != 200) {
                    throw new FetchException("SOURCE_UNREACHABLE");        // 3xx is refused as well (no redirects)
                }
                String type = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
                boolean ok = false;
                for (String allowed : ALLOWED_TYPES) {
                    ok |= type.startsWith(allowed);
                }
                if (!ok) {
                    throw new FetchException("UNSUPPORTED_CONTENT_TYPE");
                }
                byte[] bytes = body.readNBytes(props.maxBodyBytes() + 1);
                if (bytes.length > props.maxBodyBytes()) {
                    throw new FetchException("SOURCE_TOO_LARGE");
                }
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            throw new FetchException("SOURCE_UNREACHABLE");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FetchException("SOURCE_UNREACHABLE");
        }
    }
}
