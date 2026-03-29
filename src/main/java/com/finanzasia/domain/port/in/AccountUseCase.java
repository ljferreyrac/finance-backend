package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.Account;
import com.finanzasia.domain.model.AccountType;
import com.finanzasia.domain.model.NetWorth;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Input port for all account management operations.
 * Implemented by the application service; invoked by the REST controller.
 */
public interface AccountUseCase {

    List<Account> listAccounts(UUID userId);

    NetWorth getNetWorth(UUID userId);

    Account createAccount(
            UUID userId,
            String name,
            AccountType type,
            String bank,
            String currency,
            BigDecimal initialBalance,
            BigDecimal creditLimit,
            Integer closingDay,
            Integer dueDay,
            String color,
            boolean isDefault,
            UUID linkedAccountId);

    Account updateAccount(
            UUID userId,
            UUID accountId,
            String name,
            String bank,
            BigDecimal creditLimit,
            Integer closingDay,
            Integer dueDay,
            String color,
            boolean isActive,
            UUID linkedAccountId);

    void deleteAccount(UUID userId, UUID accountId);

    Account setDefaultAccount(UUID userId, UUID accountId);
}
