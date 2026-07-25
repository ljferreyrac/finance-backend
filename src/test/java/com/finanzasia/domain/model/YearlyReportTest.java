package com.finanzasia.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YearlyReportTest {

    @Test
    @DisplayName("all record accessors return the constructed values")
    void recordAccessorsReturnConstructorValues() {
        YearlyReport.YearlySummary summary =
                new YearlyReport.YearlySummary(new BigDecimal("15000.00"), "PEN", 450, new BigDecimal("1250.00"));
        YearlyReport.IncomeSummary incomeSummary =
                new YearlyReport.IncomeSummary(new BigDecimal("36000.00"), 24, "PEN");
        YearlyReport.MonthBreakdown month =
                new YearlyReport.MonthBreakdown(3, "Mar", new BigDecimal("1200.00"), 35);
        YearlyReport.CategoryBreakdown category =
                new YearlyReport.CategoryBreakdown("food", "Comida", new BigDecimal("5000.00"), 150, 33.3);
        YearlyReport.MerchantSummary merchant =
                new YearlyReport.MerchantSummary("Plaza Vea", new BigDecimal("2000.00"), 50);
        YearlyReport.Highlights highlights = new YearlyReport.Highlights(month, month, category);

        YearlyReport report = new YearlyReport(
                2026, summary, incomeSummary, List.of(month), List.of(category), List.of(merchant), highlights);

        assertThat(report.year()).isEqualTo(2026);
        assertThat(report.summary()).isEqualTo(summary);
        assertThat(report.incomeSummary()).isEqualTo(incomeSummary);
        assertThat(report.byMonth()).containsExactly(month);
        assertThat(report.byCategory()).containsExactly(category);
        assertThat(report.topMerchants()).containsExactly(merchant);
        assertThat(report.highlights()).isEqualTo(highlights);
        assertThat(report.highlights().peakMonth()).isEqualTo(month);
        assertThat(report.highlights().lowestMonth()).isEqualTo(month);
        assertThat(report.highlights().topCategory()).isEqualTo(category);
    }

    @Test
    @DisplayName("monthAbbr returns the abbreviated Spanish month name")
    void monthAbbrReturnsAbbreviation() {
        assertThat(YearlyReport.monthAbbr(1)).isEqualTo("Ene");
        assertThat(YearlyReport.monthAbbr(12)).isEqualTo("Dic");
    }

    @Test
    @DisplayName("monthFull returns the full Spanish month name")
    void monthFullReturnsFullName() {
        assertThat(YearlyReport.monthFull(1)).isEqualTo("Enero");
        assertThat(YearlyReport.monthFull(12)).isEqualTo("Diciembre");
    }
}
