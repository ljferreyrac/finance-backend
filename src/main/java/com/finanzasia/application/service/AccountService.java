package com.finanzasia.application.service;

import com.finanzasia.domain.exceptions.AccountInUseException;
import com.finanzasia.domain.exceptions.AccountNotFoundException;
import com.finanzasia.domain.model.Account;
import com.finanzasia.domain.model.AccountType;
import com.finanzasia.domain.model.NetWorth;
import com.finanzasia.domain.port.in.AccountUseCase;
import com.finanzasia.domain.port.out.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates all account management use cases.
 * Enforces ownership, default-account bookkeeping, and safe deletion.
 */
@Service
public class AccountService implements AccountUseCase {

    static final int MAX_ACCOUNTS_PER_USER = 20;

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public List<Account> listAccounts(UUID userId) {
        return accountRepository.findAllByUser(userId);
    }

    @Override
    public NetWorth getNetWorth(UUID userId) {
        List<Account> accounts = accountRepository.findAllByUser(userId);

        // Assets: bank/cash/wallet balances (positive contribution).
        // Liabilities: credit card outstanding balances subtracted using abs()
        // because the initial balance is always stored as a positive number.
        BigDecimal totalPEN = computeNetWorthForCurrency(accounts, "PEN");
        BigDecimal totalUSD = computeNetWorthForCurrency(accounts, "USD");

        return new NetWorth(totalPEN, totalUSD, accounts);
    }

    /**
     * Computes the net worth contribution for a given currency.
     * Non-credit accounts add their balance; credit card accounts subtract
     * their outstanding balance (stored as a positive number).
     */
    private BigDecimal computeNetWorthForCurrency(List<Account> accounts, String currency) {
        return accounts.stream()
                .filter(a -> currency.equals(a.getCurrency()) && a.isActive())
                .map(a -> a.getType() == AccountType.CREDIT_CARD
                        ? a.getCurrentBalance().abs().negate()
                        : a.getCurrentBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public Account createAccount(
            UUID userId,
            String name,
            AccountType type,
            String bank,
            String currency,
            BigDecimal initialBalance,
            BigDecimal creditLimit,
            Integer closingDay,
            Integer dueDay,
            String color,
            boolean isDefault,
            UUID linkedAccountId) {

        if (accountRepository.countByUser(userId) >= MAX_ACCOUNTS_PER_USER) {
            throw new com.finanzasia.domain.exceptions.AccountLimitExceededException(MAX_ACCOUNTS_PER_USER);
        }

        if (linkedAccountId != null) {
            accountRepository.findByIdAndUser(linkedAccountId, userId)
                    .orElseThrow(() -> new AccountNotFoundException(linkedAccountId));
        }

        if (isDefault) {
            accountRepository.clearDefaultForUser(userId);
        }

        BigDecimal balance = initialBalance != null
                ? initialBalance.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        Instant now = Instant.now();
        Account account = new Account(
                UUID.randomUUID(),
                userId,
                name,
                type,
                bank,
                currency != null ? currency : "PEN",
                balance,
                creditLimit,
                closingDay,
                dueDay,
                color,
                isDefault,
                true,
                linkedAccountId,
                now,
                now);

        return accountRepository.save(account);
    }

    @Override
    @Transactional
    public Account updateAccount(
            UUID userId,
            UUID accountId,
            String name,
            String bank,
            BigDecimal creditLimit,
            Integer closingDay,
            Integer dueDay,
            String color,
            boolean isActive,
            UUID linkedAccountId) {

        Account account = accountRepository.findByIdAndUser(accountId, userId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (linkedAccountId != null) {
            accountRepository.findByIdAndUser(linkedAccountId, userId)
                    .orElseThrow(() -> new AccountNotFoundException(linkedAccountId));
        }

        if (name != null) {
            account.setName(name);
        }
        account.setBank(bank);
        account.setCreditLimit(creditLimit);
        account.setClosingDay(closingDay);
        account.setDueDay(dueDay);
        account.setColor(color);
        account.setActive(isActive);
        account.setLinkedAccountId(linkedAccountId);
        account.setUpdatedAt(Instant.now());

        return accountRepository.save(account);
    }

    @Override
    @Transactional
    public void deleteAccount(UUID userId, UUID accountId) {
        Account account = accountRepository.findByIdAndUser(accountId, userId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        long txCount = accountRepository.countTransactionsByAccountId(account.getId());
        if (txCount > 0) {
            throw new AccountInUseException(accountId, txCount);
        }

        accountRepository.delete(accountId);
    }

    @Override
    @Transactional
    public Account setDefaultAccount(UUID userId, UUID accountId) {
        Account account = accountRepository.findByIdAndUser(accountId, userId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        accountRepository.clearDefaultForUser(userId);
        account.markAsDefault(Instant.now());
        return accountRepository.save(account);
    }

    /**
     * Adjusts the stored balance of an account by the given delta.
     * A positive delta credits (adds) and a negative delta debits (subtracts).
     * Package-private — only callable from {@code application.service} classes.
     * Must be called inside an active {@code @Transactional} scope.
     * Balance is never exposed as a directly writable field in any public port.
     */
    void adjustBalance(UUID accountId, BigDecimal delta) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        // If this account delegates its balance to a parent account (e.g. a wallet
        // linked to a bank account), adjust the parent instead.
        UUID linkedId = account.getLinkedAccountId();
        if (linkedId != null) {
            account = accountRepository.findById(linkedId)
                    .orElseThrow(() -> new AccountNotFoundException(linkedId));
        }

        if (delta.compareTo(BigDecimal.ZERO) >= 0) {
            account.credit(delta);
        } else {
            account.debit(delta.abs());
        }
        accountRepository.save(account);
    }
}
