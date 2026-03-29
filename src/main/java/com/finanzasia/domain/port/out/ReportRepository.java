package com.finanzasia.domain.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Output port for all report aggregation queries.
 * Implementations live in the infrastructure layer; this interface
 * must remain free of any JPA, Spring, or JDBC types.
 *
 * All methods accept optional filter parameters ({@code accountId},
 * {@code categoryId}, {@code tagId}). Passing {@code null} for any of
 * them means "no filter" (include all rows that match the other criteria).
 */
public interface ReportRepository {

    // ------------------------------------------------------------------
    // Monthly expense queries
    // ------------------------------------------------------------------

    BigDecimal sumByMonth(UUID userId, int year, int month, String currency,
                          UUID accountId, UUID categoryId, UUID tagId);

    long countByMonth(UUID userId, int year, int month, String currency,
                      UUID accountId, UUID categoryId, UUID tagId);

    List<CategoryRow> categoryBreakdownByMonth(UUID userId, int year, int month, String currency,
                                               UUID accountId, UUID categoryId, UUID tagId);

    List<WeekRow> weekBreakdownByMonth(UUID userId, int year, int month, String currency,
                                       UUID accountId, UUID categoryId, UUID tagId);

    List<MerchantRow> topMerchantsByMonth(UUID userId, int year, int month, String currency,
                                          UUID accountId, UUID categoryId, UUID tagId);

    // ------------------------------------------------------------------
    // Monthly income queries
    // ------------------------------------------------------------------

    BigDecimal sumIncomeByMonth(UUID userId, int year, int month, String currency,
                                UUID accountId, UUID categoryId, UUID tagId);

    long countIncomeByMonth(UUID userId, int year, int month, String currency,
                            UUID accountId, UUID categoryId, UUID tagId);

    // ------------------------------------------------------------------
    // Yearly expense queries
    // ------------------------------------------------------------------

    BigDecimal sumByYear(UUID userId, int year, String currency,
                         UUID accountId, UUID categoryId, UUID tagId);

    long countByYear(UUID userId, int year, String currency,
                     UUID accountId, UUID categoryId, UUID tagId);

    List<MonthRow> monthBreakdownByYear(UUID userId, int year, String currency,
                                        UUID accountId, UUID categoryId, UUID tagId);

    List<CategoryRow> categoryBreakdownByYear(UUID userId, int year, String currency,
                                              UUID accountId, UUID categoryId, UUID tagId);

    List<MerchantRow> topMerchantsByYear(UUID userId, int year, String currency,
                                         UUID accountId, UUID categoryId, UUID tagId);

    // ------------------------------------------------------------------
    // Yearly income queries
    // ------------------------------------------------------------------

    BigDecimal sumIncomeByYear(UUID userId, int year, String currency,
                               UUID accountId, UUID categoryId, UUID tagId);

    long countIncomeByYear(UUID userId, int year, String currency,
                           UUID accountId, UUID categoryId, UUID tagId);

    // ------------------------------------------------------------------
    // Projection interfaces (column aliases must match getter names)
    // ------------------------------------------------------------------

    interface CategoryRow {
        String getCategory();
        BigDecimal getAmount();
        long getCount();
    }

    interface WeekRow {
        int getWeekNumber();
        BigDecimal getAmount();
        long getCount();
    }

    interface MerchantRow {
        String getName();
        BigDecimal getAmount();
        long getCount();
    }

    interface MonthRow {
        int getMonth();
        BigDecimal getAmount();
        long getCount();
    }
}
