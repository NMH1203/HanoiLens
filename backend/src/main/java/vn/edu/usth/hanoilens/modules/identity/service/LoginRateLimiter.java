package vn.edu.usth.hanoilens.modules.identity.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import vn.edu.usth.hanoilens.shared.ApiException;

/**
 * Sliding-window limiter for credential endpoints (SECURITY TM-1.2: strict IP + account limits -> 429).
 * In-memory, so it is per instance; replace with a shared store (Redis/DB) before running several replicas.
 */
@Component
class LoginRateLimiter {

    static final int MAX_ATTEMPTS = 10;
    static final Duration WINDOW = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    /** Records an attempt for each key and throws 429 once any key exceeded the window budget. */
    void check(String... keys) {
        Instant now = Instant.now();
        boolean exceeded = false;
        for (String key : keys) {
            Deque<Instant> window = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
            synchronized (window) {
                while (!window.isEmpty() && window.peekFirst().isBefore(now.minus(WINDOW))) {
                    window.pollFirst();
                }
                if (window.size() >= MAX_ATTEMPTS) {
                    exceeded = true;
                } else {
                    window.addLast(now);
                }
            }
        }
        if (attempts.size() > 50_000) {
            attempts.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    return e.getValue().isEmpty() || e.getValue().peekLast().isBefore(now.minus(WINDOW));
                }
            });
        }
        if (exceeded) {
            throw new ApiException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                    "Too many attempts. Try again later.");
        }
    }
}
