package com.finanzasia.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * No JPA or framework annotations allowed here.
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

    public record Period(int year, int month, String label) {}

    public record Summary(
            BigDecimal totalAmount,
            String currency,
            long transactionCount,
            BigDecimal dailyAverage
    ) {}

    public record IncomeSummary(
            BigDecimal total,
            long transactionCount,
            String currency
    ) {}

    /** {@code trend} is one of "UP", "DOWN", or "FLAT". */
    public record VsLastMonth(
            BigDecimal previousAmount,
            BigDecimal changeAmount,
            double changePercent,
            String trend
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

    /** {@code weekNumber} ranges 1-5. {@code label} follows the pattern "Sem 1", "Sem 2", etc. */
    public record WeekBreakdown(
            int weekNumber,
            String label,
            BigDecimal amount,
            long count
    ) {}

    public record MerchantSummary(
            String name,
            BigDecimal amount,
            long count
    ) {}

    private static final String[] MONTH_NAMES = {
            "", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    /** Builds a human-readable period label like "Marzo 2026". */
    public static String periodLabel(int year, int month) {
        return MONTH_NAMES[month] + " " + year;
    }
}
