package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.Category;
import com.finanzasia.domain.port.out.CategoryRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JpaCategoryRepository implements CategoryRepository {

    private final JpaCategoryRepositoryPort jpaPort;

    public JpaCategoryRepository(JpaCategoryRepositoryPort jpaPort) {
        this.jpaPort = jpaPort;
    }

    @Override
    public List<Category> findAllByUser(UUID userId) {
        return jpaPort.findByUserIdOrderByPositionAscNameAsc(userId)
                .stream()
                .map(CategoryEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Category> findByIdAndUser(UUID id, UUID userId) {
        return jpaPort.findByIdAndUserId(id, userId).map(CategoryEntity::toDomain);
    }

    @Override
    public boolean existsByUserAndName(UUID userId, String name) {
        return jpaPort.existsByUserIdAndName(userId, name);
    }

    @Override
    public long countExpensesByCategory(UUID categoryId) {
        return jpaPort.countExpensesByCategory(categoryId);
    }

    @Override
    public long countCategoriesByUser(UUID userId) {
        return jpaPort.countByUserId(userId);
    }

    @Override
    public Category save(Category category) {
        CategoryEntity entity = CategoryEntity.fromDomain(category);
        return jpaPort.save(entity).toDomain();
    }

    @Override
    public void delete(UUID categoryId) {
        jpaPort.deleteById(categoryId);
    }

    @Override
    public void reassignExpenses(UUID fromCategoryId, UUID toCategoryId, UUID userId) {
        jpaPort.reassignExpenses(fromCategoryId, toCategoryId, userId);
    }

    @Override
    public void clearDefaultForUser(UUID userId) {
        jpaPort.clearDefaultForUser(userId);
    }
}
