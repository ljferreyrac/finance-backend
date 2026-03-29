package com.finanzasia.domain.exceptions;

public class DuplicateTagException extends RuntimeException {

    private final String name;

    public DuplicateTagException(String name) {
        super("A tag with that name already exists.");
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
