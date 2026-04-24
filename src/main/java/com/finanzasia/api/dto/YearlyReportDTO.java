package com.finanzasia.api.dto;

import com.finanzasia.domain.model.YearlyReport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * API response shape for the yearly expense report.
 * Mirrors {@link YearlyReport} exactly so the controller mapping is trivial.
 */
@Schema(description = "Aggregated financial data for a full calendar year")
public record YearlyReportDTO(

        @Schema(example = "2026") int year,

        @Schema(description = "High-level expense summary for the year")
        YearlySummary summary,

        @Schema(description = "High-level income summary for the year")
        IncomeSummary incomeSummary,

        @Schema(description = "Spending broken down by calendar month (only months with data are included)")
        List<MonthBreakdown> byMonth,

        @Schema(description = "Spending broken down by category, ordered by amount descending")
        List<CategoryBreakdown> byCategory,

        @Schema(description = "Top 5 merchants by total spend")
        List<MerchantSummary> topMerchants,

        @Schema(description = "Notable data points for the year")
        Highlights highlights
) {

    public record YearlySummary(
            @Schema(example = "15000.00") BigDecimal totalAmount,
            @Schema(example = "PEN", allowableValues = {"PEN", "USD"}) String currency,
            @Schema(example = "450") long transactionCount,
            @Schema(example = "1250.00") BigDecimal monthlyAverage
    ) {}

    public record IncomeSummary(
            @Schema(example = "36000.00") BigDecimal total,
            @Schema(example = "24") long transactionCount,
            @Schema(example = "PEN", allowableValues = {"PEN", "USD"}) String currency
    ) {}

    public record MonthBreakdown(
            @Schema(example = "1") int month,
            @Schema(example = "Ene") String label,
            @Schema(example = "1200.00") BigDecimal amount,
            @Schema(example = "35") long count
    ) {}

    public record CategoryBreakdown(
            @Schema(example = "food") String category,
            @Schema(example = "Comida") String label,
            @Schema(example = "5000.00") BigDecimal amount,
            @Schema(example = "150") long count,
            @Schema(example = "33.3") double percentage
    ) {}

    public record MerchantSummary(
            @Schema(example = "Plaza Vea") String name,
            @Schema(example = "2000.00") BigDecimal amount,
            @Schema(example = "50") long count
    ) {}

    public record Highlights(
            @Schema(description = "Month with the highest total spend")
            MonthBreakdown peakMonth,
            @Schema(description = "Month with the lowest total spend (among months with data)")
            MonthBreakdown lowestMonth,
            @Schema(description = "Category with the highest total spend")
            CategoryBreakdown topCategory
    ) {}

    // ------------------------------------------------------------------
    // Factory
    // ------------------------------------------------------------------

    /** Converts a domain {@link YearlyReport} to its API representation. */
    public static YearlyReportDTO from(YearlyReport report) {
        YearlySummary summary = new YearlySummary(
                report.summary().totalAmount(),
                report.summary().currency(),
                report.summary().transactionCount(),
                report.summary().monthlyAverage());

        IncomeSummary incomeSummary = new IncomeSummary(
                report.incomeSummary().total(),
                report.incomeSummary().transactionCount(),
                report.incomeSummary().currency());

        List<MonthBreakdown> byMonth = report.byMonth().stream()
                .map(m -> new MonthBreakdown(m.month(), m.label(), m.amount(), m.count()))
                .toList();

        List<CategoryBreakdown> byCategory = report.byCategory().stream()
                .map(c -> new CategoryBreakdown(
                        c.category(), c.label(), c.amount(), c.count(), c.percentage()))
                .toList();

        List<MerchantSummary> topMerchants = report.topMerchants().stream()
                .map(m -> new MerchantSummary(m.name(), m.amount(), m.count()))
                .toList();

        Highlights highlights = buildHighlights(report.highlights());

        return new YearlyReportDTO(
                report.year(), summary, incomeSummary, byMonth, byCategory, topMerchants, highlights);
    }

    private static Highlights buildHighlights(YearlyReport.Highlights src) {
        MonthBreakdown peak = src.peakMonth() != null
                ? new MonthBreakdown(
                        src.peakMonth().month(),
                        src.peakMonth().label(),
                        src.peakMonth().amount(),
                        src.peakMonth().count())
                : null;

        MonthBreakdown lowest = src.lowestMonth() != null
                ? new MonthBreakdown(
                        src.lowestMonth().month(),
                        src.lowestMonth().label(),
                        src.lowestMonth().amount(),
                        src.lowestMonth().count())
                : null;

        CategoryBreakdown topCategory = src.topCategory() != null
                ? new CategoryBreakdown(
                        src.topCategory().category(),
                        src.topCategory().label(),
                        src.topCategory().amount(),
                        src.topCategory().count(),
                        src.topCategory().percentage())
                : null;

        return new Highlights(peak, lowest, topCategory);
    }
}
