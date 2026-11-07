package vn.edu.usth.hanoilens.modules.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import vn.edu.usth.hanoilens.modules.identity.dto.TokenResponse;
import vn.edu.usth.hanoilens.modules.identity.entity.RefreshToken;
import vn.edu.usth.hanoilens.modules.identity.entity.User;
import vn.edu.usth.hanoilens.modules.identity.repository.RefreshTokenRepository;
import vn.edu.usth.hanoilens.shared.Ids;
import vn.edu.usth.hanoilens.shared.JwtProperties;

/** Mints access JWTs (minimal claims) and opaque refresh tokens (only their SHA-256 is persisted). */
@Component
class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder encoder;
    private final JwtProperties props;
    private final RefreshTokenRepository refreshTokens;

    TokenService(JwtEncoder encoder, JwtProperties props, RefreshTokenRepository refreshTokens) {
        this.encoder = encoder;
        this.props = props;
        this.refreshTokens = refreshTokens;
    }

    /** Issues a pair inside {@code family}; pass a fresh family for a new login, the old one when rotating. */
    TokenResponse issue(User user, UUID family) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .audience(List.of(props.audience()))
                .subject(user.getId().toString())     // no email, coordinates or object keys in claims
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiresAt(now.plus(props.accessTtl()))
                .build();
        String access = encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        refreshTokens.save(new RefreshToken(Ids.newId(), user.getId(), family, sha256Hex(refresh), now,
                now.plus(props.refreshTtl())));

        return new TokenResponse("Bearer", access, refresh, props.accessTtl().toSeconds());
    }

    static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
