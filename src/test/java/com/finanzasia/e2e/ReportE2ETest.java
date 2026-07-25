package com.finanzasia.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The integration-level counterpart to Phase 6's {@code JpaReportRepositoryPortTest}: this
 * confirms the full DTO assembly through {@code ReportController} on top of already-verified
 * queries, using today's date so the "current month/year" defaults exercise real data.
 */
class ReportE2ETest extends AbstractE2ETest {

    @Test
    @DisplayName("monthly report totals match the transactions actually seeded, and empty periods return zeros")
    void monthlyReportAggregatesSeededTransactions() {
        TokenPair user = registerAndLogin("report-monthly");
        String accountId = createAccount(user.accessToken());
        String categoryId = createCategory(user.accessToken());
        LocalDate today = LocalDate.now();

        createExpense(user.accessToken(), accountId, categoryId, "30.00", today);
        createExpense(user.accessToken(), accountId, categoryId, "20.00", today);

        Map<String, Object> report = getMonthlyReport(user.accessToken(), today.getYear(), today.getMonthValue());
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");

        assertThat(new BigDecimal(summary.get("totalAmount").toString())).isEqualByComparingTo("50.00");
        assertThat(((Number) summary.get("transactionCount")).longValue()).isEqualTo(2);

        // A year with no data at all still returns a valid, zeroed shape rather than an error.
        Map<String, Object> emptyReport = getMonthlyReport(user.accessToken(), 2019, 1);
        Map<String, Object> emptySummary = (Map<String, Object>) emptyReport.get("summary");
        assertThat(new BigDecimal(emptySummary.get("totalAmount").toString())).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(((Number) emptySummary.get("transactionCount")).longValue()).isZero();
    }

    @Test
    @DisplayName("yearly report groups the same seeded data by month")
    void yearlyReportGroupsByMonth() {
        TokenPair user = registerAndLogin("report-yearly");
        String accountId = createAccount(user.accessToken());
        String categoryId = createCategory(user.accessToken());
        int year = Year.now().getValue();
        createExpense(user.accessToken(), accountId, categoryId, "75.00", LocalDate.of(year, 1, 15));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/reports/yearly?year=" + year, HttpMethod.GET, authorized(user.accessToken()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> summary = (Map<String, Object>) response.getBody().get("summary");
        assertThat(new BigDecimal(summary.get("totalAmount").toString())).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("an invalid month is rejected with 400 through the real validation path")
    void invalidMonthReturns400() {
        ResponseEntity<Map> response = getMonthlyReportRaw(registerAndLogin("report-invalid").accessToken(), 2026, 13);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private Map<String, Object> getMonthlyReport(String accessToken, int year, int month) {
        return getMonthlyReportRaw(accessToken, year, month).getBody();
    }

    private ResponseEntity<Map> getMonthlyReportRaw(String accessToken, int year, int month) {
        return restTemplate.exchange(
                "/api/v1/reports/monthly?year=" + year + "&month=" + month,
                HttpMethod.GET, authorized(accessToken), Map.class);
    }

    private String createAccount(String accessToken) {
        Map<String, Object> body = Map.of(
                "name", "Report Account " + UUID.randomUUID(), "type", "BANK", "currency", "PEN",
                "initialBalance", 0, "isDefault", false);
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/accounts", HttpMethod.POST, authorized(body, accessToken), Map.class);
        return (String) response.getBody().get("id");
    }

    private String createCategory(String accessToken) {
        Map<String, Object> body = Map.of("name", "Food-" + UUID.randomUUID(), "isDefault", false);
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/categories", HttpMethod.POST, authorized(body, accessToken), Map.class);
        return (String) response.getBody().get("id");
    }

    private void createExpense(String accessToken, String accountId, String categoryId, String amount, LocalDate date) {
        Map<String, Object> body = Map.of(
                "type", "EXPENSE", "amount", new BigDecimal(amount), "currency", "PEN",
                "accountId", accountId, "categoryId", categoryId,
                "transactionDate", date.toString());
        restTemplate.exchange("/api/v1/transactions", HttpMethod.POST, authorized(body, accessToken), Map.class);
    }
}
