package com.finanzasia.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code /api/v1/accounts} endpoints that Phase 7 did not reach: update, delete,
 * set-default and net-worth.
 *
 * <p>Set-default is the one that genuinely needs a real database: {@code accounts} carries
 * {@code CREATE UNIQUE INDEX uidx_accounts_one_default_per_user ON accounts (user_id)
 * WHERE is_default = TRUE}. If {@code clearDefaultForUser} ever stopped running before the
 * new default is written, Postgres would reject the second one - a constraint no mock can enforce.
 */
class AccountLifecycleE2ETest extends AbstractE2ETest {

    @Test
    @DisplayName("update changes mutable fields and leaves the balance untouched")
    void updateChangesMutableFields() {
        TokenPair user = registerAndLogin("acct-update");
        String accountId = createAccount(user.accessToken(), "BANK", "PEN", "250.00");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/accounts/" + accountId, HttpMethod.PUT,
                authorized(Map.of("name", "Renamed Account", "color", "#123ABC"), user.accessToken()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("name")).isEqualTo("Renamed Account");
        assertThat(response.getBody().get("color")).isEqualTo("#123ABC");
        assertThat(new BigDecimal(response.getBody().get("currentBalance").toString()))
                .isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("updating another user's account returns 404")
    void updatingAnotherUsersAccountReturns404() {
        TokenPair userA = registerAndLogin("acct-upd-a");
        TokenPair userB = registerAndLogin("acct-upd-b");
        String accountId = createAccount(userA.accessToken(), "BANK", "PEN", "0");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/accounts/" + accountId, HttpMethod.PUT,
                authorized(Map.of("name", "Hijacked"), userB.accessToken()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("setting a default clears the previous one, satisfying the one-default-per-user index")
    void settingDefaultClearsThePreviousDefault() {
        TokenPair user = registerAndLogin("acct-default");
        String first = createAccount(user.accessToken(), "BANK", "PEN", "0");
        String second = createAccount(user.accessToken(), "CASH", "PEN", "0");

        ResponseEntity<Map> setFirst = restTemplate.exchange(
                "/api/v1/accounts/" + first + "/default", HttpMethod.PATCH,
                authorized(null, user.accessToken()), Map.class);
        assertThat(setFirst.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(setFirst.getBody().get("isDefault")).isEqualTo(true);

        // Would violate uidx_accounts_one_default_per_user if the previous default weren't cleared.
        ResponseEntity<Map> setSecond = restTemplate.exchange(
                "/api/v1/accounts/" + second + "/default", HttpMethod.PATCH,
                authorized(null, user.accessToken()), Map.class);
        assertThat(setSecond.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<Map<String, Object>> accounts = listAccounts(user.accessToken());
        assertThat(accounts).filteredOn(a -> Boolean.TRUE.equals(a.get("isDefault")))
                .extracting(a -> a.get("id"))
                .containsExactly(second);
    }

    @Test
    @DisplayName("delete removes an account that has no transactions")
    void deleteRemovesUnusedAccount() {
        TokenPair user = registerAndLogin("acct-delete");
        String accountId = createAccount(user.accessToken(), "BANK", "PEN", "0");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/accounts/" + accountId, HttpMethod.DELETE,
                authorized(user.accessToken()), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(listAccounts(user.accessToken())).isEmpty();
    }

    @Test
    @DisplayName("deleting an account that still has transactions returns 409 with the count")
    void deletingAccountWithTransactionsReturns409() {
        TokenPair user = registerAndLogin("acct-in-use");
        String accountId = createAccount(user.accessToken(), "BANK", "PEN", "500.00");
        String categoryId = createCategory(user.accessToken());
        createExpense(user.accessToken(), accountId, categoryId, "10.00");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/accounts/" + accountId, HttpMethod.DELETE,
                authorized(user.accessToken()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("title")).isEqualTo("Account In Use");
        assertThat(((Number) response.getBody().get("transactionCount")).longValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("net worth totals PEN and USD balances separately")
    void netWorthSeparatesCurrencies() {
        TokenPair user = registerAndLogin("acct-networth");
        createAccount(user.accessToken(), "BANK", "PEN", "1000.00");
        createAccount(user.accessToken(), "BANK", "PEN", "500.50");
        createAccount(user.accessToken(), "BANK", "USD", "200.00");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/accounts/net-worth", HttpMethod.GET, authorized(user.accessToken()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new BigDecimal(response.getBody().get("totalPEN").toString()))
                .isEqualByComparingTo("1500.50");
        assertThat(new BigDecimal(response.getBody().get("totalUSD").toString()))
                .isEqualByComparingTo("200.00");
        assertThat((List<?>) response.getBody().get("accounts")).hasSize(3);
    }

    @Test
    @DisplayName("net worth never includes another user's accounts")
    void netWorthIsScopedToOwner() {
        TokenPair userA = registerAndLogin("acct-nw-a");
        TokenPair userB = registerAndLogin("acct-nw-b");
        createAccount(userA.accessToken(), "BANK", "PEN", "9999.00");

        ResponseEntity<Map> forB = restTemplate.exchange(
                "/api/v1/accounts/net-worth", HttpMethod.GET, authorized(userB.accessToken()), Map.class);

        assertThat(new BigDecimal(forB.getBody().get("totalPEN").toString()))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat((List<?>) forB.getBody().get("accounts")).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listAccounts(String accessToken) {
        return (List<Map<String, Object>>) (List<?>) restTemplate.exchange(
                "/api/v1/accounts", HttpMethod.GET, authorized(accessToken), List.class).getBody();
    }

    private String createAccount(String accessToken, String type, String currency, String initialBalance) {
        Map<String, Object> body = Map.of(
                "name", "Account " + UUID.randomUUID(), "type", type, "currency", currency,
                "initialBalance", new BigDecimal(initialBalance), "isDefault", false);
        return (String) restTemplate.exchange(
                "/api/v1/accounts", HttpMethod.POST, authorized(body, accessToken), Map.class)
                .getBody().get("id");
    }

    private String createCategory(String accessToken) {
        Map<String, Object> body = Map.of("name", "Food-" + UUID.randomUUID(), "isDefault", false);
        return (String) restTemplate.exchange(
                "/api/v1/categories", HttpMethod.POST, authorized(body, accessToken), Map.class)
                .getBody().get("id");
    }

    private void createExpense(String accessToken, String accountId, String categoryId, String amount) {
        Map<String, Object> body = Map.of(
                "type", "EXPENSE", "amount", new BigDecimal(amount), "currency", "PEN",
                "accountId", accountId, "categoryId", categoryId,
                "transactionDate", LocalDate.now().toString());
        restTemplate.exchange("/api/v1/transactions", HttpMethod.POST, authorized(body, accessToken), Map.class);
    }
}
