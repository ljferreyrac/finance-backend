package com.finanzasia.api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderConfigTest {

    @Test
    @DisplayName("passwordEncoder builds a BCrypt-backed encoder that can hash and verify a password")
    void passwordEncoderBeanHashesAndVerifies() {
        PasswordEncoder encoder = new PasswordEncoderConfig().passwordEncoder();

        String hash = encoder.encode("MiContrasena123");

        assertThat(hash).isNotEqualTo("MiContrasena123");
        assertThat(encoder.matches("MiContrasena123", hash)).isTrue();
        assertThat(encoder.matches("wrong-password", hash)).isFalse();
    }
}
