package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.Category;

import java.util.List;
import java.util.UUID;

/**
 * All methods enforce ownership: operations on categories belonging to other
 * users are rejected with CategoryNotFoundException (no information leakage).
 */
public interface CategoryUseCase {

    /** Ordered by position ASC, name ASC. */
    List<Category> listCategories(UUID userId);

    /**
     * If isDefault is true, any previously default category is cleared first.
     * Throws DuplicateCategoryNameException (409) if the name is already taken.
     */
    Category createCategory(UUID userId, String name, String color, String icon, boolean isDefault);

    /**
     * Only fields with non-null values in the request are applied; name and
     * position may be changed. Color and icon are always replaced (nullable).
     */
    Category updateCategory(UUID userId, UUID categoryId, String name, String color,
                            String icon, Integer position);

    /**
     * If the category still has expenses attached and reassignTo is null,
     * throws CategoryInUseException carrying the count. If reassignTo is
     * provided it must belong to the same user; expenses are migrated first.
     */
    void deleteCategory(UUID userId, UUID categoryId, UUID reassignTo);

    /** Marks a category as the default, clearing the previous default first. */
    Category setDefaultCategory(UUID userId, UUID categoryId);
}
