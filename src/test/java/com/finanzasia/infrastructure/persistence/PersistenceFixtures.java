package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.AccountType;
import com.finanzasia.domain.model.TransactionType;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared fixture builder for the repository integration tests in this package.
 *
 * <p>Every method persists and flushes through {@link TestEntityManager} so the returned
 * entity's generated/assigned id is immediately usable for further fixtures or for calling the
 * {@code Jpa*RepositoryPort} under test. Defaults satisfy every {@code CHECK} constraint in
 * {@code V1__initial_schema.sql}; override only the fields a given test cares about.
 */
final class PersistenceFixtures {

    // Guarantees a distinct email per fixture user within a single JVM run, since
    // users.email is UNIQUE and tests share the singleton container/database.
    private static final AtomicInteger EMAIL_SEQ = new AtomicInteger();

    private PersistenceFixtures() {}

    static UserEntity user(TestEntityManager em) {
        Instant now = Instant.now();
        UserEntity entity = new UserEntity();
        entity.setId(UUID.randomUUID());
        entity.setEmail("fixture-" + EMAIL_SEQ.incrementAndGet() + "@example.com");
        entity.setPasswordHash("$2a$10$fixturefixturefixturefixturefixturefixturefixturefix");
        entity.setFullName("Fixture User");
        entity.setCurrency("PEN");
        entity.setTimezone("America/Lima");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return em.persistFlushFind(entity);
    }

    static AccountEntity account(TestEntityManager em, UUID userId, AccountType type, String currency) {
        Instant now = Instant.now();
        AccountEntity entity = new AccountEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setName("Fixture Account");
        entity.setType(type);
        entity.setBank(type == AccountType.CREDIT_CARD || type == AccountType.BANK ? "BCP" : null);
        entity.setCurrency(currency);
        entity.setCurrentBalance(BigDecimal.ZERO);
        entity.setDefault(false);
        entity.setActive(true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return em.persistFlushFind(entity);
    }

    static AccountEntity account(TestEntityManager em, UUID userId) {
        return account(em, userId, AccountType.BANK, "PEN");
    }

    static CategoryEntity category(TestEntityManager em, UUID userId, String name) {
        Instant now = Instant.now();
        CategoryEntity entity = new CategoryEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setName(name);
        entity.setDefault(false);
        entity.setPosition(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return em.persistFlushFind(entity);
    }

    // TagEntity uses @GeneratedValue + @PrePersist, so id/timestamps must not be set here.
    static TagEntity tag(TestEntityManager em, UUID userId, String name) {
        TagEntity entity = new TagEntity();
        entity.setUserId(userId);
        entity.setName(name);
        entity.setColor("#ff5733");
        return em.persistFlushFind(entity);
    }

    static TransactionEntity expense(
            TestEntityManager em, UUID userId, UUID accountId, UUID categoryId,
            BigDecimal amount, String currency, LocalDate date, String merchant) {
        return transaction(em, userId, TransactionType.EXPENSE, amount, currency,
                accountId, null, null, categoryId, date, merchant);
    }

    static TransactionEntity income(
            TestEntityManager em, UUID userId, UUID accountId, UUID categoryId,
            BigDecimal amount, String currency, LocalDate date) {
        return transaction(em, userId, TransactionType.INCOME, amount, currency,
                accountId, null, null, categoryId, date, null);
    }

    static TransactionEntity transfer(
            TestEntityManager em, UUID userId, UUID fromAccountId, UUID toAccountId,
            BigDecimal amount, String currency, LocalDate date) {
        return transaction(em, userId, TransactionType.TRANSFER, amount, currency,
                null, fromAccountId, toAccountId, null, date, null);
    }

    private static TransactionEntity transaction(
            TestEntityManager em, UUID userId, TransactionType type, BigDecimal amount, String currency,
            UUID accountId, UUID fromAccountId, UUID toAccountId, UUID categoryId,
            LocalDate date, String merchant) {
        Instant now = Instant.now();
        TransactionEntity entity = new TransactionEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setType(type);
        entity.setAmount(amount);
        entity.setCurrency(currency);
        entity.setAccountId(accountId);
        entity.setFromAccountId(fromAccountId);
        entity.setToAccountId(toAccountId);
        entity.setCategoryId(categoryId);
        entity.setMerchant(merchant);
        entity.setTransactionDate(date);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setTags(new HashSet<>());
        return em.persistFlushFind(entity);
    }

    // TransactionEntity.fromDomain deliberately never replaces the lazily-loaded tags
    // collection (see its comment), so join rows must be written explicitly like this.
    static void attachTag(TestEntityManager em, UUID transactionId, UUID tagId) {
        TransactionEntity transaction = em.find(TransactionEntity.class, transactionId);
        TagEntity tag = em.find(TagEntity.class, tagId);
        transaction.getTags().add(tag);
        em.flush();
    }

    static ExchangeRateEntity exchangeRate(
            TestEntityManager em, String from, String to, BigDecimal buyRate, BigDecimal sellRate, LocalDate date) {
        Instant now = Instant.now();
        ExchangeRateEntity entity = new ExchangeRateEntity();
        entity.setId(UUID.randomUUID());
        entity.setCurrencyFrom(from);
        entity.setCurrencyTo(to);
        entity.setBuyRate(buyRate);
        entity.setSellRate(sellRate);
        entity.setRateDate(date);
        entity.setSource("fixture");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return em.persistFlushFind(entity);
    }
}
