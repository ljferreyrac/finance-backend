package com.finanzasia.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * No JPA or framework annotations allowed here.
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

    public record YearlySummary(
            BigDecimal totalAmount,
            String currency,
            long transactionCount,
            BigDecimal monthlyAverage
    ) {}

    public record IncomeSummary(
            BigDecimal total,
            long transactionCount,
            String currency
    ) {}

    /** {@code label} is the abbreviated Spanish month name, e.g. "Ene". */
    public record MonthBreakdown(
            int month,
            String label,
            BigDecimal amount,
            long count
    ) {}

    /**
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

    public record MerchantSummary(
            String name,
            BigDecimal amount,
            long count
    ) {}

    /** {@code lowestMonth} only considers months that have at least one transaction. */
    public record Highlights(
            MonthBreakdown peakMonth,
            MonthBreakdown lowestMonth,
            CategoryBreakdown topCategory
    ) {}

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
