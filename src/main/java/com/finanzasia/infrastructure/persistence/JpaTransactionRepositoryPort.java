package com.finanzasia.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA interface for transaction persistence.
 * The paged list query uses a native SQL cursor approach (keyset pagination)
 * so it remains efficient for large result sets.
 */
public interface JpaTransactionRepositoryPort extends JpaRepository<TransactionEntity, UUID> {

    Optional<TransactionEntity> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    @Modifying
    @Query("UPDATE TransactionEntity t SET t.deletedAt = :now, t.updatedAt = :now WHERE t.id = :id")
    void softDelete(@Param("id") UUID id, @Param("now") Instant now);

    @Query(value = """
            SELECT DISTINCT t.* FROM transactions t
            LEFT JOIN transaction_tags tt
                ON (:tagId IS NULL OR (tt.transaction_id = t.id AND tt.tag_id = CAST(:tagId AS uuid)))
            WHERE t.user_id = :userId
              AND t.deleted_at IS NULL
              AND (:type IS NULL OR t.type = :type)
              AND (:accountId IS NULL OR t.account_id = :accountId
                   OR t.from_account_id = :accountId
                   OR t.to_account_id = :accountId)
              AND (:categoryId IS NULL OR t.category_id = :categoryId)
              AND (:currency IS NULL OR t.currency = :currency)
              AND (:from IS NULL OR t.transaction_date >= CAST(:from AS date))
              AND (:to IS NULL OR t.transaction_date <= CAST(:to AS date))
              AND (:tagId IS NULL OR tt.tag_id IS NOT NULL)
              AND (CAST(:cursorDate AS date) IS NULL OR t.transaction_date < :cursorDate
                   OR (t.transaction_date = :cursorDate AND CAST(t.id AS text) < CAST(:cursorId AS text)))
            ORDER BY t.transaction_date DESC, t.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<TransactionEntity> findWithFilter(
            @Param("userId") UUID userId,
            @Param("type") String type,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("currency") String currency,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("tagId") String tagId,
            @Param("cursorDate") LocalDate cursorDate,
            @Param("cursorId") String cursorId,
            @Param("limit") int limit);

    @Query(value = """
            SELECT COUNT(DISTINCT t.id) FROM transactions t
            LEFT JOIN transaction_tags tt
                ON (:tagId IS NULL OR (tt.transaction_id = t.id AND tt.tag_id = CAST(:tagId AS uuid)))
            WHERE t.user_id = :userId
              AND t.deleted_at IS NULL
              AND (:type IS NULL OR t.type = :type)
              AND (:accountId IS NULL OR t.account_id = :accountId
                   OR t.from_account_id = :accountId
                   OR t.to_account_id = :accountId)
              AND (:categoryId IS NULL OR t.category_id = :categoryId)
              AND (:currency IS NULL OR t.currency = :currency)
              AND (:from IS NULL OR t.transaction_date >= CAST(:from AS date))
              AND (:to IS NULL OR t.transaction_date <= CAST(:to AS date))
              AND (:tagId IS NULL OR tt.tag_id IS NOT NULL)
            """, nativeQuery = true)
    long countWithFilter(
            @Param("userId") UUID userId,
            @Param("type") String type,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("currency") String currency,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("tagId") String tagId);
}
