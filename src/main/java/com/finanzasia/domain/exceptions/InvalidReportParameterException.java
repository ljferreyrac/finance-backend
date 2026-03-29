package com.finanzasia.domain.exceptions;

/**
 * Thrown when a report request carries an invalid parameter:
 * unsupported currency, month outside 1-12, or year outside a
 * reasonable range.
 */
public class InvalidReportParameterException extends RuntimeException {

    public InvalidReportParameterException(String message) {
        super(message);
    }
}
