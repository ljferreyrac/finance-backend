package com.finanzasia.domain.exceptions;

import java.util.UUID;

public class CategoryNotFoundException extends RuntimeException {

    private final UUID categoryId;

    public CategoryNotFoundException(UUID categoryId) {
        super("Category not found: " + categoryId);
        this.categoryId = categoryId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }
}
