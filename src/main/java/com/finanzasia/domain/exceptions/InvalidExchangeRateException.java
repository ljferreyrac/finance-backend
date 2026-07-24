package com.finanzasia.domain.exceptions;

/**
 * E.g. when the sell rate is lower than the buy rate.
 */
public class InvalidExchangeRateException extends RuntimeException {

    public InvalidExchangeRateException(String message) {
        super(message);
    }
}
