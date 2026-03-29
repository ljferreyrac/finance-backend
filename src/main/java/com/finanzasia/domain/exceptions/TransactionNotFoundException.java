package com.finanzasia.domain.exceptions;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {

    private final UUID transactionId;

    public TransactionNotFoundException(UUID transactionId) {
        super("Transaction not found: " + transactionId);
        this.transactionId = transactionId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }
}
