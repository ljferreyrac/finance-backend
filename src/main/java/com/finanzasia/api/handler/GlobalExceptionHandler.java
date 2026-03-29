package com.finanzasia.api.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.finanzasia.domain.exceptions.AIExtractionException;
import com.finanzasia.domain.exceptions.AccountInUseException;
import com.finanzasia.domain.exceptions.InvalidExchangeRateException;
import com.finanzasia.domain.exceptions.AccountLimitExceededException;
import com.finanzasia.domain.exceptions.AccountNotFoundException;
import com.finanzasia.domain.exceptions.CategoryInUseException;
import com.finanzasia.domain.exceptions.CategoryLimitExceededException;
import com.finanzasia.domain.exceptions.CategoryNotFoundException;
import com.finanzasia.domain.exceptions.DuplicateCategoryNameException;
import com.finanzasia.domain.exceptions.DuplicateTagException;
import com.finanzasia.domain.exceptions.InvalidCredentialsException;
import com.finanzasia.domain.exceptions.InvalidReportParameterException;
import com.finanzasia.domain.exceptions.InvalidTokenException;
import com.finanzasia.domain.exceptions.InvalidTransactionException;
import com.finanzasia.domain.exceptions.LastCategoryException;
import com.finanzasia.domain.exceptions.TagNotFoundException;
import com.finanzasia.domain.exceptions.TransactionNotFoundException;
import com.finanzasia.domain.exceptions.UnauthorizedAccessException;
import com.finanzasia.domain.exceptions.UserAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Translates domain exceptions into RFC 7807 Problem Detail responses.
 * Stack traces and internal error details are never exposed to callers
 * (reinforced by {@code server.error.include-stacktrace=never} in application.properties).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("User Already Exists");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setTitle("Invalid Credentials");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidToken(InvalidTokenException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setTitle("Invalid Token");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(AccountNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Account Not Found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(AccountInUseException.class)
    public ProblemDetail handleAccountInUse(AccountInUseException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Account In Use");
        pd.setDetail(ex.getMessage());
        pd.setProperty("transactionCount", ex.getTransactionCount());
        return pd;
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ProblemDetail handleTransactionNotFound(TransactionNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Transaction Not Found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ProblemDetail handleUnauthorizedAccess(UnauthorizedAccessException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setTitle("Forbidden");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(AccountLimitExceededException.class)
    public ProblemDetail handleAccountLimitExceeded(AccountLimitExceededException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        pd.setTitle("Account Limit Reached");
        pd.setDetail("No puedes crear mas de " + ex.getLimit() + " cuentas.");
        pd.setProperty("limit", ex.getLimit());
        return pd;
    }

    @ExceptionHandler(InvalidExchangeRateException.class)
    public ProblemDetail handleInvalidExchangeRate(InvalidExchangeRateException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Invalid Exchange Rate");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ProblemDetail handleInvalidTransaction(InvalidTransactionException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Invalid Transaction");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(InvalidReportParameterException.class)
    public ProblemDetail handleInvalidReportParameter(InvalidReportParameterException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Invalid Report Parameter");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ProblemDetail handleCategoryNotFound(CategoryNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Category Not Found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(CategoryInUseException.class)
    public ProblemDetail handleCategoryInUse(CategoryInUseException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Category In Use");
        pd.setDetail(ex.getMessage());
        pd.setProperty("expenseCount", ex.getExpenseCount());
        return pd;
    }

    @ExceptionHandler(LastCategoryException.class)
    public ProblemDetail handleLastCategory(LastCategoryException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Last Category");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(CategoryLimitExceededException.class)
    public ProblemDetail handleCategoryLimitExceeded(CategoryLimitExceededException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        pd.setTitle("Category Limit Reached");
        pd.setDetail("No puedes crear mas de " + ex.getLimit() + " categorias.");
        pd.setProperty("limit", ex.getLimit());
        return pd;
    }

    @ExceptionHandler(DuplicateCategoryNameException.class)
    public ProblemDetail handleDuplicateCategoryName(DuplicateCategoryNameException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Duplicate Category Name");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(TagNotFoundException.class)
    public ProblemDetail handleTagNotFound(TagNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Tag Not Found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(DuplicateTagException.class)
    public ProblemDetail handleDuplicateTag(DuplicateTagException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Duplicate Tag Name");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(AIExtractionException.class)
    public ProblemDetail handleAIExtraction(AIExtractionException ex) {
        log.error("AI extraction failed: {}", ex.getMessage(), ex);
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        pd.setTitle("AI Extraction Failed");
        pd.setDetail("The AI service could not process your request. Please try again.");
        return pd;
    }

    /**
     * Catch-all for any unhandled runtime exception.
     * Returns a generic 500 so the real HTTP status reaches the client instead
     * of being swallowed by Spring Boot's /error forwarding mechanism.
     * Internal error details are never exposed to callers.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Internal Server Error");
        pd.setDetail("An unexpected error occurred. Please try again later.");
        return pd;
    }

    /**
     * Handles bean-validation failures and returns each field error as a
     * structured map so clients can display per-field messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid value",
                        (first, second) -> first));

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation Failed");
        pd.setDetail("One or more fields failed validation.");
        pd.setProperty("errors", fieldErrors);
        return pd;
    }
}
