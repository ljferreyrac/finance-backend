package com.finanzasia.application.service;

import com.finanzasia.domain.model.MonthlyReport;
import com.finanzasia.domain.model.YearlyReport;
import com.finanzasia.domain.port.out.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    private ReportService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String PEN = "PEN";

    // Convenient filter constants shared across tests.
    // null = no filter (the default case exercised by most tests).
    private static final UUID NO_ACCOUNT  = null;
    private static final UUID NO_CATEGORY = null;
    private static final UUID NO_TAG      = null;

    @BeforeEach
    void setUp() {
        service = new ReportService(reportRepository);
    }

    // ------------------------------------------------------------------
    // Helper factories for projection stubs
    // ------------------------------------------------------------------

    private ReportRepository.CategoryRow categoryRow(String cat, BigDecimal amount, long count) {
        return new ReportRepository.CategoryRow() {
            @Override public String getCategory() { return cat; }
            @Override public BigDecimal getAmount() { return amount; }
            @Override public long getCount() { return count; }
        };
    }

    private ReportRepository.WeekRow weekRow(int weekNumber, BigDecimal amount, long count) {
        return new ReportRepository.WeekRow() {
            @Override public int getWeekNumber() { return weekNumber; }
            @Override public BigDecimal getAmount() { return amount; }
            @Override public long getCount() { return count; }
        };
    }

    private ReportRepository.MerchantRow merchantRow(String name, BigDecimal amount, long count) {
        return new ReportRepository.MerchantRow() {
            @Override public String getName() { return name; }
            @Override public BigDecimal getAmount() { return amount; }
            @Override public long getCount() { return count; }
        };
    }

    private ReportRepository.MonthRow monthRow(int month, BigDecimal amount, long count) {
        return new ReportRepository.MonthRow() {
            @Override public int getMonth() { return month; }
            @Override public BigDecimal getAmount() { return amount; }
            @Override public long getCount() { return count; }
        };
    }

    // ------------------------------------------------------------------
    // getMonthlyReport
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("getMonthlyReport")
    class GetMonthlyReport {

        @Test
        @DisplayName("builds period label in Spanish")
        void buildsPeriodLabel() {
            stubMonthlyEmpty(2026, 3);

            MonthlyReport report = service.getMonthlyReport(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG);

            assertThat(report.period().label()).isEqualTo("Marzo 2026");
            assertThat(report.period().year()).isEqualTo(2026);
            assertThat(report.period().month()).isEqualTo(3);
        }

        @Test
        @DisplayName("returns zeroed summary when no expenses exist")
        void returnsZeroedSummaryForEmptyPeriod() {
            stubMonthlyEmpty(2026, 3);

            MonthlyReport report = service.getMonthlyReport(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG);

            assertThat(report.summary().totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(report.summary().transactionCount()).isZero();
            assertThat(report.summary().dailyAverage()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("computes daily average as total divided by days in month")
        void computesDailyAverage() {
            // March has 31 days; total = 310.00 -> dailyAverage = 10.00
            when(reportRepository.sumByMonth(USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.valueOf(310.00));
            when(reportRepository.countByMonth(USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(10L);
            when(reportRepository.sumByMonth(USER_ID, 2026, 2, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.valueOf(200.00));
            when(reportRepository.categoryBreakdownByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.weekBreakdownByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.topMerchantsByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());

            MonthlyReport report = service.getMonthlyReport(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG);

            assertThat(report.summary().dailyAverage())
                    .isEqualByComparingTo(BigDecimal.valueOf(10.00));
        }

        @Test
        @DisplayName("wraps previous month to December of prior year when month is January")
        void wrapsPreviousMonthToDecemberForJanuary() {
            // Current period: January 2026. Previous: December 2025.
            when(reportRepository.sumByMonth(USER_ID, 2026, 1, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.valueOf(500.00));
            when(reportRepository.countByMonth(USER_ID, 2026, 1, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(5L);
            when(reportRepository.sumByMonth(USER_ID, 2025, 12, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.valueOf(400.00));
            when(reportRepository.categoryBreakdownByMonth(
                    USER_ID, 2026, 1, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.weekBreakdownByMonth(
                    USER_ID, 2026, 1, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.topMerchantsByMonth(
                    USER_ID, 2026, 1, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());

            MonthlyReport report = service.getMonthlyReport(
                    USER_ID, 2026, 1, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG);

            assertThat(report.vsLastMonth().previousAmount())
                    .isEqualByComparingTo(BigDecimal.valueOf(400.00));
            assertThat(report.vsLastMonth().trend()).isEqualTo("UP");
        }

        @Test
        @DisplayName("sets trend UP when current > previous")
        void trendIsUpWhenCurrentExceedsPrevious() {
            stubMonthlyWithTotal(2026, 3, BigDecimal.valueOf(1500), BigDecimal.valueOf(1000));

            MonthlyReport report = service.getMonthlyReport(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG);

            assertThat(report.vsLastMonth().trend()).isEqualTo("UP");
            assertThat(report.vsLastMonth().changeAmount())
                    .isEqualByComparingTo(BigDecimal.valueOf(500));
        }

        @Test
        @DisplayName("sets trend DOWN when current < previous")
        void trendIsDownWhenCurrentBelowPrevious() {
            stubMonthlyWithTotal(2026, 3, BigDecimal.valueOf(800), BigDecimal.valueOf(1200));

            MonthlyReport report = service.getMonthlyReport(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG);

            assertThat(report.vsLastMonth().trend()).isEqualTo("DOWN");
        }

        @Test
        @DisplayName("sets trend FLAT when current equals previous")
        void trendIsFlatWhenEqual() {
            stubMonthlyWithTotal(2026, 3, BigDecimal.valueOf(1000), BigDecimal.valueOf(1000));

            MonthlyReport report = service.getMonthlyReport(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG);

            assertThat(report.vsLastMonth().trend()).isEqualTo("FLAT");
        }

        @Test
        @DisplayName("maps category rows to CategoryBreakdown with Spanish label and percentage")
        void mapsCategoryBreakdownWithLabelAndPercentage() {
            when(reportRepository.sumByMonth(USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.valueOf(1000.00));
            when(reportRepository.countByMonth(USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(20L);
            when(reportRepository.sumByMonth(USER_ID, 2026, 2, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.ZERO);
            // The SQL query JOINs categories and returns c.name as "category",
            // so the stub returns the display name directly (not the slug).
            when(reportRepository.categoryBreakdownByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of(categoryRow("Comida", BigDecimal.valueOf(500.00), 10)));
            when(reportRepository.weekBreakdownByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.topMerchantsByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());

            MonthlyReport report = service.getMonthlyReport(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG);

            assertThat(report.byCategory()).hasSize(1);
            MonthlyReport.CategoryBreakdown cat = report.byCategory().get(0);
            assertThat(cat.category()).isEqualTo("Comida");
            assertThat(cat.label()).isEqualTo("Comida");
            assertThat(cat.percentage()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("maps week rows to WeekBreakdown with label Sem N")
        void mapsWeekBreakdownWithLabel() {
            when(reportRepository.sumByMonth(USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.valueOf(300.00));
            when(reportRepository.countByMonth(USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(5L);
            when(reportRepository.sumByMonth(USER_ID, 2026, 2, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.ZERO);
            when(reportRepository.categoryBreakdownByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.weekBreakdownByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of(weekRow(2, BigDecimal.valueOf(300.00), 5)));
            when(reportRepository.topMerchantsByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());

            MonthlyReport report = service.getMonthlyReport(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG);

            assertThat(report.byWeek()).hasSize(1);
            assertThat(report.byWeek().get(0).label()).isEqualTo("Sem 2");
        }

        @Test
        @DisplayName("includes top merchants in report")
        void includesTopMerchants() {
            when(reportRepository.sumByMonth(USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.valueOf(200.00));
            when(reportRepository.countByMonth(USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(3L);
            when(reportRepository.sumByMonth(USER_ID, 2026, 2, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.ZERO);
            when(reportRepository.categoryBreakdownByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.weekBreakdownByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.topMerchantsByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of(merchantRow("Plaza Vea", BigDecimal.valueOf(200.00), 3)));

            MonthlyReport report = service.getMonthlyReport(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG);

            assertThat(report.topMerchants()).hasSize(1);
            assertThat(report.topMerchants().get(0).name()).isEqualTo("Plaza Vea");
        }

        @Test
        @DisplayName("passes accountId filter down to all repository calls")
        void passesAccountFilterToRepository() {
            UUID account = UUID.randomUUID();
            when(reportRepository.sumByMonth(USER_ID, 2026, 3, PEN, account, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.valueOf(500.00));
            when(reportRepository.countByMonth(USER_ID, 2026, 3, PEN, account, NO_CATEGORY, NO_TAG))
                    .thenReturn(5L);
            when(reportRepository.sumByMonth(USER_ID, 2026, 2, PEN, account, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.ZERO);
            when(reportRepository.categoryBreakdownByMonth(
                    USER_ID, 2026, 3, PEN, account, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.weekBreakdownByMonth(
                    USER_ID, 2026, 3, PEN, account, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.topMerchantsByMonth(
                    USER_ID, 2026, 3, PEN, account, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());

            MonthlyReport report = service.getMonthlyReport(
                    USER_ID, 2026, 3, PEN, account, NO_CATEGORY, NO_TAG);

            assertThat(report.summary().totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(500.00));
        }

        @Test
        @DisplayName("passes categoryId filter down to all repository calls")
        void passesCategoryFilterToRepository() {
            UUID category = UUID.randomUUID();
            when(reportRepository.sumByMonth(USER_ID, 2026, 3, PEN, NO_ACCOUNT, category, NO_TAG))
                    .thenReturn(BigDecimal.valueOf(300.00));
            when(reportRepository.countByMonth(USER_ID, 2026, 3, PEN, NO_ACCOUNT, category, NO_TAG))
                    .thenReturn(3L);
            when(reportRepository.sumByMonth(USER_ID, 2026, 2, PEN, NO_ACCOUNT, category, NO_TAG))
                    .thenReturn(BigDecimal.ZERO);
            when(reportRepository.categoryBreakdownByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, category, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.weekBreakdownByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, category, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.topMerchantsByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, category, NO_TAG))
                    .thenReturn(List.of());

            MonthlyReport report = service.getMonthlyReport(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, category, NO_TAG);

            assertThat(report.summary().totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(300.00));
        }

        @Test
        @DisplayName("passes tagId filter down to all repository calls")
        void passesTagFilterToRepository() {
            UUID tag = UUID.randomUUID();
            when(reportRepository.sumByMonth(USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, tag))
                    .thenReturn(BigDecimal.valueOf(150.00));
            when(reportRepository.countByMonth(USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, tag))
                    .thenReturn(2L);
            when(reportRepository.sumByMonth(USER_ID, 2026, 2, PEN, NO_ACCOUNT, NO_CATEGORY, tag))
                    .thenReturn(BigDecimal.ZERO);
            when(reportRepository.categoryBreakdownByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, tag))
                    .thenReturn(List.of());
            when(reportRepository.weekBreakdownByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, tag))
                    .thenReturn(List.of());
            when(reportRepository.topMerchantsByMonth(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, tag))
                    .thenReturn(List.of());

            MonthlyReport report = service.getMonthlyReport(
                    USER_ID, 2026, 3, PEN, NO_ACCOUNT, NO_CATEGORY, tag);

            assertThat(report.summary().totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
        }

        // helper stubs

        private void stubMonthlyEmpty(int year, int month) {
            int prevYear  = (month == 1) ? year - 1 : year;
            int prevMonth = (month == 1) ? 12 : month - 1;
            when(reportRepository.sumByMonth(USER_ID, year, month, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.ZERO);
            when(reportRepository.countByMonth(USER_ID, year, month, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(0L);
            when(reportRepository.sumByMonth(USER_ID, prevYear, prevMonth, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.ZERO);
            when(reportRepository.categoryBreakdownByMonth(
                    USER_ID, year, month, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.weekBreakdownByMonth(
                    USER_ID, year, month, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.topMerchantsByMonth(
                    USER_ID, year, month, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
        }

        private void stubMonthlyWithTotal(int year, int month,
                BigDecimal current, BigDecimal previous) {
            int prevYear  = (month == 1) ? year - 1 : year;
            int prevMonth = (month == 1) ? 12 : month - 1;
            when(reportRepository.sumByMonth(USER_ID, year, month, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(current);
            when(reportRepository.countByMonth(USER_ID, year, month, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(10L);
            when(reportRepository.sumByMonth(USER_ID, prevYear, prevMonth, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(previous);
            when(reportRepository.categoryBreakdownByMonth(
                    USER_ID, year, month, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.weekBreakdownByMonth(
                    USER_ID, year, month, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.topMerchantsByMonth(
                    USER_ID, year, month, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
        }
    }

    // ------------------------------------------------------------------
    // getYearlyReport
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("getYearlyReport")
    class GetYearlyReport {

        @Test
        @DisplayName("returns zeroed summary when no expenses exist")
        void returnsZeroedSummaryForEmptyYear() {
            stubYearlyEmpty(2026);

            YearlyReport report = service.getYearlyReport(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG);

            assertThat(report.summary().totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(report.summary().transactionCount()).isZero();
        }

        @Test
        @DisplayName("computes monthly average as total divided by 12")
        void computesMonthlyAverage() {
            // total = 1200.00 -> monthlyAverage = 100.00
            when(reportRepository.sumByYear(USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.valueOf(1200.00));
            when(reportRepository.countByYear(USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(24L);
            when(reportRepository.monthBreakdownByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.categoryBreakdownByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.topMerchantsByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());

            YearlyReport report = service.getYearlyReport(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG);

            assertThat(report.summary().monthlyAverage())
                    .isEqualByComparingTo(BigDecimal.valueOf(100.00));
        }

        @Test
        @DisplayName("maps month rows with abbreviated Spanish labels")
        void mapsMonthRowsWithAbbreviatedLabels() {
            when(reportRepository.sumByYear(USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.valueOf(500));
            when(reportRepository.countByYear(USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(5L);
            when(reportRepository.monthBreakdownByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of(
                            monthRow(1, BigDecimal.valueOf(200), 2),
                            monthRow(12, BigDecimal.valueOf(300), 3)));
            when(reportRepository.categoryBreakdownByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.topMerchantsByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());

            YearlyReport report = service.getYearlyReport(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG);

            assertThat(report.byMonth()).hasSize(2);
            assertThat(report.byMonth().get(0).label()).isEqualTo("Ene");
            assertThat(report.byMonth().get(1).label()).isEqualTo("Dic");
        }

        @Test
        @DisplayName("identifies peak and lowest month from byMonth data")
        void identifiesPeakAndLowestMonths() {
            when(reportRepository.sumByYear(USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.valueOf(500));
            when(reportRepository.countByYear(USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(5L);
            when(reportRepository.monthBreakdownByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of(
                            monthRow(2, BigDecimal.valueOf(100), 2),
                            monthRow(6, BigDecimal.valueOf(300), 3),
                            monthRow(11, BigDecimal.valueOf(100), 2)));
            when(reportRepository.categoryBreakdownByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.topMerchantsByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());

            YearlyReport report = service.getYearlyReport(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG);

            assertThat(report.highlights().peakMonth().month()).isEqualTo(6);
            assertThat(report.highlights().peakMonth().amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(300));
        }

        @Test
        @DisplayName("maps category breakdown with Spanish label and percentage")
        void mapsCategoryBreakdownWithLabelAndPercentage() {
            when(reportRepository.sumByYear(USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.valueOf(1000));
            when(reportRepository.countByYear(USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(10L);
            when(reportRepository.monthBreakdownByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            // The SQL query JOINs categories and returns c.name as "category",
            // so the stub returns the display name directly (not the slug).
            when(reportRepository.categoryBreakdownByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of(categoryRow("Transporte", BigDecimal.valueOf(250), 4)));
            when(reportRepository.topMerchantsByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());

            YearlyReport report = service.getYearlyReport(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG);

            assertThat(report.byCategory()).hasSize(1);
            YearlyReport.CategoryBreakdown cat = report.byCategory().get(0);
            assertThat(cat.label()).isEqualTo("Transporte");
            assertThat(cat.percentage()).isEqualTo(25.0);
        }

        @Test
        @DisplayName("highlights topCategory as first entry of byCategory")
        void highlightsTopCategoryIsFirstCategory() {
            when(reportRepository.sumByYear(USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.valueOf(1000));
            when(reportRepository.countByYear(USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(10L);
            when(reportRepository.monthBreakdownByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            // SQL JOINs categories and returns c.name; stubs return display names directly.
            when(reportRepository.categoryBreakdownByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of(
                            categoryRow("Comida", BigDecimal.valueOf(600), 6),
                            categoryRow("Transporte", BigDecimal.valueOf(400), 4)));
            when(reportRepository.topMerchantsByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());

            YearlyReport report = service.getYearlyReport(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG);

            assertThat(report.highlights().topCategory().category()).isEqualTo("Comida");
        }

        @Test
        @DisplayName("highlights are null when no data exists")
        void highlightsAreNullWhenNoData() {
            stubYearlyEmpty(2026);

            YearlyReport report = service.getYearlyReport(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG);

            assertThat(report.highlights().peakMonth()).isNull();
            assertThat(report.highlights().lowestMonth()).isNull();
            assertThat(report.highlights().topCategory()).isNull();
        }

        @Test
        @DisplayName("passes accountId filter down to all repository calls")
        void passesAccountFilterToRepository() {
            UUID account = UUID.randomUUID();
            when(reportRepository.sumByYear(USER_ID, 2026, PEN, account, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.valueOf(800.00));
            when(reportRepository.countByYear(USER_ID, 2026, PEN, account, NO_CATEGORY, NO_TAG))
                    .thenReturn(8L);
            when(reportRepository.monthBreakdownByYear(
                    USER_ID, 2026, PEN, account, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.categoryBreakdownByYear(
                    USER_ID, 2026, PEN, account, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.topMerchantsByYear(
                    USER_ID, 2026, PEN, account, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());

            YearlyReport report = service.getYearlyReport(
                    USER_ID, 2026, PEN, account, NO_CATEGORY, NO_TAG);

            assertThat(report.summary().totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(800.00));
        }

        @Test
        @DisplayName("passes tagId filter down to all repository calls")
        void passesTagFilterToRepository() {
            UUID tag = UUID.randomUUID();
            when(reportRepository.sumByYear(USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, tag))
                    .thenReturn(BigDecimal.valueOf(200.00));
            when(reportRepository.countByYear(USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, tag))
                    .thenReturn(2L);
            when(reportRepository.monthBreakdownByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, tag))
                    .thenReturn(List.of());
            when(reportRepository.categoryBreakdownByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, tag))
                    .thenReturn(List.of());
            when(reportRepository.topMerchantsByYear(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, tag))
                    .thenReturn(List.of());

            YearlyReport report = service.getYearlyReport(
                    USER_ID, 2026, PEN, NO_ACCOUNT, NO_CATEGORY, tag);

            assertThat(report.summary().totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(200.00));
        }

        // helper stubs

        private void stubYearlyEmpty(int year) {
            when(reportRepository.sumByYear(USER_ID, year, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(BigDecimal.ZERO);
            when(reportRepository.countByYear(USER_ID, year, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(0L);
            when(reportRepository.monthBreakdownByYear(
                    USER_ID, year, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.categoryBreakdownByYear(
                    USER_ID, year, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
            when(reportRepository.topMerchantsByYear(
                    USER_ID, year, PEN, NO_ACCOUNT, NO_CATEGORY, NO_TAG))
                    .thenReturn(List.of());
        }
    }
}
