package com.finanzasia.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Named "Port" to distinguish it from the domain-facing JpaAccountRepository adapter.
public interface JpaAccountRepositoryPort extends JpaRepository<AccountEntity, UUID> {

    List<AccountEntity> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<AccountEntity> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    @Query(value = """
            SELECT COUNT(t) FROM TransactionEntity t
            WHERE (t.accountId = :id OR t.fromAccountId = :id OR t.toAccountId = :id)
              AND t.deletedAt IS NULL
            """)
    long countTransactionsByAccountId(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE AccountEntity a SET a.isDefault = false WHERE a.userId = :userId")
    void clearDefaultForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE AccountEntity a SET a.currentBalance = a.currentBalance + :delta WHERE a.id = :id")
    void adjustBalance(@Param("id") UUID id, @Param("delta") BigDecimal delta);
}
