package com.finanzasia.application.service;

import com.finanzasia.domain.exceptions.AccountInUseException;
import com.finanzasia.domain.exceptions.AccountNotFoundException;
import com.finanzasia.domain.model.Account;
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
import static org.mockito.ArgumentMatchers.eq;
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
        void listAccounts_returnsUserAccounts() {
            Account a = buildAccount(ACCOUNT_ID, "PEN", BigDecimal.valueOf(100), false);
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of(a));

            List<Account> result = service.listAccounts(USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(ACCOUNT_ID);
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
        void createAccount_isDefault_clearsExisting() {
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createAccount(USER_ID, "Main", AccountType.BANK,
                    "BCP", "PEN", BigDecimal.ZERO, null, null, null, null, true, null);

            verify(accountRepository).clearDefaultForUser(USER_ID);
        }

        @Test
        @DisplayName("does not clear default when isDefault=false")
        void createAccount_notDefault_doesNotClearExisting() {
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createAccount(USER_ID, "Secondary", AccountType.BANK,
                    null, "PEN", BigDecimal.ZERO, null, null, null, null, false, null);

            verify(accountRepository, never()).clearDefaultForUser(any());
        }

        @Test
        @DisplayName("sets currentBalance to initialBalance")
        void createAccount_withInitialBalance() {
            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            when(accountRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.createAccount(USER_ID, "Savings", AccountType.BANK,
                    null, "PEN", BigDecimal.valueOf(500), null, null, null, null, false, null);

            assertThat(captor.getValue().getCurrentBalance())
                    .isEqualByComparingTo(BigDecimal.valueOf(500));
        }

        @Test
        @DisplayName("defaults balance to zero when initialBalance is null")
        void createAccount_nullInitialBalance_defaultsToZero() {
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
        void updateAccount_notOwner_throws404() {
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAccount(USER_ID, ACCOUNT_ID,
                    "New Name", null, null, null, null, null, true, null))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("updates only provided fields")
        void updateAccount_partialUpdate() {
            Account existing = buildAccount(ACCOUNT_ID, "PEN", BigDecimal.valueOf(200), false);
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(existing));
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Account result = service.updateAccount(USER_ID, ACCOUNT_ID,
                    "Updated Name", "BBVA", null, null, null, null, true, null);

            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getBank()).isEqualTo("BBVA");
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
        void deleteAccount_withTransactions_throws409() {
            Account account = buildAccount(ACCOUNT_ID, "PEN", BigDecimal.ZERO, false);
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
            when(accountRepository.countTransactionsByAccountId(ACCOUNT_ID)).thenReturn(3L);

            assertThatThrownBy(() -> service.deleteAccount(USER_ID, ACCOUNT_ID))
                    .isInstanceOf(AccountInUseException.class)
                    .satisfies(ex -> assertThat(((AccountInUseException) ex).getTransactionCount()).isEqualTo(3L));
        }

        @Test
        @DisplayName("deletes account when it has no transactions")
        void deleteAccount_noTransactions_success() {
            Account account = buildAccount(ACCOUNT_ID, "PEN", BigDecimal.ZERO, false);
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
            when(accountRepository.countTransactionsByAccountId(ACCOUNT_ID)).thenReturn(0L);

            service.deleteAccount(USER_ID, ACCOUNT_ID);

            verify(accountRepository).delete(ACCOUNT_ID);
        }

        @Test
        @DisplayName("throws AccountNotFoundException when account does not belong to user")
        void deleteAccount_notOwner_throws404() {
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
        void setDefaultAccount_clearsOldDefault() {
            Account account = buildAccount(ACCOUNT_ID, "PEN", BigDecimal.ZERO, false);
            when(accountRepository.findByIdAndUser(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Account result = service.setDefaultAccount(USER_ID, ACCOUNT_ID);

            verify(accountRepository).clearDefaultForUser(USER_ID);
            assertThat(result.isDefault()).isTrue();
        }

        @Test
        @DisplayName("throws AccountNotFoundException when account not found")
        void setDefaultAccount_notFound_throws() {
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
        void getNetWorth_sumsByCurrency() {
            Account pen1 = buildAccount(UUID.randomUUID(), "PEN", BigDecimal.valueOf(1000), false);
            Account pen2 = buildAccount(UUID.randomUUID(), "PEN", BigDecimal.valueOf(500), false);
            Account usd1 = buildAccount(UUID.randomUUID(), "USD", BigDecimal.valueOf(200), false);
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of(pen1, pen2, usd1));

            NetWorth result = service.getNetWorth(USER_ID);

            assertThat(result.totalPEN()).isEqualByComparingTo(BigDecimal.valueOf(1500));
            assertThat(result.totalUSD()).isEqualByComparingTo(BigDecimal.valueOf(200));
            assertThat(result.accounts()).hasSize(3);
        }

        @Test
        @DisplayName("returns zero totals when user has no accounts")
        void getNetWorth_noAccounts_returnsZero() {
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of());

            NetWorth result = service.getNetWorth(USER_ID);

            assertThat(result.totalPEN()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.totalUSD()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
