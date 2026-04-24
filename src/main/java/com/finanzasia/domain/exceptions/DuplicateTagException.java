package com.finanzasia.domain.exceptions;

public class DuplicateTagException extends RuntimeException {

    private final String name;

    public DuplicateTagException(String name) {
        super("A tag named '" + name + "' already exists.");
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
