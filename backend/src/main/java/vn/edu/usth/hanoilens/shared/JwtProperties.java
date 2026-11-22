package vn.edu.usth.hanoilens.shared;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("hanoilens.jwt")
public record JwtProperties(String secret, String issuer, String audience, Duration accessTtl, Duration refreshTtl) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("hanoilens.jwt.secret must be at least 32 bytes (set JWT_SECRET).");
        }
        if (accessTtl == null || accessTtl.compareTo(Duration.ofMinutes(15)) > 0) {
            throw new IllegalStateException("hanoilens.jwt.access-ttl must be set and at most 15 minutes.");
        }
    }

    public byte[] secretBytes() {
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
