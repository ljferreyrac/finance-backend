package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    @DisplayName("toEntity followed by toDomain round-trips every field")
    void roundTripsAllFields() {
        Instant createdAt = Instant.now().minusSeconds(120);
        Instant updatedAt = Instant.now().minusSeconds(60);
        Instant deletedAt = Instant.now();
        User user = new User(UUID.randomUUID(), "user@example.com", "hashed-password", "Juan Perez",
                "PEN", "America/Lima", createdAt, updatedAt, deletedAt);

        UserEntity entity = mapper.toEntity(user);
        User roundTripped = mapper.toDomain(entity);

        assertThat(roundTripped.getId()).isEqualTo(user.getId());
        assertThat(roundTripped.getEmail()).isEqualTo(user.getEmail());
        assertThat(roundTripped.getPasswordHash()).isEqualTo(user.getPasswordHash());
        assertThat(roundTripped.getFullName()).isEqualTo(user.getFullName());
        assertThat(roundTripped.getCurrency()).isEqualTo(user.getCurrency());
        assertThat(roundTripped.getTimezone()).isEqualTo(user.getTimezone());
        assertThat(roundTripped.getCreatedAt()).isEqualTo(createdAt);
        assertThat(roundTripped.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(roundTripped.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    @DisplayName("a null deletedAt (active user) survives the round trip as null")
    void nullDeletedAtSurvivesRoundTrip() {
        Instant now = Instant.now();
        User user = new User(UUID.randomUUID(), "user@example.com", "hash", "Name",
                "PEN", "America/Lima", now, now, null);

        User roundTripped = mapper.toDomain(mapper.toEntity(user));

        assertThat(roundTripped.getDeletedAt()).isNull();
    }
}
