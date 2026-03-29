package com.finanzasia.domain.exceptions;

import java.util.UUID;

public class CategoryInUseException extends RuntimeException {

    private final UUID categoryId;
    private final long expenseCount;

    public CategoryInUseException(UUID categoryId, long expenseCount) {
        super("Category " + categoryId + " is still referenced by " + expenseCount + " expense(s). "
                + "Provide a reassignTo category to migrate the expenses before deleting.");
        this.categoryId = categoryId;
        this.expenseCount = expenseCount;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public long getExpenseCount() {
        return expenseCount;
    }
}
