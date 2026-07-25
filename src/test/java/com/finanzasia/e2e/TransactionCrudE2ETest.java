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
 * A full create/update/delete cycle for a transaction, through real HTTP, asserting the
 * side effect that matters most: the account's {@code current_balance} actually moves, and
 * moves back when the transaction is edited or removed.
 */
class TransactionCrudE2ETest extends AbstractE2ETest {

    @Test
    @DisplayName("creating an expense debits the account balance by exactly the expense amount")
    void creatingExpenseDebitsAccountBalance() {
        TokenPair user = registerAndLogin("crud-create");
        String accountId = createAccount(user.accessToken(), "0");
        String categoryId = createCategory(user.accessToken());

        createExpense(user.accessToken(), accountId, categoryId, "150.00");

        assertThat(currentBalance(user.accessToken(), accountId)).isEqualByComparingTo("-150.00");
    }

    @Test
    @DisplayName("updating an expense's amount reverses the old effect and applies the new one")
    void updatingExpenseAmountAdjustsBalanceByTheDifference() {
        TokenPair user = registerAndLogin("crud-update");
        String accountId = createAccount(user.accessToken(), "1000.00");
        String categoryId = createCategory(user.accessToken());
        String transactionId = createExpense(user.accessToken(), accountId, categoryId, "100.00");
        assertThat(currentBalance(user.accessToken(), accountId)).isEqualByComparingTo("900.00");

        Map<String, Object> update = Map.of("amount", new BigDecimal("300.00"));
        ResponseEntity<Map> updateResponse = restTemplate.exchange(
                "/api/v1/transactions/" + transactionId, HttpMethod.PUT,
                authorized(update, user.accessToken()), Map.class);

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(currentBalance(user.accessToken(), accountId)).isEqualByComparingTo("700.00");
    }

    @Test
    @DisplayName("deleting a transaction reverses its balance effect and soft-deletes it")
    void deletingTransactionReversesBalanceEffect() {
        TokenPair user = registerAndLogin("crud-delete");
        String accountId = createAccount(user.accessToken(), "500.00");
        String categoryId = createCategory(user.accessToken());
        String transactionId = createExpense(user.accessToken(), accountId, categoryId, "50.00");
        assertThat(currentBalance(user.accessToken(), accountId)).isEqualByComparingTo("450.00");

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/v1/transactions/" + transactionId, HttpMethod.DELETE,
                authorized(user.accessToken()), Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(currentBalance(user.accessToken(), accountId)).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("a deleted transaction disappears from the transaction list")
    void deletedTransactionDisappearsFromList() {
        TokenPair user = registerAndLogin("crud-list");
        String accountId = createAccount(user.accessToken(), "0");
        String categoryId = createCategory(user.accessToken());
        String keptId = createExpense(user.accessToken(), accountId, categoryId, "10.00");
        String deletedId = createExpense(user.accessToken(), accountId, categoryId, "20.00");

        restTemplate.exchange(
                "/api/v1/transactions/" + deletedId, HttpMethod.DELETE, authorized(user.accessToken()), Void.class);

        ResponseEntity<Map> page = restTemplate.exchange(
                "/api/v1/transactions", HttpMethod.GET, authorized(user.accessToken()), Map.class);
        List<Map<String, Object>> items = (List<Map<String, Object>>) page.getBody().get("items");

        assertThat(items).extracting(item -> item.get("id"))
                .containsExactly(keptId)
                .doesNotContain(deletedId);
    }

    @Test
    @DisplayName("getting a soft-deleted transaction by id returns 404")
    void gettingDeletedTransactionReturnsNotFound() {
        TokenPair user = registerAndLogin("crud-get-deleted");
        String accountId = createAccount(user.accessToken(), "0");
        String categoryId = createCategory(user.accessToken());
        String transactionId = createExpense(user.accessToken(), accountId, categoryId, "10.00");

        restTemplate.exchange(
                "/api/v1/transactions/" + transactionId, HttpMethod.DELETE,
                authorized(user.accessToken()), Void.class);

        ResponseEntity<Map> getResponse = restTemplate.exchange(
                "/api/v1/transactions/" + transactionId, HttpMethod.GET,
                authorized(user.accessToken()), Map.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @SuppressWarnings("unchecked")
    private BigDecimal currentBalance(String accessToken, String accountId) {
        ResponseEntity<List> response = restTemplate.exchange(
                "/api/v1/accounts", HttpMethod.GET, authorized(accessToken), List.class);
        List<Map<String, Object>> accounts = (List<Map<String, Object>>) (List<?>) response.getBody();
        return accounts.stream()
                .filter(a -> accountId.equals(a.get("id")))
                .map(a -> new BigDecimal(a.get("currentBalance").toString()))
                .findFirst()
                .orElseThrow();
    }

    private String createAccount(String accessToken, String initialBalance) {
        Map<String, Object> body = Map.of(
                "name", "Test Account " + UUID.randomUUID(), "type", "BANK", "currency", "PEN",
                "initialBalance", new BigDecimal(initialBalance), "isDefault", false);
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

    private String createExpense(String accessToken, String accountId, String categoryId, String amount) {
        Map<String, Object> body = Map.of(
                "type", "EXPENSE", "amount", new BigDecimal(amount), "currency", "PEN",
                "accountId", accountId, "categoryId", categoryId,
                "transactionDate", LocalDate.now().toString());
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/transactions", HttpMethod.POST, authorized(body, accessToken), Map.class);
        return (String) response.getBody().get("id");
    }
}
