package com.finanzasia.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Named "Port" to distinguish it from the domain-facing JpaCategoryRepository adapter.
public interface JpaCategoryRepositoryPort extends JpaRepository<CategoryEntity, UUID> {

    List<CategoryEntity> findByUserIdOrderByPositionAscNameAsc(UUID userId);

    Optional<CategoryEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndName(UUID userId, String name);

    long countByUserId(UUID userId);

    @Query("SELECT COUNT(t) FROM TransactionEntity t WHERE t.categoryId = :categoryId AND t.deletedAt IS NULL")
    long countExpensesByCategory(@Param("categoryId") UUID categoryId);

    @Modifying
    @Query(value = "UPDATE transactions SET category_id = :toId WHERE category_id = :fromId AND user_id = :userId",
           nativeQuery = true)
    void reassignExpenses(@Param("fromId") UUID fromId, @Param("toId") UUID toId, @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE CategoryEntity c SET c.isDefault = false WHERE c.userId = :userId")
    void clearDefaultForUser(@Param("userId") UUID userId);
}
