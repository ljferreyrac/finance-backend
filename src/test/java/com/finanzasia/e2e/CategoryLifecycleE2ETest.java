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
 * The {@code /api/v1/categories} endpoints Phase 7 did not reach: update, delete and set-default.
 *
 * <p>Two things here only a real database can prove. {@code categories} has
 * {@code CREATE UNIQUE INDEX uidx_categories_one_default_per_user ... WHERE is_default = TRUE},
 * so set-default must clear the old default first. And deleting a category that still has
 * transactions runs the native {@code reassignExpenses} UPDATE - the query Phase 6 verified in
 * isolation, exercised here through the full stack.
 */
class CategoryLifecycleE2ETest extends AbstractE2ETest {

    @Test
    @DisplayName("update changes name, colour and position")
    void updateChangesFields() {
        TokenPair user = registerAndLogin("cat-update");
        String categoryId = createCategory(user.accessToken());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/categories/" + categoryId, HttpMethod.PUT,
                authorized(Map.of("name", "Renamed", "color", "#3498DB", "position", 3), user.accessToken()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("name")).isEqualTo("Renamed");
        assertThat(response.getBody().get("color")).isEqualTo("#3498DB");
    }

    @Test
    @DisplayName("renaming a category to a name the user already has returns 409")
    void duplicateNameReturns409() {
        TokenPair user = registerAndLogin("cat-dup");
        createNamedCategory(user.accessToken(), "Existing");
        String other = createNamedCategory(user.accessToken(), "Other");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/categories/" + other, HttpMethod.PUT,
                authorized(Map.of("name", "Existing"), user.accessToken()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("setting a default clears the previous one, satisfying the one-default-per-user index")
    void settingDefaultClearsThePreviousDefault() {
        TokenPair user = registerAndLogin("cat-default");
        String first = createCategory(user.accessToken());
        String second = createCategory(user.accessToken());

        restTemplate.exchange("/api/v1/categories/" + first + "/default", HttpMethod.PATCH,
                authorized(null, user.accessToken()), Map.class);
        ResponseEntity<Map> setSecond = restTemplate.exchange(
                "/api/v1/categories/" + second + "/default", HttpMethod.PATCH,
                authorized(null, user.accessToken()), Map.class);

        assertThat(setSecond.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listCategories(user.accessToken()))
                .filteredOn(c -> Boolean.TRUE.equals(c.get("isDefault")))
                .extracting(c -> c.get("id"))
                .containsExactly(second);
    }

    @Test
    @DisplayName("deleting the user's only category returns 409")
    void deletingLastCategoryReturns409() {
        TokenPair user = registerAndLogin("cat-last");
        String only = createCategory(user.accessToken());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/categories/" + only, HttpMethod.DELETE, authorized(user.accessToken()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("title")).isEqualTo("Last Category");
    }

    @Test
    @DisplayName("deleting an unused category succeeds when it isn't the last one")
    void deletingUnusedCategorySucceeds() {
        TokenPair user = registerAndLogin("cat-delete");
        String keep = createCategory(user.accessToken());
        String remove = createCategory(user.accessToken());

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/categories/" + remove, HttpMethod.DELETE, authorized(user.accessToken()), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(listCategories(user.accessToken()))
                .extracting(c -> c.get("id"))
                .containsExactly(keep);
    }

    @Test
    @DisplayName("deleting a category with transactions and no reassignTo returns 409 with the count")
    void deletingCategoryInUseWithoutReassignReturns409() {
        TokenPair user = registerAndLogin("cat-in-use");
        String inUse = createCategory(user.accessToken());
        createCategory(user.accessToken());
        String accountId = createAccount(user.accessToken());
        createExpense(user.accessToken(), accountId, inUse, "15.00");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/categories/" + inUse, HttpMethod.DELETE, authorized(user.accessToken()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("title")).isEqualTo("Category In Use");
        assertThat(((Number) response.getBody().get("expenseCount")).longValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("deleting with reassignTo migrates the transactions and then removes the category")
    void deletingWithReassignMigratesTransactions() {
        TokenPair user = registerAndLogin("cat-reassign");
        String source = createCategory(user.accessToken());
        String target = createCategory(user.accessToken());
        String accountId = createAccount(user.accessToken());
        String transactionId = createExpense(user.accessToken(), accountId, source, "42.00");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/categories/" + source + "?reassignTo=" + target, HttpMethod.DELETE,
                authorized(user.accessToken()), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // The transaction survives and now points at the target category (native reassignExpenses).
        ResponseEntity<Map> transaction = restTemplate.exchange(
                "/api/v1/transactions/" + transactionId, HttpMethod.GET,
                authorized(user.accessToken()), Map.class);
        assertThat(transaction.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) transaction.getBody().get("category")).get("id")).isEqualTo(target);

        assertThat(listCategories(user.accessToken()))
                .extracting(c -> c.get("id"))
                .containsExactly(target);
    }

    @Test
    @DisplayName("deleting another user's category returns 404")
    void deletingAnotherUsersCategoryReturns404() {
        TokenPair userA = registerAndLogin("cat-del-a");
        TokenPair userB = registerAndLogin("cat-del-b");
        String categoryId = createCategory(userA.accessToken());
        createCategory(userA.accessToken());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/categories/" + categoryId, HttpMethod.DELETE,
                authorized(userB.accessToken()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listCategories(String accessToken) {
        return (List<Map<String, Object>>) (List<?>) restTemplate.exchange(
                "/api/v1/categories", HttpMethod.GET, authorized(accessToken), List.class).getBody();
    }

    private String createCategory(String accessToken) {
        return createNamedCategory(accessToken, "Cat-" + UUID.randomUUID());
    }

    private String createNamedCategory(String accessToken, String name) {
        Map<String, Object> body = Map.of("name", name, "isDefault", false);
        return (String) restTemplate.exchange(
                "/api/v1/categories", HttpMethod.POST, authorized(body, accessToken), Map.class)
                .getBody().get("id");
    }

    private String createAccount(String accessToken) {
        Map<String, Object> body = Map.of(
                "name", "Account " + UUID.randomUUID(), "type", "BANK", "currency", "PEN",
                "initialBalance", new BigDecimal("500.00"), "isDefault", false);
        return (String) restTemplate.exchange(
                "/api/v1/accounts", HttpMethod.POST, authorized(body, accessToken), Map.class)
                .getBody().get("id");
    }

    private String createExpense(String accessToken, String accountId, String categoryId, String amount) {
        Map<String, Object> body = Map.of(
                "type", "EXPENSE", "amount", new BigDecimal(amount), "currency", "PEN",
                "accountId", accountId, "categoryId", categoryId,
                "transactionDate", LocalDate.now().toString());
        return (String) restTemplate.exchange(
                "/api/v1/transactions", HttpMethod.POST, authorized(body, accessToken), Map.class)
                .getBody().get("id");
    }
}
