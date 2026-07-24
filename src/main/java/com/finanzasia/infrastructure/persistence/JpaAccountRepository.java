package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.Account;
import com.finanzasia.domain.port.out.AccountRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JpaAccountRepository implements AccountRepository {

    private final JpaAccountRepositoryPort jpaPort;

    public JpaAccountRepository(JpaAccountRepositoryPort jpaPort) {
        this.jpaPort = jpaPort;
    }

    @Override
    public List<Account> findAllByUser(UUID userId) {
        return jpaPort.findByUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .map(AccountEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Account> findByIdAndUser(UUID id, UUID userId) {
        return jpaPort.findByIdAndUserId(id, userId).map(AccountEntity::toDomain);
    }

    @Override
    public boolean hasTransactions(UUID accountId) {
        return jpaPort.countTransactionsByAccountId(accountId) > 0;
    }

    @Override
    public long countByUser(UUID userId) {
        return jpaPort.countByUserId(userId);
    }

    @Override
    public long countTransactionsByAccountId(UUID accountId) {
        return jpaPort.countTransactionsByAccountId(accountId);
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = AccountEntity.fromDomain(account);
        return jpaPort.save(entity).toDomain();
    }

    @Override
    public void delete(UUID accountId) {
        jpaPort.deleteById(accountId);
    }

    @Override
    public void clearDefaultForUser(UUID userId) {
        jpaPort.clearDefaultForUser(userId);
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jpaPort.findById(id).map(AccountEntity::toDomain);
    }
}
