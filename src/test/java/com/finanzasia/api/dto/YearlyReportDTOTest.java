package com.finanzasia.api.dto;

import com.finanzasia.domain.model.YearlyReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YearlyReportDTOTest {

    private YearlyReport.MonthBreakdown month(int m) {
        return new YearlyReport.MonthBreakdown(m, "Mes" + m, new BigDecimal("100.00"), 5);
    }

    private YearlyReport.CategoryBreakdown category() {
        return new YearlyReport.CategoryBreakdown("food", "Comida", new BigDecimal("500.00"), 10, 33.3);
    }

    private YearlyReport buildReport(YearlyReport.Highlights highlights) {
        YearlyReport.YearlySummary summary =
                new YearlyReport.YearlySummary(new BigDecimal("15000.00"), "PEN", 450, new BigDecimal("1250.00"));
        YearlyReport.IncomeSummary incomeSummary =
                new YearlyReport.IncomeSummary(new BigDecimal("36000.00"), 24, "PEN");
        YearlyReport.MerchantSummary merchant =
                new YearlyReport.MerchantSummary("Plaza Vea", new BigDecimal("2000.00"), 50);

        return new YearlyReport(2026, summary, incomeSummary,
                List.of(month(3)), List.of(category()), List.of(merchant), highlights);
    }

    @Nested
    @DisplayName("from, with all highlights present")
    class WithHighlights {

        @Test
        @DisplayName("maps every field of the domain report onto the DTO")
        void mapsAllFields() {
            YearlyReport.Highlights highlights =
                    new YearlyReport.Highlights(month(1), month(6), category());
            YearlyReport report = buildReport(highlights);

            YearlyReportDTO dto = YearlyReportDTO.from(report);

            assertThat(dto.year()).isEqualTo(2026);
            assertThat(dto.summary().totalAmount()).isEqualByComparingTo("15000.00");
            assertThat(dto.summary().currency()).isEqualTo("PEN");
            assertThat(dto.summary().transactionCount()).isEqualTo(450);
            assertThat(dto.summary().monthlyAverage()).isEqualByComparingTo("1250.00");

            assertThat(dto.incomeSummary().total()).isEqualByComparingTo("36000.00");
            assertThat(dto.incomeSummary().transactionCount()).isEqualTo(24);
            assertThat(dto.incomeSummary().currency()).isEqualTo("PEN");

            assertThat(dto.byMonth()).hasSize(1);
            assertThat(dto.byMonth().get(0).month()).isEqualTo(3);
            assertThat(dto.byMonth().get(0).label()).isEqualTo("Mes3");

            assertThat(dto.byCategory()).hasSize(1);
            assertThat(dto.byCategory().get(0).category()).isEqualTo("food");
            assertThat(dto.byCategory().get(0).percentage()).isEqualTo(33.3);

            assertThat(dto.topMerchants()).hasSize(1);
            assertThat(dto.topMerchants().get(0).name()).isEqualTo("Plaza Vea");

            assertThat(dto.highlights().peakMonth().month()).isEqualTo(1);
            assertThat(dto.highlights().lowestMonth().month()).isEqualTo(6);
            assertThat(dto.highlights().topCategory().category()).isEqualTo("food");
        }
    }

    @Nested
    @DisplayName("from, with absent highlight fields")
    class WithoutHighlights {

        @Test
        @DisplayName("a null peakMonth, lowestMonth or topCategory maps to null, not an NPE")
        void nullHighlightFieldsStayNull() {
            YearlyReport.Highlights highlights = new YearlyReport.Highlights(null, null, null);
            YearlyReport report = buildReport(highlights);

            YearlyReportDTO dto = YearlyReportDTO.from(report);

            assertThat(dto.highlights().peakMonth()).isNull();
            assertThat(dto.highlights().lowestMonth()).isNull();
            assertThat(dto.highlights().topCategory()).isNull();
        }
    }
}
