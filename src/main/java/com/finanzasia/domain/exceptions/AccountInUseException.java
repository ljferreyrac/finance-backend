package com.finanzasia.domain.exceptions;

import java.util.UUID;

public class AccountInUseException extends RuntimeException {

    private final UUID accountId;
    private final long transactionCount;

    public AccountInUseException(UUID accountId, long transactionCount) {
        super("Account " + accountId + " still has " + transactionCount
                + " transaction(s). Delete or reassign them before removing the account.");
        this.accountId = accountId;
        this.transactionCount = transactionCount;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public long getTransactionCount() {
        return transactionCount;
    }
}
