package com.finanzasia.domain.model;

import java.math.BigDecimal;

/**
 * An account together with the figures a client needs alongside it.
 *
 * <p>Exists so the transaction count is resolved by the service that owns the
 * repository, rather than by the web layer decorating an {@link Account} after
 * the fact.
 *
 * @param transactionCount how many transactions reference this account; drives
 *                         whether the client offers deletion
 * @param availableCredit  {@code creditLimit - |currentBalance|} for credit
 *                         cards with a limit set, otherwise null
 */
public record AccountDetail(
        Account account,
        long transactionCount,
        BigDecimal availableCredit) {
}
