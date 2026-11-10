package vn.edu.usth.hanoilens.modules.identity;

import java.util.Optional;
import java.util.UUID;

/** Published application-service interface of the identity module. */
public interface IdentityApi {

    Optional<UserView> findUser(UUID userId);

    /** Applies only the supplied (non-null) fields and returns the updated user. */
    UserView updateProfile(UUID userId, ProfileUpdate update);
}
