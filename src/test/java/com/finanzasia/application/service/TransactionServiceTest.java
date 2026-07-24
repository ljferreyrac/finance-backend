package com.finanzasia.application.service;

import com.finanzasia.domain.exceptions.CategoryNotFoundException;
import com.finanzasia.domain.exceptions.InvalidTransactionException;
import com.finanzasia.domain.exceptions.TransactionNotFoundException;
import com.finanzasia.domain.model.Account;
import com.finanzasia.domain.model.AccountType;
import com.finanzasia.domain.model.Category;
import com.finanzasia.domain.model.Transaction;
import com.finanzasia.domain.model.TransactionDetail;
import com.finanzasia.domain.model.TransactionType;
import com.finanzasia.domain.model.ExchangeRate;
import com.finanzasia.domain.port.in.GetTodayExchangeRateUseCase;
import com.finanzasia.domain.port.out.AccountRepository;
import com.finanzasia.domain.port.out.CategoryRepository;
import com.finanzasia.domain.port.out.TagRepository;
import com.finanzasia.domain.port.out.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TagRepository tagRepository;
    @Mock private AccountService accountService;
    @Mock private GetTodayExchangeRateUseCase getTodayExchangeRateUseCase;

    private TransactionService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID FROM_ACCOUNT_ID = UUID.randomUUID();
    private static final UUID TO_ACCOUNT_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID TX_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(100);
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 16);

    @BeforeEach
    void setUp() {
        service = new TransactionService(
                transactionRepository, accountRepository, categoryRepository,
                tagRepository, accountService, getTodayExchangeRateUseCase);
    }

    private Account buildAccount(UUID id) {
        Instant now = Instant.now();
        return new Account(id, USER_ID, "Account", AccountType.BANK,
                "BCP", "PEN", BigDecimal.valueOf(500), null, null, null,
                null, false, true, null, now, now);
    }

    private Category buildCategory(UUID id) {
        Instant now = Instant.now();
        return new Category(id, USER_ID, "Food", "#FF0000", "food", false, 1, now, now);
    }

    private Transaction buildTransaction(UUID id, TransactionType type, UUID accountId,
                                         UUID fromId, UUID toId, UUID catId, BigDecimal amount) {
        Instant now = Instant.now();
        return new Transaction(id, USER_ID, type, amount, "PEN",
                accountId, fromId, toId, catId,
                "Merchant", "Desc", null, TODAY, now, now, null,
                java.util.Collections.emptyList(), null, null);
    }

    /** Builds a USD transaction with pre-stored conversion fields (used in reversal tests). */
    private Transaction buildUsdTransaction(UUID id, TransactionType type, UUID accountId,
                                            BigDecimal amount, BigDecimal exchangeRateApplied,
                                            BigDecimal amountLocal) {
        Instant now = Instant.now();
        return new Transaction(id, USER_ID, type, amount, "USD",
                accountId, null, null, CATEGORY_ID,
                "Merchant", "Desc", null, TODAY, now, now, null,
                java.util.Collections.emptyList(), exchangeRateApplied, amountLocal);
    }

    private ExchangeRate buildExchangeRate(BigDecimal buy, BigDecimal sell) {
        Instant now = Instant.now();
        return new ExchangeRate(UUID.randomUUID(), "USD", "PEN", buy, sell,
                TODAY, "MANUAL", now, now);
    }

    // ------------------------------------------------------------------
    // createTransaction
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("createTransaction")
    class CreateTransaction {

        @Test
        @DisplayName("EXPENSE debits the account")
        void createExpenseDebitsAccount() {
            Account penAccount = buildAccount(ACCOUNT_ID);
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID))
                    .thenReturn(Optional.of(penAccount));
            when(accountRepository.findById(ACCOUNT_ID))
                    .thenReturn(Optional.of(penAccount));
            when(categoryRepository.findByIdAndUser(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.of(buildCategory(CATEGORY_ID)));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createTransaction(USER_ID, TransactionType.EXPENSE, AMOUNT, "PEN",
                    ACCOUNT_ID, null, null, CATEGORY_ID, "Merchant", null, TODAY, null, null);

            verify(accountService).adjustBalance(ACCOUNT_ID, AMOUNT.negate());
        }

        @Test
        @DisplayName("INCOME credits the account")
        void createIncomeCreditsAccount() {
            Account penAccount = buildAccount(ACCOUNT_ID);
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID))
                    .thenReturn(Optional.of(penAccount));
            when(accountRepository.findById(ACCOUNT_ID))
                    .thenReturn(Optional.of(penAccount));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createTransaction(USER_ID, TransactionType.INCOME, AMOUNT, "PEN",
                    ACCOUNT_ID, null, null, null, "Salary", null, TODAY, null, null);

            verify(accountService).adjustBalance(ACCOUNT_ID, AMOUNT);
        }

        @Test
        @DisplayName("TRANSFER debits fromAccount and credits toAccount")
        void createTransferDebitsFromCreditsTo() {
            when(accountRepository.findByIdAndUser(FROM_ACCOUNT_ID, USER_ID))
                    .thenReturn(Optional.of(buildAccount(FROM_ACCOUNT_ID)));
            when(accountRepository.findByIdAndUser(TO_ACCOUNT_ID, USER_ID))
                    .thenReturn(Optional.of(buildAccount(TO_ACCOUNT_ID)));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createTransaction(USER_ID, TransactionType.TRANSFER, AMOUNT, "PEN",
                    null, FROM_ACCOUNT_ID, TO_ACCOUNT_ID, null, null, null, TODAY, null, null);

            verify(accountService).adjustBalance(FROM_ACCOUNT_ID, AMOUNT.negate());
            verify(accountService).adjustBalance(TO_ACCOUNT_ID, AMOUNT);
        }

        @Test
        @DisplayName("TRANSFER to same account throws InvalidTransactionException")
        void createTransferSameAccountThrowsInvalidTransaction() {
            assertThatThrownBy(() -> service.createTransaction(USER_ID, TransactionType.TRANSFER, AMOUNT, "PEN",
                    null, ACCOUNT_ID, ACCOUNT_ID, null, null, null, TODAY, null, null))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("same account");
        }

        @Test
        @DisplayName("EXPENSE with unowned category throws CategoryNotFoundException")
        void createExpenseCategoryNotOwnedThrows404() {
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID))
                    .thenReturn(Optional.of(buildAccount(ACCOUNT_ID)));
            when(categoryRepository.findByIdAndUser(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createTransaction(USER_ID, TransactionType.EXPENSE, AMOUNT, "PEN",
                    ACCOUNT_ID, null, null, CATEGORY_ID, "Merchant", null, TODAY, null, null))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(accountService, never()).adjustBalance(any(), any());
        }

        @Test
        @DisplayName("EXPENSE without categoryId throws InvalidTransactionException")
        void createExpenseMissingCategoryThrowsInvalid() {
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID))
                    .thenReturn(Optional.of(buildAccount(ACCOUNT_ID)));

            assertThatThrownBy(() -> service.createTransaction(USER_ID, TransactionType.EXPENSE, AMOUNT, "PEN",
                    ACCOUNT_ID, null, null, null, "Merchant", null, TODAY, null, null))
                    .isInstanceOf(InvalidTransactionException.class);
        }

        @Test
        @DisplayName("USD EXPENSE on a PEN account applies sell rate and stores it on the transaction")
        void createExpenseCrossCurrencyAppliesSellRate() {
            Account penAccount = buildAccount(ACCOUNT_ID);
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID))
                    .thenReturn(Optional.of(penAccount));
            when(accountRepository.findById(ACCOUNT_ID))
                    .thenReturn(Optional.of(penAccount));
            when(categoryRepository.findByIdAndUser(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.of(buildCategory(CATEGORY_ID)));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ExchangeRate rate = buildExchangeRate(new BigDecimal("3.69"), new BigDecimal("3.74"));
            when(getTodayExchangeRateUseCase.getOrCreateDefault()).thenReturn(rate);

            // USD 100 at sell rate 3.74 = PEN 374.00
            service.createTransaction(USER_ID, TransactionType.EXPENSE, AMOUNT, "USD",
                    ACCOUNT_ID, null, null, CATEGORY_ID, "Amazon", null, TODAY, null, null);

            verify(accountService).adjustBalance(ACCOUNT_ID, new BigDecimal("374.00").negate());
        }

        @Test
        @DisplayName("USD EXPENSE with user-supplied amountLocal uses it directly without fetching exchange rate")
        void createExpenseUserSuppliedAmountLocalUsesItDirectly() {
            Account penAccount = buildAccount(ACCOUNT_ID);
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID))
                    .thenReturn(Optional.of(penAccount));
            when(accountRepository.findById(ACCOUNT_ID))
                    .thenReturn(Optional.of(penAccount));
            when(categoryRepository.findByIdAndUser(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.of(buildCategory(CATEGORY_ID)));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // User confirmed the bank charged PEN 376.50 for USD 100 (overriding the auto rate)
            BigDecimal userConfirmedLocal = new BigDecimal("376.50");
            service.createTransaction(USER_ID, TransactionType.EXPENSE, AMOUNT, "USD",
                    ACCOUNT_ID, null, null, CATEGORY_ID, "Amazon", null, TODAY, null, userConfirmedLocal);

            // Exchange rate use case must NOT be called -- user value takes precedence
            verify(getTodayExchangeRateUseCase, never()).getOrCreateDefault();
            verify(accountService).adjustBalance(ACCOUNT_ID, userConfirmedLocal.negate());
        }

        @Test
        @DisplayName("USD EXPENSE with user-supplied amountLocal stores derived exchangeRateApplied")
        void createExpenseUserSuppliedAmountLocalStoresComputedRate() {
            Account penAccount = buildAccount(ACCOUNT_ID);
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID))
                    .thenReturn(Optional.of(penAccount));
            when(accountRepository.findById(ACCOUNT_ID))
                    .thenReturn(Optional.of(penAccount));
            when(categoryRepository.findByIdAndUser(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.of(buildCategory(CATEGORY_ID)));

            // Capture the saved transaction to inspect its fields
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BigDecimal userConfirmedLocal = new BigDecimal("374.00");
            // USD 100 -> PEN 374.00; derived rate = 374.00 / 100 = 3.7400
            TransactionDetail result = service.createTransaction(
                    USER_ID, TransactionType.EXPENSE, AMOUNT, "USD",
                    ACCOUNT_ID, null, null, CATEGORY_ID, "Amazon", null, TODAY, null, userConfirmedLocal);

            assertThat(result.transaction().getAmountLocal()).isEqualByComparingTo(userConfirmedLocal);
            assertThat(result.transaction().getExchangeRateApplied())
                    .isEqualByComparingTo(new BigDecimal("3.7400"));
        }
    }

    // ------------------------------------------------------------------
    // updateTransaction
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("updateTransaction")
    class UpdateTransaction {

        @Test
        @DisplayName("reverses old balance effect and applies new one")
        void updateTransactionReversesOldBalanceAppliesNew() {
            Transaction existing = buildTransaction(TX_ID, TransactionType.EXPENSE,
                    ACCOUNT_ID, null, null, CATEGORY_ID, AMOUNT);
            UUID newAccountId = UUID.randomUUID();
            BigDecimal newAmount = BigDecimal.valueOf(200);
            Account newAccount = buildAccount(newAccountId);

            when(transactionRepository.findByIdAndUser(TX_ID, USER_ID)).thenReturn(Optional.of(existing));
            when(accountRepository.findByIdAndUser(newAccountId, USER_ID))
                    .thenReturn(Optional.of(newAccount));
            when(accountRepository.findById(newAccountId))
                    .thenReturn(Optional.of(newAccount));
            when(categoryRepository.findByIdAndUser(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.of(buildCategory(CATEGORY_ID)));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateTransaction(USER_ID, TX_ID, newAmount, "PEN",
                    newAccountId, null, null, CATEGORY_ID, "Updated", null, TODAY, null, null);

            // Reversal of old EXPENSE (no stored rate, same currency): credit old account by old amount
            verify(accountService).adjustBalance(ACCOUNT_ID, AMOUNT);
            // Apply new EXPENSE: debit new account by new amount
            verify(accountService).adjustBalance(newAccountId, newAmount.negate());
        }
    }

    // ------------------------------------------------------------------
    // deleteTransaction
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("deleteTransaction")
    class DeleteTransaction {

        @Test
        @DisplayName("soft-deletes and reverses the balance effect")
        void deleteTransactionReversesBalance() {
            Transaction existing = buildTransaction(TX_ID, TransactionType.EXPENSE,
                    ACCOUNT_ID, null, null, CATEGORY_ID, AMOUNT);
            when(transactionRepository.findByIdAndUser(TX_ID, USER_ID)).thenReturn(Optional.of(existing));

            service.deleteTransaction(USER_ID, TX_ID);

            verify(transactionRepository).softDelete(eq(TX_ID), any(Instant.class));
            // Reversal of EXPENSE: credit back the account
            verify(accountService).adjustBalance(ACCOUNT_ID, AMOUNT);
        }

        @Test
        @DisplayName("reverses TRANSFER balance on delete")
        void deleteTransferReversesFromAndTo() {
            Transaction existing = buildTransaction(TX_ID, TransactionType.TRANSFER,
                    null, FROM_ACCOUNT_ID, TO_ACCOUNT_ID, null, AMOUNT);
            when(transactionRepository.findByIdAndUser(TX_ID, USER_ID)).thenReturn(Optional.of(existing));

            service.deleteTransaction(USER_ID, TX_ID);

            verify(transactionRepository).softDelete(eq(TX_ID), any(Instant.class));
            verify(accountService).adjustBalance(FROM_ACCOUNT_ID, AMOUNT);
            verify(accountService).adjustBalance(TO_ACCOUNT_ID, AMOUNT.negate());
        }

        @Test
        @DisplayName("reversal of cross-currency EXPENSE uses stored amountLocal directly")
        void deleteExpenseCrossCurrencyUsesStoredAmountLocal() {
            BigDecimal storedLocal = new BigDecimal("376.50");
            BigDecimal storedRate  = new BigDecimal("3.7650");
            Transaction existing = buildUsdTransaction(TX_ID, TransactionType.EXPENSE,
                    ACCOUNT_ID, AMOUNT, storedRate, storedLocal);
            when(transactionRepository.findByIdAndUser(TX_ID, USER_ID)).thenReturn(Optional.of(existing));

            service.deleteTransaction(USER_ID, TX_ID);

            // Reversal must credit exactly the stored amountLocal, not amount * storedRate
            verify(accountService).adjustBalance(ACCOUNT_ID, storedLocal);
        }

        @Test
        @DisplayName("reversal falls back to storedRate multiplication when amountLocal is absent (legacy row)")
        void deleteExpenseLegacyRowUsesStoredRateFallback() {
            BigDecimal storedRate = new BigDecimal("3.74");
            // amountLocal is null -- simulates a row created before this column existed
            Transaction existing = buildUsdTransaction(TX_ID, TransactionType.EXPENSE,
                    ACCOUNT_ID, AMOUNT, storedRate, null);
            when(transactionRepository.findByIdAndUser(TX_ID, USER_ID)).thenReturn(Optional.of(existing));

            service.deleteTransaction(USER_ID, TX_ID);

            // USD 100 * 3.74 = PEN 374.00 credited back
            verify(accountService).adjustBalance(ACCOUNT_ID, new BigDecimal("374.00"));
        }
    }

    // ------------------------------------------------------------------
    // getTransaction
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("getTransaction")
    class GetTransaction {

        @Test
        @DisplayName("throws TransactionNotFoundException when not owned by user")
        void getTransactionNotOwnerThrows404() {
            when(transactionRepository.findByIdAndUser(TX_ID, USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTransaction(USER_ID, TX_ID))
                    .isInstanceOf(TransactionNotFoundException.class);
        }
    }
}
