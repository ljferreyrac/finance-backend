package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.Tag;

import java.util.List;
import java.util.UUID;

public interface TagUseCase {

    List<Tag> listTags(UUID userId);

    Tag createTag(UUID userId, String name, String color);

    Tag updateTag(UUID userId, UUID tagId, String name, String color);

    void deleteTag(UUID userId, UUID tagId);
}
