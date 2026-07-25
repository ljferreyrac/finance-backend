package com.finanzasia.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private Account buildAccount() {
        Instant now = Instant.now();
        return new Account(ID, USER_ID, "BCP Soles", AccountType.BANK, "BCP", "PEN",
                new BigDecimal("1000.00"), null, null, null, "#0066CC", false, true, null, now, now);
    }

    @Test
    @DisplayName("getters return the values passed to the constructor")
    void gettersReturnConstructorValues() {
        Instant createdAt = Instant.now().minusSeconds(60);
        Instant updatedAt = Instant.now();
        UUID linkedAccountId = UUID.randomUUID();

        Account account = new Account(ID, USER_ID, "Yape", AccountType.WALLET, null, "PEN",
                new BigDecimal("50.00"), new BigDecimal("500.00"), 6, 26, "#FF0000",
                true, true, linkedAccountId, createdAt, updatedAt);

        assertThat(account.getId()).isEqualTo(ID);
        assertThat(account.getUserId()).isEqualTo(USER_ID);
        assertThat(account.getName()).isEqualTo("Yape");
        assertThat(account.getType()).isEqualTo(AccountType.WALLET);
        assertThat(account.getBank()).isNull();
        assertThat(account.getCurrency()).isEqualTo("PEN");
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("50.00");
        assertThat(account.getCreditLimit()).isEqualByComparingTo("500.00");
        assertThat(account.getClosingDay()).isEqualTo(6);
        assertThat(account.getDueDay()).isEqualTo(26);
        assertThat(account.getColor()).isEqualTo("#FF0000");
        assertThat(account.isDefault()).isTrue();
        assertThat(account.isActive()).isTrue();
        assertThat(account.getLinkedAccountId()).isEqualTo(linkedAccountId);
        assertThat(account.getCreatedAt()).isEqualTo(createdAt);
        assertThat(account.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Nested
    @DisplayName("belongsTo")
    class BelongsTo {

        @Test
        @DisplayName("true when the owner id matches")
        void trueWhenOwnerMatches() {
            assertThat(buildAccount().belongsTo(USER_ID)).isTrue();
        }

        @Test
        @DisplayName("false when the owner id differs")
        void falseWhenOwnerDiffers() {
            assertThat(buildAccount().belongsTo(UUID.randomUUID())).isFalse();
        }
    }

    @Test
    @DisplayName("markAsDefault sets isDefault true and bumps updatedAt")
    void markAsDefaultSetsFlagAndTimestamp() {
        Account account = buildAccount();
        Instant now = Instant.now();

        account.markAsDefault(now);

        assertThat(account.isDefault()).isTrue();
        assertThat(account.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("clearDefault sets isDefault false and bumps updatedAt")
    void clearDefaultClearsFlagAndBumpsTimestamp() {
        Account account = buildAccount();
        Instant now = Instant.now();
        account.markAsDefault(now);

        Instant later = now.plusSeconds(5);
        account.clearDefault(later);

        assertThat(account.isDefault()).isFalse();
        assertThat(account.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    @DisplayName("debit subtracts from the current balance")
    void debitSubtractsAmount() {
        Account account = buildAccount();

        account.debit(new BigDecimal("100.00"));

        assertThat(account.getCurrentBalance()).isEqualByComparingTo("900.00");
    }

    @Test
    @DisplayName("credit adds to the current balance")
    void creditAddsAmount() {
        Account account = buildAccount();

        account.credit(new BigDecimal("100.00"));

        assertThat(account.getCurrentBalance()).isEqualByComparingTo("1100.00");
    }

    @Test
    @DisplayName("setters update their corresponding getters")
    void settersUpdateState() {
        Account account = buildAccount();
        UUID newLinkedId = UUID.randomUUID();
        Instant newUpdatedAt = Instant.now();

        account.setName("Nueva Cuenta");
        account.setBank("Interbank");
        account.setLinkedAccountId(newLinkedId);
        account.setCreditLimit(new BigDecimal("2000.00"));
        account.setClosingDay(10);
        account.setDueDay(30);
        account.setColor("#00FF00");
        account.setActive(false);
        account.setUpdatedAt(newUpdatedAt);

        assertThat(account.getName()).isEqualTo("Nueva Cuenta");
        assertThat(account.getBank()).isEqualTo("Interbank");
        assertThat(account.getLinkedAccountId()).isEqualTo(newLinkedId);
        assertThat(account.getCreditLimit()).isEqualByComparingTo("2000.00");
        assertThat(account.getClosingDay()).isEqualTo(10);
        assertThat(account.getDueDay()).isEqualTo(30);
        assertThat(account.getColor()).isEqualTo("#00FF00");
        assertThat(account.isActive()).isFalse();
        assertThat(account.getUpdatedAt()).isEqualTo(newUpdatedAt);
    }
}
