package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.Account;
import com.finanzasia.domain.model.AccountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountEntityTest {

    @Test
    @DisplayName("fromDomain followed by toDomain round-trips every field")
    void roundTripsAllFields() {
        Instant now = Instant.now();
        UUID linkedAccountId = UUID.randomUUID();
        Account account = new Account(UUID.randomUUID(), UUID.randomUUID(), "Yape", AccountType.WALLET,
                "BCP", "PEN", new BigDecimal("100.00"), new BigDecimal("500.00"), 6, 26,
                "#0066CC", true, true, linkedAccountId, now, now);

        AccountEntity entity = AccountEntity.fromDomain(account);
        Account roundTripped = entity.toDomain();

        assertThat(roundTripped.getId()).isEqualTo(account.getId());
        assertThat(roundTripped.getUserId()).isEqualTo(account.getUserId());
        assertThat(roundTripped.getName()).isEqualTo(account.getName());
        assertThat(roundTripped.getType()).isEqualTo(account.getType());
        assertThat(roundTripped.getBank()).isEqualTo(account.getBank());
        assertThat(roundTripped.getCurrency()).isEqualTo(account.getCurrency());
        assertThat(roundTripped.getCurrentBalance()).isEqualByComparingTo(account.getCurrentBalance());
        assertThat(roundTripped.getCreditLimit()).isEqualByComparingTo(account.getCreditLimit());
        assertThat(roundTripped.getClosingDay()).isEqualTo(account.getClosingDay());
        assertThat(roundTripped.getDueDay()).isEqualTo(account.getDueDay());
        assertThat(roundTripped.getColor()).isEqualTo(account.getColor());
        assertThat(roundTripped.isDefault()).isEqualTo(account.isDefault());
        assertThat(roundTripped.isActive()).isEqualTo(account.isActive());
        assertThat(roundTripped.getLinkedAccountId()).isEqualTo(linkedAccountId);
        assertThat(roundTripped.getCreatedAt()).isEqualTo(account.getCreatedAt());
        assertThat(roundTripped.getUpdatedAt()).isEqualTo(account.getUpdatedAt());
    }

    @Test
    @DisplayName("nullable fields (bank, creditLimit, closingDay, dueDay, linkedAccountId) survive as null")
    void nullableFieldsSurviveAsNull() {
        Instant now = Instant.now();
        Account account = new Account(UUID.randomUUID(), UUID.randomUUID(), "Efectivo", AccountType.CASH,
                null, "PEN", BigDecimal.ZERO, null, null, null, "#000000", false, true, null, now, now);

        AccountEntity entity = AccountEntity.fromDomain(account);
        Account roundTripped = entity.toDomain();

        assertThat(roundTripped.getBank()).isNull();
        assertThat(roundTripped.getCreditLimit()).isNull();
        assertThat(roundTripped.getClosingDay()).isNull();
        assertThat(roundTripped.getDueDay()).isNull();
        assertThat(roundTripped.getLinkedAccountId()).isNull();
    }

    @Test
    @DisplayName("every getter reflects the value passed to its setter")
    void gettersReflectSetterValues() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID linkedAccountId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(60);
        Instant updatedAt = Instant.now();

        AccountEntity entity = new AccountEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setName("BCP Soles");
        entity.setType(AccountType.BANK);
        entity.setBank("BCP");
        entity.setCurrency("PEN");
        entity.setCurrentBalance(new BigDecimal("1000.00"));
        entity.setCreditLimit(new BigDecimal("500.00"));
        entity.setClosingDay(6);
        entity.setDueDay(26);
        entity.setColor("#0066CC");
        entity.setLinkedAccountId(linkedAccountId);
        entity.setDefault(true);
        entity.setActive(true);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getUserId()).isEqualTo(userId);
        assertThat(entity.getName()).isEqualTo("BCP Soles");
        assertThat(entity.getType()).isEqualTo(AccountType.BANK);
        assertThat(entity.getBank()).isEqualTo("BCP");
        assertThat(entity.getCurrency()).isEqualTo("PEN");
        assertThat(entity.getCurrentBalance()).isEqualByComparingTo("1000.00");
        assertThat(entity.getCreditLimit()).isEqualByComparingTo("500.00");
        assertThat(entity.getClosingDay()).isEqualTo(6);
        assertThat(entity.getDueDay()).isEqualTo(26);
        assertThat(entity.getColor()).isEqualTo("#0066CC");
        assertThat(entity.getLinkedAccountId()).isEqualTo(linkedAccountId);
        assertThat(entity.isDefault()).isTrue();
        assertThat(entity.isActive()).isTrue();
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
