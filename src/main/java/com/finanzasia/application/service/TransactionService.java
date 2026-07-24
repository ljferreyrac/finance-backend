package com.finanzasia.application.service;

import com.finanzasia.domain.exceptions.AccountNotFoundException;
import com.finanzasia.domain.exceptions.CategoryNotFoundException;
import com.finanzasia.domain.exceptions.InvalidTransactionException;
import com.finanzasia.domain.exceptions.TagNotFoundException;
import com.finanzasia.domain.exceptions.TransactionNotFoundException;
import com.finanzasia.domain.model.Account;
import com.finanzasia.domain.model.ExchangeRate;
import com.finanzasia.domain.model.Tag;
import com.finanzasia.domain.model.Transaction;
import com.finanzasia.domain.model.TransactionFilter;
import com.finanzasia.domain.model.TransactionPage;
import com.finanzasia.domain.model.TransactionType;
import com.finanzasia.domain.port.in.GetTodayExchangeRateUseCase;
import com.finanzasia.domain.port.in.TransactionUseCase;
import com.finanzasia.domain.port.out.AccountRepository;
import com.finanzasia.domain.port.out.CategoryRepository;
import com.finanzasia.domain.port.out.TagRepository;
import com.finanzasia.domain.port.out.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates all transaction use cases.
 * Every mutating method is transactional so that the transaction record
 * and the balance adjustment are committed or rolled back together.
 */
@Service
public class TransactionService implements TransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final AccountService accountService;
    private final GetTodayExchangeRateUseCase getTodayExchangeRateUseCase;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            TagRepository tagRepository,
            AccountService accountService,
            GetTodayExchangeRateUseCase getTodayExchangeRateUseCase) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.accountService = accountService;
        this.getTodayExchangeRateUseCase = getTodayExchangeRateUseCase;
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionPage listTransactions(TransactionFilter filter) {
        return transactionRepository.findWithFilter(filter);
    }

    @Override
    @Transactional(readOnly = true)
    public Transaction getTransaction(UUID userId, UUID transactionId) {
        return transactionRepository.findByIdAndUser(transactionId, userId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }

    @Override
    @Transactional
    public Transaction createTransaction(
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
            LocalDate transactionDate,
            List<UUID> tagIds,
            BigDecimal amountLocal) {

        validateOwnership(userId, type, accountId, fromAccountId, toAccountId, categoryId);

        List<Tag> tags = resolveTagsForUser(userId, tagIds);

        Instant now = Instant.now();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                userId,
                type,
                amount,
                currency,
                accountId,
                fromAccountId,
                toAccountId,
                categoryId,
                merchant,
                description,
                null,
                transactionDate,
                now,
                now,
                null,
                tags);

        // Set before the balance effect runs so applyBalanceEffect can use it directly.
        if (amountLocal != null) {
            transaction.setAmountLocal(amountLocal);
        }

        Transaction saved = transactionRepository.save(transaction);
        applyBalanceEffect(saved, type, accountId, fromAccountId, toAccountId, amount);
        // Re-save only if a conversion set amountLocal / exchangeRateApplied
        if (saved.getAmountLocal() != null) {
            transactionRepository.save(saved);
        }
        return saved;
    }

    @Override
    @Transactional
    public Transaction updateTransaction(
            UUID userId,
            UUID transactionId,
            BigDecimal amount,
            String currency,
            UUID accountId,
            UUID fromAccountId,
            UUID toAccountId,
            UUID categoryId,
            String merchant,
            String description,
            LocalDate transactionDate,
            List<UUID> tagIds,
            BigDecimal amountLocal) {

        Transaction existing = transactionRepository.findByIdAndUser(transactionId, userId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        // Merge first, then validate ownership against the merged values, not the raw inputs,
        // so a request cannot substitute another user's account/category after the check.
        UUID mergedAccountId     = (accountId != null)     ? accountId     : existing.getAccountId();
        UUID mergedFromAccountId = (fromAccountId != null) ? fromAccountId : existing.getFromAccountId();
        UUID mergedToAccountId   = (toAccountId != null)   ? toAccountId   : existing.getToAccountId();
        // null categoryId means "keep existing"; EXPENSE must always end up with one
        UUID mergedCategoryId    = (categoryId != null)    ? categoryId    : existing.getCategoryId();

        validateOwnership(userId, existing.getType(),
                mergedAccountId, mergedFromAccountId, mergedToAccountId, mergedCategoryId);

        if (existing.isExpense() && mergedCategoryId == null) {
            throw new InvalidTransactionException("categoryId is required for EXPENSE transactions");
        }

        // Reverse before applying the new effect; uses the stored amountLocal so the reversal
        // mirrors the exact PEN amount originally applied, regardless of today's rate.
        reverseBalanceEffect(
                existing.getType(),
                existing.getAccountId(),
                existing.getFromAccountId(),
                existing.getToAccountId(),
                existing.getAmount(),
                existing.getAmountLocal(),
                existing.getExchangeRateApplied());

        if (amount != null) { existing.setAmount(amount); }
        if (currency != null) { existing.setCurrency(currency); }
        existing.setAccountId(mergedAccountId);
        existing.setFromAccountId(mergedFromAccountId);
        existing.setToAccountId(mergedToAccountId);
        existing.setCategoryId(mergedCategoryId);
        existing.setMerchant(merchant);
        existing.setDescription(description);
        if (transactionDate != null) { existing.setTransactionDate(transactionDate); }
        existing.setUpdatedAt(Instant.now());

        // null tagIds means "do not change tags"; empty list means "remove all tags"
        if (tagIds != null) {
            List<Tag> tags = resolveTagsForUser(userId, tagIds);
            existing.setTags(tags);
        }

        existing.setAmountLocal(null);
        existing.setExchangeRateApplied(null);

        if (amountLocal != null) {
            existing.setAmountLocal(amountLocal);
        }

        Transaction saved = transactionRepository.save(existing);
        applyBalanceEffect(
                saved,
                saved.getType(),
                saved.getAccountId(),
                saved.getFromAccountId(),
                saved.getToAccountId(),
                saved.getAmount());
        // Re-save only if a conversion set amountLocal / exchangeRateApplied
        if (saved.getAmountLocal() != null) {
            transactionRepository.save(saved);
        }
        return saved;
    }

    @Override
    @Transactional
    public void deleteTransaction(UUID userId, UUID transactionId) {
        Transaction transaction = transactionRepository.findByIdAndUser(transactionId, userId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        transactionRepository.softDelete(transactionId, Instant.now());

        reverseBalanceEffect(
                transaction.getType(),
                transaction.getAccountId(),
                transaction.getFromAccountId(),
                transaction.getToAccountId(),
                transaction.getAmount(),
                transaction.getAmountLocal(),
                transaction.getExchangeRateApplied());
    }

    private void validateOwnership(
            UUID userId,
            TransactionType type,
            UUID accountId,
            UUID fromAccountId,
            UUID toAccountId,
            UUID categoryId) {

        if (type == TransactionType.EXPENSE || type == TransactionType.INCOME) {
            if (accountId == null) {
                throw new InvalidTransactionException("accountId is required for " + type);
            }
            accountRepository.findByIdAndUser(accountId, userId)
                    .orElseThrow(() -> new AccountNotFoundException(accountId));

            if (type == TransactionType.EXPENSE) {
                if (categoryId == null) {
                    throw new InvalidTransactionException("categoryId is required for EXPENSE transactions");
                }
                categoryRepository.findByIdAndUser(categoryId, userId)
                        .orElseThrow(() -> new CategoryNotFoundException(categoryId));
            }
        } else if (type == TransactionType.TRANSFER) {
            if (fromAccountId == null) {
                throw new InvalidTransactionException("fromAccountId is required for TRANSFER transactions");
            }
            if (toAccountId == null) {
                throw new InvalidTransactionException("toAccountId is required for TRANSFER transactions");
            }
            if (fromAccountId.equals(toAccountId)) {
                throw new InvalidTransactionException("Cannot transfer to the same account");
            }
            accountRepository.findByIdAndUser(fromAccountId, userId)
                    .orElseThrow(() -> new AccountNotFoundException(fromAccountId));
            accountRepository.findByIdAndUser(toAccountId, userId)
                    .orElseThrow(() -> new AccountNotFoundException(toAccountId));
        }
    }

    /**
     * On a currency mismatch, converts via today's sell rate and stores the applied rate on the
     * transaction so a future reversal uses the exact same value.
     */
    private void applyBalanceEffect(
            Transaction transaction,
            TransactionType type,
            UUID accountId,
            UUID fromAccountId,
            UUID toAccountId,
            BigDecimal amount) {

        switch (type) {
            case EXPENSE -> {
                BigDecimal debitAmount = convertIfNeeded(transaction, accountId, amount);
                accountService.adjustBalance(accountId, debitAmount.negate());
            }
            case INCOME -> {
                BigDecimal creditAmount = convertIfNeeded(transaction, accountId, amount);
                accountService.adjustBalance(accountId, creditAmount);
            }
            case TRANSFER -> {
                accountService.adjustBalance(fromAccountId, amount.negate());
                // Cross-currency: credit the confirmed received amount (amountLocal); null when same-currency, use amount.
                BigDecimal toAmount = (transaction.getAmountLocal() != null)
                        ? transaction.getAmountLocal()
                        : amount;
                accountService.adjustBalance(toAccountId, toAmount);
            }
        }
    }

    /**
     * Prefers {@code storedAmountLocal} for a symmetric reversal; falls back to reconstructing
     * via {@code storedRate} for rows persisted before {@code amount_local} existed.
     */
    private void reverseBalanceEffect(
            TransactionType type,
            UUID accountId,
            UUID fromAccountId,
            UUID toAccountId,
            BigDecimal amount,
            BigDecimal storedAmountLocal,
            BigDecimal storedRate) {

        switch (type) {
            case EXPENSE -> {
                BigDecimal debitAmount = resolveLocalAmount(amount, storedAmountLocal, storedRate);
                accountService.adjustBalance(accountId, debitAmount);
            }
            case INCOME -> {
                BigDecimal creditAmount = resolveLocalAmount(amount, storedAmountLocal, storedRate);
                accountService.adjustBalance(accountId, creditAmount.negate());
            }
            case TRANSFER -> {
                accountService.adjustBalance(fromAccountId, amount);
                // Mirrors the amount originally credited in applyBalanceEffect.
                BigDecimal toAmount = (storedAmountLocal != null) ? storedAmountLocal : amount;
                accountService.adjustBalance(toAccountId, toAmount.negate());
            }
        }
    }

    /**
     * Returns the effective local-currency amount for a balance operation.
     * Priority: storedAmountLocal (user-confirmed) > amount * storedRate (legacy fallback) > amount (same-currency).
     */
    private BigDecimal resolveLocalAmount(
            BigDecimal amount, BigDecimal storedAmountLocal, BigDecimal storedRate) {
        if (storedAmountLocal != null) {
            return storedAmountLocal;
        }
        if (storedRate != null) {
            return amount.multiply(storedRate).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return amount;
    }

    /**
     * Cross-currency resolution order: use {@code transaction.amountLocal} if already
     * user-confirmed (deriving the rate from it), otherwise fall back to today's sell rate and
     * store both fields for audit/reversal. Same-currency: original amount, fields untouched.
     */
    private BigDecimal convertIfNeeded(Transaction transaction, UUID accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        // Must match adjustBalance's own linkedAccountId resolution to stay consistent.
        if (account.getLinkedAccountId() != null) {
            UUID linkedId = account.getLinkedAccountId();
            account = accountRepository.findById(linkedId)
                    .orElseThrow(() -> new AccountNotFoundException(linkedId));
        }

        if (transaction.getCurrency().equals(account.getCurrency())) {
            return amount;
        }

        if (transaction.getAmountLocal() != null) {
            BigDecimal local = transaction.getAmountLocal();
            BigDecimal rate = local.divide(amount, 4, java.math.RoundingMode.HALF_UP);
            transaction.setExchangeRateApplied(rate);
            return local;
        }

        ExchangeRate rate = getTodayExchangeRateUseCase.getOrCreateDefault();
        BigDecimal converted = rate.toAccountCurrency(amount);
        transaction.setAmountLocal(converted);
        transaction.setExchangeRateApplied(rate.getSellRate());
        return converted;
    }

    /**
     * Loads and ownership-validates a list of tag IDs for the given user.
     * Throws {@link TagNotFoundException} for the first ID that does not belong to the user.
     */
    private List<Tag> resolveTagsForUser(UUID userId, List<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<UUID> uniqueIds = new HashSet<>(tagIds);
        List<Tag> found = tagRepository.findByIdsAndUserId(uniqueIds, userId);
        if (found.size() != uniqueIds.size()) {
            Set<UUID> foundIds = new HashSet<>();
            for (Tag t : found) {
                foundIds.add(t.id());
            }
            UUID missing = uniqueIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .orElse(null);
            throw new TagNotFoundException(missing);
        }
        return found;
    }
}
