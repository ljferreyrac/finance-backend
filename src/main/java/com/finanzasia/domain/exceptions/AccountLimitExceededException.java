package com.finanzasia.domain.exceptions;

/**
 * Thrown when a user attempts to create more accounts than the configured maximum.
 */
public class AccountLimitExceededException extends RuntimeException {

    private final int limit;

    public AccountLimitExceededException(int limit) {
        super("Account limit of " + limit + " per user has been reached");
        this.limit = limit;
    }

    public int getLimit() {
        return limit;
    }
}
