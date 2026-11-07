package vn.edu.usth.hanoilens.modules.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Argon2id with the Spring Security 5.8+ recommended parameters (SECURITY §5). Needs BouncyCastle on the classpath. */
@Configuration
class PasswordConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
