package com.finanzasia.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * No JPA or framework annotations are allowed here.
 */
public final class Account {

    private final UUID id;
    private final UUID userId;
    private String name;
    private final AccountType type;
    private String bank;
    private final String currency;
    private BigDecimal currentBalance;
    private BigDecimal creditLimit;
    private Integer closingDay;
    private Integer dueDay;
    private String color;
    private boolean isDefault;
    private boolean isActive;
    private UUID linkedAccountId;
    private final Instant createdAt;
    private Instant updatedAt;

    public Account(
            UUID id,
            UUID userId,
            String name,
            AccountType type,
            String bank,
            String currency,
            BigDecimal currentBalance,
            BigDecimal creditLimit,
            Integer closingDay,
            Integer dueDay,
            String color,
            boolean isDefault,
            boolean isActive,
            UUID linkedAccountId,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.bank = bank;
        this.currency = currency;
        this.currentBalance = currentBalance;
        this.creditLimit = creditLimit;
        this.closingDay = closingDay;
        this.dueDay = dueDay;
        this.color = color;
        this.isDefault = isDefault;
        this.isActive = isActive;
        this.linkedAccountId = linkedAccountId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public AccountType getType() { return type; }
    public String getBank() { return bank; }
    public String getCurrency() { return currency; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public Integer getClosingDay() { return closingDay; }
    public Integer getDueDay() { return dueDay; }
    public String getColor() { return color; }
    public boolean isDefault() { return isDefault; }
    public boolean isActive() { return isActive; }
    public UUID getLinkedAccountId() { return linkedAccountId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean belongsTo(UUID ownerId) {
        return userId.equals(ownerId);
    }

    public void markAsDefault(Instant now) {
        this.isDefault = true;
        this.updatedAt = now;
    }

    public void clearDefault(Instant now) {
        this.isDefault = false;
        this.updatedAt = now;
    }

    /** For credit cards, debiting increases the outstanding balance (balance goes negative). */
    public void debit(BigDecimal amount) {
        this.currentBalance = this.currentBalance.subtract(amount);
    }

    /** For credit cards, crediting reduces the outstanding balance. */
    public void credit(BigDecimal amount) {
        this.currentBalance = this.currentBalance.add(amount);
    }

    public void setName(String name) { this.name = name; }
    public void setBank(String bank) { this.bank = bank; }
    public void setLinkedAccountId(UUID linkedAccountId) { this.linkedAccountId = linkedAccountId; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    public void setClosingDay(Integer closingDay) { this.closingDay = closingDay; }
    public void setDueDay(Integer dueDay) { this.dueDay = dueDay; }
    public void setColor(String color) { this.color = color; }
    public void setActive(boolean isActive) { this.isActive = isActive; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
