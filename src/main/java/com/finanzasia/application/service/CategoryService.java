package com.finanzasia.application.service;

import com.finanzasia.domain.exceptions.CategoryInUseException;
import com.finanzasia.domain.exceptions.CategoryNotFoundException;
import com.finanzasia.domain.exceptions.LastCategoryException;
import com.finanzasia.domain.model.Category;
import com.finanzasia.domain.model.CategoryDetail;
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
    public List<CategoryDetail> listCategories(UUID userId) {
        return categoryRepository.findAllByUser(userId).stream()
                .map(this::toDetail)
                .toList();
    }

    @Override
    @Transactional
    public CategoryDetail createCategory(UUID userId, String name, String color,
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

        return toDetail(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryDetail updateCategory(UUID userId, UUID categoryId,
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
        return toDetail(categoryRepository.save(category));
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
            categoryRepository.findByIdAndUser(reassignTo, userId)
                    .orElseThrow(() -> new CategoryNotFoundException(reassignTo));
            categoryRepository.reassignExpenses(categoryId, reassignTo, userId);
        }

        categoryRepository.delete(category.getId());
    }

    @Override
    @Transactional
    public CategoryDetail setDefaultCategory(UUID userId, UUID categoryId) {
        Category category = categoryRepository.findByIdAndUser(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        categoryRepository.clearDefaultForUser(userId);
        category.markAsDefault(Instant.now());
        return toDetail(categoryRepository.save(category));
    }

    /** Appends after the current max position so existing ordering is never disturbed. */
    private int nextPosition(UUID userId) {
        List<Category> existing = categoryRepository.findAllByUser(userId);
        return existing.stream()
                .mapToInt(Category::getPosition)
                .max()
                .orElse(0) + 1;
    }

    /**
     * Attaches the expense count so the web layer never has to query for it
     * itself.
     */
    private CategoryDetail toDetail(Category category) {
        return new CategoryDetail(
                category,
                categoryRepository.countExpensesByCategory(category.getId()));
    }
}
