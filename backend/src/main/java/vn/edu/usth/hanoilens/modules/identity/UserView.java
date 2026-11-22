package vn.edu.usth.hanoilens.modules.identity;

import java.time.Instant;
import java.util.UUID;
import vn.edu.usth.hanoilens.shared.LocationPrecision;

/** Read model of a user; never contains the password hash. */
public record UserView(UUID id, String email, String displayName, String role, String locale,
                       LocationPrecision defaultLocationPrecision, Instant createdAt) {
}
