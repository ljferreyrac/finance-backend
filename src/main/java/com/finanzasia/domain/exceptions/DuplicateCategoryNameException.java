package com.finanzasia.domain.exceptions;

public class DuplicateCategoryNameException extends RuntimeException {

    private final String name;

    public DuplicateCategoryNameException(String name) {
        super("A category with the name \"" + name + "\" already exists for this user.");
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
