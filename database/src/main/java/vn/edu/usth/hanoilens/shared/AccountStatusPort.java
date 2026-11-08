package vn.edu.usth.hanoilens.shared;

import java.util.UUID;

/**
 * Lets the security layer check "current account status" on every request (SECURITY §5) without
 * {@code shared} depending on a business module. Implemented by the identity module.
 */
public interface AccountStatusPort {

    boolean isActive(UUID userId);
}
