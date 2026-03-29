package com.finanzasia.domain.exceptions;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String reason) {
        super("Token is invalid: " + reason);
    }
}
