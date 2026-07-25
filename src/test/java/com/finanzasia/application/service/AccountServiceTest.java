package com.finanzasia.application.service;

import com.finanzasia.domain.exceptions.AccountInUseException;
import com.finanzasia.domain.exceptions.AccountLimitExceededException;
import com.finanzasia.domain.exceptions.AccountNotFoundException;
import com.finanzasia.domain.model.Account;
import com.finanzasia.domain.model.AccountDetail;
import com.finanzasia.domain.model.AccountType;
import com.finanzasia.domain.model.NetWorth;
import com.finanzasia.domain.port.out.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AccountService(accountRepository);
    }

    private Account buildAccount(UUID id, String currency, BigDecimal balance, boolean isDefault) {
        Instant now = Instant.now();
        return new Account(id, USER_ID, "Test Account", AccountType.BANK,
                "BCP", currency, balance, null, null, null,
                "#FF0000", isDefault, true, null, now, now);
    }

    // ------------------------------------------------------------------
    // listAccounts
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("listAccounts")
    class ListAccounts {

        @Test
        @DisplayName("returns accounts owned by user")
        void listAccountsReturnsUserAccounts() {
            Account a = buildAccount(ACCOUNT_ID, "PEN", BigDecimal.valueOf(100), false);
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of(a));

            List<AccountDetail> result = service.listAccounts(USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).account().getId()).isEqualTo(ACCOUNT_ID);
        }
    }

    // ------------------------------------------------------------------
    // createAccount
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("createAccount")
    class CreateAccount {

        @Test
        @DisplayName("clears existing default when isDefault=true")
        void createAccountIsDefaultClearsExisting() {
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createAccount(USER_ID, "Main", AccountType.BANK,
                    "BCP", "PEN", BigDecimal.ZERO, null, null, null, null, true, null);

            verify(accountRepository).clearDefaultForUser(USER_ID);
        }

        @Test
        @DisplayName("does not clear default when isDefault=false")
        void createAccountNotDefaultDoesNotClearExisting() {
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createAccount(USER_ID, "Secondary", AccountType.BANK,
                    null, "PEN", BigDecimal.ZERO, null, null, null, null, false, null);

            verify(accountRepository, never()).clearDefaultForUser(any());
        }

        @Test
        @DisplayName("sets currentBalance to initialBalance")
        void createAccountWithInitialBalance() {
            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            when(accountRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.createAccount(USER_ID, "Savings", AccountType.BANK,
                    null, "PEN", BigDecimal.valueOf(500), null, null, null, null, false, null);

            assertThat(captor.getValue().getCurrentBalance())
                    .isEqualByComparingTo(BigDecimal.valueOf(500));
        }

        @Test
        @DisplayName("defaults balance to zero when initialBalance is null")
        void createAccountNullInitialBalanceDefaultsToZero() {
            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            when(accountRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.createAccount(USER_ID, "Cash", AccountType.CASH,
                    null, "PEN", null, null, null, null, null, false, null);

            assertThat(captor.getValue().getCurrentBalance())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ------------------------------------------------------------------
    // updateAccount
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("updateAccount")
    class UpdateAccount {

        @Test
        @DisplayName("throws AccountNotFoundException when not owned by user")
        void updateAccountNotOwnerThrows404() {
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAccount(USER_ID, ACCOUNT_ID,
                    "New Name", null, null, null, null, null, true, null))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("updates only provided fields")
        void updateAccountPartialUpdate() {
            Account existing = buildAccount(ACCOUNT_ID, "PEN", BigDecimal.valueOf(200), false);
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(existing));
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AccountDetail result = service.updateAccount(USER_ID, ACCOUNT_ID,
                    "Updated Name", "BBVA", null, null, null, null, true, null);

            assertThat(result.account().getName()).isEqualTo("Updated Name");
            assertThat(result.account().getBank()).isEqualTo("BBVA");
        }
    }

    // ------------------------------------------------------------------
    // deleteAccount
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccount {

        @Test
        @DisplayName("throws AccountInUseException when account has transactions")
        void deleteAccountWithTransactionsThrows409() {
            Account account = buildAccount(ACCOUNT_ID, "PEN", BigDecimal.ZERO, false);
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
            when(accountRepository.countTransactionsByAccountId(ACCOUNT_ID)).thenReturn(3L);

            assertThatThrownBy(() -> service.deleteAccount(USER_ID, ACCOUNT_ID))
                    .isInstanceOf(AccountInUseException.class)
                    .satisfies(ex -> assertThat(((AccountInUseException) ex).getTransactionCount()).isEqualTo(3L));
        }

        @Test
        @DisplayName("deletes account when it has no transactions")
        void deleteAccountNoTransactionsSuccess() {
            Account account = buildAccount(ACCOUNT_ID, "PEN", BigDecimal.ZERO, false);
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
            when(accountRepository.countTransactionsByAccountId(ACCOUNT_ID)).thenReturn(0L);

            service.deleteAccount(USER_ID, ACCOUNT_ID);

            verify(accountRepository).delete(ACCOUNT_ID);
        }

        @Test
        @DisplayName("throws AccountNotFoundException when account does not belong to user")
        void deleteAccountNotOwnerThrows404() {
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteAccount(USER_ID, ACCOUNT_ID))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------
    // setDefaultAccount
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("setDefaultAccount")
    class SetDefaultAccount {

        @Test
        @DisplayName("clears old default and marks new account as default")
        void setDefaultAccountClearsOldDefault() {
            Account account = buildAccount(ACCOUNT_ID, "PEN", BigDecimal.ZERO, false);
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AccountDetail result = service.setDefaultAccount(USER_ID, ACCOUNT_ID);

            verify(accountRepository).clearDefaultForUser(USER_ID);
            assertThat(result.account().isDefault()).isTrue();
        }

        @Test
        @DisplayName("throws AccountNotFoundException when account not found")
        void setDefaultAccountNotFoundThrows() {
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setDefaultAccount(USER_ID, ACCOUNT_ID))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------
    // getNetWorth
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("getNetWorth")
    class GetNetWorth {

        @Test
        @DisplayName("sums balances by currency across active accounts")
        void getNetWorthSumsByCurrency() {
            Account pen1 = buildAccount(UUID.randomUUID(), "PEN", BigDecimal.valueOf(1000), false);
            Account pen2 = buildAccount(UUID.randomUUID(), "PEN", BigDecimal.valueOf(500), false);
            Account usd1 = buildAccount(UUID.randomUUID(), "USD", BigDecimal.valueOf(200), false);
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of(pen1, pen2, usd1));

            NetWorth result = service.getNetWorth(USER_ID, true);

            assertThat(result.totalPEN()).isEqualByComparingTo(BigDecimal.valueOf(1500));
            assertThat(result.totalUSD()).isEqualByComparingTo(BigDecimal.valueOf(200));
            assertThat(result.accounts()).hasSize(3);
        }

        @Test
        @DisplayName("returns zero totals when user has no accounts")
        void getNetWorthNoAccountsReturnsZero() {
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of());

            NetWorth result = service.getNetWorth(USER_ID, true);

            assertThat(result.totalPEN()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.totalUSD()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("excludes inactive accounts from the totals")
        void getNetWorthSkipsInactiveAccounts() {
            Account active = buildAccount(ACCOUNT_ID, "PEN", BigDecimal.valueOf(100), false);
            Account inactive = new Account(UUID.randomUUID(), USER_ID, "Closed", AccountType.BANK,
                    "BCP", "PEN", BigDecimal.valueOf(999), null, null, null,
                    "#000000", false, false, null, Instant.now(), Instant.now());
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of(active, inactive));

            NetWorth result = service.getNetWorth(USER_ID, true);

            assertThat(result.totalPEN()).isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("subtracts the credit limit as debt when includeDebt is on")
        void getNetWorthCreditCardWithLimitSubtractsDebt() {
            Account card = creditCard(BigDecimal.valueOf(200), BigDecimal.valueOf(1000));
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of(card));

            NetWorth result = service.getNetWorth(USER_ID, true);

            // balance - limit = 200 - 1000
            assertThat(result.totalPEN()).isEqualByComparingTo(BigDecimal.valueOf(-800));
        }

        @Test
        @DisplayName("skips credit cards entirely when includeDebt is off")
        void getNetWorthExcludesCreditCardsWhenDebtOff() {
            Account card = creditCard(BigDecimal.valueOf(200), BigDecimal.valueOf(1000));
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of(card));

            NetWorth result = service.getNetWorth(USER_ID, false);

            assertThat(result.totalPEN()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("treats a credit card with no limit set as its plain balance")
        void getNetWorthCreditCardWithoutLimitUsesBalance() {
            Account card = creditCard(BigDecimal.valueOf(-300), null);
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of(card));

            NetWorth result = service.getNetWorth(USER_ID, true);

            // Without a limit there is no debt figure to derive, so the balance stands.
            assertThat(result.totalPEN()).isEqualByComparingTo(BigDecimal.valueOf(-300));
        }
    }

    // ------------------------------------------------------------------
    // availableCredit (exposed through AccountDetail)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("availableCredit")
    class AvailableCredit {

        @Test
        @DisplayName("is null for a non-credit account")
        void nullForNonCreditAccount() {
            Account bank = buildAccount(ACCOUNT_ID, "PEN", BigDecimal.valueOf(500), false);
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of(bank));

            List<AccountDetail> result = service.listAccounts(USER_ID);

            assertThat(result.get(0).availableCredit()).isNull();
        }

        @Test
        @DisplayName("is null for a credit card with no limit set")
        void nullForCreditCardWithoutLimit() {
            when(accountRepository.findAllByUser(USER_ID))
                    .thenReturn(List.of(creditCard(BigDecimal.valueOf(-200), null)));

            List<AccountDetail> result = service.listAccounts(USER_ID);

            assertThat(result.get(0).availableCredit()).isNull();
        }

        @Test
        @DisplayName("is limit minus the absolute outstanding balance")
        void limitMinusOutstanding() {
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(
                    List.of(creditCard(BigDecimal.valueOf(-300), BigDecimal.valueOf(1000))));

            List<AccountDetail> result = service.listAccounts(USER_ID);

            assertThat(result.get(0).availableCredit())
                    .isEqualByComparingTo(BigDecimal.valueOf(700));
        }

        @Test
        @DisplayName("uses the magnitude of the balance regardless of its sign")
        void usesAbsoluteValueOfBalance() {
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(
                    List.of(creditCard(BigDecimal.valueOf(300), BigDecimal.valueOf(1000))));

            List<AccountDetail> result = service.listAccounts(USER_ID);

            assertThat(result.get(0).availableCredit())
                    .isEqualByComparingTo(BigDecimal.valueOf(700));
        }
    }

    // ------------------------------------------------------------------
    // linked account validation
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("linked account validation")
    class LinkedAccountValidation {

        private final UUID linkedId = UUID.randomUUID();

        @Test
        @DisplayName("create rejects a linked account the user does not own")
        void createRejectsUnknownLinkedAccount() {
            when(accountRepository.countByUser(USER_ID)).thenReturn(1L);
            when(accountRepository.findByIdAndUser(linkedId, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createAccount(USER_ID, "Yape", AccountType.WALLET,
                    null, "PEN", BigDecimal.ZERO, null, null, null, "#FFF", false, linkedId))
                    .isInstanceOf(AccountNotFoundException.class);

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("create accepts a linked account the user owns")
        void createAcceptsOwnedLinkedAccount() {
            Account parent = buildAccount(linkedId, "PEN", BigDecimal.valueOf(100), false);
            when(accountRepository.countByUser(USER_ID)).thenReturn(1L);
            when(accountRepository.findByIdAndUser(linkedId, USER_ID))
                    .thenReturn(Optional.of(parent));
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AccountDetail result = service.createAccount(USER_ID, "Yape", AccountType.WALLET,
                    null, "PEN", BigDecimal.ZERO, null, null, null, "#FFF", false, linkedId);

            assertThat(result.account().getLinkedAccountId()).isEqualTo(linkedId);
        }

        @Test
        @DisplayName("update rejects a linked account the user does not own")
        void updateRejectsUnknownLinkedAccount() {
            Account existing = buildAccount(ACCOUNT_ID, "PEN", BigDecimal.valueOf(100), false);
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID))
                    .thenReturn(Optional.of(existing));
            when(accountRepository.findByIdAndUser(linkedId, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAccount(USER_ID, ACCOUNT_ID, "n", "BCP",
                    null, null, null, "#FFF", null, linkedId))
                    .isInstanceOf(AccountNotFoundException.class);

            verify(accountRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------
    // account limit
    // ------------------------------------------------------------------

    @Test
    @DisplayName("createAccount rejects once the per-user account limit is reached")
    void createAccountAtLimitThrows() {
        when(accountRepository.countByUser(USER_ID))
                .thenReturn((long) AccountService.MAX_ACCOUNTS_PER_USER);

        assertThatThrownBy(() -> service.createAccount(USER_ID, "One too many", AccountType.BANK,
                "BCP", "PEN", BigDecimal.ZERO, null, null, null, "#FFF", false, null))
                .isInstanceOf(AccountLimitExceededException.class);

        verify(accountRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // adjustBalance
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("adjustBalance")
    class AdjustBalance {

        @Test
        @DisplayName("credits the account when the delta is positive")
        void positiveDeltaCredits() {
            Account account = buildAccount(ACCOUNT_ID, "PEN", BigDecimal.valueOf(100), false);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.adjustBalance(ACCOUNT_ID, BigDecimal.valueOf(50));

            assertThat(account.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(150));
        }

        @Test
        @DisplayName("credits rather than debits on a zero delta")
        void zeroDeltaCredits() {
            Account account = buildAccount(ACCOUNT_ID, "PEN", BigDecimal.valueOf(100), false);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.adjustBalance(ACCOUNT_ID, BigDecimal.ZERO);

            assertThat(account.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("debits the absolute amount when the delta is negative")
        void negativeDeltaDebits() {
            Account account = buildAccount(ACCOUNT_ID, "PEN", BigDecimal.valueOf(100), false);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.adjustBalance(ACCOUNT_ID, BigDecimal.valueOf(-40));

            assertThat(account.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(60));
        }

        @Test
        @DisplayName("applies the adjustment to the parent when the account is linked")
        void linkedAccountDelegatesToParent() {
            UUID parentId = UUID.randomUUID();
            Account child = new Account(ACCOUNT_ID, USER_ID, "Yape", AccountType.WALLET,
                    null, "PEN", BigDecimal.valueOf(0), null, null, null,
                    "#FFF", false, true, parentId, Instant.now(), Instant.now());
            Account parent = buildAccount(parentId, "PEN", BigDecimal.valueOf(500), false);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(child));
            when(accountRepository.findById(parentId)).thenReturn(Optional.of(parent));
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.adjustBalance(ACCOUNT_ID, BigDecimal.valueOf(-100));

            assertThat(parent.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(400));
            assertThat(child.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            verify(accountRepository).save(parent);
        }

        @Test
        @DisplayName("throws when the account does not exist")
        void missingAccountThrows() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.adjustBalance(ACCOUNT_ID, BigDecimal.TEN))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("throws when the linked parent account is missing")
        void missingLinkedParentThrows() {
            UUID parentId = UUID.randomUUID();
            Account child = new Account(ACCOUNT_ID, USER_ID, "Yape", AccountType.WALLET,
                    null, "PEN", BigDecimal.ZERO, null, null, null,
                    "#FFF", false, true, parentId, Instant.now(), Instant.now());
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(child));
            when(accountRepository.findById(parentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.adjustBalance(ACCOUNT_ID, BigDecimal.TEN))
                    .isInstanceOf(AccountNotFoundException.class);

            verify(accountRepository, never()).save(any());
        }
    }

    private Account creditCard(BigDecimal balance, BigDecimal creditLimit) {
        Instant now = Instant.now();
        return new Account(ACCOUNT_ID, USER_ID, "Visa", AccountType.CREDIT_CARD,
                "BCP", "PEN", balance, creditLimit, 6, 26,
                "#FF0000", false, true, null, now, now);
    }
}
