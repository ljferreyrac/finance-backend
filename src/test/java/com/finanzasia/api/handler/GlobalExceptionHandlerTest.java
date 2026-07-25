package com.finanzasia.api.handler;

import com.finanzasia.domain.exceptions.AIExtractionException;
import com.finanzasia.domain.exceptions.AccountInUseException;
import com.finanzasia.domain.exceptions.AccountLimitExceededException;
import com.finanzasia.domain.exceptions.AccountNotFoundException;
import com.finanzasia.domain.exceptions.CategoryInUseException;
import com.finanzasia.domain.exceptions.CategoryLimitExceededException;
import com.finanzasia.domain.exceptions.CategoryNotFoundException;
import com.finanzasia.domain.exceptions.DuplicateCategoryNameException;
import com.finanzasia.domain.exceptions.DuplicateTagException;
import com.finanzasia.domain.exceptions.InvalidCredentialsException;
import com.finanzasia.domain.exceptions.InvalidExchangeRateException;
import com.finanzasia.domain.exceptions.InvalidReportParameterException;
import com.finanzasia.domain.exceptions.InvalidTokenException;
import com.finanzasia.domain.exceptions.InvalidTransactionException;
import com.finanzasia.domain.exceptions.LastCategoryException;
import com.finanzasia.domain.exceptions.TagNotFoundException;
import com.finanzasia.domain.exceptions.TransactionNotFoundException;
import com.finanzasia.domain.exceptions.UnauthorizedAccessException;
import com.finanzasia.domain.exceptions.UserAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every {@code @ExceptionHandler} translates a domain exception into an RFC 7807
 * {@link ProblemDetail}. These tests pin the status code, title and detail for each
 * one, since a controller test suite ({@code @WebMvcTest}) does not exist yet and this
 * handler is otherwise invoked only through a live Spring MVC dispatch.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("simple domain exceptions")
    class SimpleMappings {

        @Test
        @DisplayName("UserAlreadyExistsException maps to 409 Conflict")
        void userAlreadyExists() {
            ProblemDetail pd = handler.handleUserAlreadyExists(new UserAlreadyExistsException("a@b.com"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getTitle()).isEqualTo("User Already Exists");
            assertThat(pd.getDetail()).isEqualTo("Registration failed.");
        }

        @Test
        @DisplayName("InvalidCredentialsException maps to 401 Unauthorized")
        void invalidCredentials() {
            ProblemDetail pd = handler.handleInvalidCredentials(new InvalidCredentialsException());

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(pd.getTitle()).isEqualTo("Invalid Credentials");
            assertThat(pd.getDetail()).isEqualTo("Invalid email or password.");
        }

        @Test
        @DisplayName("InvalidTokenException maps to 401 Unauthorized")
        void invalidToken() {
            ProblemDetail pd = handler.handleInvalidToken(new InvalidTokenException("expired"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(pd.getTitle()).isEqualTo("Invalid Token");
            assertThat(pd.getDetail()).isEqualTo("Token is invalid: expired");
        }

        @Test
        @DisplayName("AccountNotFoundException maps to 404 Not Found")
        void accountNotFound() {
            UUID accountId = UUID.randomUUID();
            ProblemDetail pd = handler.handleAccountNotFound(new AccountNotFoundException(accountId));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(pd.getTitle()).isEqualTo("Account Not Found");
            assertThat(pd.getDetail()).contains(accountId.toString());
        }

        @Test
        @DisplayName("TransactionNotFoundException maps to 404 Not Found")
        void transactionNotFound() {
            UUID txId = UUID.randomUUID();
            ProblemDetail pd = handler.handleTransactionNotFound(new TransactionNotFoundException(txId));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(pd.getTitle()).isEqualTo("Transaction Not Found");
            assertThat(pd.getDetail()).contains(txId.toString());
        }

        @Test
        @DisplayName("UnauthorizedAccessException maps to 403 Forbidden")
        void unauthorizedAccess() {
            ProblemDetail pd = handler.handleUnauthorizedAccess(new UnauthorizedAccessException("not yours"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
            assertThat(pd.getTitle()).isEqualTo("Forbidden");
            assertThat(pd.getDetail()).isEqualTo("not yours");
        }

        @Test
        @DisplayName("InvalidExchangeRateException maps to 400 Bad Request")
        void invalidExchangeRate() {
            ProblemDetail pd =
                    handler.handleInvalidExchangeRate(new InvalidExchangeRateException("sell below buy"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(pd.getTitle()).isEqualTo("Invalid Exchange Rate");
            assertThat(pd.getDetail()).isEqualTo("sell below buy");
        }

        @Test
        @DisplayName("InvalidTransactionException maps to 400 Bad Request")
        void invalidTransaction() {
            ProblemDetail pd =
                    handler.handleInvalidTransaction(new InvalidTransactionException("amount must be positive"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(pd.getTitle()).isEqualTo("Invalid Transaction");
            assertThat(pd.getDetail()).isEqualTo("amount must be positive");
        }

        @Test
        @DisplayName("InvalidReportParameterException maps to 400 Bad Request")
        void invalidReportParameter() {
            ProblemDetail pd = handler.handleInvalidReportParameter(
                    new InvalidReportParameterException("month must be 1-12"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(pd.getTitle()).isEqualTo("Invalid Report Parameter");
            assertThat(pd.getDetail()).isEqualTo("month must be 1-12");
        }

        @Test
        @DisplayName("CategoryNotFoundException maps to 404 Not Found")
        void categoryNotFound() {
            UUID categoryId = UUID.randomUUID();
            ProblemDetail pd = handler.handleCategoryNotFound(new CategoryNotFoundException(categoryId));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(pd.getTitle()).isEqualTo("Category Not Found");
            assertThat(pd.getDetail()).contains(categoryId.toString());
        }

        @Test
        @DisplayName("LastCategoryException maps to 409 Conflict")
        void lastCategory() {
            ProblemDetail pd = handler.handleLastCategory(new LastCategoryException());

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getTitle()).isEqualTo("Last Category");
            assertThat(pd.getDetail())
                    .isEqualTo("Cannot delete the last category. At least one category must remain.");
        }

        @Test
        @DisplayName("DuplicateCategoryNameException maps to 409 Conflict")
        void duplicateCategoryName() {
            ProblemDetail pd =
                    handler.handleDuplicateCategoryName(new DuplicateCategoryNameException("Comida"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getTitle()).isEqualTo("Duplicate Category Name");
            assertThat(pd.getDetail()).contains("Comida");
        }

        @Test
        @DisplayName("TagNotFoundException maps to 404 Not Found")
        void tagNotFound() {
            ProblemDetail pd = handler.handleTagNotFound(new TagNotFoundException(UUID.randomUUID()));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(pd.getTitle()).isEqualTo("Tag Not Found");
        }

        @Test
        @DisplayName("DuplicateTagException maps to 409 Conflict")
        void duplicateTag() {
            ProblemDetail pd = handler.handleDuplicateTag(new DuplicateTagException("viaje"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getTitle()).isEqualTo("Duplicate Tag Name");
            assertThat(pd.getDetail()).contains("viaje");
        }

        @Test
        @DisplayName("AIExtractionException maps to 502 Bad Gateway with a generic detail")
        void aiExtraction() {
            ProblemDetail pd = handler.handleAIExtraction(new AIExtractionException("raw stack trace detail"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
            assertThat(pd.getTitle()).isEqualTo("AI Extraction Failed");
            // The detail returned to the client must never be the raw exception message.
            assertThat(pd.getDetail()).doesNotContain("raw stack trace detail");
        }

        @Test
        @DisplayName("an unmapped exception maps to 500 with a generic detail, not the raw message")
        void unexpectedException() {
            ProblemDetail pd = handler.handleUnexpected(new RuntimeException("npe at line 42"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            assertThat(pd.getTitle()).isEqualTo("Internal Server Error");
            assertThat(pd.getDetail()).doesNotContain("npe at line 42");
        }
    }

    @Nested
    @DisplayName("exceptions that attach extra properties")
    class ExtraProperties {

        @Test
        @DisplayName("AccountInUseException attaches the transaction count")
        void accountInUse() {
            ProblemDetail pd = handler.handleAccountInUse(new AccountInUseException(UUID.randomUUID(), 4));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getTitle()).isEqualTo("Account In Use");
            assertThat(pd.getProperties()).containsEntry("transactionCount", 4L);
        }

        @Test
        @DisplayName("CategoryInUseException attaches the expense count")
        void categoryInUse() {
            ProblemDetail pd = handler.handleCategoryInUse(new CategoryInUseException(UUID.randomUUID(), 9));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getTitle()).isEqualTo("Category In Use");
            assertThat(pd.getProperties()).containsEntry("expenseCount", 9L);
        }

        @Test
        @DisplayName("AccountLimitExceededException reports the limit in Spanish and attaches it as a property")
        void accountLimitExceeded() {
            ProblemDetail pd = handler.handleAccountLimitExceeded(new AccountLimitExceededException(20));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
            assertThat(pd.getTitle()).isEqualTo("Account Limit Reached");
            assertThat(pd.getDetail()).isEqualTo("No puedes crear mas de 20 cuentas.");
            assertThat(pd.getProperties()).containsEntry("limit", 20);
        }

        @Test
        @DisplayName("CategoryLimitExceededException reports the limit in Spanish and attaches it as a property")
        void categoryLimitExceeded() {
            ProblemDetail pd = handler.handleCategoryLimitExceeded(new CategoryLimitExceededException(15));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
            assertThat(pd.getTitle()).isEqualTo("Category Limit Reached");
            assertThat(pd.getDetail()).isEqualTo("No puedes crear mas de 15 categorias.");
            assertThat(pd.getProperties()).containsEntry("limit", 15);
        }
    }

    @Nested
    @DisplayName("handleValidationErrors")
    class ValidationErrors {

        // Any method with at least one parameter works: MethodArgumentNotValidException
        // only needs a MethodParameter to build, it never actually invokes the method.
        @SuppressWarnings("unused")
        private void dummyTarget(String field) {
        }

        private MethodArgumentNotValidException buildException(FieldError... fieldErrors) throws NoSuchMethodException {
            Method method = getClass().getDeclaredMethod("dummyTarget", String.class);
            MethodParameter methodParameter = new MethodParameter(method, 0);
            BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
            for (FieldError fieldError : fieldErrors) {
                bindingResult.addError(fieldError);
            }
            return new MethodArgumentNotValidException(methodParameter, bindingResult);
        }

        @Test
        @DisplayName("maps to 400 with each field error's default message")
        void mapsFieldErrorsToMessages() throws NoSuchMethodException {
            MethodArgumentNotValidException ex = buildException(
                    new FieldError("request", "email", "must not be blank"),
                    new FieldError("request", "amount", "must be at least 0.01"));

            ProblemDetail pd = handler.handleValidationErrors(ex);

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(pd.getTitle()).isEqualTo("Validation Failed");
            @SuppressWarnings("unchecked")
            Map<String, String> errors = (Map<String, String>) pd.getProperties().get("errors");
            assertThat(errors)
                    .containsEntry("email", "must not be blank")
                    .containsEntry("amount", "must be at least 0.01");
        }

        @Test
        @DisplayName("falls back to a generic message when the field error has no default message")
        void fallsBackWhenDefaultMessageIsNull() throws NoSuchMethodException {
            FieldError noMessage = new FieldError("request", "amount", null, false, null, null, null);
            MethodArgumentNotValidException ex = buildException(noMessage);

            ProblemDetail pd = handler.handleValidationErrors(ex);

            @SuppressWarnings("unchecked")
            Map<String, String> errors = (Map<String, String>) pd.getProperties().get("errors");
            assertThat(errors).containsEntry("amount", "invalid value");
        }

        @Test
        @DisplayName("keeps only the first error when two errors target the same field")
        void keepsFirstErrorOnDuplicateField() throws NoSuchMethodException {
            MethodArgumentNotValidException ex = buildException(
                    new FieldError("request", "amount", "first message"),
                    new FieldError("request", "amount", "second message"));

            ProblemDetail pd = handler.handleValidationErrors(ex);

            @SuppressWarnings("unchecked")
            Map<String, String> errors = (Map<String, String>) pd.getProperties().get("errors");
            assertThat(errors).containsEntry("amount", "first message");
        }
    }
}
