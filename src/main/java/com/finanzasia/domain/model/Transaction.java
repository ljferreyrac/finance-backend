package com.finanzasia.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Covers expenses, income, and account-to-account transfers.
 * No JPA or framework annotations are allowed here.
 */
public final class Transaction {

    private final UUID id;
    private final UUID userId;
    private final TransactionType type;
    private BigDecimal amount;
    private String currency;
    private UUID accountId;
    private UUID fromAccountId;
    private UUID toAccountId;
    private UUID categoryId;
    private String merchant;
    private String description;
    private String receiptUrl;
    private LocalDate transactionDate;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private List<Tag> tags;
    /**
     * Confirmed amount in the account's primary currency (PEN) for cross-currency transactions;
     * the user may override the auto-calculated value before saving. Null when currencies match.
     */
    private BigDecimal amountLocal;

    /** Effective rate (amountLocal / amount), kept for audit. Null when currencies match. */
    private BigDecimal exchangeRateApplied;

    public Transaction(
            UUID id,
            UUID userId,
            TransactionType type,
            BigDecimal amount,
            String currency,
            UUID accountId,
            UUID fromAccountId,
            UUID toAccountId,
            UUID categoryId,
            String merchant,
            String description,
            String receiptUrl,
            LocalDate transactionDate,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt) {
        this(id, userId, type, amount, currency, accountId, fromAccountId, toAccountId,
                categoryId, merchant, description, receiptUrl, transactionDate,
                createdAt, updatedAt, deletedAt, Collections.emptyList(), null, null);
    }

    public Transaction(
            UUID id,
            UUID userId,
            TransactionType type,
            BigDecimal amount,
            String currency,
            UUID accountId,
            UUID fromAccountId,
            UUID toAccountId,
            UUID categoryId,
            String merchant,
            String description,
            String receiptUrl,
            LocalDate transactionDate,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            List<Tag> tags) {
        this(id, userId, type, amount, currency, accountId, fromAccountId, toAccountId,
                categoryId, merchant, description, receiptUrl, transactionDate,
                createdAt, updatedAt, deletedAt, tags, null, null);
    }

    public Transaction(
            UUID id,
            UUID userId,
            TransactionType type,
            BigDecimal amount,
            String currency,
            UUID accountId,
            UUID fromAccountId,
            UUID toAccountId,
            UUID categoryId,
            String merchant,
            String description,
            String receiptUrl,
            LocalDate transactionDate,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            List<Tag> tags,
            BigDecimal exchangeRateApplied) {
        this(id, userId, type, amount, currency, accountId, fromAccountId, toAccountId,
                categoryId, merchant, description, receiptUrl, transactionDate,
                createdAt, updatedAt, deletedAt, tags, exchangeRateApplied, null);
    }

    /** Prefer narrower constructors when {@code amountLocal}/{@code exchangeRateApplied} are not yet known. */
    public Transaction(
            UUID id,
            UUID userId,
            TransactionType type,
            BigDecimal amount,
            String currency,
            UUID accountId,
            UUID fromAccountId,
            UUID toAccountId,
            UUID categoryId,
            String merchant,
            String description,
            String receiptUrl,
            LocalDate transactionDate,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            List<Tag> tags,
            BigDecimal exchangeRateApplied,
            BigDecimal amountLocal) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.accountId = accountId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.categoryId = categoryId;
        this.merchant = merchant;
        this.description = description;
        this.receiptUrl = receiptUrl;
        this.transactionDate = transactionDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.tags = (tags != null) ? List.copyOf(tags) : Collections.emptyList();
        this.exchangeRateApplied = exchangeRateApplied;
        this.amountLocal = amountLocal;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public UUID getAccountId() { return accountId; }
    public UUID getFromAccountId() { return fromAccountId; }
    public UUID getToAccountId() { return toAccountId; }
    public UUID getCategoryId() { return categoryId; }
    public String getMerchant() { return merchant; }
    public String getDescription() { return description; }
    public String getReceiptUrl() { return receiptUrl; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    public boolean belongsTo(UUID ownerId) {
        return userId.equals(ownerId);
    }

    /** True once the transaction has been soft-deleted (deletedAt set), never hard-removed. */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public boolean isExpense() {
        return type == TransactionType.EXPENSE;
    }

    public boolean isIncome() {
        return type == TransactionType.INCOME;
    }

    public boolean isTransfer() {
        return type == TransactionType.TRANSFER;
    }

    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public void setFromAccountId(UUID fromAccountId) { this.fromAccountId = fromAccountId; }
    public void setToAccountId(UUID toAccountId) { this.toAccountId = toAccountId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public void setMerchant(String merchant) { this.merchant = merchant; }
    public void setDescription(String description) { this.description = description; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<Tag> getTags() { return tags; }
    public void setTags(List<Tag> tags) {
        this.tags = (tags != null) ? List.copyOf(tags) : Collections.emptyList();
    }

    public BigDecimal getExchangeRateApplied() { return exchangeRateApplied; }
    public void setExchangeRateApplied(BigDecimal exchangeRateApplied) {
        this.exchangeRateApplied = exchangeRateApplied;
    }

    public BigDecimal getAmountLocal() { return amountLocal; }
    public void setAmountLocal(BigDecimal amountLocal) { this.amountLocal = amountLocal; }
}
