package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.Account;
import com.finanzasia.domain.model.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaAccountRepositoryTest {

    @Mock
    private JpaAccountRepositoryPort jpaPort;

    private JpaAccountRepository repository;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = new JpaAccountRepository(jpaPort);
    }

    private Account buildAccount() {
        Instant now = Instant.now();
        return new Account(ACCOUNT_ID, USER_ID, "BCP Soles", AccountType.BANK, "BCP", "PEN",
                new BigDecimal("1000.00"), null, null, null, "#0066CC", false, true, null, now, now);
    }

    @Test
    @DisplayName("findAllByUser maps every returned entity to a domain Account")
    void findAllByUserMapsEntities() {
        AccountEntity entity = AccountEntity.fromDomain(buildAccount());
        when(jpaPort.findByUserIdOrderByCreatedAtAsc(USER_ID)).thenReturn(List.of(entity));

        List<Account> result = repository.findAllByUser(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(ACCOUNT_ID);
    }

    @Test
    @DisplayName("findByIdAndUser maps a present entity")
    void findByIdAndUserMapsPresentEntity() {
        AccountEntity entity = AccountEntity.fromDomain(buildAccount());
        when(jpaPort.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(entity));

        Optional<Account> result = repository.findByIdAndUser(ACCOUNT_ID, USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(ACCOUNT_ID);
    }

    @Test
    @DisplayName("findByIdAndUser returns empty when the port finds nothing")
    void findByIdAndUserReturnsEmptyWhenAbsent() {
        when(jpaPort.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThat(repository.findByIdAndUser(ACCOUNT_ID, USER_ID)).isEmpty();
    }

    @Test
    @DisplayName("hasTransactions is true when the transaction count is positive")
    void hasTransactionsTrueWhenCountPositive() {
        when(jpaPort.countTransactionsByAccountId(ACCOUNT_ID)).thenReturn(3L);

        assertThat(repository.hasTransactions(ACCOUNT_ID)).isTrue();
    }

    @Test
    @DisplayName("hasTransactions is false when the transaction count is zero")
    void hasTransactionsFalseWhenCountZero() {
        when(jpaPort.countTransactionsByAccountId(ACCOUNT_ID)).thenReturn(0L);

        assertThat(repository.hasTransactions(ACCOUNT_ID)).isFalse();
    }

    @Test
    @DisplayName("countByUser delegates directly to the port")
    void countByUserDelegates() {
        when(jpaPort.countByUserId(USER_ID)).thenReturn(5L);

        assertThat(repository.countByUser(USER_ID)).isEqualTo(5L);
    }

    @Test
    @DisplayName("countTransactionsByAccountId delegates directly to the port")
    void countTransactionsByAccountIdDelegates() {
        when(jpaPort.countTransactionsByAccountId(ACCOUNT_ID)).thenReturn(7L);

        assertThat(repository.countTransactionsByAccountId(ACCOUNT_ID)).isEqualTo(7L);
    }

    @Test
    @DisplayName("save converts the domain object to an entity and back")
    void saveConvertsToEntityAndBack() {
        Account account = buildAccount();
        AccountEntity savedEntity = AccountEntity.fromDomain(account);
        when(jpaPort.save(org.mockito.ArgumentMatchers.any(AccountEntity.class))).thenReturn(savedEntity);

        Account result = repository.save(account);

        assertThat(result.getId()).isEqualTo(account.getId());
        verify(jpaPort, times(1)).save(org.mockito.ArgumentMatchers.any(AccountEntity.class));
    }

    @Test
    @DisplayName("delete forwards to deleteById")
    void deleteForwardsToDeleteById() {
        repository.delete(ACCOUNT_ID);

        verify(jpaPort).deleteById(ACCOUNT_ID);
        verify(jpaPort, never()).deleteById(USER_ID);
    }

    @Test
    @DisplayName("clearDefaultForUser forwards to the port")
    void clearDefaultForUserForwards() {
        repository.clearDefaultForUser(USER_ID);

        verify(jpaPort).clearDefaultForUser(USER_ID);
    }

    @Test
    @DisplayName("findById maps a present entity, ignoring ownership")
    void findByIdMapsPresentEntity() {
        AccountEntity entity = AccountEntity.fromDomain(buildAccount());
        when(jpaPort.findById(ACCOUNT_ID)).thenReturn(Optional.of(entity));

        Optional<Account> result = repository.findById(ACCOUNT_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(ACCOUNT_ID);
    }
}
