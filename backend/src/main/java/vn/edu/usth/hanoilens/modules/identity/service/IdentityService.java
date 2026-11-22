package vn.edu.usth.hanoilens.modules.identity.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.usth.hanoilens.modules.identity.IdentityApi;
import vn.edu.usth.hanoilens.modules.identity.ProfileUpdate;
import vn.edu.usth.hanoilens.modules.identity.UserView;
import vn.edu.usth.hanoilens.modules.identity.entity.User;
import vn.edu.usth.hanoilens.modules.identity.repository.UserRepository;
import vn.edu.usth.hanoilens.shared.ApiException;
import vn.edu.usth.hanoilens.shared.LocationPrecision;

@Service
class IdentityService implements IdentityApi {

    private final UserRepository users;

    IdentityService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserView> findUser(UUID userId) {
        return users.findById(userId).map(IdentityService::toView);
    }

    @Override
    @Transactional
    public UserView updateProfile(UUID userId, ProfileUpdate update) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found."));
        if (update.displayName() != null) {
            String name = update.displayName().trim();
            if (name.isEmpty() || name.length() > 120) {
                throw ApiException.invalidField("displayName", "Must be 1 to 120 characters.");
            }
            user.setDisplayName(name);
        }
        if (update.locale() != null) {
            user.setLocale(update.locale());
        }
        if (update.defaultLocationPrecision() != null) {
            user.setDefaultLocationDisplay(update.defaultLocationPrecision().name());
        }
        return toView(user);          // managed entity: flushed on commit
    }

    static UserView toView(User u) {
        return new UserView(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(), u.getLocale(),
                LocationPrecision.valueOf(u.getDefaultLocationDisplay()), u.getCreatedAt());
    }
}
