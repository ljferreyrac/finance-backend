package com.finanzasia.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Holds the password encoder on its own, deliberately kept out of
 * {@link SecurityConfig}.
 *
 * <p>SecurityConfig depends on {@link JwtAuthFilter}, which authenticates via
 * AuthService, which needs this encoder. Declaring the bean on SecurityConfig
 * closes that loop into a bean cycle and the context refuses to start. Keeping
 * it here leaves the encoder with no dependencies of its own, so nothing can
 * cycle back through it.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
