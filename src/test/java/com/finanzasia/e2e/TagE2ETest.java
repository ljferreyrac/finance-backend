package com.finanzasia.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code /api/v1/tags} end to end. The interesting property here is name normalisation:
 * {@code TagService} lowercases and strips names, and the {@code tags} table has a
 * {@code CHECK (name = lower(trim(name)))} constraint - so if the service ever stopped
 * normalising, the database would reject the insert outright. Only a real Postgres shows that.
 */
class TagE2ETest extends AbstractE2ETest {

    @Test
    @DisplayName("create returns 201 and normalises the name to lowercase, satisfying the DB CHECK")
    void createNormalisesName() {
        TokenPair user = registerAndLogin("tag-create");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/tags", HttpMethod.POST,
                authorized(Map.of("name", "  Reimbursable  ", "color", "#FF5733"), user.accessToken()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("name")).isEqualTo("reimbursable");
        assertThat(response.getBody().get("color")).isEqualTo("#FF5733");
    }

    @Test
    @DisplayName("creating a tag whose normalised name already exists returns 409")
    void duplicateNameReturns409() {
        TokenPair user = registerAndLogin("tag-dup");
        createTag(user.accessToken(), "travel");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/tags", HttpMethod.POST,
                authorized(Map.of("name", "TRAVEL"), user.accessToken()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("title")).isEqualTo("Duplicate Tag Name");
    }

    @Test
    @DisplayName("two different users may each hold a tag with the same name")
    void sameNameAllowedAcrossUsers() {
        TokenPair userA = registerAndLogin("tag-user-a");
        TokenPair userB = registerAndLogin("tag-user-b");
        createTag(userA.accessToken(), "shared-name");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/tags", HttpMethod.POST,
                authorized(Map.of("name", "shared-name"), userB.accessToken()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("list returns only the caller's tags")
    void listIsScopedToOwner() {
        TokenPair userA = registerAndLogin("tag-list-a");
        TokenPair userB = registerAndLogin("tag-list-b");
        createTag(userA.accessToken(), "mine");

        ResponseEntity<List> forB = restTemplate.exchange(
                "/api/v1/tags", HttpMethod.GET, authorized(userB.accessToken()), List.class);

        assertThat(forB.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(forB.getBody()).isEmpty();
    }

    @Test
    @DisplayName("update renames a tag, normalising the new name too")
    void updateRenamesAndNormalises() {
        TokenPair user = registerAndLogin("tag-update");
        String tagId = createTag(user.accessToken(), "before");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/tags/" + tagId, HttpMethod.PUT,
                authorized(Map.of("name", "AFTER"), user.accessToken()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("name")).isEqualTo("after");
    }

    @Test
    @DisplayName("updating another user's tag returns 404 and leaves it untouched")
    void updatingAnotherUsersTagReturns404() {
        TokenPair userA = registerAndLogin("tag-upd-a");
        TokenPair userB = registerAndLogin("tag-upd-b");
        String tagId = createTag(userA.accessToken(), "owned-by-a");

        ResponseEntity<Map> attempt = restTemplate.exchange(
                "/api/v1/tags/" + tagId, HttpMethod.PUT,
                authorized(Map.of("name", "hijacked"), userB.accessToken()), Map.class);
        assertThat(attempt.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<List> ownersTags = restTemplate.exchange(
                "/api/v1/tags", HttpMethod.GET, authorized(userA.accessToken()), List.class);
        assertThat(ownersTags.getBody()).hasSize(1);
        assertThat(((Map<?, ?>) ownersTags.getBody().get(0)).get("name")).isEqualTo("owned-by-a");
    }

    @Test
    @DisplayName("delete removes the tag; deleting another user's tag returns 404")
    void deleteRemovesTagAndIsScopedToOwner() {
        TokenPair userA = registerAndLogin("tag-del-a");
        TokenPair userB = registerAndLogin("tag-del-b");
        String tagId = createTag(userA.accessToken(), "to-delete");

        ResponseEntity<Map> byOtherUser = restTemplate.exchange(
                "/api/v1/tags/" + tagId, HttpMethod.DELETE, authorized(userB.accessToken()), Map.class);
        assertThat(byOtherUser.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<Void> byOwner = restTemplate.exchange(
                "/api/v1/tags/" + tagId, HttpMethod.DELETE, authorized(userA.accessToken()), Void.class);
        assertThat(byOwner.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<List> remaining = restTemplate.exchange(
                "/api/v1/tags", HttpMethod.GET, authorized(userA.accessToken()), List.class);
        assertThat(remaining.getBody()).isEmpty();
    }

    @Test
    @DisplayName("deleting a tag detaches it from transactions instead of deleting them")
    void deletingTagLeavesTransactionsIntact() {
        TokenPair user = registerAndLogin("tag-detach");
        String tagId = createTag(user.accessToken(), "detach-me");
        String accountId = createAccount(user.accessToken());
        String categoryId = createCategory(user.accessToken());
        String transactionId = createExpenseWithTag(user.accessToken(), accountId, categoryId, tagId);

        restTemplate.exchange(
                "/api/v1/tags/" + tagId, HttpMethod.DELETE, authorized(user.accessToken()), Void.class);

        // ON DELETE CASCADE on transaction_tags must remove only the join row, not the transaction.
        ResponseEntity<Map> transaction = restTemplate.exchange(
                "/api/v1/transactions/" + transactionId, HttpMethod.GET,
                authorized(user.accessToken()), Map.class);
        assertThat(transaction.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) transaction.getBody().get("tags")).isEmpty();
    }

    @Test
    @DisplayName("a blank name is rejected with 400 before reaching the database")
    void blankNameReturns400() {
        TokenPair user = registerAndLogin("tag-blank");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/tags", HttpMethod.POST,
                authorized(Map.of("name", "   "), user.accessToken()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private String createTag(String accessToken, String name) {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/tags", HttpMethod.POST,
                authorized(Map.of("name", name), accessToken), Map.class);
        return (String) response.getBody().get("id");
    }

    private String createAccount(String accessToken) {
        Map<String, Object> body = Map.of(
                "name", "Tag Account " + UUID.randomUUID(), "type", "BANK", "currency", "PEN",
                "initialBalance", 0, "isDefault", false);
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

    private String createExpenseWithTag(String accessToken, String accountId, String categoryId, String tagId) {
        Map<String, Object> body = Map.of(
                "type", "EXPENSE", "amount", new java.math.BigDecimal("25.00"), "currency", "PEN",
                "accountId", accountId, "categoryId", categoryId,
                "transactionDate", java.time.LocalDate.now().toString(),
                "tagIds", List.of(tagId));
        return (String) restTemplate.exchange(
                "/api/v1/transactions", HttpMethod.POST, authorized(body, accessToken), Map.class)
                .getBody().get("id");
    }
}
