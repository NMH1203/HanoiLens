package vn.edu.usth.hanoilens.modules.identity.service;

import java.util.UUID;
import org.springframework.stereotype.Component;
import vn.edu.usth.hanoilens.modules.identity.repository.UserRepository;
import vn.edu.usth.hanoilens.shared.AccountStatusPort;

/** Checked on every authenticated request: a disabled account's still-valid JWT stops working immediately. */
@Component
class AccountStatusAdapter implements AccountStatusPort {

    private final UserRepository users;

    AccountStatusAdapter(UserRepository users) {
        this.users = users;
    }

    @Override
    public boolean isActive(UUID userId) {
        return users.findById(userId).map(u -> u.isActive()).orElse(false);
    }
}
