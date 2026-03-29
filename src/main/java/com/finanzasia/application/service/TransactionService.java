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

        // Carry the user-supplied local amount into the domain object before the
        // balance effect is applied so that applyBalanceEffect can use it directly.
        if (amountLocal != null) {
            transaction.setAmountLocal(amountLocal);
        }

        Transaction saved = transactionRepository.save(transaction);
        applyBalanceEffect(saved, type, accountId, fromAccountId, toAccountId, amount);
        // Persist amountLocal / exchangeRateApplied if conversion was needed
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

        // Compute merged state FIRST, then validate ownership against it.
        // This ensures the values used in applyBalanceEffect are the same ones
        // that were ownership-checked — prevents cross-user account substitution.
        UUID mergedAccountId     = (accountId != null)     ? accountId     : existing.getAccountId();
        UUID mergedFromAccountId = (fromAccountId != null) ? fromAccountId : existing.getFromAccountId();
        UUID mergedToAccountId   = (toAccountId != null)   ? toAccountId   : existing.getToAccountId();
        // null categoryId on update means "keep existing" — EXPENSE must always have a category
        UUID mergedCategoryId    = (categoryId != null)    ? categoryId    : existing.getCategoryId();

        validateOwnership(userId, existing.getType(),
                mergedAccountId, mergedFromAccountId, mergedToAccountId, mergedCategoryId);

        // Guard: EXPENSE must retain a category after the merge
        if (existing.isExpense() && mergedCategoryId == null) {
            throw new InvalidTransactionException("categoryId is required for EXPENSE transactions");
        }

        // Reverse the old balance effect before applying the new one.
        // Use amountLocal when available so the reversal mirrors the exact PEN amount
        // that was originally applied, regardless of today's rate.
        reverseBalanceEffect(
                existing.getType(),
                existing.getAccountId(),
                existing.getFromAccountId(),
                existing.getToAccountId(),
                existing.getAmount(),
                existing.getAmountLocal(),
                existing.getExchangeRateApplied());

        if (amount != null)          existing.setAmount(amount);
        if (currency != null)        existing.setCurrency(currency);
        existing.setAccountId(mergedAccountId);
        existing.setFromAccountId(mergedFromAccountId);
        existing.setToAccountId(mergedToAccountId);
        existing.setCategoryId(mergedCategoryId);
        existing.setMerchant(merchant);
        existing.setDescription(description);
        if (transactionDate != null) existing.setTransactionDate(transactionDate);
        existing.setUpdatedAt(Instant.now());

        // null tagIds means "do not change tags"; empty list means "remove all tags"
        if (tagIds != null) {
            List<Tag> tags = resolveTagsForUser(userId, tagIds);
            existing.setTags(tags);
        }

        // Clear any previously stored conversion fields before recomputing
        existing.setAmountLocal(null);
        existing.setExchangeRateApplied(null);

        // Carry the user-supplied local amount for the new balance calculation
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
        // Persist updated amountLocal / exchangeRateApplied if conversion was needed
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

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

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
     * Applies the balance change for a committed transaction.
     * When the transaction currency differs from the account currency, the sell
     * rate from today's exchange rate is used to convert the amount, and the
     * applied rate is stored on the transaction so that a future reversal uses
     * the exact same value.
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
                // Transfers are always within the same user; no currency conversion applied
                accountService.adjustBalance(fromAccountId, amount.negate());
                accountService.adjustBalance(toAccountId, amount);
            }
        }
    }

    /**
     * Reverses a previously applied balance change.
     * Prefers {@code storedAmountLocal} (the exact PEN amount that was applied) when
     * non-null so that the reversal is perfectly symmetric. Falls back to reconstructing
     * the amount via {@code storedRate} for rows persisted before {@code amount_local} was
     * added. For same-currency transactions both will be null and the original amount is used.
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
                accountService.adjustBalance(toAccountId, amount.negate());
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
     * Returns the amount in the account's currency for balance effect purposes.
     *
     * <p>Resolution order for cross-currency transactions:
     * <ol>
     *   <li>If {@code transaction.amountLocal} is already set (user-confirmed value), use it
     *       directly and compute {@code exchangeRateApplied = amountLocal / amount}.</li>
     *   <li>Otherwise, fall back to today's sell rate, set both {@code amountLocal} and
     *       {@code exchangeRateApplied} on the transaction for audit and future reversal.</li>
     * </ol>
     * When currencies match, neither field is modified and the original amount is returned.
     */
    private BigDecimal convertIfNeeded(Transaction transaction, UUID accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        // Follow linkedAccountId to the actual balance holder (same as adjustBalance does)
        if (account.getLinkedAccountId() != null) {
            UUID linkedId = account.getLinkedAccountId();
            account = accountRepository.findById(linkedId)
                    .orElseThrow(() -> new AccountNotFoundException(linkedId));
        }

        if (transaction.getCurrency().equals(account.getCurrency())) {
            // Same currency: no conversion fields needed
            return amount;
        }

        // Cross-currency: use user-supplied amountLocal when available
        if (transaction.getAmountLocal() != null) {
            BigDecimal local = transaction.getAmountLocal();
            BigDecimal rate = local.divide(amount, 4, java.math.RoundingMode.HALF_UP);
            transaction.setExchangeRateApplied(rate);
            return local;
        }

        // Auto-convert using today's sell rate
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
            // At least one ID is unknown or belongs to a different user
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
