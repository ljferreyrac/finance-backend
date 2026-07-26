package com.finanzasia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * {@link UserDetailsServiceAutoConfiguration} is excluded deliberately. It activates when no
 * {@code UserDetailsService}, {@code AuthenticationManager} or {@code AuthenticationProvider}
 * bean exists and creates an {@code InMemoryUserDetailsManager} holding user "user" with a
 * random password it prints at every startup.
 *
 * <p>This application authenticates only by JWT: {@code JwtAuthFilter} is the sole filter that
 * can populate the security context, and {@code SecurityConfig} enables neither form login nor
 * HTTP Basic, so there is no entry point that would ever accept those generated credentials.
 * The auto-configured account is therefore unusable but not harmless - it logs a password on
 * every boot, and it would silently become a live account the moment anyone added
 * {@code httpBasic()} or {@code formLogin()}. Excluding it states the intent instead of relying
 * on an unrelated bean happening to suppress it.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class FinanzasIaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanzasIaApplication.class, args);
    }
}
