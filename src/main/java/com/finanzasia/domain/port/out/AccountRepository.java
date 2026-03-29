package com.finanzasia.domain.port.out;

import com.finanzasia.domain.model.Account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for account persistence.
 * Implementations live in the infrastructure layer.
 */
public interface AccountRepository {

    List<Account> findAllByUser(UUID userId);

    Optional<Account> findByIdAndUser(UUID id, UUID userId);

    /** Returns true when the account has at least one non-deleted transaction. */
    boolean hasTransactions(UUID accountId);

    long countByUser(UUID userId);

    long countTransactionsByAccountId(UUID accountId);

    Account save(Account account);

    void delete(UUID accountId);

    void clearDefaultForUser(UUID userId);

    /**
     * Loads an account by its primary key without an ownership check.
     * For internal use by AccountService.adjustBalance only.
     * All external callers must use {@link #findByIdAndUser(UUID, UUID)}.
     */
    Optional<Account> findById(UUID id);
}
