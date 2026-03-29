package com.finanzasia.domain.exceptions;

/**
 * Thrown when email or password cannot be verified during login.
 * The message is intentionally generic to avoid leaking whether
 * the email exists or the password was wrong.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password.");
    }
}
