package com.finanzasia.domain.exceptions;

public class CategoryLimitExceededException extends RuntimeException {

    private final int limit;

    public CategoryLimitExceededException(int limit) {
        super("Category limit of " + limit + " per user has been reached");
        this.limit = limit;
    }

    public int getLimit() {
        return limit;
    }
}
