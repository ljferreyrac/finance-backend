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

class JpaAccountRepositoryPortTest extends AbstractPostgresTest {

    @Autowired
    private JpaAccountRepositoryPort accountPort;

    @Autowired
    private TestEntityManager em;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = PersistenceFixtures.user(em).getId();
    }

    @Nested
    @DisplayName("derived finders")
    class DerivedFinders {

        @Test
        @DisplayName("findByUserIdOrderByCreatedAtAsc orders oldest first")
        void ordersByCreatedAtAscending() throws InterruptedException {
            AccountEntity first = PersistenceFixtures.account(em, userId);
            Thread.sleep(5);
            AccountEntity second = PersistenceFixtures.account(em, userId);

            List<AccountEntity> rows = accountPort.findByUserIdOrderByCreatedAtAsc(userId);

            assertThat(rows).extracting(AccountEntity::getId).containsExactly(first.getId(), second.getId());
        }

        @Test
        @DisplayName("findByIdAndUserId does not return another user's account")
        void doesNotLeakAcrossUsers() {
            AccountEntity account = PersistenceFixtures.account(em, userId);
            UUID otherUserId = PersistenceFixtures.user(em).getId();

            Optional<AccountEntity> found = accountPort.findByIdAndUserId(account.getId(), otherUserId);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("countByUserId counts only this user's accounts")
        void countsOnlyThisUsersAccounts() {
            PersistenceFixtures.account(em, userId);
            PersistenceFixtures.account(em, userId);
            UUID otherUserId = PersistenceFixtures.user(em).getId();
            PersistenceFixtures.account(em, otherUserId);

            assertThat(accountPort.countByUserId(userId)).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("countTransactionsByAccountId")
    class CountTransactionsByAccountId {

        @Test
        @DisplayName("counts non-deleted transactions referencing the account in any of its three roles")
        void countsAcrossAllThreeAccountRoles() {
            UUID accountId = PersistenceFixtures.account(em, userId).getId();
            UUID otherAccountId = PersistenceFixtures.account(em, userId).getId();
            PersistenceFixtures.expense(em, userId, accountId, null,
                    new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 3, 1), "A");
            PersistenceFixtures.transfer(em, userId, accountId, otherAccountId,
                    new BigDecimal("20.00"), "PEN", LocalDate.of(2026, 3, 2));
            PersistenceFixtures.transfer(em, userId, otherAccountId, accountId,
                    new BigDecimal("30.00"), "PEN", LocalDate.of(2026, 3, 3));
            TransactionEntity deleted = PersistenceFixtures.expense(em, userId, accountId, null,
                    new BigDecimal("40.00"), "PEN", LocalDate.of(2026, 3, 4), "B");
            em.find(TransactionEntity.class, deleted.getId()).setDeletedAt(java.time.Instant.now());
            em.flush();

            long count = accountPort.countTransactionsByAccountId(accountId);

            assertThat(count).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("clearDefaultForUser")
    class ClearDefaultForUser {

        @Test
        @DisplayName("unsets isDefault only for the given user's accounts")
        void clearsOnlyThisUsersAccounts() {
            AccountEntity mine = PersistenceFixtures.account(em, userId);
            em.find(AccountEntity.class, mine.getId()).setDefault(true);
            em.flush();

            UUID otherUserId = PersistenceFixtures.user(em).getId();
            AccountEntity othersDefault = PersistenceFixtures.account(em, otherUserId);
            em.find(AccountEntity.class, othersDefault.getId()).setDefault(true);
            em.flush();

            accountPort.clearDefaultForUser(userId);
            em.flush();
            em.clear();

            assertThat(em.find(AccountEntity.class, mine.getId()).isDefault()).isFalse();
            assertThat(em.find(AccountEntity.class, othersDefault.getId()).isDefault()).isTrue();
        }
    }

    @Nested
    @DisplayName("adjustBalance")
    class AdjustBalance {

        @Test
        @DisplayName("adds a positive delta and is only observable after flush + clear")
        void addsPositiveDelta() {
            AccountEntity account = PersistenceFixtures.account(em, userId);

            accountPort.adjustBalance(account.getId(), new BigDecimal("50.00"));
            em.flush();
            em.clear();

            assertThat(em.find(AccountEntity.class, account.getId()).getCurrentBalance())
                    .isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("a negative delta can drive the balance negative (credit card debt is valid)")
        void negativeDeltaAllowsNegativeBalance() {
            AccountEntity account = PersistenceFixtures.account(em, userId);

            accountPort.adjustBalance(account.getId(), new BigDecimal("-75.00"));
            em.flush();
            em.clear();

            assertThat(em.find(AccountEntity.class, account.getId()).getCurrentBalance())
                    .isEqualByComparingTo("-75.00");
        }

        @Test
        @DisplayName("deltas accumulate across multiple calls")
        void deltasAccumulate() {
            AccountEntity account = PersistenceFixtures.account(em, userId);

            accountPort.adjustBalance(account.getId(), new BigDecimal("100.00"));
            accountPort.adjustBalance(account.getId(), new BigDecimal("-30.00"));
            em.flush();
            em.clear();

            assertThat(em.find(AccountEntity.class, account.getId()).getCurrentBalance())
                    .isEqualByComparingTo("70.00");
        }
    }
}
