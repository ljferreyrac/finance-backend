package com.finanzasia.domain.port.out;

import com.finanzasia.domain.model.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for category persistence.
 * Implementations live in the infrastructure layer; this interface must
 * remain free of any JPA, Spring, or JDBC types.
 */
public interface CategoryRepository {

    /** Returns all categories belonging to the user, ordered by position ASC, name ASC. */
    List<Category> findAllByUser(UUID userId);

    Optional<Category> findByIdAndUser(UUID id, UUID userId);

    boolean existsByUserAndName(UUID userId, String name);

    /** Returns the number of non-deleted expenses referencing this category. */
    long countExpensesByCategory(UUID categoryId);

    long countCategoriesByUser(UUID userId);

    Category save(Category category);

    void delete(UUID categoryId);

    /** Moves all expenses from one category to another, scoped to the owning user. */
    void reassignExpenses(UUID fromCategoryId, UUID toCategoryId, UUID userId);

    /** Clears the isDefault flag on all categories owned by the user. */
    void clearDefaultForUser(UUID userId);
}
