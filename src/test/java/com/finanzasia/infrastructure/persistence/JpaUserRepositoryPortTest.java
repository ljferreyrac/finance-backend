package com.finanzasia.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JpaUserRepositoryPortTest extends AbstractPostgresTest {

    @Autowired
    private JpaUserRepositoryPort userPort;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("findByEmail finds an existing user by exact email")
    void findByEmailFindsExistingUser() {
        UserEntity user = PersistenceFixtures.user(em);

        Optional<UserEntity> found = userPort.findByEmail(user.getEmail());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("findByEmail returns empty for an unknown email")
    void findByEmailEmptyWhenUnknown() {
        Optional<UserEntity> found = userPort.findByEmail("nobody@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail reflects whether the email is already registered")
    void existsByEmail() {
        UserEntity user = PersistenceFixtures.user(em);

        assertThat(userPort.existsByEmail(user.getEmail())).isTrue();
        assertThat(userPort.existsByEmail("nobody@example.com")).isFalse();
    }
}
