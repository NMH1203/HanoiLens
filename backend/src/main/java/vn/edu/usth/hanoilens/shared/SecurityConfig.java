package vn.edu.usth.hanoilens.shared;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless bearer-JWT security (SECURITY §5): HS256 allowlist, issuer + audience + expiry validation and a
 * live account-status check. Method/role rules: {@code /v1/admin/**} needs ADMIN; everything else except the
 * public discovery and auth endpoints needs a valid access token.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, JwtDecoder jwtDecoder, ApiSecurityHandlers handlers)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())            // stateless bearer API, no cookies
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/v1/auth/register", "/v1/auth/login",
                                "/v1/auth/refresh", "/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/destinations", "/v1/destinations/nearby",
                                "/v1/destinations/*", "/v1/destinations/*/landmarks", "/v1/landmarks/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/events", "/v1/events/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(handlers).accessDeniedHandler(handlers))
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.decoder(jwtDecoder).jwtAuthenticationConverter(roleConverter()))
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties props, AccountStatusPort accounts) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(props.secretBytes(), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)                         // explicit algorithm allowlist
                .build();

        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                aud -> aud != null && aud.contains(props.audience()));
        OAuth2TokenValidator<Jwt> activeAccount = token -> {
            try {
                return accounts.isActive(UUID.fromString(token.getSubject()))
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Account is not active.", null));
            } catch (IllegalArgumentException | NullPointerException ex) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid subject.", null));
            }
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(props.issuer()),    // exp/nbf + iss
                audience,
                activeAccount));
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties props) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(props.secretBytes()));
    }

    private static JwtAuthenticationConverter roleConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
