package com.finanzasia.api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    @DisplayName("exposes the id, uses email as the username, and has no usable password or authorities")
    void exposesIdentityWithNoCredentials() {
        UUID id = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(id, "user@example.com");

        assertThat(principal.getId()).isEqualTo(id);
        assertThat(principal.getUsername()).isEqualTo("user@example.com");
        assertThat(principal.getPassword()).isNull();
        assertThat(principal.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("is always reported as an active, non-expired, non-locked account")
    void alwaysReportsActiveAccount() {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "user@example.com");

        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
        assertThat(principal.isEnabled()).isTrue();
    }
}
