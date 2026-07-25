package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.port.out.ReportRepository.CategoryRow;
import com.finanzasia.domain.port.out.ReportRepository.MerchantRow;
import com.finanzasia.domain.port.out.ReportRepository.MonthRow;
import com.finanzasia.domain.port.out.ReportRepository.WeekRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * All 14 native queries in {@link JpaReportRepositoryPort} against real Postgres.
 *
 * <p>These queries were 100% line-covered by Phase 2's mocked adapter tests and 0% executed:
 * this class is what actually runs the SQL. The risk is not "does it throw" but "does it filter
 * correctly" - every query implements its optional filters with a
 * {@code (:x IS NULL OR col = :x)} pattern that a typo can silently turn into always-true or
 * always-false, so both sides of every filter get a test.
 */
class JpaReportRepositoryPortTest extends AbstractPostgresTest {

    @Autowired
    private JpaReportRepositoryPort reportPort;

    @Autowired
    private TestEntityManager em;

    private UUID userId;
    private UUID otherUserId;
    private UUID accountId;
    private UUID otherAccountId;
    private UUID categoryId;
    private UUID otherCategoryId;
    private UUID tagId;
    private UUID otherTagId;

    @BeforeEach
    void setUp() {
        userId = PersistenceFixtures.user(em).getId();
        otherUserId = PersistenceFixtures.user(em).getId();
        accountId = PersistenceFixtures.account(em, userId).getId();
        otherAccountId = PersistenceFixtures.account(em, userId).getId();
        categoryId = PersistenceFixtures.category(em, userId, "food-" + UUID.randomUUID()).getId();
        otherCategoryId = PersistenceFixtures.category(em, userId, "transport-" + UUID.randomUUID()).getId();
        tagId = PersistenceFixtures.tag(em, userId, "reimbursable").getId();
        otherTagId = PersistenceFixtures.tag(em, userId, "personal").getId();
    }

    @Nested
    @DisplayName("sumByMonth / countByMonth")
    class MonthlyExpenseSums {

        @Test
        @DisplayName("sums only matching-currency EXPENSE rows in the given month for the user")
        void sumsMatchingRows() {
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("100.00"), "PEN", LocalDate.of(2026, 3, 15), "Wong");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("50.00"), "PEN", LocalDate.of(2026, 3, 20), "Metro");

            BigDecimal sum = reportPort.sumByMonth(userId, 2026, 3, "PEN", null, null, null);
            long count = reportPort.countByMonth(userId, 2026, 3, "PEN", null, null, null);

            assertThat(sum).isEqualByComparingTo("150.00");
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("returns 0, not null, when nothing matches")
        void coalescesToZeroOnEmpty() {
            BigDecimal sum = reportPort.sumByMonth(userId, 2026, 3, "PEN", null, null, null);
            long count = reportPort.countByMonth(userId, 2026, 3, "PEN", null, null, null);

            assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("excludes soft-deleted rows")
        void excludesDeletedRows() {
            TransactionEntity deleted = PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("100.00"), "PEN", LocalDate.of(2026, 3, 15), "Wong");
            TransactionEntity managed = em.find(TransactionEntity.class, deleted.getId());
            managed.setDeletedAt(java.time.Instant.now());
            em.flush();

            BigDecimal sum = reportPort.sumByMonth(userId, 2026, 3, "PEN", null, null, null);

            assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("never includes another user's rows")
        void excludesOtherUsersRows() {
            PersistenceFixtures.expense(em, otherUserId, PersistenceFixtures.account(em, otherUserId).getId(),
                    null, new BigDecimal("999.00"), "PEN", LocalDate.of(2026, 3, 15), "Other");

            BigDecimal sum = reportPort.sumByMonth(userId, 2026, 3, "PEN", null, null, null);

            assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("separates currencies")
        void separatesCurrencies() {
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("100.00"), "PEN", LocalDate.of(2026, 3, 15), "Wong");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("30.00"), "USD", LocalDate.of(2026, 3, 15), "Amazon");

            assertThat(reportPort.sumByMonth(userId, 2026, 3, "PEN", null, null, null))
                    .isEqualByComparingTo("100.00");
            assertThat(reportPort.sumByMonth(userId, 2026, 3, "USD", null, null, null))
                    .isEqualByComparingTo("30.00");
        }

        @Test
        @DisplayName("accountId filter: null includes all, set excludes non-matching rows")
        void accountIdFilterBothSides() {
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("100.00"), "PEN", LocalDate.of(2026, 3, 15), "Wong");
            PersistenceFixtures.expense(em, userId, otherAccountId, categoryId,
                    new BigDecimal("40.00"), "PEN", LocalDate.of(2026, 3, 16), "Tottus");

            assertThat(reportPort.sumByMonth(userId, 2026, 3, "PEN", null, null, null))
                    .isEqualByComparingTo("140.00");
            assertThat(reportPort.sumByMonth(userId, 2026, 3, "PEN", accountId, null, null))
                    .isEqualByComparingTo("100.00");
            assertThat(reportPort.sumByMonth(userId, 2026, 3, "PEN", otherAccountId, null, null))
                    .isEqualByComparingTo("40.00");
        }

        @Test
        @DisplayName("categoryId filter: null includes all, set excludes non-matching rows")
        void categoryIdFilterBothSides() {
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("100.00"), "PEN", LocalDate.of(2026, 3, 15), "Wong");
            PersistenceFixtures.expense(em, userId, accountId, otherCategoryId,
                    new BigDecimal("40.00"), "PEN", LocalDate.of(2026, 3, 16), "Uber");

            assertThat(reportPort.sumByMonth(userId, 2026, 3, "PEN", null, null, null))
                    .isEqualByComparingTo("140.00");
            assertThat(reportPort.sumByMonth(userId, 2026, 3, "PEN", null, categoryId, null))
                    .isEqualByComparingTo("100.00");
            assertThat(reportPort.sumByMonth(userId, 2026, 3, "PEN", null, otherCategoryId, null))
                    .isEqualByComparingTo("40.00");
        }

        @Test
        @DisplayName("tagId filter: null includes all, set excludes rows without that tag")
        void tagIdFilterBothSides() {
            TransactionEntity tagged = PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("100.00"), "PEN", LocalDate.of(2026, 3, 15), "Wong");
            PersistenceFixtures.attachTag(em, tagged.getId(), tagId);
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("40.00"), "PEN", LocalDate.of(2026, 3, 16), "Tottus");

            assertThat(reportPort.sumByMonth(userId, 2026, 3, "PEN", null, null, null))
                    .isEqualByComparingTo("140.00");
            assertThat(reportPort.sumByMonth(userId, 2026, 3, "PEN", null, null, tagId))
                    .isEqualByComparingTo("100.00");
            assertThat(reportPort.sumByMonth(userId, 2026, 3, "PEN", null, null, otherTagId))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("EXTRACT(YEAR/MONTH) boundary: 31 Dec and 1 Jan don't bleed across periods")
        void yearMonthBoundary() {
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("100.00"), "PEN", LocalDate.of(2025, 12, 31), "NYE shop");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("50.00"), "PEN", LocalDate.of(2026, 1, 1), "New year shop");

            assertThat(reportPort.sumByMonth(userId, 2025, 12, "PEN", null, null, null))
                    .isEqualByComparingTo("100.00");
            assertThat(reportPort.sumByMonth(userId, 2026, 1, "PEN", null, null, null))
                    .isEqualByComparingTo("50.00");
        }
    }

    @Nested
    @DisplayName("sumIncomeByMonth / countIncomeByMonth")
    class MonthlyIncomeSums {

        @Test
        @DisplayName("sums INCOME rows separately from EXPENSE rows")
        void separatesIncomeFromExpense() {
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("100.00"), "PEN", LocalDate.of(2026, 3, 15), "Wong");
            PersistenceFixtures.income(em, userId, accountId, categoryId,
                    new BigDecimal("2500.00"), "PEN", LocalDate.of(2026, 3, 5));

            assertThat(reportPort.sumByMonth(userId, 2026, 3, "PEN", null, null, null))
                    .isEqualByComparingTo("100.00");
            assertThat(reportPort.sumIncomeByMonth(userId, 2026, 3, "PEN", null, null, null))
                    .isEqualByComparingTo("2500.00");
            assertThat(reportPort.countIncomeByMonth(userId, 2026, 3, "PEN", null, null, null))
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("sumByYear / countByYear / sumIncomeByYear / countIncomeByYear")
    class YearlySums {

        @Test
        @DisplayName("aggregates across the whole year regardless of month")
        void aggregatesWholeYear() {
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("100.00"), "PEN", LocalDate.of(2026, 1, 10), "Jan");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("200.00"), "PEN", LocalDate.of(2026, 11, 20), "Nov");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("999.00"), "PEN", LocalDate.of(2025, 12, 31), "PriorYear");
            PersistenceFixtures.income(em, userId, accountId, categoryId,
                    new BigDecimal("3000.00"), "PEN", LocalDate.of(2026, 6, 1));

            assertThat(reportPort.sumByYear(userId, 2026, "PEN", null, null, null))
                    .isEqualByComparingTo("300.00");
            assertThat(reportPort.countByYear(userId, 2026, "PEN", null, null, null)).isEqualTo(2);
            assertThat(reportPort.sumIncomeByYear(userId, 2026, "PEN", null, null, null))
                    .isEqualByComparingTo("3000.00");
            assertThat(reportPort.countIncomeByYear(userId, 2026, "PEN", null, null, null)).isEqualTo(1);
        }

        @Test
        @DisplayName("categoryId filter applies at year granularity too")
        void categoryFilterAtYearGranularity() {
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("100.00"), "PEN", LocalDate.of(2026, 1, 10), "Food");
            PersistenceFixtures.expense(em, userId, accountId, otherCategoryId,
                    new BigDecimal("40.00"), "PEN", LocalDate.of(2026, 2, 1), "Transport");

            assertThat(reportPort.sumByYear(userId, 2026, "PEN", null, categoryId, null))
                    .isEqualByComparingTo("100.00");
            assertThat(reportPort.sumByYear(userId, 2026, "PEN", null, null, null))
                    .isEqualByComparingTo("140.00");
        }
    }

    @Nested
    @DisplayName("findCategoryBreakdownByMonth / findCategoryBreakdownByYear")
    class CategoryBreakdown {

        @Test
        @DisplayName("groups by category, sums per group, orders by amount desc, projection binds")
        void groupsAndOrdersByAmount() {
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("30.00"), "PEN", LocalDate.of(2026, 3, 5), "Wong");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("20.00"), "PEN", LocalDate.of(2026, 3, 6), "Tottus");
            PersistenceFixtures.expense(em, userId, accountId, otherCategoryId,
                    new BigDecimal("100.00"), "PEN", LocalDate.of(2026, 3, 7), "Uber");

            List<CategoryRow> rows = reportPort.findCategoryBreakdownByMonth(
                    userId, 2026, 3, "PEN", null, null, null);

            assertThat(rows).hasSize(2);
            assertThat(rows.get(0).getAmount()).isEqualByComparingTo("100.00");
            assertThat(rows.get(0).getCount()).isEqualTo(1);
            assertThat(rows.get(1).getAmount()).isEqualByComparingTo("50.00");
            assertThat(rows.get(1).getCount()).isEqualTo(2);
            assertThat(rows).extracting(CategoryRow::getCategory).doesNotContainNull();
        }

        @Test
        @DisplayName("yearly variant groups across the whole year")
        void yearlyVariantGroupsWholeYear() {
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("30.00"), "PEN", LocalDate.of(2026, 1, 5), "Jan");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("20.00"), "PEN", LocalDate.of(2026, 11, 6), "Nov");

            List<CategoryRow> rows = reportPort.findCategoryBreakdownByYear(
                    userId, 2026, "PEN", null, null, null);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).getAmount()).isEqualByComparingTo("50.00");
            assertThat(rows.get(0).getCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("findWeekBreakdownByMonth")
    class WeekBreakdown {

        @Test
        @DisplayName("CEIL(day / 7.0) bucketing at day boundaries 1, 7, 8, 28, 29, 31")
        void bucketsAtDayBoundaries() {
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("1.00"), "PEN", LocalDate.of(2026, 3, 1), "d1");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("2.00"), "PEN", LocalDate.of(2026, 3, 7), "d7");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("4.00"), "PEN", LocalDate.of(2026, 3, 8), "d8");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("8.00"), "PEN", LocalDate.of(2026, 3, 28), "d28");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("16.00"), "PEN", LocalDate.of(2026, 3, 29), "d29");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("32.00"), "PEN", LocalDate.of(2026, 3, 31), "d31");

            List<WeekRow> rows = reportPort.findWeekBreakdownByMonth(
                    userId, 2026, 3, "PEN", null, null, null);

            // CEIL(7/7.0) is exactly 1, not 2: day 7 lands in week 1 alongside day 1, not week 2
            // alone. So the distinct week numbers are still {1,2,4,5}, but week 1 sums d1+d7.
            assertThat(rows).extracting(WeekRow::getWeekNumber).containsExactly(1, 2, 4, 5);
            java.util.Map<Integer, BigDecimal> byWeek = new java.util.HashMap<>();
            rows.forEach(r -> byWeek.put(r.getWeekNumber(), r.getAmount()));
            assertThat(byWeek.get(1)).isEqualByComparingTo("3.00");
            assertThat(byWeek.get(2)).isEqualByComparingTo("4.00");
            assertThat(byWeek.get(4)).isEqualByComparingTo("8.00");
            // Days 29 and 31 both fall in week 5 (CEIL(29/7.0)=5, CEIL(31/7.0)=5): 16 + 32.
            assertThat(byWeek.get(5)).isEqualByComparingTo("48.00");
        }
    }

    @Nested
    @DisplayName("findTopMerchantsByMonth / findTopMerchantsByYear")
    class TopMerchants {

        @Test
        @DisplayName("orders by amount desc, respects LIMIT 5, excludes null and empty merchant")
        void limitsOrdersAndExcludesBlankMerchant() {
            for (int i = 0; i < 6; i++) {
                PersistenceFixtures.expense(em, userId, accountId, categoryId,
                        new BigDecimal(String.valueOf(10 + i)), "PEN",
                        LocalDate.of(2026, 3, 2 + i), "Merchant" + i);
            }
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("500.00"), "PEN", LocalDate.of(2026, 3, 20), null);
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("500.00"), "PEN", LocalDate.of(2026, 3, 21), "");

            List<MerchantRow> rows = reportPort.findTopMerchantsByMonth(
                    userId, 2026, 3, "PEN", null, null, null);

            assertThat(rows).hasSize(5);
            assertThat(rows).extracting(MerchantRow::getName)
                    .containsExactly("Merchant5", "Merchant4", "Merchant3", "Merchant2", "Merchant1");
            assertThat(rows).extracting(MerchantRow::getAmount)
                    .isSortedAccordingTo(java.util.Comparator.reverseOrder());
        }

        @Test
        @DisplayName("yearly variant applies the same LIMIT/ordering/exclusion rules")
        void yearlyVariantSameRules() {
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 1, 2), "Alpha");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("20.00"), "PEN", LocalDate.of(2026, 11, 2), "Beta");

            List<MerchantRow> rows = reportPort.findTopMerchantsByYear(userId, 2026, "PEN", null, null, null);

            assertThat(rows).extracting(MerchantRow::getName).containsExactly("Beta", "Alpha");
        }
    }

    @Nested
    @DisplayName("findMonthBreakdownByYear")
    class MonthBreakdown {

        @Test
        @DisplayName("groups by month across the year and projection binds correctly")
        void groupsByMonth() {
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("10.00"), "PEN", LocalDate.of(2026, 1, 5), "Jan1");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("5.00"), "PEN", LocalDate.of(2026, 1, 20), "Jan2");
            PersistenceFixtures.expense(em, userId, accountId, categoryId,
                    new BigDecimal("30.00"), "PEN", LocalDate.of(2026, 7, 1), "Jul");

            List<MonthRow> rows = reportPort.findMonthBreakdownByYear(userId, 2026, "PEN", null, null, null);

            assertThat(rows).extracting(MonthRow::getMonth).containsExactly(1, 7);
            assertThat(rows.get(0).getAmount()).isEqualByComparingTo("15.00");
            assertThat(rows.get(0).getCount()).isEqualTo(2);
            assertThat(rows.get(1).getAmount()).isEqualByComparingTo("30.00");
        }
    }
}
