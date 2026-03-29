package com.finanzasia.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable domain value object that carries all data for a full
 * calendar year report. No JPA or framework annotations allowed here.
 */
public record YearlyReport(
        int year,
        YearlySummary summary,
        IncomeSummary incomeSummary,
        List<MonthBreakdown> byMonth,
        List<CategoryBreakdown> byCategory,
        List<MerchantSummary> topMerchants,
        Highlights highlights
) {

    // ------------------------------------------------------------------
    // Nested value types
    // ------------------------------------------------------------------

    /** High-level expense summary for the year. */
    public record YearlySummary(
            BigDecimal totalAmount,
            String currency,
            long transactionCount,
            BigDecimal monthlyAverage
    ) {}

    /** High-level income summary for the year. */
    public record IncomeSummary(
            BigDecimal total,
            long transactionCount,
            String currency
    ) {}

    /**
     * Spending aggregated by calendar month.
     * {@code label} is the abbreviated Spanish month name, e.g. "Ene".
     */
    public record MonthBreakdown(
            int month,
            String label,
            BigDecimal amount,
            long count
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

    /** Top merchant by total spend within the year. */
    public record MerchantSummary(
            String name,
            BigDecimal amount,
            long count
    ) {}

    /**
     * Notable data points for the year.
     * {@code lowestMonth} only considers months that have at least one transaction.
     */
    public record Highlights(
            MonthBreakdown peakMonth,
            MonthBreakdown lowestMonth,
            CategoryBreakdown topCategory
    ) {}

    // ------------------------------------------------------------------
    // Spanish label helpers
    // ------------------------------------------------------------------

    private static final String[] MONTH_ABBR = {
            "", "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    };

    private static final String[] MONTH_FULL = {
            "", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    /** Abbreviated Spanish month name, e.g. "Ene" for month 1. */
    public static String monthAbbr(int month) {
        return MONTH_ABBR[month];
    }

    /** Full Spanish month name, e.g. "Enero" for month 1. */
    public static String monthFull(int month) {
        return MONTH_FULL[month];
    }
}
