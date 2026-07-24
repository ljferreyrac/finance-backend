package com.finanzasia.domain.exceptions;

/**
 * Message is intentionally generic to avoid leaking whether the email exists or the password was wrong.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password.");
    }
}
