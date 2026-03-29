package com.finanzasia.domain.exceptions;

import java.util.UUID;

public class TagNotFoundException extends RuntimeException {

    private final UUID tagId;

    public TagNotFoundException(UUID tagId) {
        super("Tag not found or does not belong to the current user.");
        this.tagId = tagId;
    }

    public UUID getTagId() {
        return tagId;
    }
}
