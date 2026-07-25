package com.finanzasia.domain.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Each domain exception is a small value carrier with no branching logic;
 * these tests exist to pin the exact message text and, where present, the
 * getters that expose the identifiers used to build it.
 */
class DomainExceptionsTest {

    @Test
    @DisplayName("AIExtractionException carries a plain message")
    void aiExtractionExceptionMessageOnly() {
        AIExtractionException ex = new AIExtractionException("bad json");
        assertThat(ex.getMessage()).isEqualTo("bad json");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("AIExtractionException carries a message and cause")
    void aiExtractionExceptionMessageAndCause() {
        Throwable cause = new RuntimeException("network error");
        AIExtractionException ex = new AIExtractionException("gemini call failed", cause);
        assertThat(ex.getMessage()).isEqualTo("gemini call failed");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("AccountInUseException reports the account id and transaction count")
    void accountInUseException() {
        UUID accountId = UUID.randomUUID();
        AccountInUseException ex = new AccountInUseException(accountId, 3);

        assertThat(ex.getAccountId()).isEqualTo(accountId);
        assertThat(ex.getTransactionCount()).isEqualTo(3);
        assertThat(ex.getMessage()).contains(accountId.toString()).contains("3");
    }

    @Test
    @DisplayName("AccountLimitExceededException reports the configured limit")
    void accountLimitExceededException() {
        AccountLimitExceededException ex = new AccountLimitExceededException(20);

        assertThat(ex.getLimit()).isEqualTo(20);
        assertThat(ex.getMessage()).contains("20");
    }

    @Test
    @DisplayName("AccountNotFoundException reports the missing account id")
    void accountNotFoundException() {
        UUID accountId = UUID.randomUUID();
        AccountNotFoundException ex = new AccountNotFoundException(accountId);

        assertThat(ex.getAccountId()).isEqualTo(accountId);
        assertThat(ex.getMessage()).contains(accountId.toString());
    }

    @Test
    @DisplayName("CategoryInUseException reports the category id and expense count")
    void categoryInUseException() {
        UUID categoryId = UUID.randomUUID();
        CategoryInUseException ex = new CategoryInUseException(categoryId, 7);

        assertThat(ex.getCategoryId()).isEqualTo(categoryId);
        assertThat(ex.getExpenseCount()).isEqualTo(7);
        assertThat(ex.getMessage()).contains(categoryId.toString()).contains("7");
    }

    @Test
    @DisplayName("CategoryLimitExceededException reports the configured limit")
    void categoryLimitExceededException() {
        CategoryLimitExceededException ex = new CategoryLimitExceededException(15);

        assertThat(ex.getLimit()).isEqualTo(15);
        assertThat(ex.getMessage()).contains("15");
    }

    @Test
    @DisplayName("CategoryNotFoundException reports the missing category id")
    void categoryNotFoundException() {
        UUID categoryId = UUID.randomUUID();
        CategoryNotFoundException ex = new CategoryNotFoundException(categoryId);

        assertThat(ex.getCategoryId()).isEqualTo(categoryId);
        assertThat(ex.getMessage()).contains(categoryId.toString());
    }

    @Test
    @DisplayName("DuplicateCategoryNameException reports the offending name")
    void duplicateCategoryNameException() {
        DuplicateCategoryNameException ex = new DuplicateCategoryNameException("Comida");

        assertThat(ex.getName()).isEqualTo("Comida");
        assertThat(ex.getMessage()).contains("Comida");
    }

    @Test
    @DisplayName("DuplicateTagException reports the offending name")
    void duplicateTagException() {
        DuplicateTagException ex = new DuplicateTagException("viaje");

        assertThat(ex.getName()).isEqualTo("viaje");
        assertThat(ex.getMessage()).contains("viaje");
    }

    @Test
    @DisplayName("InvalidCredentialsException uses a fixed, generic message")
    void invalidCredentialsException() {
        InvalidCredentialsException ex = new InvalidCredentialsException();
        assertThat(ex.getMessage()).isEqualTo("Invalid email or password.");
    }

    @Test
    @DisplayName("InvalidExchangeRateException carries the caller-supplied message")
    void invalidExchangeRateException() {
        InvalidExchangeRateException ex = new InvalidExchangeRateException("sell rate below buy rate");
        assertThat(ex.getMessage()).isEqualTo("sell rate below buy rate");
    }

    @Test
    @DisplayName("InvalidReportParameterException carries the caller-supplied message")
    void invalidReportParameterException() {
        InvalidReportParameterException ex = new InvalidReportParameterException("month must be 1-12");
        assertThat(ex.getMessage()).isEqualTo("month must be 1-12");
    }

    @Test
    @DisplayName("InvalidTokenException prefixes the reason with a fixed message")
    void invalidTokenException() {
        InvalidTokenException ex = new InvalidTokenException("expired");
        assertThat(ex.getMessage()).isEqualTo("Token is invalid: expired");
    }

    @Test
    @DisplayName("InvalidTransactionException carries the caller-supplied message")
    void invalidTransactionException() {
        InvalidTransactionException ex = new InvalidTransactionException("amount must be positive");
        assertThat(ex.getMessage()).isEqualTo("amount must be positive");
    }

    @Test
    @DisplayName("LastCategoryException uses a fixed message")
    void lastCategoryException() {
        LastCategoryException ex = new LastCategoryException();
        assertThat(ex.getMessage()).isEqualTo("Cannot delete the last category. At least one category must remain.");
    }

    @Test
    @DisplayName("TagNotFoundException reports the missing tag id via getter, but not in the message")
    void tagNotFoundException() {
        UUID tagId = UUID.randomUUID();
        TagNotFoundException ex = new TagNotFoundException(tagId);

        assertThat(ex.getTagId()).isEqualTo(tagId);
        assertThat(ex.getMessage()).isEqualTo("Tag not found or does not belong to the current user.");
    }

    @Test
    @DisplayName("TransactionNotFoundException reports the missing transaction id")
    void transactionNotFoundException() {
        UUID transactionId = UUID.randomUUID();
        TransactionNotFoundException ex = new TransactionNotFoundException(transactionId);

        assertThat(ex.getTransactionId()).isEqualTo(transactionId);
        assertThat(ex.getMessage()).contains(transactionId.toString());
    }

    @Test
    @DisplayName("UnauthorizedAccessException carries the caller-supplied message")
    void unauthorizedAccessException() {
        UnauthorizedAccessException ex = new UnauthorizedAccessException("not your account");
        assertThat(ex.getMessage()).isEqualTo("not your account");
    }

    @Test
    @DisplayName("UserAlreadyExistsException never echoes the email back, to avoid user enumeration")
    void userAlreadyExistsExceptionDoesNotLeakEmail() {
        UserAlreadyExistsException ex = new UserAlreadyExistsException("victim@example.com");

        assertThat(ex.getMessage()).isEqualTo("Registration failed.");
        assertThat(ex.getMessage()).doesNotContain("victim@example.com");
    }
}
