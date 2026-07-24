package com.finanzasia.domain.port.out;

import com.finanzasia.domain.model.Tag;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TagRepository {

    List<Tag> findByUserId(UUID userId);

    Optional<Tag> findByIdAndUserId(UUID tagId, UUID userId);

    /**
     * Returns only the tags whose IDs are in {@code tagIds} AND whose owner is {@code userId}.
     * Used to validate tag ownership in bulk before attaching them to a transaction.
     */
    List<Tag> findByIdsAndUserId(Set<UUID> tagIds, UUID userId);

    boolean existsByUserIdAndName(UUID userId, String name);

    Tag save(Tag tag);

    /**
     * Deletes the tag only if it belongs to {@code userId}.
     * Returns true if a row was deleted, false if the tag did not exist or is owned by a different user.
     */
    boolean deleteByIdAndUserId(UUID tagId, UUID userId);
}
