package com.finanzasia.domain.port.out;

import com.finanzasia.domain.model.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Must remain free of any JPA, Spring, or JDBC types.
 */
public interface CategoryRepository {

    /** Ordered by position ASC, name ASC. */
    List<Category> findAllByUser(UUID userId);

    Optional<Category> findByIdAndUser(UUID id, UUID userId);

    boolean existsByUserAndName(UUID userId, String name);

    /** Counts only non-deleted expenses. */
    long countExpensesByCategory(UUID categoryId);

    long countCategoriesByUser(UUID userId);

    Category save(Category category);

    void delete(UUID categoryId);

    /** Scoped to the owning user. */
    void reassignExpenses(UUID fromCategoryId, UUID toCategoryId, UUID userId);

    void clearDefaultForUser(UUID userId);
}
