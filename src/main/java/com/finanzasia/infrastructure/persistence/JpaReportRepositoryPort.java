package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.port.out.ReportRepository.CategoryRow;
import com.finanzasia.domain.port.out.ReportRepository.MerchantRow;
import com.finanzasia.domain.port.out.ReportRepository.MonthRow;
import com.finanzasia.domain.port.out.ReportRepository.WeekRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA interface for report aggregation queries.
 * All queries target the {@code transactions} table and filter by type
 * so that expense and income reports remain independent.
 *
 * Optional filters ({@code accountId}, {@code categoryId}, {@code tagId}) are applied
 * via the {@code IS NULL OR} pattern: passing {@code null} disables each filter
 * without requiring a separate query variant.
 * Tag filtering uses an EXISTS subquery against the {@code transaction_tags} join table.
 */
public interface JpaReportRepositoryPort extends JpaRepository<TransactionEntity, UUID> {

    // ------------------------------------------------------------------
    // Monthly: expense totals
    // ------------------------------------------------------------------

    @Query(value = """
            SELECT COALESCE(SUM(amount), 0)
            FROM transactions
            WHERE user_id = :userId
              AND type = 'EXPENSE'
              AND currency = :currency
              AND EXTRACT(YEAR  FROM transaction_date) = :year
              AND EXTRACT(MONTH FROM transaction_date) = :month
              AND deleted_at IS NULL
              AND (:accountId  IS NULL OR account_id  = :accountId)
              AND (:categoryId IS NULL OR category_id = :categoryId)
              AND (:tagId IS NULL OR EXISTS (
                      SELECT 1 FROM transaction_tags tt
                      WHERE tt.transaction_id = id AND tt.tag_id = :tagId))
            """, nativeQuery = true)
    BigDecimal sumByMonth(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("currency") String currency,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("tagId") UUID tagId);

    @Query(value = """
            SELECT COUNT(*)
            FROM transactions
            WHERE user_id = :userId
              AND type = 'EXPENSE'
              AND currency = :currency
              AND EXTRACT(YEAR  FROM transaction_date) = :year
              AND EXTRACT(MONTH FROM transaction_date) = :month
              AND deleted_at IS NULL
              AND (:accountId  IS NULL OR account_id  = :accountId)
              AND (:categoryId IS NULL OR category_id = :categoryId)
              AND (:tagId IS NULL OR EXISTS (
                      SELECT 1 FROM transaction_tags tt
                      WHERE tt.transaction_id = id AND tt.tag_id = :tagId))
            """, nativeQuery = true)
    long countByMonth(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("currency") String currency,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("tagId") UUID tagId);

    // ------------------------------------------------------------------
    // Monthly: income totals
    // ------------------------------------------------------------------

    @Query(value = """
            SELECT COALESCE(SUM(amount), 0)
            FROM transactions
            WHERE user_id = :userId
              AND type = 'INCOME'
              AND currency = :currency
              AND EXTRACT(YEAR  FROM transaction_date) = :year
              AND EXTRACT(MONTH FROM transaction_date) = :month
              AND deleted_at IS NULL
              AND (:accountId  IS NULL OR account_id  = :accountId)
              AND (:categoryId IS NULL OR category_id = :categoryId)
              AND (:tagId IS NULL OR EXISTS (
                      SELECT 1 FROM transaction_tags tt
                      WHERE tt.transaction_id = id AND tt.tag_id = :tagId))
            """, nativeQuery = true)
    BigDecimal sumIncomeByMonth(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("currency") String currency,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("tagId") UUID tagId);

    @Query(value = """
            SELECT COUNT(*)
            FROM transactions
            WHERE user_id = :userId
              AND type = 'INCOME'
              AND currency = :currency
              AND EXTRACT(YEAR  FROM transaction_date) = :year
              AND EXTRACT(MONTH FROM transaction_date) = :month
              AND deleted_at IS NULL
              AND (:accountId  IS NULL OR account_id  = :accountId)
              AND (:categoryId IS NULL OR category_id = :categoryId)
              AND (:tagId IS NULL OR EXISTS (
                      SELECT 1 FROM transaction_tags tt
                      WHERE tt.transaction_id = id AND tt.tag_id = :tagId))
            """, nativeQuery = true)
    long countIncomeByMonth(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("currency") String currency,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("tagId") UUID tagId);

    // ------------------------------------------------------------------
    // Monthly: category breakdown (expenses only)
    // ------------------------------------------------------------------

    @Query(value = """
            SELECT c.name    AS category,
                   SUM(t.amount) AS amount,
                   COUNT(*)      AS count
            FROM transactions t
            JOIN categories c ON t.category_id = c.id
            WHERE t.user_id = :userId
              AND t.type = 'EXPENSE'
              AND t.currency = :currency
              AND EXTRACT(YEAR  FROM t.transaction_date) = :year
              AND EXTRACT(MONTH FROM t.transaction_date) = :month
              AND t.deleted_at IS NULL
              AND (:accountId  IS NULL OR t.account_id  = :accountId)
              AND (:categoryId IS NULL OR t.category_id = :categoryId)
              AND (:tagId IS NULL OR EXISTS (
                      SELECT 1 FROM transaction_tags tt
                      WHERE tt.transaction_id = t.id AND tt.tag_id = :tagId))
            GROUP BY c.id, c.name
            ORDER BY amount DESC
            """, nativeQuery = true)
    List<CategoryRow> findCategoryBreakdownByMonth(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("currency") String currency,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("tagId") UUID tagId);

    // ------------------------------------------------------------------
    // Monthly: week-of-month breakdown (expenses only, weeks 1-5)
    // ------------------------------------------------------------------

    @Query(value = """
            SELECT CAST(CEIL(EXTRACT(DAY FROM transaction_date) / 7.0) AS INTEGER) AS weekNumber,
                   SUM(amount) AS amount,
                   COUNT(*)    AS count
            FROM transactions
            WHERE user_id = :userId
              AND type = 'EXPENSE'
              AND currency = :currency
              AND EXTRACT(YEAR  FROM transaction_date) = :year
              AND EXTRACT(MONTH FROM transaction_date) = :month
              AND deleted_at IS NULL
              AND (:accountId  IS NULL OR account_id  = :accountId)
              AND (:categoryId IS NULL OR category_id = :categoryId)
              AND (:tagId IS NULL OR EXISTS (
                      SELECT 1 FROM transaction_tags tt
                      WHERE tt.transaction_id = id AND tt.tag_id = :tagId))
            GROUP BY weekNumber
            ORDER BY weekNumber
            """, nativeQuery = true)
    List<WeekRow> findWeekBreakdownByMonth(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("currency") String currency,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("tagId") UUID tagId);

    // ------------------------------------------------------------------
    // Monthly: top merchants (expenses only)
    // ------------------------------------------------------------------

    @Query(value = """
            SELECT merchant AS name,
                   SUM(amount) AS amount,
                   COUNT(*)    AS count
            FROM transactions
            WHERE user_id = :userId
              AND type = 'EXPENSE'
              AND currency = :currency
              AND EXTRACT(YEAR  FROM transaction_date) = :year
              AND EXTRACT(MONTH FROM transaction_date) = :month
              AND deleted_at IS NULL
              AND merchant IS NOT NULL AND merchant <> ''
              AND (:accountId  IS NULL OR account_id  = :accountId)
              AND (:categoryId IS NULL OR category_id = :categoryId)
              AND (:tagId IS NULL OR EXISTS (
                      SELECT 1 FROM transaction_tags tt
                      WHERE tt.transaction_id = id AND tt.tag_id = :tagId))
            GROUP BY merchant
            ORDER BY amount DESC
            LIMIT 5
            """, nativeQuery = true)
    List<MerchantRow> findTopMerchantsByMonth(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("currency") String currency,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("tagId") UUID tagId);

    // ------------------------------------------------------------------
    // Yearly: expense totals
    // ------------------------------------------------------------------

    @Query(value = """
            SELECT COALESCE(SUM(amount), 0)
            FROM transactions
            WHERE user_id = :userId
              AND type = 'EXPENSE'
              AND currency = :currency
              AND EXTRACT(YEAR FROM transaction_date) = :year
              AND deleted_at IS NULL
              AND (:accountId  IS NULL OR account_id  = :accountId)
              AND (:categoryId IS NULL OR category_id = :categoryId)
              AND (:tagId IS NULL OR EXISTS (
                      SELECT 1 FROM transaction_tags tt
                      WHERE tt.transaction_id = id AND tt.tag_id = :tagId))
            """, nativeQuery = true)
    BigDecimal sumByYear(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("currency") String currency,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("tagId") UUID tagId);

    @Query(value = """
            SELECT COUNT(*)
            FROM transactions
            WHERE user_id = :userId
              AND type = 'EXPENSE'
              AND currency = :currency
              AND EXTRACT(YEAR FROM transaction_date) = :year
              AND deleted_at IS NULL
              AND (:accountId  IS NULL OR account_id  = :accountId)
              AND (:categoryId IS NULL OR category_id = :categoryId)
              AND (:tagId IS NULL OR EXISTS (
                      SELECT 1 FROM transaction_tags tt
                      WHERE tt.transaction_id = id AND tt.tag_id = :tagId))
            """, nativeQuery = true)
    long countByYear(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("currency") String currency,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("tagId") UUID tagId);

    // ------------------------------------------------------------------
    // Yearly: income totals
    // ------------------------------------------------------------------

    @Query(value = """
            SELECT COALESCE(SUM(amount), 0)
            FROM transactions
            WHERE user_id = :userId
              AND type = 'INCOME'
              AND currency = :currency
              AND EXTRACT(YEAR FROM transaction_date) = :year
              AND deleted_at IS NULL
              AND (:accountId  IS NULL OR account_id  = :accountId)
              AND (:categoryId IS NULL OR category_id = :categoryId)
              AND (:tagId IS NULL OR EXISTS (
                      SELECT 1 FROM transaction_tags tt
                      WHERE tt.transaction_id = id AND tt.tag_id = :tagId))
            """, nativeQuery = true)
    BigDecimal sumIncomeByYear(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("currency") String currency,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("tagId") UUID tagId);

    @Query(value = """
            SELECT COUNT(*)
            FROM transactions
            WHERE user_id = :userId
              AND type = 'INCOME'
              AND currency = :currency
              AND EXTRACT(YEAR FROM transaction_date) = :year
              AND deleted_at IS NULL
              AND (:accountId  IS NULL OR account_id  = :accountId)
              AND (:categoryId IS NULL OR category_id = :categoryId)
              AND (:tagId IS NULL OR EXISTS (
                      SELECT 1 FROM transaction_tags tt
                      WHERE tt.transaction_id = id AND tt.tag_id = :tagId))
            """, nativeQuery = true)
    long countIncomeByYear(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("currency") String currency,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("tagId") UUID tagId);

    // ------------------------------------------------------------------
    // Yearly: month breakdown (expenses only)
    // ------------------------------------------------------------------

    @Query(value = """
            SELECT CAST(EXTRACT(MONTH FROM transaction_date) AS INTEGER) AS month,
                   SUM(amount) AS amount,
                   COUNT(*)    AS count
            FROM transactions
            WHERE user_id = :userId
              AND type = 'EXPENSE'
              AND currency = :currency
              AND EXTRACT(YEAR FROM transaction_date) = :year
              AND deleted_at IS NULL
              AND (:accountId  IS NULL OR account_id  = :accountId)
              AND (:categoryId IS NULL OR category_id = :categoryId)
              AND (:tagId IS NULL OR EXISTS (
                      SELECT 1 FROM transaction_tags tt
                      WHERE tt.transaction_id = id AND tt.tag_id = :tagId))
            GROUP BY month
            ORDER BY month
            """, nativeQuery = true)
    List<MonthRow> findMonthBreakdownByYear(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("currency") String currency,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("tagId") UUID tagId);

    // ------------------------------------------------------------------
    // Yearly: category breakdown (expenses only)
    // ------------------------------------------------------------------

    @Query(value = """
            SELECT c.name    AS category,
                   SUM(t.amount) AS amount,
                   COUNT(*)      AS count
            FROM transactions t
            JOIN categories c ON t.category_id = c.id
            WHERE t.user_id = :userId
              AND t.type = 'EXPENSE'
              AND t.currency = :currency
              AND EXTRACT(YEAR FROM t.transaction_date) = :year
              AND t.deleted_at IS NULL
              AND (:accountId  IS NULL OR t.account_id  = :accountId)
              AND (:categoryId IS NULL OR t.category_id = :categoryId)
              AND (:tagId IS NULL OR EXISTS (
                      SELECT 1 FROM transaction_tags tt
                      WHERE tt.transaction_id = t.id AND tt.tag_id = :tagId))
            GROUP BY c.id, c.name
            ORDER BY amount DESC
            """, nativeQuery = true)
    List<CategoryRow> findCategoryBreakdownByYear(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("currency") String currency,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("tagId") UUID tagId);

    // ------------------------------------------------------------------
    // Yearly: top merchants (expenses only)
    // ------------------------------------------------------------------

    @Query(value = """
            SELECT merchant AS name,
                   SUM(amount) AS amount,
                   COUNT(*)    AS count
            FROM transactions
            WHERE user_id = :userId
              AND type = 'EXPENSE'
              AND currency = :currency
              AND EXTRACT(YEAR FROM transaction_date) = :year
              AND deleted_at IS NULL
              AND merchant IS NOT NULL AND merchant <> ''
              AND (:accountId  IS NULL OR account_id  = :accountId)
              AND (:categoryId IS NULL OR category_id = :categoryId)
              AND (:tagId IS NULL OR EXISTS (
                      SELECT 1 FROM transaction_tags tt
                      WHERE tt.transaction_id = id AND tt.tag_id = :tagId))
            GROUP BY merchant
            ORDER BY amount DESC
            LIMIT 5
            """, nativeQuery = true)
    List<MerchantRow> findTopMerchantsByYear(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("currency") String currency,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("tagId") UUID tagId);
}
