package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.User;
import com.finanzasia.domain.port.out.UserRepository;

import java.util.Optional;
import java.util.UUID;

public class JpaUserRepository implements UserRepository {

    private final JpaUserRepositoryPort jpaPort;
    private final UserMapper mapper;

    public JpaUserRepository(JpaUserRepositoryPort jpaPort, UserMapper mapper) {
        this.jpaPort = jpaPort;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        UserEntity saved = jpaPort.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaPort.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaPort.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaPort.existsByEmail(email);
    }
}
