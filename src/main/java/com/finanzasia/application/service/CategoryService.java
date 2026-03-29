package com.finanzasia.application.service;

import com.finanzasia.domain.exceptions.CategoryInUseException;
import com.finanzasia.domain.exceptions.CategoryNotFoundException;
import com.finanzasia.domain.exceptions.LastCategoryException;
import com.finanzasia.domain.model.Category;
import com.finanzasia.domain.port.in.CategoryUseCase;
import com.finanzasia.domain.port.out.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates all category use cases.
 * Enforces ownership checks, name uniqueness, default-category bookkeeping,
 * and safe deletion with optional expense reassignment.
 */
@Service
public class CategoryService implements CategoryUseCase {

    static final int MAX_CATEGORIES_PER_USER = 50;

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Category> listCategories(UUID userId) {
        return categoryRepository.findAllByUser(userId);
    }

    @Override
    @Transactional
    public Category createCategory(UUID userId, String name, String color,
                                   String icon, boolean isDefault) {
        if (categoryRepository.countCategoriesByUser(userId) >= MAX_CATEGORIES_PER_USER) {
            throw new com.finanzasia.domain.exceptions.CategoryLimitExceededException(MAX_CATEGORIES_PER_USER);
        }

        if (categoryRepository.existsByUserAndName(userId, name.strip())) {
            throw new com.finanzasia.domain.exceptions.DuplicateCategoryNameException(name);
        }

        Instant now = Instant.now();

        if (isDefault) {
            categoryRepository.clearDefaultForUser(userId);
        }

        Category category = new Category(
                UUID.randomUUID(),
                userId,
                name,
                color,
                icon,
                isDefault,
                nextPosition(userId),
                now,
                now);

        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category updateCategory(UUID userId, UUID categoryId,
                                   String name, String color, String icon, Integer position) {
        Category category = categoryRepository.findByIdAndUser(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        String trimmedName = (name != null) ? name.strip() : null;
        if (trimmedName != null && trimmedName.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (trimmedName != null && !trimmedName.equals(category.getName())
                && categoryRepository.existsByUserAndName(userId, trimmedName)) {
            throw new com.finanzasia.domain.exceptions.DuplicateCategoryNameException(trimmedName);
        }

        category.update(trimmedName, color, icon, position, Instant.now());
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID userId, UUID categoryId, UUID reassignTo) {
        Category category = categoryRepository.findByIdAndUser(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        if (categoryRepository.countCategoriesByUser(userId) <= 1) {
            throw new LastCategoryException();
        }

        long expenseCount = categoryRepository.countExpensesByCategory(categoryId);
        if (expenseCount > 0) {
            if (reassignTo == null) {
                throw new CategoryInUseException(categoryId, expenseCount);
            }
            // Validate that the reassignment target also belongs to this user
            categoryRepository.findByIdAndUser(reassignTo, userId)
                    .orElseThrow(() -> new CategoryNotFoundException(reassignTo));
            categoryRepository.reassignExpenses(categoryId, reassignTo, userId);
        }

        categoryRepository.delete(category.getId());
    }

    @Override
    @Transactional
    public Category setDefaultCategory(UUID userId, UUID categoryId) {
        Category category = categoryRepository.findByIdAndUser(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        categoryRepository.clearDefaultForUser(userId);
        category.markAsDefault(Instant.now());
        return categoryRepository.save(category);
    }

    // --- private helpers ---

    /**
     * Computes the next available position for a new category.
     * New categories are appended after the current last position.
     */
    private int nextPosition(UUID userId) {
        List<Category> existing = categoryRepository.findAllByUser(userId);
        return existing.stream()
                .mapToInt(Category::getPosition)
                .max()
                .orElse(0) + 1;
    }
}
