package vn.edu.usth.hanoilens.modules.event.adapter;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * SSRF guard for event sources (SECURITY TM-4.4): HTTPS only, no credentials in the URL, default port only, and no
 * loopback / private / link-local / metadata address. Registration checks the text of the URL (no DNS);
 * {@link #requirePublicAddresses} resolves the host right before each fetch.
 */
public final class OutboundUrlPolicy {

    /** A host made only of digits, hex, dots and colons is an IP literal in some notation (e.g. 2130706433). */
    private static final Pattern LITERAL = Pattern.compile("^[0-9a-fA-FxX.:\\[\\]]+$");

    private OutboundUrlPolicy() {
    }

    /** @throws IllegalArgumentException with a short machine-readable reason */
    public static URI requireAllowedUrl(String url) {
        URI uri;
        try {
            uri = new URI(url == null ? "" : url.trim());
        } catch (java.net.URISyntaxException ex) {
            throw new IllegalArgumentException("URL is not valid.");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Only https URLs are allowed.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL has no host.");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URL must not contain credentials.");
        }
        if (uri.getPort() != -1 && uri.getPort() != 443) {
            throw new IllegalArgumentException("Only the default https port is allowed.");
        }
        String lower = host.toLowerCase(Locale.ROOT);
        if (lower.equals("localhost") || lower.endsWith(".localhost") || lower.endsWith(".local")
                || lower.endsWith(".internal") || lower.endsWith(".lan")) {
            throw new IllegalArgumentException("Internal host names are not allowed.");
        }
        if (LITERAL.matcher(host).matches()) {
            // Literals are parsed without any DNS lookup.
            try {
                if (isBlocked(InetAddress.getByName(host.replace("[", "").replace("]", "")))) {
                    throw new IllegalArgumentException("Private or internal addresses are not allowed.");
                }
            } catch (UnknownHostException ex) {
                throw new IllegalArgumentException("URL host is not valid.");
            }
        }
        return uri;
    }

    /** Resolves the host and rejects it if ANY address is non-public. Call right before connecting. */
    public static void requirePublicAddresses(URI uri) throws UnknownHostException {
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (isBlocked(address)) {
                throw new IllegalArgumentException("Private or internal addresses are not allowed.");
            }
        }
    }

    static boolean isBlocked(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress()
                || a.isMulticastAddress()) {
            return true;                                   // 0.0.0.0, 127/8, 169.254/16 (cloud metadata), 10/8, 172.16/12, 192.168/16
        }
        byte[] b = a.getAddress();
        if (b.length == 4) {
            int first = b[0] & 0xff;
            int second = b[1] & 0xff;
            return first == 0                              // 0.0.0.0/8
                    || (first == 100 && second >= 64 && second <= 127);   // 100.64.0.0/10 carrier-grade NAT
        }
        return (b[0] & 0xfe) == 0xfc;                      // fc00::/7 unique local IPv6
    }
}
