package com.finanzasia.application.service;

import com.finanzasia.domain.model.Account;
import com.finanzasia.domain.model.AccountType;
import com.finanzasia.domain.model.Category;
import com.finanzasia.domain.model.TransactionDraft;
import com.finanzasia.domain.model.TransactionType;
import com.finanzasia.domain.port.out.AIExtractionPort;
import com.finanzasia.domain.port.out.AIExtractionPort.AccountContext;
import com.finanzasia.domain.port.out.AIExtractionPort.AITransactionRaw;
import com.finanzasia.domain.port.out.AIExtractionPort.CategoryContext;
import com.finanzasia.domain.port.out.AccountRepository;
import com.finanzasia.domain.port.out.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExtractTransactionsServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AIExtractionPort aiExtractionPort;

    private ExtractTransactionsService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ExtractTransactionsService(categoryRepository, accountRepository, aiExtractionPort);
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private Category buildCategory(UUID id, String name) {
        return new Category(id, USER_ID, name, "#FF5733", "food", false, 1, Instant.now(), Instant.now());
    }

    private Account buildAccount(UUID id, String name, String bank, String currency) {
        return new Account(id, USER_ID, name, AccountType.BANK, bank, currency,
                BigDecimal.ZERO, null, null, null, "#0000FF", false, true, null,
                Instant.now(), Instant.now());
    }

    private AITransactionRaw rawExpense(UUID categoryId, UUID accountId, String date) {
        return new AITransactionRaw(
                "EXPENSE",
                new BigDecimal("25.50"),
                "PEN",
                categoryId != null ? categoryId.toString() : null,
                accountId != null ? accountId.toString() : null,
                "Wong",
                "Weekly groceries",
                date,
                0.92);
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("extract - happy path")
    class HappyPath {

        @Test
        @DisplayName("maps raw AI result to TransactionDraft with resolved names")
        void mapsRawResultToTypedDraft() {
            Category food = buildCategory(CATEGORY_ID, "Food");
            Account bcp = buildAccount(ACCOUNT_ID, "BCP Cuenta", "BCP", "PEN");

            when(categoryRepository.findAllByUser(USER_ID)).thenReturn(List.of(food));
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of(bcp));
            when(aiExtractionPort.extractFromText(any(), anyList(), anyList()))
                    .thenReturn(List.of(rawExpense(CATEGORY_ID, ACCOUNT_ID, "2026-03-28")));

            List<TransactionDraft> result = service.extract(USER_ID, "Gaste 25.50 soles en Wong");

            assertThat(result).hasSize(1);
            TransactionDraft draft = result.get(0);
            assertThat(draft.type()).isEqualTo(TransactionType.EXPENSE);
            assertThat(draft.amount()).isEqualByComparingTo("25.50");
            assertThat(draft.currency()).isEqualTo("PEN");
            assertThat(draft.categoryId()).isEqualTo(CATEGORY_ID);
            assertThat(draft.categoryName()).isEqualTo("Food");
            assertThat(draft.accountId()).isEqualTo(ACCOUNT_ID);
            assertThat(draft.accountName()).isEqualTo("BCP Cuenta");
            assertThat(draft.merchant()).isEqualTo("Wong");
            assertThat(draft.transactionDate()).isEqualTo(LocalDate.of(2026, 3, 28));
            assertThat(draft.confidence()).isEqualTo(0.92);
        }

        @Test
        @DisplayName("passes user categories and accounts as context to the AI port")
        void passesContextToAIPort() {
            Category food = buildCategory(CATEGORY_ID, "Food");
            Account bcp = buildAccount(ACCOUNT_ID, "BCP", "BCP", "PEN");

            when(categoryRepository.findAllByUser(USER_ID)).thenReturn(List.of(food));
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of(bcp));
            when(aiExtractionPort.extractFromText(any(), anyList(), anyList()))
                    .thenReturn(List.of());

            service.extract(USER_ID, "some text");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<CategoryContext>> catCaptor = ArgumentCaptor.forClass(List.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<AccountContext>> accCaptor = ArgumentCaptor.forClass(List.class);

            verify(aiExtractionPort).extractFromText(eq("some text"), catCaptor.capture(), accCaptor.capture());

            assertThat(catCaptor.getValue()).hasSize(1);
            assertThat(catCaptor.getValue().get(0).id()).isEqualTo(CATEGORY_ID);
            assertThat(catCaptor.getValue().get(0).name()).isEqualTo("Food");

            assertThat(accCaptor.getValue()).hasSize(1);
            assertThat(accCaptor.getValue().get(0).id()).isEqualTo(ACCOUNT_ID);
            assertThat(accCaptor.getValue().get(0).name()).isEqualTo("BCP");
        }

        @Test
        @DisplayName("returns empty list when AI detects no transactions")
        void returnsEmptyListWhenNoTransactions() {
            when(categoryRepository.findAllByUser(USER_ID)).thenReturn(List.of());
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of());
            when(aiExtractionPort.extractFromText(any(), anyList(), anyList())).thenReturn(List.of());

            List<TransactionDraft> result = service.extract(USER_ID, "Hola como estas");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("handles multiple drafts in one call")
        void handlesMultipleDrafts() {
            Category food = buildCategory(CATEGORY_ID, "Food");
            Account bcp = buildAccount(ACCOUNT_ID, "BCP", "BCP", "PEN");

            when(categoryRepository.findAllByUser(USER_ID)).thenReturn(List.of(food));
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of(bcp));

            AITransactionRaw raw1 = rawExpense(CATEGORY_ID, ACCOUNT_ID, "2026-03-28");
            AITransactionRaw raw2 = new AITransactionRaw(
                    "INCOME", new BigDecimal("1000.00"), "PEN",
                    null, ACCOUNT_ID.toString(),
                    null, "Salary", "2026-03-25", 0.98);

            when(aiExtractionPort.extractFromText(any(), anyList(), anyList()))
                    .thenReturn(List.of(raw1, raw2));

            List<TransactionDraft> result = service.extract(USER_ID, "some multi-transaction text");

            assertThat(result).hasSize(2);
            assertThat(result.get(0).type()).isEqualTo(TransactionType.EXPENSE);
            assertThat(result.get(1).type()).isEqualTo(TransactionType.INCOME);
        }
    }

    @Nested
    @DisplayName("extract - type parsing edge cases")
    class TypeParsing {

        @Test
        @DisplayName("defaults type to EXPENSE when AI returns unknown string")
        void defaultsToExpenseForUnknownType() {
            when(categoryRepository.findAllByUser(USER_ID)).thenReturn(List.of());
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of());

            AITransactionRaw raw = new AITransactionRaw(
                    "GIBBERISH", new BigDecimal("10.00"), "PEN",
                    null, null, null, null, "2026-03-28", 0.5);

            when(aiExtractionPort.extractFromText(any(), anyList(), anyList()))
                    .thenReturn(List.of(raw));

            List<TransactionDraft> result = service.extract(USER_ID, "text");

            assertThat(result.get(0).type()).isEqualTo(TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("defaults type to EXPENSE when AI returns null type")
        void defaultsToExpenseForNullType() {
            when(categoryRepository.findAllByUser(USER_ID)).thenReturn(List.of());
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of());

            AITransactionRaw raw = new AITransactionRaw(
                    null, new BigDecimal("10.00"), "PEN",
                    null, null, null, null, "2026-03-28", 0.5);

            when(aiExtractionPort.extractFromText(any(), anyList(), anyList()))
                    .thenReturn(List.of(raw));

            assertThat(service.extract(USER_ID, "text").get(0).type())
                    .isEqualTo(TransactionType.EXPENSE);
        }
    }

    @Nested
    @DisplayName("extract - date parsing edge cases")
    class DateParsing {

        @Test
        @DisplayName("falls back to today when AI returns null date")
        void fallsBackToTodayForNullDate() {
            when(categoryRepository.findAllByUser(USER_ID)).thenReturn(List.of());
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of());

            AITransactionRaw raw = new AITransactionRaw(
                    "EXPENSE", new BigDecimal("5.00"), "PEN",
                    null, null, null, null, null, 0.4);

            when(aiExtractionPort.extractFromText(any(), anyList(), anyList()))
                    .thenReturn(List.of(raw));

            TransactionDraft draft = service.extract(USER_ID, "text").get(0);

            assertThat(draft.transactionDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("falls back to today when AI returns invalid date format")
        void fallsBackToTodayForBadDate() {
            when(categoryRepository.findAllByUser(USER_ID)).thenReturn(List.of());
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of());

            AITransactionRaw raw = new AITransactionRaw(
                    "EXPENSE", new BigDecimal("5.00"), "PEN",
                    null, null, null, null, "not-a-date", 0.4);

            when(aiExtractionPort.extractFromText(any(), anyList(), anyList()))
                    .thenReturn(List.of(raw));

            assertThat(service.extract(USER_ID, "text").get(0).transactionDate())
                    .isEqualTo(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("extract - ID resolution edge cases")
    class IdResolution {

        @Test
        @DisplayName("keeps categoryId and accountId as null when AI returns null")
        void nullIdsRemainNull() {
            when(categoryRepository.findAllByUser(USER_ID)).thenReturn(List.of());
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of());

            AITransactionRaw raw = new AITransactionRaw(
                    "EXPENSE", new BigDecimal("50.00"), "USD",
                    null, null, "Amazon", null, "2026-03-28", 0.85);

            when(aiExtractionPort.extractFromText(any(), anyList(), anyList()))
                    .thenReturn(List.of(raw));

            TransactionDraft draft = service.extract(USER_ID, "text").get(0);

            assertThat(draft.categoryId()).isNull();
            assertThat(draft.accountId()).isNull();
        }

        @Test
        @DisplayName("treats malformed UUID string as null ID")
        void malformedUuidBecomesNull() {
            when(categoryRepository.findAllByUser(USER_ID)).thenReturn(List.of());
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of());

            AITransactionRaw raw = new AITransactionRaw(
                    "EXPENSE", new BigDecimal("10.00"), "PEN",
                    "not-a-uuid", "also-not-a-uuid", null, null, "2026-03-28", 0.3);

            when(aiExtractionPort.extractFromText(any(), anyList(), anyList()))
                    .thenReturn(List.of(raw));

            TransactionDraft draft = service.extract(USER_ID, "text").get(0);

            assertThat(draft.categoryId()).isNull();
            assertThat(draft.accountId()).isNull();
        }

        @Test
        @DisplayName("resolves category name when AI returns a valid UUID present in context")
        void resolvesCategoryNameFromContext() {
            Category transport = buildCategory(CATEGORY_ID, "Transport");
            when(categoryRepository.findAllByUser(USER_ID)).thenReturn(List.of(transport));
            when(accountRepository.findAllByUser(USER_ID)).thenReturn(List.of());

            AITransactionRaw raw = new AITransactionRaw(
                    "EXPENSE", new BigDecimal("8.00"), "PEN",
                    CATEGORY_ID.toString(), null, "Uber", null, "2026-03-28", 0.9);

            when(aiExtractionPort.extractFromText(any(), anyList(), anyList()))
                    .thenReturn(List.of(raw));

            TransactionDraft draft = service.extract(USER_ID, "text").get(0);

            assertThat(draft.categoryId()).isEqualTo(CATEGORY_ID);
            assertThat(draft.categoryName()).isEqualTo("Transport");
        }
    }
}
