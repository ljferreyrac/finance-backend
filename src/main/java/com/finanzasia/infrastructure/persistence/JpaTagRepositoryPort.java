package com.finanzasia.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// Named "Port" to distinguish it from the domain-facing JpaTagRepository adapter.
public interface JpaTagRepositoryPort extends JpaRepository<TagEntity, UUID> {

    List<TagEntity> findByUserId(UUID userId);

    Optional<TagEntity> findByIdAndUserId(UUID tagId, UUID userId);

    List<TagEntity> findByIdInAndUserId(Set<UUID> ids, UUID userId);

    boolean existsByUserIdAndName(UUID userId, String name);

    long deleteByIdAndUserId(UUID tagId, UUID userId);
}
