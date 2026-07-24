package com.finanzasia.domain.model;

/**
 * A transaction with its account and category references already resolved.
 *
 * <p>Exists so the service, which owns the repositories, does the lookup once
 * per page rather than the web layer loading every account and category to
 * decorate the response itself.
 *
 * <p>Every reference is nullable because which ones apply depends on the type:
 * expenses and income carry {@code account} and usually {@code category},
 * transfers carry {@code fromAccount} and {@code toAccount} instead.
 */
public record TransactionDetail(
        Transaction transaction,
        Account account,
        Account fromAccount,
        Account toAccount,
        Category category) {
}
