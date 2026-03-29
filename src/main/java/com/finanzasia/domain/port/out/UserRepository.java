package com.finanzasia.domain.port.out;

import com.finanzasia.domain.model.User;

import java.util.Optional;
import java.util.UUID;

/** Output port: persistence operations for the {@link User} aggregate. */
public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    boolean existsByEmail(String email);
}
