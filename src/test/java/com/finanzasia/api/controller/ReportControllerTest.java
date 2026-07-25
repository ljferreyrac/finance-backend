package com.finanzasia.api.controller;

import com.finanzasia.api.security.UserPrincipal;
import com.finanzasia.domain.model.MonthlyReport;
import com.finanzasia.domain.model.YearlyReport;
import com.finanzasia.domain.port.in.AuthenticateAccessTokenUseCase;
import com.finanzasia.domain.port.in.GetMonthlyReportUseCase;
import com.finanzasia.domain.port.in.GetYearlyReportUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetMonthlyReportUseCase getMonthlyReport;

    @MockitoBean
    private GetYearlyReportUseCase getYearlyReport;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessToken;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    private UserPrincipal principal;
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(USER_ID, "user@example.com");
    }

    private MonthlyReport buildMonthlyReport() {
        MonthlyReport.Period period = new MonthlyReport.Period(2026, 3, "Marzo 2026");
        MonthlyReport.Summary summary = new MonthlyReport.Summary(
                new BigDecimal("100.00"), "PEN", 5, new BigDecimal("3.33"));
        MonthlyReport.IncomeSummary income = new MonthlyReport.IncomeSummary(BigDecimal.ZERO, 0, "PEN");
        MonthlyReport.VsLastMonth vsLastMonth =
                new MonthlyReport.VsLastMonth(BigDecimal.ZERO, BigDecimal.ZERO, 0.0, "FLAT");
        return new MonthlyReport(period, summary, income, vsLastMonth, List.of(), List.of(), List.of());
    }

    private YearlyReport buildYearlyReport() {
        YearlyReport.YearlySummary summary =
                new YearlyReport.YearlySummary(new BigDecimal("1000.00"), "PEN", 20, new BigDecimal("83.33"));
        YearlyReport.IncomeSummary income = new YearlyReport.IncomeSummary(BigDecimal.ZERO, 0, "PEN");
        YearlyReport.Highlights highlights = new YearlyReport.Highlights(null, null, null);
        return new YearlyReport(2026, summary, income, List.of(), List.of(), List.of(), highlights);
    }

    @Nested
    @DisplayName("GET /api/v1/reports/monthly")
    class Monthly {

        @Test
        @DisplayName("returns 200 with the aggregated report")
        void returnsReport() throws Exception {
            when(getMonthlyReport.getMonthlyReport(eq(USER_ID), eq(2026), eq(3), eq("PEN"),
                    isNull(), isNull(), isNull()))
                    .thenReturn(buildMonthlyReport());

            mockMvc.perform(get("/api/v1/reports/monthly")
                            .param("year", "2026").param("month", "3").param("currency", "PEN")
                            .with(user(principal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.period.label").value("Marzo 2026"));
        }

        @Test
        @DisplayName("forwards accountId, categoryId and tagId filters")
        void forwardsOptionalFilters() throws Exception {
            UUID accountId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UUID tagId = UUID.randomUUID();
            when(getMonthlyReport.getMonthlyReport(any(), anyInt(), anyInt(), any(), any(), any(), any()))
                    .thenReturn(buildMonthlyReport());

            mockMvc.perform(get("/api/v1/reports/monthly")
                            .param("year", "2026").param("month", "3")
                            .param("accountId", accountId.toString())
                            .param("categoryId", categoryId.toString())
                            .param("tagId", tagId.toString())
                            .with(user(principal)))
                    .andExpect(status().isOk());

            verify(getMonthlyReport).getMonthlyReport(USER_ID, 2026, 3, "PEN", accountId, categoryId, tagId);
        }

        @Test
        @DisplayName("a year outside 2000-2100 is rejected with 400")
        void yearOutOfRangeReturns400() throws Exception {
            mockMvc.perform(get("/api/v1/reports/monthly")
                            .param("year", "1999").param("month", "3")
                            .with(user(principal)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("a month outside 1-12 is rejected with 400")
        void monthOutOfRangeReturns400() throws Exception {
            mockMvc.perform(get("/api/v1/reports/monthly")
                            .param("year", "2026").param("month", "13")
                            .with(user(principal)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("an unsupported currency is rejected with 400")
        void unsupportedCurrencyReturns400() throws Exception {
            mockMvc.perform(get("/api/v1/reports/monthly")
                            .param("year", "2026").param("month", "3").param("currency", "EUR")
                            .with(user(principal)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reports/yearly")
    class Yearly {

        @Test
        @DisplayName("returns 200 with the aggregated report")
        void returnsReport() throws Exception {
            when(getYearlyReport.getYearlyReport(eq(USER_ID), eq(2026), eq("PEN"), isNull(), isNull(), isNull()))
                    .thenReturn(buildYearlyReport());

            mockMvc.perform(get("/api/v1/reports/yearly")
                            .param("year", "2026").param("currency", "PEN")
                            .with(user(principal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.year").value(2026));
        }

        @Test
        @DisplayName("a year outside 2000-2100 is rejected with 400")
        void yearOutOfRangeReturns400() throws Exception {
            mockMvc.perform(get("/api/v1/reports/yearly")
                            .param("year", "2101")
                            .with(user(principal)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("an unsupported currency is rejected with 400")
        void unsupportedCurrencyReturns400() throws Exception {
            mockMvc.perform(get("/api/v1/reports/yearly")
                            .param("year", "2026").param("currency", "EUR")
                            .with(user(principal)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("GET /api/v1/reports/monthly without authentication is rejected with 401")
    void monthlyWithoutAuthIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/reports/monthly").param("year", "2026").param("month", "3"))
                .andExpect(status().isUnauthorized());
    }
}
