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
 * Aggregation queries are delegated to {@link ReportRepository}; percentage calculation, trend
 * detection, and previous-period comparison live exclusively here. Category names come straight
 * from the SQL join, so no static label mapping is needed.
 */
@Service
public class ReportService implements GetMonthlyReportUseCase, GetYearlyReportUseCase {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Override
    public MonthlyReport getMonthlyReport(UUID userId, int year, int month, String currency,
                                          UUID accountId, UUID categoryId, UUID tagId) {

        BigDecimal total = nullSafe(
                reportRepository.sumByMonth(userId, year, month, currency, accountId, categoryId, tagId));
        long count = reportRepository.countByMonth(userId, year, month, currency, accountId, categoryId, tagId);

        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        BigDecimal dailyAverage = count == 0
                ? ZERO
                : total.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);

        // Wraps January back to December of the prior year; same filters are reused for an apples-to-apples comparison.
        int prevYear  = (month == 1) ? year - 1 : year;
        int prevMonth = (month == 1) ? 12 : month - 1;
        BigDecimal prevTotal = nullSafe(
                reportRepository.sumByMonth(userId, prevYear, prevMonth, currency, accountId, categoryId, tagId));

        MonthlyReport.VsLastMonth vsLastMonth = buildVsLastMonth(total, prevTotal);

        BigDecimal incomeTotal = nullSafe(
                reportRepository.sumIncomeByMonth(userId, year, month, currency, accountId, categoryId, tagId));
        long incomeCount = reportRepository.countIncomeByMonth(
                userId, year, month, currency, accountId, categoryId, tagId);
        MonthlyReport.IncomeSummary incomeSummary =
                new MonthlyReport.IncomeSummary(incomeTotal, incomeCount, currency);

        // row.getCategory() is both key and label: the JOIN already returns the human-readable name.
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

        List<MonthlyReport.WeekBreakdown> byWeek =
                reportRepository.weekBreakdownByMonth(userId, year, month, currency, accountId, categoryId, tagId)
                        .stream()
                        .map(row -> new MonthlyReport.WeekBreakdown(
                                row.getWeekNumber(),
                                "Sem " + row.getWeekNumber(),
                                row.getAmount(),
                                row.getCount()))
                        .toList();

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

    @Override
    public YearlyReport getYearlyReport(UUID userId, int year, String currency,
                                        UUID accountId, UUID categoryId, UUID tagId) {

        BigDecimal total = nullSafe(
                reportRepository.sumByYear(userId, year, currency, accountId, categoryId, tagId));
        long count = reportRepository.countByYear(userId, year, currency, accountId, categoryId, tagId);

        BigDecimal monthlyAverage = total.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        YearlyReport.YearlySummary summary = new YearlyReport.YearlySummary(
                total, currency, count, monthlyAverage);

        BigDecimal incomeTotal = nullSafe(
                reportRepository.sumIncomeByYear(userId, year, currency, accountId, categoryId, tagId));
        long incomeCount = reportRepository.countIncomeByYear(
                userId, year, currency, accountId, categoryId, tagId);
        YearlyReport.IncomeSummary incomeSummary =
                new YearlyReport.IncomeSummary(incomeTotal, incomeCount, currency);

        List<YearlyReport.MonthBreakdown> byMonth =
                reportRepository.monthBreakdownByYear(userId, year, currency, accountId, categoryId, tagId)
                        .stream()
                        .map(row -> new YearlyReport.MonthBreakdown(
                                row.getMonth(),
                                YearlyReport.monthAbbr(row.getMonth()),
                                row.getAmount(),
                                row.getCount()))
                        .toList();

        // row.getCategory() is both key and label: the JOIN already returns the human-readable name.
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

        List<YearlyReport.MerchantSummary> topMerchants =
                reportRepository.topMerchantsByYear(userId, year, currency, accountId, categoryId, tagId)
                        .stream()
                        .map(row -> new YearlyReport.MerchantSummary(
                                row.getName(), row.getAmount(), row.getCount()))
                        .toList();

        YearlyReport.Highlights highlights = buildHighlights(byMonth, byCategory);

        return new YearlyReport(year, summary, incomeSummary, byMonth, byCategory, topMerchants, highlights);
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : ZERO;
    }

    /** Returns 0.0 when total is zero to avoid division-by-zero. */
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

    /** Top category is just the first entry since the list already arrives sorted by amount DESC from SQL. */
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
