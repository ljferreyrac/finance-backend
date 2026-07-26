package com.finanzasia.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard on what the issued tokens actually carry, raised by the security review of
 * the Phase 7 PR.
 *
 * <p>A JWT payload is only base64url - anyone holding a token can read every claim in it without
 * the signing key. So a claim is not a place to put anything the bearer shouldn't see. Today
 * {@code JwtService} embeds only {@code sub} and {@code email} (plus the standard {@code iat} /
 * {@code exp} / {@code jti}), which is fine - these assertions exist so that if someone later
 * adds, say, a RUC, an account balance or a password hash to the token "for convenience", a test
 * fails instead of the leak shipping silently.
 *
 * <p>Deliberately a closed set, not a blacklist of forbidden names: a blacklist only catches the
 * leaks somebody already thought of.
 */
class JwtClaimsE2ETest extends AbstractE2ETest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("the access token carries exactly sub, email, iat and exp - nothing else")
    void accessTokenClaimSetIsClosed() {
        TokenPair user = registerAndLogin("jwt-access");

        JsonNode claims = decodePayload(user.accessToken());

        assertThat(fieldNames(claims)).containsExactlyInAnyOrder("sub", "email", "iat", "exp");
    }

    @Test
    @DisplayName("the refresh token carries exactly sub, jti, iat and exp - and never the email")
    void refreshTokenClaimSetIsClosed() {
        TokenPair user = registerAndLogin("jwt-refresh");

        JsonNode claims = decodePayload(user.refreshToken());

        assertThat(fieldNames(claims)).containsExactlyInAnyOrder("sub", "jti", "iat", "exp");
        // The refresh token is the longer-lived of the two (7 days vs 15 minutes), so it carries
        // strictly less: an opaque jti for revocation, and no personally identifying claim.
        assertThat(claims.has("email")).isFalse();
    }

    @Test
    @DisplayName("no token claim contains the user's password or its hash")
    void tokensNeverCarryCredentials() {
        TokenPair user = registerAndLogin("jwt-creds");

        String accessPayload = decodePayload(user.accessToken()).toString();
        String refreshPayload = decodePayload(user.refreshToken()).toString();

        assertThat(accessPayload).doesNotContain("correct-horse-battery-staple").doesNotContain("$2a$");
        assertThat(refreshPayload).doesNotContain("correct-horse-battery-staple").doesNotContain("$2a$");
    }

    @Test
    @DisplayName("the access token's subject is a UUID and its email matches the registered account")
    void accessTokenSubjectAndEmailAreCorrect() {
        String email = "jwt-subject-" + java.util.UUID.randomUUID() + "@example.com";
        var response = restTemplate.postForEntity(
                "/api/v1/auth/register", jsonBody(RegisterPayload.of(email)), java.util.Map.class);
        String accessToken = (String) response.getBody().get("accessToken");

        JsonNode claims = decodePayload(accessToken);

        assertThat(claims.get("email").asText()).isEqualTo(email);
        assertThat(java.util.UUID.fromString(claims.get("sub").asText())).isNotNull();
    }

    /** Reads the JWT payload without verifying the signature - exactly what any bearer can do. */
    private JsonNode decodePayload(String jwt) {
        String[] parts = jwt.split("\\.");
        assertThat(parts).as("a JWT has three dot-separated sections").hasSize(3);
        byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
        try {
            return MAPPER.readTree(new String(decoded, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("JWT payload was not valid JSON", ex);
        }
    }

    private List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
