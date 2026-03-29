package com.finanzasia.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable domain value object that carries all data for a single
 * calendar month report. No JPA or framework annotations allowed here.
 */
public record MonthlyReport(
        Period period,
        Summary summary,
        IncomeSummary incomeSummary,
        VsLastMonth vsLastMonth,
        List<CategoryBreakdown> byCategory,
        List<WeekBreakdown> byWeek,
        List<MerchantSummary> topMerchants
) {

    // ------------------------------------------------------------------
    // Nested value types
    // ------------------------------------------------------------------

    /** Identifies the calendar month being reported. */
    public record Period(int year, int month, String label) {}

    /** High-level expense summary for the month. */
    public record Summary(
            BigDecimal totalAmount,
            String currency,
            long transactionCount,
            BigDecimal dailyAverage
    ) {}

    /** High-level income summary for the month. */
    public record IncomeSummary(
            BigDecimal total,
            long transactionCount,
            String currency
    ) {}

    /**
     * Comparison against the immediately preceding calendar month.
     * {@code trend} is one of "UP", "DOWN", or "FLAT".
     */
    public record VsLastMonth(
            BigDecimal previousAmount,
            BigDecimal changeAmount,
            double changePercent,
            String trend
    ) {}

    /**
     * Spending aggregated by expense category.
     * {@code label} is the display name returned directly from the categories table.
     * {@code percentage} is rounded to one decimal place.
     */
    public record CategoryBreakdown(
            String category,
            String label,
            BigDecimal amount,
            long count,
            double percentage
    ) {}

    /**
     * Spending aggregated by week-of-month (1-5).
     * {@code label} follows the pattern "Sem 1", "Sem 2", etc.
     */
    public record WeekBreakdown(
            int weekNumber,
            String label,
            BigDecimal amount,
            long count
    ) {}

    /** Top merchant by total spend within the period. */
    public record MerchantSummary(
            String name,
            BigDecimal amount,
            long count
    ) {}

    // ------------------------------------------------------------------
    // Spanish month label
    // ------------------------------------------------------------------

    private static final String[] MONTH_NAMES = {
            "", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    /** Builds a human-readable period label like "Marzo 2026". */
    public static String periodLabel(int year, int month) {
        return MONTH_NAMES[month] + " " + year;
    }
}
