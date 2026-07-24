package com.finanzasia.domain.exceptions;

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
