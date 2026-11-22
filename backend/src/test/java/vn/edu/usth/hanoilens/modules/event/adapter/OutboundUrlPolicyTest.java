package vn.edu.usth.hanoilens.modules.event.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** SECURITY TM-4.4: the SSRF guard for event sources. No DNS is used (only literals and host names). */
class OutboundUrlPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "http://events.example.org/feed",              // not https
            "ftp://events.example.org/feed",
            "https://localhost/feed",
            "https://app.localhost/feed",
            "https://printer.local/feed",
            "https://db.internal/feed",
            "https://127.0.0.1/feed",                      // loopback
            "https://169.254.169.254/latest/meta-data",    // cloud metadata
            "https://10.1.2.3/feed",                       // private ranges
            "https://172.16.0.9/feed",
            "https://192.168.1.10/feed",
            "https://100.64.0.1/feed",                     // carrier-grade NAT
            "https://0.0.0.0/feed",
            "https://2130706433/feed",                     // 127.0.0.1 as a decimal number
            "https://[::1]/feed",                          // IPv6 loopback
            "https://[fd00::1]/feed",                      // IPv6 unique local
            "https://user:secret@events.example.org/feed", // credentials in the URL
            "https://events.example.org:8443/feed",        // non-default port
            "https:///feed",                               // no host
            "not a url"})
    void unsafeUrlsAreRejected(String url) {
        assertThatThrownBy(() -> OutboundUrlPolicy.requireAllowedUrl(url)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://events.example.org/feed", "https://baotang.vn/su-kien?page=2",
            "https://8.8.8.8/feed", "https://events.example.org:443/feed"})
    void publicHttpsUrlsAreAllowed(String url) {
        assertThatCode(() -> OutboundUrlPolicy.requireAllowedUrl(url)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "10.0.0.1", "192.168.0.1", "172.31.255.255", "169.254.169.254", "0.0.0.0",
            "100.100.100.100", "224.0.0.1", "::1", "fe80::1", "fc00::1", "fdff::1"})
    void nonPublicAddressesAreBlocked(String address) throws Exception {
        assertThat(OutboundUrlPolicy.isBlocked(InetAddress.getByName(address))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "1.1.1.1", "203.0.113.5", "100.63.255.255", "2001:4860:4860::8888"})
    void publicAddressesAreNotBlocked(String address) throws Exception {
        assertThat(OutboundUrlPolicy.isBlocked(InetAddress.getByName(address))).isFalse();
    }
}
