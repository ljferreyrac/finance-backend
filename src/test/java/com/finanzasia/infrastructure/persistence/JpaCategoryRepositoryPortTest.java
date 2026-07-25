package com.finanzasia.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JpaCategoryRepositoryPortTest extends AbstractPostgresTest {

    @Autowired
    private JpaCategoryRepositoryPort categoryPort;

    @Autowired
    private TestEntityManager em;

    private UUID userId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        userId = PersistenceFixtures.user(em).getId();
        accountId = PersistenceFixtures.account(em, userId).getId();
    }

    @Nested
    @DisplayName("derived finders")
    class DerivedFinders {

        @Test
        @DisplayName("findByUserIdOrderByPositionAscNameAsc orders by position then name")
        void ordersByPositionThenName() {
            CategoryEntity c1 = PersistenceFixtures.category(em, userId, "zzz");
            em.find(CategoryEntity.class, c1.getId()).setPosition(1);
            CategoryEntity c2 = PersistenceFixtures.category(em, userId, "aaa");
            em.find(CategoryEntity.class, c2.getId()).setPosition(0);
            em.flush();

            List<CategoryEntity> rows = categoryPort.findByUserIdOrderByPositionAscNameAsc(userId);

            assertThat(rows).extracting(CategoryEntity::getName).containsExactly("aaa", "zzz");
        }

        @Test
        @DisplayName("findByIdAndUserId does not return another user's category")
        void findByIdAndUserIdScopesToOwner() {
            CategoryEntity category = PersistenceFixtures.category(em, userId, "food");
            UUID otherUserId = PersistenceFixtures.user(em).getId();

            Optional<CategoryEntity> found = categoryPort.findByIdAndUserId(category.getId(), otherUserId);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("existsByUserIdAndName is case-sensitive and user-scoped")
        void existsByUserIdAndName() {
            PersistenceFixtures.category(em, userId, "groceries");
            UUID otherUserId = PersistenceFixtures.user(em).getId();

            assertThat(categoryPort.existsByUserIdAndName(userId, "groceries")).isTrue();
            assertThat(categoryPort.existsByUserIdAndName(userId, "Groceries")).isFalse();
            assertThat(categoryPort.existsByUserIdAndName(otherUserId, "groceries")).isFalse();
        }
    }

    @Nested
    @DisplayName("countExpensesByCategory")
    class CountExpensesByCategory {

        @Test
        @DisplayName("counts non-deleted transactions in the category, regardless of type")
        void countsNonDeletedTransactions() {
            CategoryEntity category = PersistenceFixtures.category(em, userId, "food");
            PersistenceFixtures.expense(em, userId, accountId, category.getId(),
                    new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 3, 1), "A");
            TransactionEntity deleted = PersistenceFixtures.expense(em, userId, accountId, category.getId(),
                    new BigDecimal("20.00"), "PEN", LocalDate.of(2026, 3, 2), "B");
            em.find(TransactionEntity.class, deleted.getId()).setDeletedAt(java.time.Instant.now());
            em.flush();

            long count = categoryPort.countExpensesByCategory(category.getId());

            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("reassignExpenses (native SQL)")
    class ReassignExpenses {

        @Test
        @DisplayName("moves all of one category's transactions to another, scoped to the user")
        void movesTransactionsBetweenCategories() {
            CategoryEntity source = PersistenceFixtures.category(em, userId, "old");
            CategoryEntity target = PersistenceFixtures.category(em, userId, "new");
            TransactionEntity t1 = PersistenceFixtures.expense(em, userId, accountId, source.getId(),
                    new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 3, 1), "A");
            TransactionEntity t2 = PersistenceFixtures.expense(em, userId, accountId, source.getId(),
                    new BigDecimal("20.00"), "PEN", LocalDate.of(2026, 3, 2), "B");

            categoryPort.reassignExpenses(source.getId(), target.getId(), userId);
            em.flush();
            em.clear();

            assertThat(em.find(TransactionEntity.class, t1.getId()).getCategoryId()).isEqualTo(target.getId());
            assertThat(em.find(TransactionEntity.class, t2.getId()).getCategoryId()).isEqualTo(target.getId());
        }

        @Test
        @DisplayName("does not move another user's transactions even if categorized under the same id")
        void doesNotMoveAnotherUsersTransactions() {
            UUID otherUserId = PersistenceFixtures.user(em).getId();
            UUID otherAccountId = PersistenceFixtures.account(em, otherUserId).getId();
            CategoryEntity source = PersistenceFixtures.category(em, userId, "old");
            CategoryEntity target = PersistenceFixtures.category(em, userId, "new");
            TransactionEntity otherUsersTransaction = PersistenceFixtures.expense(
                    em, otherUserId, otherAccountId, null,
                    new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 3, 1), "Other");

            categoryPort.reassignExpenses(source.getId(), target.getId(), userId);
            em.flush();
            em.clear();

            assertThat(em.find(TransactionEntity.class, otherUsersTransaction.getId()).getCategoryId()).isNull();
        }
    }

    @Nested
    @DisplayName("clearDefaultForUser")
    class ClearDefaultForUser {

        @Test
        @DisplayName("unsets isDefault on every category for the user, leaves other users untouched")
        void clearsOnlyThisUsersCategories() {
            CategoryEntity mine = PersistenceFixtures.category(em, userId, "food");
            em.find(CategoryEntity.class, mine.getId()).setDefault(true);
            em.flush();

            UUID otherUserId = PersistenceFixtures.user(em).getId();
            CategoryEntity othersDefault = PersistenceFixtures.category(em, otherUserId, "food");
            em.find(CategoryEntity.class, othersDefault.getId()).setDefault(true);
            em.flush();

            categoryPort.clearDefaultForUser(userId);
            em.flush();
            em.clear();

            assertThat(em.find(CategoryEntity.class, mine.getId()).isDefault()).isFalse();
            assertThat(em.find(CategoryEntity.class, othersDefault.getId()).isDefault()).isTrue();
        }
    }
}
