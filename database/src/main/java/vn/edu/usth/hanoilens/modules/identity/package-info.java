/**
 * Identity: accounts, credentials, JWT access tokens and rotating refresh tokens.
 * Owns tables: users, refresh_tokens. Other modules use {@link vn.edu.usth.hanoilens.modules.identity.IdentityApi}.
 */
@ApplicationModule(displayName = "Identity")
package vn.edu.usth.hanoilens.modules.identity;

import org.springframework.modulith.ApplicationModule;
