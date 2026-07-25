package com.finanzasia.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JpaTransactionRepositoryPort} against real Postgres: the native keyset-pagination
 * query, its filters, and the JPQL soft-delete.
 */
class JpaTransactionRepositoryPortTest extends AbstractPostgresTest {

    @Autowired
    private JpaTransactionRepositoryPort transactionPort;

    @Autowired
    private TestEntityManager em;

    private UUID userId;
    private UUID accountId;
    private UUID otherAccountId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        userId = PersistenceFixtures.user(em).getId();
        accountId = PersistenceFixtures.account(em, userId).getId();
        otherAccountId = PersistenceFixtures.account(em, userId).getId();
        categoryId = PersistenceFixtures.category(em, userId, "food-" + UUID.randomUUID()).getId();
    }

    @Nested
    @DisplayName("findWithFilter / countWithFilter")
    class Filters {

        @Test
        @DisplayName("with no filters, returns every non-deleted row for the user")
        void noFiltersReturnsAll() {
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 3, 1), "A");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("20.00"), "PEN", LocalDate.of(2026, 3, 2), "B");

            List<TransactionEntity> rows = transactionPort.findWithFilter(
                    userId, null, null, null, null, null, null, null, null, null, 50);
            long count = transactionPort.countWithFilter(
                    userId, null, null, null, null, null, null, null);

            assertThat(rows).hasSize(2);
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("excludes soft-deleted rows and rows from other users")
        void excludesDeletedAndOtherUsers() {
            TransactionEntity deleted = PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 3, 1), "A");
            TransactionEntity managed = em.find(TransactionEntity.class, deleted.getId());
            managed.setDeletedAt(Instant.now());
            em.flush();

            UUID otherUserId = PersistenceFixtures.user(em).getId();
            PersistenceFixtures.expense(em, otherUserId, PersistenceFixtures.account(em, otherUserId).getId(),
                    null, new BigDecimal("999.00"), "PEN", LocalDate.of(2026, 3, 1), "Other");

            List<TransactionEntity> rows = transactionPort.findWithFilter(
                    userId, null, null, null, null, null, null, null, null, null, 50);

            assertThat(rows).isEmpty();
        }

        @Test
        @DisplayName("type filter matches EXPENSE/INCOME/TRANSFER")
        void typeFilter() {
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 3, 1), "A");
            PersistenceFixtures.income(em, userId, accountId, categoryId,
                    new BigDecimal("2000.00"), "PEN", LocalDate.of(2026, 3, 2));

            List<TransactionEntity> expenses = transactionPort.findWithFilter(
                    userId, "EXPENSE", null, null, null, null, null, null, null, null, 50);
            List<TransactionEntity> incomes = transactionPort.findWithFilter(
                    userId, "INCOME", null, null, null, null, null, null, null, null, 50);

            assertThat(expenses).hasSize(1);
            assertThat(expenses.get(0).getType().name()).isEqualTo("EXPENSE");
            assertThat(incomes).hasSize(1);
            assertThat(incomes.get(0).getType().name()).isEqualTo("INCOME");
        }

        @Test
        @DisplayName("accountId filter matches account_id, from_account_id, or to_account_id")
        void accountIdMatchesAnyOfThreeColumns() {
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 3, 1), "Expense on account");
            PersistenceFixtures.transfer(em, userId, accountId, otherAccountId,
                    new BigDecimal("50.00"), "PEN", LocalDate.of(2026, 3, 2));
            PersistenceFixtures.transfer(em, userId, otherAccountId, accountId,
                    new BigDecimal("30.00"), "PEN", LocalDate.of(2026, 3, 3));
            // Unrelated to `accountId` entirely.
            UUID thirdAccount = PersistenceFixtures.account(em, userId).getId();
            PersistenceFixtures.expense(em, userId, thirdAccount, categoryId,
                    new BigDecimal("5.00"), "PEN", LocalDate.of(2026, 3, 4), "Unrelated");

            List<TransactionEntity> rows = transactionPort.findWithFilter(
                    userId, null, accountId, null, null, null, null, null, null, null, 50);

            assertThat(rows).hasSize(3);
        }

        @Test
        @DisplayName("currency and date-range filters, both boundaries inclusive")
        void currencyAndDateRangeFilters() {
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 3, 1), "A");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("20.00"), "USD", LocalDate.of(2026, 3, 15), "B");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("30.00"), "PEN", LocalDate.of(2026, 3, 31), "C");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("40.00"), "PEN", LocalDate.of(2026, 4, 1), "D");

            List<TransactionEntity> penInMarch = transactionPort.findWithFilter(
                    userId, null, null, null, "PEN",
                    LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                    null, null, null, 50);

            assertThat(penInMarch).extracting(TransactionEntity::getMerchant)
                    .containsExactlyInAnyOrder("A", "C");
        }

        @Test
        @DisplayName("categoryId filter excludes non-matching rows")
        void categoryIdFilter() {
            UUID otherCategoryId = PersistenceFixtures.category(em, userId, "transport-" + UUID.randomUUID()).getId();
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 3, 1), "A");
            PersistenceFixtures.expense(em, userId, accountId, otherCategoryId,
                    new BigDecimal("20.00"), "PEN", LocalDate.of(2026, 3, 2), "B");

            List<TransactionEntity> rows = transactionPort.findWithFilter(
                    userId, null, null, categoryId, null, null, null, null, null, null, 50);

            assertThat(rows).extracting(TransactionEntity::getMerchant).containsExactly("A");
        }

        @Test
        @DisplayName("tagId filter returns DISTINCT rows even when a transaction has multiple tags")
        void tagIdFilterReturnsDistinctRows() {
            TransactionEntity multiTagged = PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 3, 1), "Tagged");
            UUID tag1 = PersistenceFixtures.tag(em, userId, "a").getId();
            UUID tag2 = PersistenceFixtures.tag(em, userId, "b").getId();
            PersistenceFixtures.attachTag(em, multiTagged.getId(), tag1);
            PersistenceFixtures.attachTag(em, multiTagged.getId(), tag2);
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("20.00"), "PEN", LocalDate.of(2026, 3, 2), "Untagged");

            List<TransactionEntity> filteredByTag1 = transactionPort.findWithFilter(
                    userId, null, null, null, null, null, null, tag1.toString(), null, null, 50);
            List<TransactionEntity> unfiltered = transactionPort.findWithFilter(
                    userId, null, null, null, null, null, null, null, null, null, 50);

            assertThat(filteredByTag1).hasSize(1);
            assertThat(filteredByTag1.get(0).getId()).isEqualTo(multiTagged.getId());
            // The unfiltered case joins transaction_tags unconditionally (tagId IS NULL branch of
            // the ON clause), so without DISTINCT this transaction would appear twice - once per tag.
            assertThat(unfiltered).hasSize(2);
        }

        @Test
        @DisplayName("countWithFilter agrees with the row count a full findWithFilter walk returns")
        void countAgreesWithFullWalk() {
            for (int i = 0; i < 5; i++) {
                PersistenceFixtures.expense(em, userId, accountId, categoryId,
                        new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 3, 1 + i), "M" + i);
            }

            long count = transactionPort.countWithFilter(
                    userId, null, null, null, null, null, null, null);
            List<TransactionEntity> all = transactionPort.findWithFilter(
                    userId, null, null, null, null, null, null, null, null, null, 50);

            assertThat(count).isEqualTo(all.size()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("keyset pagination (findWithFilter cursor)")
    class KeysetPagination {

        @Test
        @DisplayName("a full walk with a small page size visits every row exactly once, in order")
        void fullWalkVisitsEveryRowExactlyOnce() {
            List<UUID> expectedIds = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                TransactionEntity t = PersistenceFixtures.expense(em, userId, accountId, categoryId,
                        new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 3, 1 + i), "M" + i);
                expectedIds.add(t.getId());
            }

            List<UUID> visited = walkAll(3);

            assertThat(visited).hasSize(7);
            assertThat(visited).containsExactlyInAnyOrderElementsOf(expectedIds);
            assertThat(visited).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("rows sharing the same transaction_date paginate without duplication or omission")
        void sameDateRowsPaginateCorrectly() {
            List<UUID> expectedIds = new ArrayList<>();
            LocalDate sameDate = LocalDate.of(2026, 3, 15);
            for (int i = 0; i < 6; i++) {
                TransactionEntity t = PersistenceFixtures.expense(em, userId, accountId, categoryId,
                        new BigDecimal("10.00"), "PEN", sameDate, "SameDate" + i);
                expectedIds.add(t.getId());
            }

            List<UUID> visited = walkAll(2);

            assertThat(visited).hasSize(6);
            assertThat(visited).containsExactlyInAnyOrderElementsOf(expectedIds);
            assertThat(visited).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("walk order matches a single unpaginated call ordered by date desc, id desc")
        void walkOrderMatchesSingleCall() {
            for (int i = 0; i < 5; i++) {
                PersistenceFixtures.expense(em, userId, accountId, categoryId,
                        new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 3, 1 + i), "M" + i);
            }

            List<UUID> singleCallOrder = transactionPort.findWithFilter(
                            userId, null, null, null, null, null, null, null, null, null, 50)
                    .stream().map(TransactionEntity::getId).collect(Collectors.toList());
            List<UUID> walkedOrder = walkAll(2);

            assertThat(walkedOrder).containsExactlyElementsOf(singleCallOrder);
        }

        private List<UUID> walkAll(int pageSize) {
            List<UUID> visited = new ArrayList<>();
            LocalDate cursorDate = null;
            String cursorId = null;
            while (true) {
                List<TransactionEntity> page = transactionPort.findWithFilter(
                        userId, null, null, null, null, null, null, null,
                        cursorDate, cursorId, pageSize);
                if (page.isEmpty()) {
                    break;
                }
                for (TransactionEntity t : page) {
                    visited.add(t.getId());
                }
                TransactionEntity last = page.get(page.size() - 1);
                cursorDate = last.getTransactionDate();
                cursorId = last.getId().toString();
                if (page.size() < pageSize) {
                    break;
                }
            }
            return visited;
        }
    }

    @Nested
    @DisplayName("softDelete")
    class SoftDelete {

        @Test
        @DisplayName("sets deleted_at, observable only after flush + clear of the persistence context")
        void setsDeletedAtAndIsObservableAfterFlushClear() {
            TransactionEntity created = PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 3, 1), "A");
            Instant now = Instant.now();

            transactionPort.softDelete(created.getId(), now);
            em.flush();
            em.clear();

            Optional<TransactionEntity> found = transactionPort.findByIdAndUserIdAndDeletedAtIsNull(
                    created.getId(), userId);
            assertThat(found).isEmpty();

            TransactionEntity reloaded = em.find(TransactionEntity.class, created.getId());
            assertThat(reloaded.getDeletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("findByIdAndUserIdAndDeletedAtIsNull")
    class FindById {

        @Test
        @DisplayName("does not return another user's transaction")
        void doesNotLeakAcrossUsers() {
            TransactionEntity created = PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 3, 1), "A");
            UUID otherUserId = PersistenceFixtures.user(em).getId();

            Optional<TransactionEntity> found = transactionPort.findByIdAndUserIdAndDeletedAtIsNull(
                    created.getId(), otherUserId);

            assertThat(found).isEmpty();
        }
    }
}
