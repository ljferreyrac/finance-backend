package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.Account;
import com.finanzasia.domain.model.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AccountType type;

    @Column(name = "bank", length = 100)
    private String bank;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "current_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "credit_limit", precision = 14, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "closing_day")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer closingDay;

    @Column(name = "due_day")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer dueDay;

    @Column(name = "color", length = 7)
    private String color;

    @Column(name = "linked_account_id")
    private UUID linkedAccountId;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AccountEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public AccountType getType() { return type; }
    public void setType(AccountType type) { this.type = type; }

    public String getBank() { return bank; }
    public void setBank(String bank) { this.bank = bank; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }

    public Integer getClosingDay() { return closingDay; }
    public void setClosingDay(Integer closingDay) { this.closingDay = closingDay; }

    public Integer getDueDay() { return dueDay; }
    public void setDueDay(Integer dueDay) { this.dueDay = dueDay; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public UUID getLinkedAccountId() { return linkedAccountId; }
    public void setLinkedAccountId(UUID linkedAccountId) { this.linkedAccountId = linkedAccountId; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Account toDomain() {
        return new Account(
                id, userId, name, type, bank, currency,
                currentBalance, creditLimit, closingDay, dueDay,
                color, isDefault, isActive, linkedAccountId, createdAt, updatedAt);
    }

    public static AccountEntity fromDomain(Account domain) {
        AccountEntity entity = new AccountEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setName(domain.getName());
        entity.setType(domain.getType());
        entity.setBank(domain.getBank());
        entity.setCurrency(domain.getCurrency());
        entity.setCurrentBalance(domain.getCurrentBalance());
        entity.setCreditLimit(domain.getCreditLimit());
        entity.setClosingDay(domain.getClosingDay());
        entity.setDueDay(domain.getDueDay());
        entity.setColor(domain.getColor());
        entity.setDefault(domain.isDefault());
        entity.setActive(domain.isActive());
        entity.setLinkedAccountId(domain.getLinkedAccountId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
