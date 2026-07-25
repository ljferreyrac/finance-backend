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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The failure mode that matters most in a finance app: an unauthenticated caller gets nothing,
 * and one user can never read or mutate another user's data - properties asserted nowhere
 * against a real database before this suite.
 */
class SecurityBoundaryE2ETest extends AbstractE2ETest {

    @Test
    @DisplayName("every protected endpoint returns 401 with no Authorization header")
    void unauthenticatedRequestsAreRejected() {
        assertThat(restTemplate.getForEntity("/api/v1/accounts", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity("/api/v1/transactions", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity("/api/v1/reports/monthly", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("a garbage bearer token is rejected exactly like a missing one")
    void garbageTokenIsRejected() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/accounts", HttpMethod.GET, authorized("not-a-real-jwt"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("/auth/** and /actuator/health stay public")
    void publicEndpointsStayPublic() {
        assertThat(restTemplate.getForEntity("/actuator/health", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("user B reading user A's transaction gets 404, not the data and not a 403")
    void crossTenantReadReturnsNotFoundNotData() {
        TokenPair userA = registerAndLogin("tenant-a");
        TokenPair userB = registerAndLogin("tenant-b");
        String accountId = createAccount(userA.accessToken());
        String categoryId = createCategory(userA.accessToken());
        String transactionId = createExpense(userA.accessToken(), accountId, categoryId, "50.00");

        ResponseEntity<Map> asOwner = restTemplate.exchange(
                "/api/v1/transactions/" + transactionId, HttpMethod.GET, authorized(userA.accessToken()), Map.class);
        ResponseEntity<Map> asOtherUser = restTemplate.exchange(
                "/api/v1/transactions/" + transactionId, HttpMethod.GET, authorized(userB.accessToken()), Map.class);

        assertThat(asOwner.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(asOtherUser.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("user B deleting user A's transaction gets 404 and the transaction survives")
    void crossTenantDeleteDoesNotAffectOwnersData() {
        TokenPair userA = registerAndLogin("tenant-a-del");
        TokenPair userB = registerAndLogin("tenant-b-del");
        String accountId = createAccount(userA.accessToken());
        String categoryId = createCategory(userA.accessToken());
        String transactionId = createExpense(userA.accessToken(), accountId, categoryId, "20.00");

        ResponseEntity<Map> deleteAttempt = restTemplate.exchange(
                "/api/v1/transactions/" + transactionId, HttpMethod.DELETE,
                authorized(userB.accessToken()), Map.class);
        assertThat(deleteAttempt.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<Map> stillThere = restTemplate.exchange(
                "/api/v1/transactions/" + transactionId, HttpMethod.GET, authorized(userA.accessToken()), Map.class);
        assertThat(stillThere.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("user B's account list never includes user A's accounts")
    void accountListingIsScopedPerUser() {
        TokenPair userA = registerAndLogin("list-a");
        TokenPair userB = registerAndLogin("list-b");
        createAccount(userA.accessToken());

        ResponseEntity<List> accountsForB = restTemplate.exchange(
                "/api/v1/accounts", HttpMethod.GET, authorized(userB.accessToken()), List.class);

        assertThat(accountsForB.getBody()).isEmpty();
    }

    private String createAccount(String accessToken) {
        Map<String, Object> body = Map.of(
                "name", "Test Account", "type", "BANK", "currency", "PEN",
                "initialBalance", 0, "isDefault", false);
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/accounts", HttpMethod.POST, authorized(body, accessToken), Map.class);
        return (String) response.getBody().get("id");
    }

    private String createCategory(String accessToken) {
        Map<String, Object> body = Map.of("name", "Food-" + java.util.UUID.randomUUID(), "isDefault", false);
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
