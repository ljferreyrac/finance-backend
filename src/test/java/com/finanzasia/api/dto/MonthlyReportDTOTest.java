package com.finanzasia.api.dto;

import com.finanzasia.domain.model.MonthlyReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MonthlyReportDTOTest {

    @Test
    @DisplayName("from maps every field of the domain report onto the DTO")
    void mapsAllFields() {
        MonthlyReport.Period period = new MonthlyReport.Period(2026, 3, "Marzo 2026");
        MonthlyReport.Summary summary =
                new MonthlyReport.Summary(new BigDecimal("1500.00"), "PEN", 45, new BigDecimal("50.00"));
        MonthlyReport.IncomeSummary incomeSummary =
                new MonthlyReport.IncomeSummary(new BigDecimal("3000.00"), 2, "PEN");
        MonthlyReport.VsLastMonth vsLastMonth =
                new MonthlyReport.VsLastMonth(new BigDecimal("1200.00"), new BigDecimal("300.00"), 25.0, "UP");
        MonthlyReport.CategoryBreakdown category =
                new MonthlyReport.CategoryBreakdown("food", "Comida", new BigDecimal("500.00"), 20, 33.3);
        MonthlyReport.WeekBreakdown week =
                new MonthlyReport.WeekBreakdown(1, "Sem 1", new BigDecimal("300.00"), 10);
        MonthlyReport.MerchantSummary merchant =
                new MonthlyReport.MerchantSummary("Plaza Vea", new BigDecimal("200.00"), 5);

        MonthlyReport report = new MonthlyReport(period, summary, incomeSummary, vsLastMonth,
                List.of(category), List.of(week), List.of(merchant));

        MonthlyReportDTO dto = MonthlyReportDTO.from(report);

        assertThat(dto.period().year()).isEqualTo(2026);
        assertThat(dto.period().month()).isEqualTo(3);
        assertThat(dto.period().label()).isEqualTo("Marzo 2026");

        assertThat(dto.summary().totalAmount()).isEqualByComparingTo("1500.00");
        assertThat(dto.summary().currency()).isEqualTo("PEN");
        assertThat(dto.summary().transactionCount()).isEqualTo(45);
        assertThat(dto.summary().dailyAverage()).isEqualByComparingTo("50.00");

        assertThat(dto.incomeSummary().total()).isEqualByComparingTo("3000.00");
        assertThat(dto.incomeSummary().transactionCount()).isEqualTo(2);
        assertThat(dto.incomeSummary().currency()).isEqualTo("PEN");

        assertThat(dto.vsLastMonth().previousAmount()).isEqualByComparingTo("1200.00");
        assertThat(dto.vsLastMonth().changeAmount()).isEqualByComparingTo("300.00");
        assertThat(dto.vsLastMonth().changePercent()).isEqualTo(25.0);
        assertThat(dto.vsLastMonth().trend()).isEqualTo("UP");

        assertThat(dto.byCategory()).hasSize(1);
        assertThat(dto.byCategory().get(0).category()).isEqualTo("food");
        assertThat(dto.byCategory().get(0).percentage()).isEqualTo(33.3);

        assertThat(dto.byWeek()).hasSize(1);
        assertThat(dto.byWeek().get(0).weekNumber()).isEqualTo(1);
        assertThat(dto.byWeek().get(0).label()).isEqualTo("Sem 1");

        assertThat(dto.topMerchants()).hasSize(1);
        assertThat(dto.topMerchants().get(0).name()).isEqualTo("Plaza Vea");
    }

    @Test
    @DisplayName("periodLabel builds the Spanish 'Month Year' label")
    void periodLabelBuildsSpanishLabel() {
        assertThat(MonthlyReport.periodLabel(2026, 3)).isEqualTo("Marzo 2026");
        assertThat(MonthlyReport.periodLabel(2025, 12)).isEqualTo("Diciembre 2025");
    }
}
