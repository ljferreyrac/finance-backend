package com.finanzasia.domain.exceptions;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String email) {
        super("Registration failed.");
    }
}
