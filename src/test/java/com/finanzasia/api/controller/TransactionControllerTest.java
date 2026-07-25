package com.finanzasia.api.controller;

import com.finanzasia.api.security.UserPrincipal;
import com.finanzasia.domain.exceptions.AccountNotFoundException;
import com.finanzasia.domain.exceptions.TransactionNotFoundException;
import com.finanzasia.domain.model.Account;
import com.finanzasia.domain.model.AccountType;
import com.finanzasia.domain.model.Category;
import com.finanzasia.domain.model.Transaction;
import com.finanzasia.domain.model.TransactionCursor;
import com.finanzasia.domain.model.TransactionDetail;
import com.finanzasia.domain.model.TransactionDetailPage;
import com.finanzasia.domain.model.TransactionFilter;
import com.finanzasia.domain.model.TransactionType;
import com.finanzasia.domain.port.in.AuthenticateAccessTokenUseCase;
import com.finanzasia.domain.port.in.TransactionUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionUseCase transactionUseCase;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessToken;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    private UserPrincipal principal;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TX_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(USER_ID, "user@example.com");
    }

    private TransactionDetail buildDetail() {
        Instant now = Instant.now();
        Transaction tx = new Transaction(TX_ID, USER_ID, TransactionType.EXPENSE, new BigDecimal("50.00"), "PEN",
                ACCOUNT_ID, null, null, CATEGORY_ID, "Wong", "groceries", null,
                LocalDate.now(), now, now, null);
        Account account = new Account(ACCOUNT_ID, USER_ID, "BCP Soles", AccountType.BANK, "BCP", "PEN",
                new BigDecimal("500.00"), null, null, null, "#0066CC", false, true, null, now, now);
        Category category = new Category(CATEGORY_ID, USER_ID, "Comida", "#FF0000", "food", false, 0, now, now);
        return new TransactionDetail(tx, account, null, null, category);
    }

    @Nested
    @DisplayName("GET /api/v1/transactions")
    class ListTransactions {

        @Test
        @DisplayName("returns a page of transactions with no cursor supplied")
        void listReturnsPage() throws Exception {
            when(transactionUseCase.listTransactions(any()))
                    .thenReturn(new TransactionDetailPage(List.of(buildDetail()), null, false));

            mockMvc.perform(get("/api/v1/transactions").with(user(principal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].id").value(TX_ID.toString()))
                    .andExpect(jsonPath("$.hasMore").value(false))
                    .andExpect(jsonPath("$.totalCount").value(1));
        }

        @Test
        @DisplayName("GET /api/v1/transactions without authentication is rejected with 401")
        void listWithoutAuthIsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/transactions")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("a limit above the maximum is clamped to 100")
        void limitAboveMaxIsClamped() throws Exception {
            when(transactionUseCase.listTransactions(any()))
                    .thenReturn(new TransactionDetailPage(List.of(), null, false));
            ArgumentCaptor<TransactionFilter> captor = ArgumentCaptor.forClass(TransactionFilter.class);

            mockMvc.perform(get("/api/v1/transactions").param("limit", "500").with(user(principal)))
                    .andExpect(status().isOk());

            verify(transactionUseCase).listTransactions(captor.capture());
            assertThat(captor.getValue().limit()).isEqualTo(100);
        }

        @Test
        @DisplayName("a limit below 1 is clamped up to 1")
        void limitBelowMinIsClampedUp() throws Exception {
            when(transactionUseCase.listTransactions(any()))
                    .thenReturn(new TransactionDetailPage(List.of(), null, false));
            ArgumentCaptor<TransactionFilter> captor = ArgumentCaptor.forClass(TransactionFilter.class);

            mockMvc.perform(get("/api/v1/transactions").param("limit", "0").with(user(principal)))
                    .andExpect(status().isOk());

            verify(transactionUseCase).listTransactions(captor.capture());
            assertThat(captor.getValue().limit()).isEqualTo(1);
        }

        @Test
        @DisplayName("a valid cursor is decoded into cursorDate and cursorId on the filter")
        void validCursorIsDecoded() throws Exception {
            LocalDate date = LocalDate.of(2026, 3, 20);
            UUID cursorId = UUID.randomUUID();
            String cursor = TransactionCursor.encode(date, cursorId);
            when(transactionUseCase.listTransactions(any()))
                    .thenReturn(new TransactionDetailPage(List.of(), null, false));
            ArgumentCaptor<TransactionFilter> captor = ArgumentCaptor.forClass(TransactionFilter.class);

            mockMvc.perform(get("/api/v1/transactions").param("cursor", cursor).with(user(principal)))
                    .andExpect(status().isOk());

            verify(transactionUseCase).listTransactions(captor.capture());
            assertThat(captor.getValue().cursorDate()).isEqualTo(date);
            assertThat(captor.getValue().cursorId()).isEqualTo(cursorId);
        }

        @Test
        @DisplayName("a malformed cursor decodes to null and is treated as the first page")
        void malformedCursorDecodesToNull() throws Exception {
            when(transactionUseCase.listTransactions(any()))
                    .thenReturn(new TransactionDetailPage(List.of(), null, false));
            ArgumentCaptor<TransactionFilter> captor = ArgumentCaptor.forClass(TransactionFilter.class);

            mockMvc.perform(get("/api/v1/transactions").param("cursor", "not-valid-base64!!!").with(user(principal)))
                    .andExpect(status().isOk());

            verify(transactionUseCase).listTransactions(captor.capture());
            assertThat(captor.getValue().cursorDate()).isNull();
            assertThat(captor.getValue().cursorId()).isNull();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transactions")
    class CreateTransaction {

        @Test
        @DisplayName("creates a transaction and returns 201")
        void createReturns201() throws Exception {
            when(transactionUseCase.createTransaction(any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any()))
                    .thenReturn(buildDetail());

            mockMvc.perform(post("/api/v1/transactions")
                            .with(user(principal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\":\"EXPENSE\",\"amount\":50.00,\"currency\":\"PEN\","
                                    + "\"accountId\":\"" + ACCOUNT_ID + "\",\"categoryId\":\"" + CATEGORY_ID + "\","
                                    + "\"transactionDate\":\"2026-03-28\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(TX_ID.toString()));
        }

        @Test
        @DisplayName("a missing required field fails bean validation with 400")
        void missingRequiredFieldReturns400() throws Exception {
            mockMvc.perform(post("/api/v1/transactions")
                            .with(user(principal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currency\":\"PEN\",\"transactionDate\":\"2026-03-28\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("an unknown account surfaces as 404 via GlobalExceptionHandler")
        void unknownAccountReturns404() throws Exception {
            when(transactionUseCase.createTransaction(any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any()))
                    .thenThrow(new AccountNotFoundException(ACCOUNT_ID));

            mockMvc.perform(post("/api/v1/transactions")
                            .with(user(principal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\":\"EXPENSE\",\"amount\":50.00,\"currency\":\"PEN\","
                                    + "\"accountId\":\"" + ACCOUNT_ID + "\",\"categoryId\":\"" + CATEGORY_ID + "\","
                                    + "\"transactionDate\":\"2026-03-28\"}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transactions/{id}")
    class GetTransaction {

        @Test
        @DisplayName("returns 200 with the transaction")
        void getReturns200() throws Exception {
            when(transactionUseCase.getTransaction(USER_ID, TX_ID)).thenReturn(buildDetail());

            mockMvc.perform(get("/api/v1/transactions/" + TX_ID).with(user(principal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TX_ID.toString()));
        }

        @Test
        @DisplayName("an unknown transaction surfaces as 404")
        void unknownTransactionReturns404() throws Exception {
            when(transactionUseCase.getTransaction(any(), any())).thenThrow(new TransactionNotFoundException(TX_ID));

            mockMvc.perform(get("/api/v1/transactions/" + TX_ID).with(user(principal)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("PUT /api/v1/transactions/{id} updates a transaction and returns 200")
    void updateReturns200() throws Exception {
        when(transactionUseCase.updateTransaction(any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any()))
                .thenReturn(buildDetail());

        mockMvc.perform(put("/api/v1/transactions/" + TX_ID)
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchant\":\"Nuevo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TX_ID.toString()));
    }

    @Test
    @DisplayName("DELETE /api/v1/transactions/{id} deletes a transaction and returns 204")
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/transactions/" + TX_ID).with(user(principal)).with(csrf()))
                .andExpect(status().isNoContent());

        verify(transactionUseCase).deleteTransaction(USER_ID, TX_ID);
    }
}
