package com.finanzasia.application.service;

import com.finanzasia.domain.model.MonthlyReport;
import com.finanzasia.domain.model.YearlyReport;
import com.finanzasia.domain.port.in.GetMonthlyReportUseCase;
import com.finanzasia.domain.port.in.GetYearlyReportUseCase;
import com.finanzasia.domain.port.out.ReportRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates both monthly and yearly report use cases.
 * All aggregation queries are delegated to {@link ReportRepository}.
 * Business logic such as percentage calculation, trend detection, and
 * previous-period comparison lives exclusively in this class.
 *
 * Category names are returned directly from the SQL query (joined from the
 * categories table) so no static label mapping is needed here.
 */
@Service
public class ReportService implements GetMonthlyReportUseCase, GetYearlyReportUseCase {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    // ------------------------------------------------------------------
    // Monthly report
    // ------------------------------------------------------------------

    @Override
    public MonthlyReport getMonthlyReport(UUID userId, int year, int month, String currency,
                                          UUID accountId, UUID categoryId, UUID tagId) {

        BigDecimal total = nullSafe(
                reportRepository.sumByMonth(userId, year, month, currency, accountId, categoryId, tagId));
        long count = reportRepository.countByMonth(userId, year, month, currency, accountId, categoryId, tagId);

        // Daily average: total / days in month
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        BigDecimal dailyAverage = count == 0
                ? ZERO
                : total.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);

        // Previous month (handles January -> December of prior year).
        // Filters are forwarded so the comparison is apples-to-apples.
        int prevYear  = (month == 1) ? year - 1 : year;
        int prevMonth = (month == 1) ? 12 : month - 1;
        BigDecimal prevTotal = nullSafe(
                reportRepository.sumByMonth(userId, prevYear, prevMonth, currency, accountId, categoryId, tagId));

        MonthlyReport.VsLastMonth vsLastMonth = buildVsLastMonth(total, prevTotal);

        // Income summary
        BigDecimal incomeTotal = nullSafe(
                reportRepository.sumIncomeByMonth(userId, year, month, currency, accountId, categoryId, tagId));
        long incomeCount = reportRepository.countIncomeByMonth(
                userId, year, month, currency, accountId, categoryId, tagId);
        MonthlyReport.IncomeSummary incomeSummary =
                new MonthlyReport.IncomeSummary(incomeTotal, incomeCount, currency);

        // Category breakdown: getCategory() returns the human-readable name from the JOIN
        List<MonthlyReport.CategoryBreakdown> byCategory =
                reportRepository.categoryBreakdownByMonth(userId, year, month, currency, accountId, categoryId, tagId)
                        .stream()
                        .map(row -> new MonthlyReport.CategoryBreakdown(
                                row.getCategory(),
                                row.getCategory(),
                                row.getAmount(),
                                row.getCount(),
                                computePercentage(row.getAmount(), total)))
                        .toList();

        // Week breakdown
        List<MonthlyReport.WeekBreakdown> byWeek =
                reportRepository.weekBreakdownByMonth(userId, year, month, currency, accountId, categoryId, tagId)
                        .stream()
                        .map(row -> new MonthlyReport.WeekBreakdown(
                                row.getWeekNumber(),
                                "Sem " + row.getWeekNumber(),
                                row.getAmount(),
                                row.getCount()))
                        .toList();

        // Top merchants
        List<MonthlyReport.MerchantSummary> topMerchants =
                reportRepository.topMerchantsByMonth(userId, year, month, currency, accountId, categoryId, tagId)
                        .stream()
                        .map(row -> new MonthlyReport.MerchantSummary(
                                row.getName(), row.getAmount(), row.getCount()))
                        .toList();

        MonthlyReport.Period period = new MonthlyReport.Period(
                year, month, MonthlyReport.periodLabel(year, month));

        MonthlyReport.Summary summary = new MonthlyReport.Summary(
                total, currency, count, dailyAverage);

        return new MonthlyReport(period, summary, incomeSummary, vsLastMonth, byCategory, byWeek, topMerchants);
    }

    // ------------------------------------------------------------------
    // Yearly report
    // ------------------------------------------------------------------

    @Override
    public YearlyReport getYearlyReport(UUID userId, int year, String currency,
                                        UUID accountId, UUID categoryId, UUID tagId) {

        BigDecimal total = nullSafe(
                reportRepository.sumByYear(userId, year, currency, accountId, categoryId, tagId));
        long count = reportRepository.countByYear(userId, year, currency, accountId, categoryId, tagId);

        BigDecimal monthlyAverage = total.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        YearlyReport.YearlySummary summary = new YearlyReport.YearlySummary(
                total, currency, count, monthlyAverage);

        // Income summary
        BigDecimal incomeTotal = nullSafe(
                reportRepository.sumIncomeByYear(userId, year, currency, accountId, categoryId, tagId));
        long incomeCount = reportRepository.countIncomeByYear(
                userId, year, currency, accountId, categoryId, tagId);
        YearlyReport.IncomeSummary incomeSummary =
                new YearlyReport.IncomeSummary(incomeTotal, incomeCount, currency);

        // Month breakdown
        List<YearlyReport.MonthBreakdown> byMonth =
                reportRepository.monthBreakdownByYear(userId, year, currency, accountId, categoryId, tagId)
                        .stream()
                        .map(row -> new YearlyReport.MonthBreakdown(
                                row.getMonth(),
                                YearlyReport.monthAbbr(row.getMonth()),
                                row.getAmount(),
                                row.getCount()))
                        .toList();

        // Category breakdown: getCategory() returns the human-readable name from the JOIN
        List<YearlyReport.CategoryBreakdown> byCategory =
                reportRepository.categoryBreakdownByYear(userId, year, currency, accountId, categoryId, tagId)
                        .stream()
                        .map(row -> new YearlyReport.CategoryBreakdown(
                                row.getCategory(),
                                row.getCategory(),
                                row.getAmount(),
                                row.getCount(),
                                computePercentage(row.getAmount(), total)))
                        .toList();

        // Top merchants
        List<YearlyReport.MerchantSummary> topMerchants =
                reportRepository.topMerchantsByYear(userId, year, currency, accountId, categoryId, tagId)
                        .stream()
                        .map(row -> new YearlyReport.MerchantSummary(
                                row.getName(), row.getAmount(), row.getCount()))
                        .toList();

        YearlyReport.Highlights highlights = buildHighlights(byMonth, byCategory);

        return new YearlyReport(year, summary, incomeSummary, byMonth, byCategory, topMerchants, highlights);
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : ZERO;
    }

    /**
     * Computes amount / total * 100 rounded to one decimal.
     * Returns 0.0 when total is zero to avoid division-by-zero.
     */
    private double computePercentage(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return amount.multiply(HUNDRED)
                .divide(total, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private MonthlyReport.VsLastMonth buildVsLastMonth(BigDecimal current, BigDecimal previous) {
        BigDecimal change = current.subtract(previous);
        double changePercent = 0.0;
        String trend = "FLAT";

        if (previous.compareTo(BigDecimal.ZERO) != 0) {
            changePercent = change.multiply(HUNDRED)
                    .divide(previous, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        if (change.compareTo(BigDecimal.ZERO) > 0) {
            trend = "UP";
        } else if (change.compareTo(BigDecimal.ZERO) < 0) {
            trend = "DOWN";
        }

        return new MonthlyReport.VsLastMonth(previous, change, changePercent, trend);
    }

    /**
     * Builds highlight data from the already-computed breakdown lists.
     * Peak and lowest are derived from months with data.
     * Top category is the first entry (list is already ordered by amount DESC from SQL).
     */
    private YearlyReport.Highlights buildHighlights(
            List<YearlyReport.MonthBreakdown> byMonth,
            List<YearlyReport.CategoryBreakdown> byCategory) {

        YearlyReport.MonthBreakdown peak = byMonth.stream()
                .max(Comparator.comparing(YearlyReport.MonthBreakdown::amount))
                .orElse(null);

        YearlyReport.MonthBreakdown lowest = byMonth.stream()
                .min(Comparator.comparing(YearlyReport.MonthBreakdown::amount))
                .orElse(null);

        YearlyReport.CategoryBreakdown topCategory = byCategory.isEmpty()
                ? null
                : byCategory.get(0);

        return new YearlyReport.Highlights(peak, lowest, topCategory);
    }
}
