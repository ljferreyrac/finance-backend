package com.finanzasia.application.service;

import com.finanzasia.domain.exceptions.DuplicateTagException;
import com.finanzasia.domain.exceptions.TagNotFoundException;
import com.finanzasia.domain.model.Tag;
import com.finanzasia.domain.port.in.TagUseCase;
import com.finanzasia.domain.port.out.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Orchestrates all tag use cases.
 * Enforces ownership checks and name uniqueness per user.
 */
@Service
public class TagService implements TagUseCase {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Override
    public List<Tag> listTags(UUID userId) {
        return tagRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public Tag createTag(UUID userId, String name, String color) {
        String trimmedName = name.strip().toLowerCase(Locale.ROOT);
        if (tagRepository.existsByUserIdAndName(userId, trimmedName)) {
            throw new DuplicateTagException(trimmedName);
        }
        Tag tag = new Tag(null, userId, trimmedName, color);
        return tagRepository.save(tag);
    }

    @Override
    @Transactional
    public Tag updateTag(UUID userId, UUID tagId, String name, String color) {
        Tag existing = tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new TagNotFoundException(tagId));

        String trimmedName = (name != null) ? name.strip().toLowerCase(Locale.ROOT) : existing.name();
        if (!trimmedName.equals(existing.name())
                && tagRepository.existsByUserIdAndName(userId, trimmedName)) {
            throw new DuplicateTagException(trimmedName);
        }

        String resolvedColor = (color != null) ? color : existing.color();

        Tag updated = new Tag(existing.id(), existing.userId(), trimmedName, resolvedColor);
        return tagRepository.save(updated);
    }

    @Override
    @Transactional
    public void deleteTag(UUID userId, UUID tagId) {
        boolean deleted = tagRepository.deleteByIdAndUserId(tagId, userId);
        if (!deleted) {
            throw new TagNotFoundException(tagId);
        }
    }
}
