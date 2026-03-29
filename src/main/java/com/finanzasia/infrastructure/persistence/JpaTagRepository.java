package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.Tag;
import com.finanzasia.domain.port.out.TagRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter that implements the domain {@link TagRepository} output port using Spring Data JPA.
 */
public class JpaTagRepository implements TagRepository {

    private final JpaTagRepositoryPort jpaPort;

    public JpaTagRepository(JpaTagRepositoryPort jpaPort) {
        this.jpaPort = jpaPort;
    }

    @Override
    public List<Tag> findByUserId(UUID userId) {
        return jpaPort.findByUserId(userId)
                .stream()
                .map(TagEntity::toTag)
                .toList();
    }

    @Override
    public Optional<Tag> findByIdAndUserId(UUID tagId, UUID userId) {
        return jpaPort.findByIdAndUserId(tagId, userId).map(TagEntity::toTag);
    }

    @Override
    public List<Tag> findByIdsAndUserId(Set<UUID> tagIds, UUID userId) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        return jpaPort.findByIdInAndUserId(tagIds, userId)
                .stream()
                .map(TagEntity::toTag)
                .toList();
    }

    @Override
    public boolean existsByUserIdAndName(UUID userId, String name) {
        return jpaPort.existsByUserIdAndName(userId, name);
    }

    @Override
    public Tag save(Tag tag) {
        TagEntity entity = TagEntity.fromTag(tag);
        TagEntity saved = jpaPort.save(entity);
        return saved.toTag();
    }

    @Override
    public boolean deleteByIdAndUserId(UUID tagId, UUID userId) {
        return jpaPort.deleteByIdAndUserId(tagId, userId) > 0;
    }
}
