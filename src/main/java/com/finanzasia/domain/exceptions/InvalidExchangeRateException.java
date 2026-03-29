package com.finanzasia.domain.exceptions;

/**
 * Thrown when an exchange rate update violates business invariants,
 * for example when the sell rate is lower than the buy rate.
 */
public class InvalidExchangeRateException extends RuntimeException {

    public InvalidExchangeRateException(String message) {
        super(message);
    }
}
