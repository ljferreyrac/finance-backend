package com.finanzasia.api.dto;

import com.finanzasia.domain.model.MonthlyReport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * API response shape for the monthly expense report.
 * Mirrors {@link MonthlyReport} exactly so the controller mapping is trivial.
 */
@Schema(description = "Aggregated financial data for a single calendar month")
public record MonthlyReportDTO(

        @Schema(description = "Identifies the calendar month being reported")
        Period period,

        @Schema(description = "High-level expense summary")
        Summary summary,

        @Schema(description = "High-level income summary")
        IncomeSummary incomeSummary,

        @Schema(description = "Comparison against the preceding calendar month")
        VsLastMonth vsLastMonth,

        @Schema(description = "Spending broken down by category, ordered by amount descending")
        List<CategoryBreakdown> byCategory,

        @Schema(description = "Spending broken down by week-of-month (weeks 1-5)")
        List<WeekBreakdown> byWeek,

        @Schema(description = "Top 5 merchants by total spend")
        List<MerchantSummary> topMerchants
) {

    public record Period(
            @Schema(example = "2026") int year,
            @Schema(example = "3") int month,
            @Schema(example = "Marzo 2026") String label
    ) {}

    public record Summary(
            @Schema(example = "1500.00") BigDecimal totalAmount,
            @Schema(example = "PEN", allowableValues = {"PEN", "USD"}) String currency,
            @Schema(example = "45") long transactionCount,
            @Schema(example = "50.00") BigDecimal dailyAverage
    ) {}

    public record IncomeSummary(
            @Schema(example = "3000.00") BigDecimal total,
            @Schema(example = "2") long transactionCount,
            @Schema(example = "PEN", allowableValues = {"PEN", "USD"}) String currency
    ) {}

    public record VsLastMonth(
            @Schema(example = "1200.00") BigDecimal previousAmount,
            @Schema(example = "300.00") BigDecimal changeAmount,
            @Schema(example = "25.0") double changePercent,
            @Schema(example = "UP", allowableValues = {"UP", "DOWN", "FLAT"}) String trend
    ) {}

    public record CategoryBreakdown(
            @Schema(example = "food") String category,
            @Schema(example = "Comida") String label,
            @Schema(example = "500.00") BigDecimal amount,
            @Schema(example = "20") long count,
            @Schema(example = "33.3") double percentage
    ) {}

    public record WeekBreakdown(
            @Schema(example = "1") int weekNumber,
            @Schema(example = "Sem 1") String label,
            @Schema(example = "300.00") BigDecimal amount,
            @Schema(example = "10") long count
    ) {}

    public record MerchantSummary(
            @Schema(example = "Plaza Vea") String name,
            @Schema(example = "200.00") BigDecimal amount,
            @Schema(example = "5") long count
    ) {}

    public static MonthlyReportDTO from(MonthlyReport report) {
        Period period = new Period(
                report.period().year(),
                report.period().month(),
                report.period().label());

        Summary summary = new Summary(
                report.summary().totalAmount(),
                report.summary().currency(),
                report.summary().transactionCount(),
                report.summary().dailyAverage());

        IncomeSummary incomeSummary = new IncomeSummary(
                report.incomeSummary().total(),
                report.incomeSummary().transactionCount(),
                report.incomeSummary().currency());

        VsLastMonth vsLastMonth = new VsLastMonth(
                report.vsLastMonth().previousAmount(),
                report.vsLastMonth().changeAmount(),
                report.vsLastMonth().changePercent(),
                report.vsLastMonth().trend());

        List<CategoryBreakdown> byCategory = report.byCategory().stream()
                .map(c -> new CategoryBreakdown(
                        c.category(), c.label(), c.amount(), c.count(), c.percentage()))
                .toList();

        List<WeekBreakdown> byWeek = report.byWeek().stream()
                .map(w -> new WeekBreakdown(
                        w.weekNumber(), w.label(), w.amount(), w.count()))
                .toList();

        List<MerchantSummary> topMerchants = report.topMerchants().stream()
                .map(m -> new MerchantSummary(m.name(), m.amount(), m.count()))
                .toList();

        return new MonthlyReportDTO(period, summary, incomeSummary, vsLastMonth, byCategory, byWeek, topMerchants);
    }
}
