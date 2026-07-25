package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUserRepositoryTest {

    @Mock
    private JpaUserRepositoryPort jpaPort;

    private JpaUserRepository repository;
    private final UserMapper mapper = new UserMapper();

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = new JpaUserRepository(jpaPort, mapper);
    }

    private User buildUser() {
        Instant now = Instant.now();
        return new User(USER_ID, "user@example.com", "hash", "Juan Perez", "PEN", "America/Lima",
                now, now, null);
    }

    @Test
    @DisplayName("save maps the domain user to an entity and the saved entity back to a domain user")
    void saveConvertsToEntityAndBack() {
        User user = buildUser();
        UserEntity savedEntity = mapper.toEntity(user);
        when(jpaPort.save(any(UserEntity.class))).thenReturn(savedEntity);

        User result = repository.save(user);

        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("findByEmail maps a present entity")
    void findByEmailMapsPresentEntity() {
        UserEntity entity = mapper.toEntity(buildUser());
        when(jpaPort.findByEmail("user@example.com")).thenReturn(Optional.of(entity));

        Optional<User> result = repository.findByEmail("user@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("findByEmail returns empty when the port finds nothing")
    void findByEmailReturnsEmptyWhenAbsent() {
        when(jpaPort.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThat(repository.findByEmail("missing@example.com")).isEmpty();
    }

    @Test
    @DisplayName("findById maps a present entity")
    void findByIdMapsPresentEntity() {
        UserEntity entity = mapper.toEntity(buildUser());
        when(jpaPort.findById(USER_ID)).thenReturn(Optional.of(entity));

        Optional<User> result = repository.findById(USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("existsByEmail delegates directly to the port")
    void existsByEmailDelegates() {
        when(jpaPort.existsByEmail("user@example.com")).thenReturn(true);

        assertThat(repository.existsByEmail("user@example.com")).isTrue();
    }
}
