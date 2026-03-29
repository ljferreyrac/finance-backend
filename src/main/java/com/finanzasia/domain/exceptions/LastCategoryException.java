package com.finanzasia.domain.exceptions;

public class LastCategoryException extends RuntimeException {

    public LastCategoryException() {
        super("Cannot delete the last category. At least one category must remain.");
    }
}
