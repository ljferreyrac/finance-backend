package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.Tag;
import com.finanzasia.domain.model.Transaction;
import com.finanzasia.domain.model.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionEntityTest {

    private Transaction buildTransaction(List<Tag> tags) {
        Instant now = Instant.now();
        return new Transaction(UUID.randomUUID(), UUID.randomUUID(), TransactionType.EXPENSE,
                new BigDecimal("50.00"), "PEN", UUID.randomUUID(), null, null, UUID.randomUUID(),
                "Wong", "groceries", "https://receipt", LocalDate.now(), now, now, null,
                tags, new BigDecimal("3.75"), new BigDecimal("187.50"));
    }

    @Test
    @DisplayName("fromDomain followed by toDomain round-trips every field except tags")
    void roundTripsAllFieldsExceptTags() {
        Transaction transaction = buildTransaction(List.of());

        TransactionEntity entity = TransactionEntity.fromDomain(transaction);
        Transaction roundTripped = entity.toDomain();

        assertThat(roundTripped.getId()).isEqualTo(transaction.getId());
        assertThat(roundTripped.getUserId()).isEqualTo(transaction.getUserId());
        assertThat(roundTripped.getType()).isEqualTo(transaction.getType());
        assertThat(roundTripped.getAmount()).isEqualByComparingTo(transaction.getAmount());
        assertThat(roundTripped.getCurrency()).isEqualTo(transaction.getCurrency());
        assertThat(roundTripped.getAccountId()).isEqualTo(transaction.getAccountId());
        assertThat(roundTripped.getCategoryId()).isEqualTo(transaction.getCategoryId());
        assertThat(roundTripped.getMerchant()).isEqualTo(transaction.getMerchant());
        assertThat(roundTripped.getDescription()).isEqualTo(transaction.getDescription());
        assertThat(roundTripped.getReceiptUrl()).isEqualTo(transaction.getReceiptUrl());
        assertThat(roundTripped.getTransactionDate()).isEqualTo(transaction.getTransactionDate());
        assertThat(roundTripped.getCreatedAt()).isEqualTo(transaction.getCreatedAt());
        assertThat(roundTripped.getUpdatedAt()).isEqualTo(transaction.getUpdatedAt());
        assertThat(roundTripped.getAmountLocal()).isEqualByComparingTo(transaction.getAmountLocal());
        assertThat(roundTripped.getExchangeRateApplied())
                .isEqualByComparingTo(transaction.getExchangeRateApplied());
    }

    @Test
    @DisplayName("fromDomain does not copy tags: they are managed separately by the repository adapter")
    void fromDomainDoesNotCopyTags() {
        Tag tag = new Tag(UUID.randomUUID(), UUID.randomUUID(), "viaje", "#FFF");
        Transaction transaction = buildTransaction(List.of(tag));

        TransactionEntity entity = TransactionEntity.fromDomain(transaction);

        assertThat(entity.getTags()).isEmpty();
    }

    @Test
    @DisplayName("toDomain maps the entity's managed TagEntity set into domain Tag objects")
    void toDomainMapsManagedTags() {
        Tag domainTag = new Tag(UUID.randomUUID(), UUID.randomUUID(), "viaje", "#FFF");
        TagEntity tagEntity = TagEntity.fromTag(domainTag);
        Transaction transaction = buildTransaction(List.of());
        TransactionEntity entity = TransactionEntity.fromDomain(transaction);
        entity.setTags(Set.of(tagEntity));

        Transaction roundTripped = entity.toDomain();

        assertThat(roundTripped.getTags()).containsExactly(domainTag);
    }

    @Test
    @DisplayName("every getter reflects the value passed to its setter")
    void gettersReflectSetterValues() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate transactionDate = LocalDate.of(2026, 3, 15);
        Instant createdAt = Instant.now().minusSeconds(120);
        Instant updatedAt = Instant.now().minusSeconds(60);
        Instant deletedAt = Instant.now();

        TransactionEntity entity = new TransactionEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setType(TransactionType.EXPENSE);
        entity.setAmount(new BigDecimal("50.00"));
        entity.setCurrency("PEN");
        entity.setAccountId(accountId);
        entity.setFromAccountId(fromAccountId);
        entity.setToAccountId(toAccountId);
        entity.setCategoryId(categoryId);
        entity.setMerchant("Wong");
        entity.setDescription("groceries");
        entity.setReceiptUrl("https://receipt");
        entity.setTransactionDate(transactionDate);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);
        entity.setDeletedAt(deletedAt);
        entity.setAmountLocal(new BigDecimal("187.50"));
        entity.setExchangeRateApplied(new BigDecimal("3.75"));

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getUserId()).isEqualTo(userId);
        assertThat(entity.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(entity.getAmount()).isEqualByComparingTo("50.00");
        assertThat(entity.getCurrency()).isEqualTo("PEN");
        assertThat(entity.getAccountId()).isEqualTo(accountId);
        assertThat(entity.getFromAccountId()).isEqualTo(fromAccountId);
        assertThat(entity.getToAccountId()).isEqualTo(toAccountId);
        assertThat(entity.getCategoryId()).isEqualTo(categoryId);
        assertThat(entity.getMerchant()).isEqualTo("Wong");
        assertThat(entity.getDescription()).isEqualTo("groceries");
        assertThat(entity.getReceiptUrl()).isEqualTo("https://receipt");
        assertThat(entity.getTransactionDate()).isEqualTo(transactionDate);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(entity.getAmountLocal()).isEqualByComparingTo("187.50");
        assertThat(entity.getExchangeRateApplied()).isEqualByComparingTo("3.75");
    }

    @Nested
    @DisplayName("setTags")
    class SetTags {

        @Test
        @DisplayName("a non-null set is stored as-is")
        void nonNullSetIsStored() {
            TransactionEntity entity = new TransactionEntity();
            TagEntity tagEntity = TagEntity.fromTag(
                    new Tag(UUID.randomUUID(), UUID.randomUUID(), "viaje", "#FFF"));

            entity.setTags(Set.of(tagEntity));

            assertThat(entity.getTags()).containsExactly(tagEntity);
        }

        @Test
        @DisplayName("a null set is normalized to an empty set, not left null")
        void nullSetBecomesEmpty() {
            TransactionEntity entity = new TransactionEntity();

            entity.setTags(null);

            assertThat(entity.getTags()).isEmpty();
        }
    }
}
