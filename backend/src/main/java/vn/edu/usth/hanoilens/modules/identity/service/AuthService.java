package vn.edu.usth.hanoilens.modules.identity.service;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.usth.hanoilens.modules.identity.UserView;
import vn.edu.usth.hanoilens.modules.identity.dto.LoginRequest;
import vn.edu.usth.hanoilens.modules.identity.dto.RegisterRequest;
import vn.edu.usth.hanoilens.modules.identity.dto.TokenResponse;
import vn.edu.usth.hanoilens.modules.identity.entity.RefreshToken;
import vn.edu.usth.hanoilens.modules.identity.entity.User;
import vn.edu.usth.hanoilens.modules.identity.repository.RefreshTokenRepository;
import vn.edu.usth.hanoilens.modules.identity.repository.UserRepository;
import vn.edu.usth.hanoilens.shared.ApiException;
import vn.edu.usth.hanoilens.shared.Ids;
import vn.edu.usth.hanoilens.shared.LocationPrecision;

@Service
public class AuthService {

    static final String GENERIC_LOGIN_FAILURE = "Invalid email or password.";
    static final String GENERIC_REFRESH_FAILURE = "The refresh token is invalid or expired.";

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final LoginRateLimiter limiter;
    /** Verified against when the email is unknown, so response time does not reveal registered emails. */
    private final String dummyHash;

    AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder,
            TokenService tokens, LoginRateLimiter limiter) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.limiter = limiter;
        this.dummyHash = passwordEncoder.encode("hanoilens-timing-equaliser");
    }

    @Transactional
    public UserView register(RegisterRequest request, String clientIp) {
        limiter.check("register-ip:" + clientIp);
        String email = normalise(request.email());
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_ALREADY_REGISTERED", "This email is already registered.");
        }
        // Role and status are fixed here; they are not part of RegisterRequest (TM-1.1).
        User user = new User(Ids.newId(), email, passwordEncoder.encode(request.password()),
                request.displayName().trim(), User.ROLE_USER, User.STATUS_ACTIVE, "vi",
                LocationPrecision.DESTINATION.name(), Instant.now());
        users.save(user);
        return IdentityService.toView(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request, String clientIp) {
        String email = normalise(request.email());
        limiter.check("login-ip:" + clientIp, "login-email:" + email);

        Optional<User> found = users.findByEmail(email);
        boolean passwordOk = passwordEncoder.matches(request.password(),
                found.map(User::getPasswordHash).orElse(dummyHash));
        if (found.isEmpty() || !passwordOk || !found.get().isActive()) {
            throw ApiException.unauthorized(GENERIC_LOGIN_FAILURE);   // one message for every failure cause
        }
        return tokens.issue(found.get(), Ids.newId());
    }

    /**
     * Rotates a refresh token. Presenting an already-used (rotated) token is treated as theft: the whole
     * family is revoked. {@code noRollbackFor} keeps that revocation even though we answer 401.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse refresh(String rawToken) {
        Instant now = Instant.now();
        RefreshToken stored = refreshTokens.findByTokenHash(TokenService.sha256Hex(rawToken))
                .orElseThrow(() -> ApiException.unauthorized(GENERIC_REFRESH_FAILURE));

        if (stored.getRevokedAt() != null) {
            throw ApiException.unauthorized(GENERIC_REFRESH_FAILURE);
        }
        if (stored.getUsedAt() != null) {                       // replay of a rotated token
            refreshTokens.revokeFamily(stored.getTokenFamily(), now);
            throw ApiException.unauthorized(GENERIC_REFRESH_FAILURE);
        }
        if (!stored.getExpiresAt().isAfter(now)) {
            throw ApiException.unauthorized(GENERIC_REFRESH_FAILURE);
        }
        User user = users.findById(stored.getUserId()).filter(User::isActive).orElse(null);
        if (user == null) {
            refreshTokens.revokeFamily(stored.getTokenFamily(), now);
            throw ApiException.unauthorized(GENERIC_REFRESH_FAILURE);
        }
        // Atomic guard against two concurrent refreshes with the same token.
        if (refreshTokens.markUsed(stored.getId(), now) == 0) {
            refreshTokens.revokeFamily(stored.getTokenFamily(), now);
            throw ApiException.unauthorized(GENERIC_REFRESH_FAILURE);
        }
        return tokens.issue(user, stored.getTokenFamily());
    }

    /**
     * With a refresh token: revokes that token's family. Without one (the Android client sends no body):
     * revokes every session of the authenticated caller. Always idempotent.
     */
    @Transactional
    public void logout(String rawToken, UUID authenticatedUserId) {
        Instant now = Instant.now();
        if (rawToken != null && !rawToken.isBlank()) {
            refreshTokens.findByTokenHash(TokenService.sha256Hex(rawToken))
                    .ifPresent(t -> refreshTokens.revokeFamily(t.getTokenFamily(), now));
        } else if (authenticatedUserId != null) {
            refreshTokens.revokeAllForUser(authenticatedUserId, now);
        }
    }

    private static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
