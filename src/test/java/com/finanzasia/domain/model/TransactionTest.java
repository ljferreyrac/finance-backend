package com.finanzasia.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    private Transaction buildFullTransaction(List<Tag> tags, BigDecimal exchangeRateApplied, BigDecimal amountLocal) {
        return new Transaction(ID, USER_ID, TransactionType.EXPENSE, new BigDecimal("10.00"), "PEN",
                ACCOUNT_ID, null, null, CATEGORY_ID, "Wong", "desc", "https://receipt",
                LocalDate.now(), NOW, NOW, null, tags, exchangeRateApplied, amountLocal);
    }

    @Nested
    @DisplayName("constructors")
    class Constructors {

        @Test
        @DisplayName("the 16-arg constructor defaults tags, exchangeRateApplied and amountLocal")
        void shortestConstructorDefaults() {
            Transaction tx = new Transaction(ID, USER_ID, TransactionType.EXPENSE, new BigDecimal("10.00"),
                    "PEN", ACCOUNT_ID, null, null, CATEGORY_ID, "Wong", "desc", null,
                    LocalDate.now(), NOW, NOW, null);

            assertThat(tx.getTags()).isEmpty();
            assertThat(tx.getExchangeRateApplied()).isNull();
            assertThat(tx.getAmountLocal()).isNull();
        }

        @Test
        @DisplayName("the tags constructor defaults exchangeRateApplied and amountLocal")
        void tagsConstructorDefaults() {
            Tag tag = new Tag(UUID.randomUUID(), USER_ID, "viaje", "#FFF");
            Transaction tx = new Transaction(ID, USER_ID, TransactionType.EXPENSE, new BigDecimal("10.00"),
                    "PEN", ACCOUNT_ID, null, null, CATEGORY_ID, "Wong", "desc", null,
                    LocalDate.now(), NOW, NOW, null, List.of(tag));

            assertThat(tx.getTags()).containsExactly(tag);
            assertThat(tx.getExchangeRateApplied()).isNull();
            assertThat(tx.getAmountLocal()).isNull();
        }

        @Test
        @DisplayName("the exchangeRateApplied constructor defaults amountLocal")
        void exchangeRateConstructorDefaultsAmountLocal() {
            Transaction tx = new Transaction(ID, USER_ID, TransactionType.EXPENSE, new BigDecimal("10.00"),
                    "PEN", ACCOUNT_ID, null, null, CATEGORY_ID, "Wong", "desc", null,
                    LocalDate.now(), NOW, NOW, null, List.of(), new BigDecimal("3.75"));

            assertThat(tx.getExchangeRateApplied()).isEqualByComparingTo("3.75");
            assertThat(tx.getAmountLocal()).isNull();
        }

        @Test
        @DisplayName("a null tags list is stored as empty, not null")
        void nullTagsBecomeEmptyList() {
            Transaction tx = buildFullTransaction(null, null, null);

            assertThat(tx.getTags()).isEmpty();
        }

        @Test
        @DisplayName("a non-null tags list is copied and preserved")
        void nonNullTagsArePreserved() {
            Tag tag = new Tag(UUID.randomUUID(), USER_ID, "viaje", "#FFF");
            Transaction tx = buildFullTransaction(List.of(tag), new BigDecimal("3.75"), new BigDecimal("37.50"));

            assertThat(tx.getTags()).containsExactly(tag);
            assertThat(tx.getExchangeRateApplied()).isEqualByComparingTo("3.75");
            assertThat(tx.getAmountLocal()).isEqualByComparingTo("37.50");
        }
    }

    @Nested
    @DisplayName("getters")
    class Getters {

        @Test
        @DisplayName("every field passed to the full constructor is retrievable")
        void gettersReturnConstructorValues() {
            UUID fromAccount = UUID.randomUUID();
            UUID toAccount = UUID.randomUUID();
            Instant createdAt = Instant.now().minusSeconds(60);
            Instant updatedAt = Instant.now();
            LocalDate date = LocalDate.of(2026, 3, 15);

            Transaction tx = new Transaction(ID, USER_ID, TransactionType.TRANSFER, new BigDecimal("100.00"),
                    "USD", null, fromAccount, toAccount, null, "Merchant", "Description",
                    "https://receipt.url", date, createdAt, updatedAt, null);

            assertThat(tx.getId()).isEqualTo(ID);
            assertThat(tx.getUserId()).isEqualTo(USER_ID);
            assertThat(tx.getType()).isEqualTo(TransactionType.TRANSFER);
            assertThat(tx.getAmount()).isEqualByComparingTo("100.00");
            assertThat(tx.getCurrency()).isEqualTo("USD");
            assertThat(tx.getAccountId()).isNull();
            assertThat(tx.getFromAccountId()).isEqualTo(fromAccount);
            assertThat(tx.getToAccountId()).isEqualTo(toAccount);
            assertThat(tx.getCategoryId()).isNull();
            assertThat(tx.getMerchant()).isEqualTo("Merchant");
            assertThat(tx.getDescription()).isEqualTo("Description");
            assertThat(tx.getReceiptUrl()).isEqualTo("https://receipt.url");
            assertThat(tx.getTransactionDate()).isEqualTo(date);
            assertThat(tx.getCreatedAt()).isEqualTo(createdAt);
            assertThat(tx.getUpdatedAt()).isEqualTo(updatedAt);
            assertThat(tx.getDeletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("belongsTo")
    class BelongsTo {

        @Test
        @DisplayName("true when the owner id matches")
        void trueWhenOwnerMatches() {
            Transaction tx = buildFullTransaction(List.of(), null, null);
            assertThat(tx.belongsTo(USER_ID)).isTrue();
        }

        @Test
        @DisplayName("false when the owner id differs")
        void falseWhenOwnerDiffers() {
            Transaction tx = buildFullTransaction(List.of(), null, null);
            assertThat(tx.belongsTo(UUID.randomUUID())).isFalse();
        }
    }

    @Nested
    @DisplayName("isDeleted")
    class IsDeleted {

        @Test
        @DisplayName("false when deletedAt is null")
        void falseWhenNotDeleted() {
            Transaction tx = buildFullTransaction(List.of(), null, null);
            assertThat(tx.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("true after softDelete sets deletedAt and updatedAt")
        void trueAfterSoftDelete() {
            Transaction tx = buildFullTransaction(List.of(), null, null);
            Instant deletionTime = Instant.now();

            tx.softDelete(deletionTime);

            assertThat(tx.isDeleted()).isTrue();
            assertThat(tx.getDeletedAt()).isEqualTo(deletionTime);
            assertThat(tx.getUpdatedAt()).isEqualTo(deletionTime);
        }
    }

    @Nested
    @DisplayName("type predicates")
    class TypePredicates {

        @Test
        @DisplayName("isExpense/isIncome/isTransfer for EXPENSE")
        void expenseType() {
            Transaction tx = new Transaction(ID, USER_ID, TransactionType.EXPENSE, new BigDecimal("1"), "PEN",
                    ACCOUNT_ID, null, null, CATEGORY_ID, null, null, null, LocalDate.now(), NOW, NOW, null);
            assertThat(tx.isExpense()).isTrue();
            assertThat(tx.isIncome()).isFalse();
            assertThat(tx.isTransfer()).isFalse();
        }

        @Test
        @DisplayName("isExpense/isIncome/isTransfer for INCOME")
        void incomeType() {
            Transaction tx = new Transaction(ID, USER_ID, TransactionType.INCOME, new BigDecimal("1"), "PEN",
                    ACCOUNT_ID, null, null, null, null, null, null, LocalDate.now(), NOW, NOW, null);
            assertThat(tx.isExpense()).isFalse();
            assertThat(tx.isIncome()).isTrue();
            assertThat(tx.isTransfer()).isFalse();
        }

        @Test
        @DisplayName("isExpense/isIncome/isTransfer for TRANSFER")
        void transferType() {
            Transaction tx = new Transaction(ID, USER_ID, TransactionType.TRANSFER, new BigDecimal("1"), "PEN",
                    null, ACCOUNT_ID, UUID.randomUUID(), null, null, null, null, LocalDate.now(), NOW, NOW, null);
            assertThat(tx.isExpense()).isFalse();
            assertThat(tx.isIncome()).isFalse();
            assertThat(tx.isTransfer()).isTrue();
        }
    }

    @Nested
    @DisplayName("setters")
    class Setters {

        @Test
        @DisplayName("each setter updates the corresponding getter")
        void settersUpdateState() {
            Transaction tx = buildFullTransaction(List.of(), null, null);

            UUID newAccount = UUID.randomUUID();
            UUID newFrom = UUID.randomUUID();
            UUID newTo = UUID.randomUUID();
            UUID newCategory = UUID.randomUUID();
            LocalDate newDate = LocalDate.of(2026, 1, 1);
            Instant newUpdatedAt = Instant.now();

            tx.setAmount(new BigDecimal("55.00"));
            tx.setCurrency("USD");
            tx.setAccountId(newAccount);
            tx.setFromAccountId(newFrom);
            tx.setToAccountId(newTo);
            tx.setCategoryId(newCategory);
            tx.setMerchant("Nuevo Merchant");
            tx.setDescription("Nueva Description");
            tx.setTransactionDate(newDate);
            tx.setUpdatedAt(newUpdatedAt);
            tx.setExchangeRateApplied(new BigDecimal("3.80"));
            tx.setAmountLocal(new BigDecimal("209.00"));

            assertThat(tx.getAmount()).isEqualByComparingTo("55.00");
            assertThat(tx.getCurrency()).isEqualTo("USD");
            assertThat(tx.getAccountId()).isEqualTo(newAccount);
            assertThat(tx.getFromAccountId()).isEqualTo(newFrom);
            assertThat(tx.getToAccountId()).isEqualTo(newTo);
            assertThat(tx.getCategoryId()).isEqualTo(newCategory);
            assertThat(tx.getMerchant()).isEqualTo("Nuevo Merchant");
            assertThat(tx.getDescription()).isEqualTo("Nueva Description");
            assertThat(tx.getTransactionDate()).isEqualTo(newDate);
            assertThat(tx.getUpdatedAt()).isEqualTo(newUpdatedAt);
            assertThat(tx.getExchangeRateApplied()).isEqualByComparingTo("3.80");
            assertThat(tx.getAmountLocal()).isEqualByComparingTo("209.00");
        }

        @Test
        @DisplayName("setTags with a non-null list replaces the tag list")
        void setTagsWithNonNullList() {
            Transaction tx = buildFullTransaction(List.of(), null, null);
            Tag tag = new Tag(UUID.randomUUID(), USER_ID, "viaje", "#FFF");

            tx.setTags(List.of(tag));

            assertThat(tx.getTags()).containsExactly(tag);
        }

        @Test
        @DisplayName("setTags with null clears the tag list to empty, not null")
        void setTagsWithNullClearsToEmpty() {
            Tag tag = new Tag(UUID.randomUUID(), USER_ID, "viaje", "#FFF");
            Transaction tx = buildFullTransaction(List.of(tag), null, null);

            tx.setTags(null);

            assertThat(tx.getTags()).isEmpty();
        }
    }
}
