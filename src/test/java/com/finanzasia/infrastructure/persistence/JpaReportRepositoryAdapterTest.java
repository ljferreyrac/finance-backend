package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.port.out.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Every method on this adapter is a pure delegation to the Spring Data port; these
 * tests exist only to pin that the arguments and return value pass through unchanged.
 */
@ExtendWith(MockitoExtension.class)
class JpaReportRepositoryAdapterTest {

    @Mock
    private JpaReportRepositoryPort jpaPort;

    private JpaReportRepositoryAdapter adapter;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID TAG_ID = UUID.randomUUID();
    private static final int YEAR = 2026;
    private static final int MONTH = 3;
    private static final String CURRENCY = "PEN";

    @BeforeEach
    void setUp() {
        adapter = new JpaReportRepositoryAdapter(jpaPort);
    }

    @Test
    @DisplayName("sumByMonth delegates directly to the port")
    void sumByMonthDelegates() {
        when(jpaPort.sumByMonth(USER_ID, YEAR, MONTH, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .thenReturn(new BigDecimal("1500.00"));

        BigDecimal result = adapter.sumByMonth(USER_ID, YEAR, MONTH, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID);

        assertThat(result).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("countByMonth delegates directly to the port")
    void countByMonthDelegates() {
        when(jpaPort.countByMonth(USER_ID, YEAR, MONTH, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .thenReturn(45L);

        assertThat(adapter.countByMonth(USER_ID, YEAR, MONTH, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .isEqualTo(45L);
    }

    @Test
    @DisplayName("categoryBreakdownByMonth delegates directly to the port")
    void categoryBreakdownByMonthDelegates() {
        List<ReportRepository.CategoryRow> rows = List.of();
        when(jpaPort.findCategoryBreakdownByMonth(USER_ID, YEAR, MONTH, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .thenReturn(rows);

        assertThat(adapter.categoryBreakdownByMonth(USER_ID, YEAR, MONTH, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .isSameAs(rows);
    }

    @Test
    @DisplayName("weekBreakdownByMonth delegates directly to the port")
    void weekBreakdownByMonthDelegates() {
        List<ReportRepository.WeekRow> rows = List.of();
        when(jpaPort.findWeekBreakdownByMonth(USER_ID, YEAR, MONTH, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .thenReturn(rows);

        assertThat(adapter.weekBreakdownByMonth(USER_ID, YEAR, MONTH, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .isSameAs(rows);
    }

    @Test
    @DisplayName("topMerchantsByMonth delegates directly to the port")
    void topMerchantsByMonthDelegates() {
        List<ReportRepository.MerchantRow> rows = List.of();
        when(jpaPort.findTopMerchantsByMonth(USER_ID, YEAR, MONTH, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .thenReturn(rows);

        assertThat(adapter.topMerchantsByMonth(USER_ID, YEAR, MONTH, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .isSameAs(rows);
    }

    @Test
    @DisplayName("sumIncomeByMonth delegates directly to the port")
    void sumIncomeByMonthDelegates() {
        when(jpaPort.sumIncomeByMonth(USER_ID, YEAR, MONTH, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .thenReturn(new BigDecimal("3000.00"));

        assertThat(adapter.sumIncomeByMonth(USER_ID, YEAR, MONTH, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("countIncomeByMonth delegates directly to the port")
    void countIncomeByMonthDelegates() {
        when(jpaPort.countIncomeByMonth(USER_ID, YEAR, MONTH, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .thenReturn(2L);

        assertThat(adapter.countIncomeByMonth(USER_ID, YEAR, MONTH, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .isEqualTo(2L);
    }

    @Test
    @DisplayName("sumByYear delegates directly to the port")
    void sumByYearDelegates() {
        when(jpaPort.sumByYear(USER_ID, YEAR, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .thenReturn(new BigDecimal("15000.00"));

        assertThat(adapter.sumByYear(USER_ID, YEAR, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .isEqualByComparingTo("15000.00");
    }

    @Test
    @DisplayName("countByYear delegates directly to the port")
    void countByYearDelegates() {
        when(jpaPort.countByYear(USER_ID, YEAR, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .thenReturn(450L);

        assertThat(adapter.countByYear(USER_ID, YEAR, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .isEqualTo(450L);
    }

    @Test
    @DisplayName("monthBreakdownByYear delegates directly to the port")
    void monthBreakdownByYearDelegates() {
        List<ReportRepository.MonthRow> rows = List.of();
        when(jpaPort.findMonthBreakdownByYear(USER_ID, YEAR, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .thenReturn(rows);

        assertThat(adapter.monthBreakdownByYear(USER_ID, YEAR, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .isSameAs(rows);
    }

    @Test
    @DisplayName("categoryBreakdownByYear delegates directly to the port")
    void categoryBreakdownByYearDelegates() {
        List<ReportRepository.CategoryRow> rows = List.of();
        when(jpaPort.findCategoryBreakdownByYear(USER_ID, YEAR, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .thenReturn(rows);

        assertThat(adapter.categoryBreakdownByYear(USER_ID, YEAR, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .isSameAs(rows);
    }

    @Test
    @DisplayName("topMerchantsByYear delegates directly to the port")
    void topMerchantsByYearDelegates() {
        List<ReportRepository.MerchantRow> rows = List.of();
        when(jpaPort.findTopMerchantsByYear(USER_ID, YEAR, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .thenReturn(rows);

        assertThat(adapter.topMerchantsByYear(USER_ID, YEAR, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .isSameAs(rows);
    }

    @Test
    @DisplayName("sumIncomeByYear delegates directly to the port")
    void sumIncomeByYearDelegates() {
        when(jpaPort.sumIncomeByYear(USER_ID, YEAR, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .thenReturn(new BigDecimal("36000.00"));

        assertThat(adapter.sumIncomeByYear(USER_ID, YEAR, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .isEqualByComparingTo("36000.00");
    }

    @Test
    @DisplayName("countIncomeByYear delegates directly to the port")
    void countIncomeByYearDelegates() {
        when(jpaPort.countIncomeByYear(USER_ID, YEAR, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .thenReturn(24L);

        assertThat(adapter.countIncomeByYear(USER_ID, YEAR, CURRENCY, ACCOUNT_ID, CATEGORY_ID, TAG_ID))
                .isEqualTo(24L);
    }
}
