package com.finanzasia.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("getters return the values passed to the constructor")
    void gettersReturnConstructorValues() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(120);
        Instant updatedAt = Instant.now().minusSeconds(60);

        User user = new User(id, "user@example.com", "hashed-password", "Juan Perez",
                "PEN", "America/Lima", createdAt, updatedAt, null);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.getFullName()).isEqualTo("Juan Perez");
        assertThat(user.getCurrency()).isEqualTo("PEN");
        assertThat(user.getTimezone()).isEqualTo("America/Lima");
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(user.getDeletedAt()).isNull();
    }

    @Nested
    @DisplayName("isDeleted")
    class IsDeleted {

        @Test
        @DisplayName("false when deletedAt is null")
        void falseWhenNotDeleted() {
            Instant now = Instant.now();
            User user = new User(UUID.randomUUID(), "a@b.com", "hash", "Name",
                    "PEN", "America/Lima", now, now, null);

            assertThat(user.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("true when deletedAt is set")
        void trueWhenDeleted() {
            Instant now = Instant.now();
            User user = new User(UUID.randomUUID(), "a@b.com", "hash", "Name",
                    "PEN", "America/Lima", now, now, now);

            assertThat(user.isDeleted()).isTrue();
        }
    }
}
