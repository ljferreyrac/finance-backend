package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.Tag;
import com.finanzasia.domain.model.Transaction;
import com.finanzasia.domain.model.TransactionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    // Plain UUID columns, no @ManyToOne joins, to keep queries simple and avoid lazy-loading issues.
    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "from_account_id")
    private UUID fromAccountId;

    @Column(name = "to_account_id")
    private UUID toAccountId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "merchant", length = 255)
    private String merchant;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "receipt_url", length = 1024)
    private String receiptUrl;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "amount_local", precision = 14, scale = 2)
    private BigDecimal amountLocal;

    @Column(name = "exchange_rate_applied", precision = 10, scale = 4)
    private BigDecimal exchangeRateApplied;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @BatchSize(size = 50)
    @JoinTable(name = "transaction_tags",
            joinColumns = @JoinColumn(name = "transaction_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<TagEntity> tags = new HashSet<>();

    protected TransactionEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public UUID getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(UUID fromAccountId) { this.fromAccountId = fromAccountId; }

    public UUID getToAccountId() { return toAccountId; }
    public void setToAccountId(UUID toAccountId) { this.toAccountId = toAccountId; }

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }

    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public BigDecimal getAmountLocal() { return amountLocal; }
    public void setAmountLocal(BigDecimal amountLocal) { this.amountLocal = amountLocal; }

    public BigDecimal getExchangeRateApplied() { return exchangeRateApplied; }
    public void setExchangeRateApplied(BigDecimal exchangeRateApplied) {
        this.exchangeRateApplied = exchangeRateApplied;
    }

    public Set<TagEntity> getTags() { return tags; }
    public void setTags(Set<TagEntity> tags) { this.tags = (tags != null) ? tags : new HashSet<>(); }

    public Transaction toDomain() {
        List<Tag> domainTags = tags.stream()
                .map(TagEntity::toTag)
                .toList();
        return new Transaction(
                id, userId, type, amount, currency,
                accountId, fromAccountId, toAccountId, categoryId,
                merchant, description, receiptUrl,
                transactionDate, createdAt, updatedAt, deletedAt,
                domainTags,
                exchangeRateApplied,
                amountLocal);
    }

    public static TransactionEntity fromDomain(Transaction domain) {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setType(domain.getType());
        entity.setAmount(domain.getAmount());
        entity.setCurrency(domain.getCurrency());
        entity.setAccountId(domain.getAccountId());
        entity.setFromAccountId(domain.getFromAccountId());
        entity.setToAccountId(domain.getToAccountId());
        entity.setCategoryId(domain.getCategoryId());
        entity.setMerchant(domain.getMerchant());
        entity.setDescription(domain.getDescription());
        entity.setReceiptUrl(domain.getReceiptUrl());
        entity.setTransactionDate(domain.getTransactionDate());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setAmountLocal(domain.getAmountLocal());
        entity.setExchangeRateApplied(domain.getExchangeRateApplied());
        // Tags are managed separately via the service layer to avoid replacing the lazily-loaded collection.
        return entity;
    }
}
