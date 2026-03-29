package com.finanzasia.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable snapshot of a user's net worth across all active accounts,
 * with totals separated by currency.
 */
public record NetWorth(
        BigDecimal totalPEN,
        BigDecimal totalUSD,
        List<Account> accounts
) {}
