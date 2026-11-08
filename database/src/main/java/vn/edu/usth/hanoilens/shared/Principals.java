package vn.edu.usth.hanoilens.shared;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** The authenticated user id always comes from the verified JWT subject, never from a request body. */
public final class Principals {

    private Principals() {
    }

    public static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
