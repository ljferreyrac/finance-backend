package com.finanzasia.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Only active accounts are included; totals are kept separate per currency.
 */
public record NetWorth(
        BigDecimal totalPEN,
        BigDecimal totalUSD,
        List<Account> accounts
) {}
